package ru.tyupik.sharemylocation.ui.map

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
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import ru.tyupik.sharemylocation.business.friends.FriendsInteractor
import ru.tyupik.sharemylocation.business.map.MapInteractor
import ru.tyupik.sharemylocation.business.map.MyLocationProvider
import ru.tyupik.sharemylocation.business.profile.ProfileInteractor

/**
 * ViewModel вкладки «Карта». Realtime-поток видимых друзей перезапускается при каждом
 * ScreenShown (flatMapLatest на триггер); видимость друзей realtime-овая — интерактор
 * слушает настройки шаринга друзей по uid. Счётчик друзей — отдельный поток: его ошибка
 * глотается (как badge в MainViewModel), падает только поток карты.
 * Аватары дозагружаются по одному через [ProfileInteractor.getAvatar] и кэшируются
 * по «uid:версия», чтобы карта/список не мигали плейсхолдером при обновлениях.
 * Собственная локация — через [MyLocationProvider] (ViewModel без Context): флаг прав
 * только из фактических грантов, фиксы — для центровки камеры.
 * Единственная публичная точка входа для UI — [onIntent].
 */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class MapViewModel @Inject constructor(
    private val mapInteractor: MapInteractor,
    private val friendsInteractor: FriendsInteractor,
    private val profileInteractor: ProfileInteractor,
    private val myLocationProvider: MyLocationProvider,
    private val mapUiConverter: MapUiConverter,
) : ViewModel() {

    private val _uiState = MutableStateFlow(MapUiState())
    val uiState: StateFlow<MapUiState> = _uiState.asStateFlow()

    private val _effects = Channel<MapEffect>(Channel.BUFFERED)
    val effects: Flow<MapEffect> = _effects.receiveAsFlow()

    /**
     * Триггер перезапуска realtime-потоков: счётчик, инкрементируемый при каждом ScreenShown.
     * Именно счётчик, а не Unit: StateFlow дедуплицирует одинаковые значения, и повторный
     * ScreenShown не перезапустил бы потоки, завершившиеся по ошибке слушателя.
     */
    private val refreshTrigger = MutableStateFlow(0)

    /** Резолвленные пути аватаров по ключу «uid:версия»; null — аватара нет. */
    private val avatarPaths = mutableMapOf<String, String?>()

    /** Автозапрос прав отправляется ровно один раз за жизнь ViewModel; дальше — только кнопка. */
    private var hasRequestedPermissions = false

    /**
     * Отложенное желание центроваться: кнопка без прав НЕ центрирует — желание запоминается
     * до результата диалога прав (паттерн pendingEnableRequest из SharingSettingsViewModel).
     */
    private var pendingLocateRequest = false

    init {
        observeStreams()
    }

    fun onIntent(intent: MapIntent) {
        when (intent) {
            MapIntent.ScreenShown -> onScreenShown()
            is MapIntent.FriendSelected -> onFriendSelected(intent.uid)
            MapIntent.ShowListClicked -> _uiState.update { it.copy(isListVisible = true) }
            MapIntent.ListDismissed -> _uiState.update { it.copy(isListVisible = false) }
            MapIntent.LocateMeClicked -> onLocateMeClicked()
            is MapIntent.LocationPermissionsResult -> onLocationPermissionsResult(intent.granted)
        }
    }

    /**
     * Перезапуск realtime-потоков: вместе с триггером сбрасываем только [MapUiState.isMapLoadFailed]
     * (пассивное падение восстанавливается по Повторить/входу на вкладку). visibleFriends,
     * selectedUid и isListVisible не чистим: realtime-поток доставит свежие данные, а выбранного
     * друга и открытый список пользователь не должен терять при перечитке.
     */
    private fun onScreenShown() {
        refreshTrigger.value += 1
        _uiState.update { it.copy(isMapLoadFailed = false) }
        refreshLocationPermission()
    }

    /**
     * Проверка прав и одноразовый фикс. При правах получаем последний известный фикс
     * (myLocation обновится — камера отцентруется, если слежение не выбрано); без прав —
     * первый раз за жизнь ViewModel тихо запрашиваем их, после отказа — молчим.
     */
    private fun refreshLocationPermission() {
        val hasPermission = myLocationProvider.hasLocationPermission()
        _uiState.update { it.copy(hasLocationPermission = hasPermission) }
        if (hasPermission) {
            fetchMyLocation()
            return
        }
        if (hasRequestedPermissions) return
        hasRequestedPermissions = true
        viewModelScope.launch { _effects.send(MapEffect.RequestLocationPermissions) }
    }

    /** Toggle слежения камеры: повторный тап по выбранному другу снимает слежение. */
    private fun onFriendSelected(uid: String) {
        _uiState.update { state ->
            state.copy(selectedUid = if (state.selectedUid == uid) null else uid)
        }
    }

    /**
     * Кнопка центровки «Показать меня»: без прав — запрос прав и запоминание желания;
     * с правами — снятие слежения (иначе camera-эффект на ближайшем тике вернёт камеру
     * к другу) и центровка на свежий фикс.
     */
    private fun onLocateMeClicked() {
        if (!myLocationProvider.hasLocationPermission()) {
            pendingLocateRequest = true
            viewModelScope.launch { _effects.send(MapEffect.RequestLocationPermissions) }
            return
        }
        _uiState.update { it.copy(selectedUid = null) }
        fetchMyLocation()
    }

    /**
     * Результат диалога прав: отказ — флаг прав снимается, отложенное желание центроваться
     * отбрасывается (после отказа кнопка — единственная точка входа). Выдача — фикс и
     * центровка: если центровка была отложена с кнопки, слежение снимается (желание кнопки),
     * иначе центруемся только когда друга не выбрано.
     */
    private fun onLocationPermissionsResult(granted: Boolean) {
        val isPendingLocate = pendingLocateRequest
        pendingLocateRequest = false
        _uiState.update { state ->
            state.copy(
                hasLocationPermission = granted,
                selectedUid = if (granted && isPendingLocate) null else state.selectedUid,
            )
        }
        if (!granted) return
        fetchMyLocation()
    }

    /**
     * Одноразовый фикс для центровки: последний известный — мгновенно; его нет — one-shot
     * getCurrentLocation (задержка до секунд без индикатора — компромисс плана). null-фикс —
     * myLocation не трогаем, камеру не трогаем. При заданном selectedUid фикс всё равно
     * получаем (точка на карте обновится), но камера продолжает следить за другом.
     */
    private fun fetchMyLocation() {
        viewModelScope.launch {
            val fix = myLocationProvider.getLastKnownLocation()
                ?: myLocationProvider.getCurrentLocation()
                ?: return@launch
            _uiState.update { state ->
                state.copy(myLocation = mapUiConverter.convertMyLocation(fix, System.currentTimeMillis()))
            }
        }
    }

    private fun observeStreams() {
        viewModelScope.launch {
            refreshTrigger
                .flatMapLatest {
                    friendsInteractor.observeFriends()
                        .catch { throwable ->
                            // Ошибка счётчика глотается (подход badge MainViewModel):
                            // влияет только на текст пустого состояния.
                            Log.w(TAG, "Realtime-поток друзей (счётчик) прервался", throwable)
                        }
                }
                .collect { friends ->
                    _uiState.update { state -> state.copy(friendsCount = friends.size) }
                }
        }
        viewModelScope.launch {
            refreshTrigger
                .flatMapLatest {
                    mapInteractor.observeVisibleFriends()
                        .map { friends ->
                            val nowMillis = System.currentTimeMillis()
                            friends.map { friend -> mapUiConverter.convertVisibleFriend(friend, nowMillis) }
                        }
                        .catch { throwable ->
                            // Пассивное падение потока карты — inline-состояние, не баннер.
                            Log.w(TAG, "Realtime-поток видимых друзей прервался", throwable)
                            _uiState.update { state ->
                                state.copy(isMapLoading = false, isMapLoadFailed = true)
                            }
                        }
                }
                .collect { friends ->
                    _uiState.update { state ->
                        state.copy(
                            isMapLoading = false,
                            visibleFriends = friends.map { friend -> friend.withCachedAvatar() },
                        )
                    }
                    resolveVisibleAvatars()
                }
        }
    }

    /** Дозагрузка аватаров видимых друзей из текущего состояния; ошибка аватара не показывается. */
    private fun resolveVisibleAvatars() {
        _uiState.value.visibleFriends.forEach { friend ->
            resolveAvatar(friend.uid, friend.avatarVersion) { path ->
                _uiState.update { state ->
                    state.copy(
                        visibleFriends = state.visibleFriends.map { current ->
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

    /**
     * Резолвит аватар как в FriendsViewModel: ошибка остаётся плейсхолдером с инициалом.
     * Кэш по ключу «uid:версия» не даёт перезапускать загрузку при каждом обновлении карты;
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
    private fun VisibleFriendUiModel.withCachedAvatar(): VisibleFriendUiModel {
        val key = avatarKey(uid, avatarVersion)
        if (!avatarPaths.containsKey(key)) return this
        return copy(avatarPath = avatarPaths[key])
    }

    private companion object {
        const val TAG = "MapViewModel"

        /** Ключ кэша аватара: uid и версия, чтобы смена аватара перезагрузила файл. */
        fun avatarKey(uid: String, version: Long) = "${uid}:${version}"
    }
}
