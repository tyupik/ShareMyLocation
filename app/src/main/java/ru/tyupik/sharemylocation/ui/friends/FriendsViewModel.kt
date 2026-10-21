package ru.tyupik.sharemylocation.ui.friends

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import ru.tyupik.sharemylocation.business.common.User
import ru.tyupik.sharemylocation.business.friends.FriendRequest
import ru.tyupik.sharemylocation.business.friends.FriendsError
import ru.tyupik.sharemylocation.business.friends.FriendsException
import ru.tyupik.sharemylocation.business.friends.FriendsInteractor
import ru.tyupik.sharemylocation.business.profile.PhoneNormalizer
import ru.tyupik.sharemylocation.business.profile.ProfileInteractor

/**
 * ViewModel вкладки «Друзья». Realtime-потоки друзей и заявок перезапускаются при каждом
 * ScreenShown (flatMapLatest на триггер); потоки независимы — ошибка одного показывает
 * inline-состояние своей секции, не ломая другую. Аватары дозагружаются по одному
 * через [ProfileInteractor.getAvatar] и кэшируются по «uid:версия», чтобы списки
 * не мигали плейсхолдером при обновлениях.
 * Единственная публичная точка входа для UI — [onIntent].
 */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class FriendsViewModel @Inject constructor(
    private val friendsInteractor: FriendsInteractor,
    private val profileInteractor: ProfileInteractor,
    private val friendsUiConverter: FriendsUiConverter,
) : ViewModel() {

    private val _uiState = MutableStateFlow(FriendsUiState())
    val uiState: StateFlow<FriendsUiState> = _uiState.asStateFlow()

    private val _effects = Channel<FriendsEffect>(Channel.BUFFERED)
    val effects: Flow<FriendsEffect> = _effects.receiveAsFlow()

    /**
     * Триггер перезапуска realtime-потоков: счётчик, инкрементируемый при каждом ScreenShown.
     * Именно счётчик, а не Unit: StateFlow дедуплицирует одинаковые значения, и повторный
     * ScreenShown не перезапустил бы потоки, завершившиеся по ошибке слушателя.
     */
    private val refreshTrigger = MutableStateFlow(0)

    /** Бизнес-заявки по fromUid: нужны для accept/decline, Ui-модель их не тянет. */
    private var incomingRequestsByUid: Map<String, FriendRequest> = emptyMap()

    /** Резолвленные пути аватаров по ключу «uid:версия»; null — аватара нет. */
    private val avatarPaths = mutableMapOf<String, String?>()

    init {
        observeStreams()
    }

    fun onIntent(intent: FriendsIntent) {
        when (intent) {
            FriendsIntent.ScreenShown -> onScreenShown()
            is FriendsIntent.SearchQueryChanged -> onSearchQueryChanged(intent.value)
            FriendsIntent.SearchClicked -> search()
            FriendsIntent.SearchCleared -> clearSearch()
            is FriendsIntent.AddFriendClicked -> addFriend(intent.uid)
            is FriendsIntent.AcceptRequestClicked -> acceptRequest(intent.fromUid)
            is FriendsIntent.DeclineRequestClicked -> declineRequest(intent.fromUid)
            is FriendsIntent.RemoveFriendClicked -> removeFriend(intent.uid)
            FriendsIntent.ErrorDismissed -> _uiState.update { it.copy(actionError = null) }
        }
    }

    /**
     * Перезапуск realtime-потоков трактуется как чистый лист: вместе с триггером сбрасываем
     * [FriendsUiState.actionError], оба флага пассивной недоступности секций и поиск
     * (вход на вкладку = чистый лист поиска: searchQuery, searchState, busyUid).
     * Списки friends/incomingRequests НЕ чистим — realtime-потоки доставят свежие данные.
     * Следствие (осознанно): guard `busyUid != null → return` после сброса не защищает от
     * повторного клика по действию, оставшемуся в полёте с предыдущего показа вкладки, —
     * вечный спиннер после logout хуже теоретического дубля, а серверные операции устойчивы.
     * Внутрисессионные action-ошибки это не затрагивает: они ставятся после ScreenShown
     * и живут до dismiss или следующего показа вкладки.
     */
    private fun onScreenShown() {
        refreshTrigger.value += 1
        _uiState.update {
            it.copy(
                actionError = null,
                isFriendsLoadFailed = false,
                isRequestsLoadFailed = false,
                searchQuery = "",
                searchState = FriendsSearchUiState.Idle,
                busyUid = null,
            )
        }
    }

    private fun observeStreams() {
        viewModelScope.launch {
            refreshTrigger
                .flatMapLatest {
                    friendsInteractor.observeFriends()
                        .map { users -> users.map(friendsUiConverter::convertFriend) }
                        .catch { throwable ->
                            // Пассивное падение секции — inline-состояние, не баннер действия.
                            Log.w(TAG, "Realtime-поток друзей прервался", throwable)
                            _uiState.update { state ->
                                state.copy(isFriendsLoading = false, isFriendsLoadFailed = true)
                            }
                        }
                }
                .collect { friends ->
                    _uiState.update { state ->
                        state.copy(
                            isFriendsLoading = false,
                            friends = friends.map { friend -> friend.withCachedAvatar() },
                        )
                    }
                    resolveFriendAvatars()
                }
        }
        viewModelScope.launch {
            refreshTrigger
                .flatMapLatest {
                    friendsInteractor.observeIncomingFriendRequests()
                        .onEach { requests ->
                            incomingRequestsByUid = requests.associateBy(FriendRequest::fromUid)
                        }
                        .map { requests -> requests.map(friendsUiConverter::convertRequest) }
                        .catch { throwable ->
                            // Пассивное падение секции — inline-состояние, не баннер действия.
                            Log.w(TAG, "Realtime-поток заявок прервался", throwable)
                            _uiState.update { state -> state.copy(isRequestsLoadFailed = true) }
                        }
                }
                .collect { requests ->
                    _uiState.update { state ->
                        state.copy(incomingRequests = requests.map { request -> request.withCachedAvatar() })
                    }
                    resolveRequestAvatars()
                }
        }
    }

    private fun onSearchQueryChanged(value: String) =
        _uiState.update { it.copy(searchQuery = value, searchState = FriendsSearchUiState.Idle) }

    private fun search() {
        val query = _uiState.value.searchQuery
        if (query.isBlank() || _uiState.value.searchState is FriendsSearchUiState.Loading) return
        // Валидация до похода в репозиторий: некорректный номер сразу показываем как ошибку поиска.
        if (!PhoneNormalizer.isValid(PhoneNormalizer.normalize(query))) {
            _uiState.update { it.copy(searchState = FriendsSearchUiState.Error(FriendsUiError.InvalidPhone)) }
            return
        }
        _uiState.update { it.copy(searchState = FriendsSearchUiState.Loading) }
        viewModelScope.launch {
            friendsInteractor.searchByPhone(query).fold(
                onSuccess = { user -> onSearchSuccess(user) },
                onFailure = { throwable -> onSearchFailure(throwable) },
            )
        }
    }

    private fun onSearchSuccess(user: User) {
        val uiModel = friendsUiConverter.convertFriend(user)
        _uiState.update { it.copy(searchState = FriendsSearchUiState.Found(uiModel)) }
        resolveAvatar(uiModel.uid, uiModel.avatarVersion) { path -> updateFoundAvatar(uiModel.uid, path) }
    }

    /** UserNotFound показывается как отдельное «не найдено», остальное — как ошибка поиска. */
    private fun onSearchFailure(throwable: Throwable) {
        Log.w(TAG, "Поиск по телефону не удался", throwable)
        val error = (throwable as? FriendsException)?.error
            ?: FriendsError.Unexpected(throwable.message)
        val searchState = when (error) {
            FriendsError.UserNotFound -> FriendsSearchUiState.NotFound
            else -> FriendsSearchUiState.Error(friendsUiConverter.convertError(error))
        }
        _uiState.update { it.copy(searchState = searchState) }
    }

    private fun clearSearch() = _uiState.update {
        it.copy(searchQuery = "", searchState = FriendsSearchUiState.Idle)
    }

    private fun addFriend(uid: String) {
        if (_uiState.value.busyUid != null) return
        _uiState.update { it.copy(busyUid = uid) }
        viewModelScope.launch {
            friendsInteractor.sendFriendRequest(uid).fold(
                onSuccess = {
                    _uiState.update {
                        it.copy(busyUid = null, searchState = FriendsSearchUiState.Idle)
                    }
                    _effects.send(FriendsEffect.FriendRequestSent)
                },
                onFailure = { throwable -> onActionError(throwable) },
            )
        }
    }

    private fun acceptRequest(fromUid: String) {
        val request = incomingRequestsByUid[fromUid] ?: return
        if (_uiState.value.busyUid != null) return
        _uiState.update { it.copy(busyUid = fromUid) }
        viewModelScope.launch {
            friendsInteractor.acceptFriendRequest(request).fold(
                onSuccess = { _uiState.update { it.copy(busyUid = null) } },
                onFailure = { throwable -> onActionError(throwable) },
            )
        }
    }

    private fun declineRequest(fromUid: String) {
        val request = incomingRequestsByUid[fromUid] ?: return
        if (_uiState.value.busyUid != null) return
        _uiState.update { it.copy(busyUid = fromUid) }
        viewModelScope.launch {
            friendsInteractor.declineFriendRequest(request).fold(
                onSuccess = { _uiState.update { it.copy(busyUid = null) } },
                onFailure = { throwable -> onActionError(throwable) },
            )
        }
    }

    private fun removeFriend(uid: String) {
        if (_uiState.value.busyUid != null) return
        _uiState.update { it.copy(busyUid = uid) }
        viewModelScope.launch {
            friendsInteractor.removeFriend(uid).fold(
                onSuccess = { _uiState.update { it.copy(busyUid = null) } },
                onFailure = { throwable -> onActionError(throwable) },
            )
        }
    }

    private fun onActionError(throwable: Throwable) {
        Log.w(TAG, "Действие с друзьями не удалось", throwable)
        _uiState.update { it.copy(busyUid = null, actionError = toUiError(throwable)) }
    }

    /** Ошибки, не завёрнутые в FriendsException, считаем Unexpected. */
    private fun toUiError(throwable: Throwable): FriendsUiError {
        val error = (throwable as? FriendsException)?.error
            ?: FriendsError.Unexpected(throwable.message)
        return friendsUiConverter.convertError(error)
    }

    /** Дозагрузка аватаров друзей из текущего состояния; ошибка аватара не показывается. */
    private fun resolveFriendAvatars() {
        _uiState.value.friends.forEach { friend ->
            resolveAvatar(friend.uid, friend.avatarVersion) { path ->
                _uiState.update { state ->
                    state.copy(
                        friends = state.friends.map { current ->
                            if (current.uid == friend.uid && current.avatarVersion == friend.avatarVersion) {
                                current.copy(avatarPath = path)
                            } else {
                                current
                            }
                        },
                    )
                }
            }
        }
    }

    /** Дозагрузка аватаров заявок из текущего состояния. */
    private fun resolveRequestAvatars() {
        _uiState.value.incomingRequests.forEach { request ->
            resolveAvatar(request.fromUid, request.fromAvatarVersion) { path ->
                _uiState.update { state ->
                    state.copy(
                        incomingRequests = state.incomingRequests.map { current ->
                            if (current.fromUid == request.fromUid &&
                                current.fromAvatarVersion == request.fromAvatarVersion
                            ) {
                                current.copy(avatarPath = path)
                            } else {
                                current
                            }
                        },
                    )
                }
            }
        }
    }

    private fun updateFoundAvatar(uid: String, path: String?) {
        _uiState.update { state ->
            val searchState = state.searchState
            if (searchState is FriendsSearchUiState.Found && searchState.user.uid == uid) {
                state.copy(searchState = searchState.copy(user = searchState.user.copy(avatarPath = path)))
            } else {
                state
            }
        }
    }

    /**
     * Резолвит аватар как в ProfileViewModel: ошибка остаётся плейсхолдером с инициалом.
     * Кэш по ключу «uid:версия» не даёт перезапускать загрузку при каждом обновлении списков;
     * при неудаче ключ освобождается для повторной попытки на следующей эмиссии.
     */
    private fun resolveAvatar(uid: String, version: Long, onResolved: (String?) -> Unit) {
        if (version <= 0L) return
        val key = avatarKey(uid, version)
        if (avatarPaths.containsKey(key)) {
            onResolved(avatarPaths[key])
            return
        }
        avatarPaths[key] = null
        viewModelScope.launch {
            profileInteractor.getAvatar(uid, version).fold(
                onSuccess = { avatar ->
                    avatarPaths[key] = avatar.cachedPath
                    onResolved(avatar.cachedPath)
                },
                onFailure = { throwable ->
                    Log.w(TAG, "Не удалось загрузить аватар", throwable)
                    avatarPaths.remove(key)
                },
            )
        }
    }

    /** Подставляет уже резолвленный путь аватара, чтобы список не мигал плейсхолдером. */
    private fun FriendUiModel.withCachedAvatar(): FriendUiModel {
        val key = avatarKey(uid, avatarVersion)
        if (!avatarPaths.containsKey(key)) return this
        return copy(avatarPath = avatarPaths[key])
    }

    private fun FriendRequestUiModel.withCachedAvatar(): FriendRequestUiModel {
        val key = avatarKey(fromUid, fromAvatarVersion)
        if (!avatarPaths.containsKey(key)) return this
        return copy(avatarPath = avatarPaths[key])
    }

    private companion object {
        const val TAG = "FriendsViewModel"

        /** Ключ кэша аватара: uid и версия, чтобы смена аватара перезагрузила файл. */
        fun avatarKey(uid: String, version: Long) = "${uid}:${version}"
    }
}
