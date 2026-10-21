package ru.tyupik.sharemylocation.ui.groups

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
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import ru.tyupik.sharemylocation.business.common.User
import ru.tyupik.sharemylocation.business.friends.FriendsInteractor
import ru.tyupik.sharemylocation.business.groups.GroupsError
import ru.tyupik.sharemylocation.business.groups.GroupsException
import ru.tyupik.sharemylocation.business.groups.GroupsInteractor

/**
 * ViewModel экрана управления группой (создание/редактирование). Поток друзей перезапускается
 * при каждом ScreenShown (flatMapLatest на триггер); группа в режиме редактирования читается
 * одноразово — getGroup, ошибка → экран-ошибка с «Повторить» (повтор = повторный ScreenShown).
 *
 * Локальный выбор состава ([selectedUids]) отделён от Firestore: он хранится без пересечения
 * и пересекается с актуальными друзьями ([friendUids]) при отображении и при сохранении —
 * «протухшие» uid (друг удалён, но остался в memberUids) не попадают ни на экран, ни в запись,
 * а выбор не теряется в гонке «группа пришла раньше первой эмиссии потока друзей».
 * Единственная публичная точка входа для UI — [onIntent].
 */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class GroupEditorViewModel @Inject constructor(
    private val groupsInteractor: GroupsInteractor,
    private val friendsInteractor: FriendsInteractor,
    private val groupEditorUiConverter: GroupEditorUiConverter,
) : ViewModel() {

    private val _uiState = MutableStateFlow(GroupEditorUiState())
    val uiState: StateFlow<GroupEditorUiState> = _uiState.asStateFlow()

    // Риск (зафиксирован, не чинится): канал переживёт уход с экрана — stale Closed после pop
    // может мгновенно закрыть следующее открытие редактора; лекарство — epoch-счётчик сессии,
    // добавлять только если воспроизведётся.
    private val _effects = Channel<GroupEditorEffect>(Channel.BUFFERED)
    val effects: Flow<GroupEditorEffect> = _effects.receiveAsFlow()

    /** Триггер перезапуска realtime-потока друзей: счётчик, инкрементируемый при каждом ScreenShown. */
    private val refreshTrigger = MutableStateFlow(0)

    /**
     * Локальный выбор состава экрана: raw-набор (memberUids группы + переключения пользователя).
     * Пересечение с [friendUids] выполняется при отображении чекбоксов и при сохранении.
     */
    private var selectedUids: Set<String> = emptySet()

    /** Актуальные друзья владельца по последней эмиссии потока; защита от «протухших» uid. */
    private var friendUids: Set<String> = emptySet()

    init {
        observeFriends()
    }

    fun onIntent(intent: GroupEditorIntent) {
        when (intent) {
            is GroupEditorIntent.ScreenShown -> onScreenShown(intent.groupId)
            is GroupEditorIntent.NameChanged -> _uiState.update { it.copy(name = intent.value) }
            is GroupEditorIntent.MemberToggled -> toggleMember(intent.uid)
            GroupEditorIntent.SaveClicked -> save()
            GroupEditorIntent.DeleteClicked ->
                _uiState.update { it.copy(isDeleteDialogVisible = true) }
            GroupEditorIntent.DeleteConfirmed -> delete()
            GroupEditorIntent.DeleteDialogDismissed ->
                _uiState.update { it.copy(isDeleteDialogVisible = false) }
            GroupEditorIntent.ErrorDismissed -> _uiState.update { it.copy(actionError = null) }
        }
    }

    /**
     * Вход на экран = чистый лист: вместе с перезапуском загрузки сбрасываются транзиентные
     * флаги прошлой сессии (isSaving, isDeleting, isDeleteDialogVisible) и форма create-режима
     * (name, selectedUids; в edit-режиме их перезаполнит загрузка группы). ViewModel переживает
     * пересоздание Nav-entry, поэтому флаг, не сброшенный умершей корутиной, навсегда дизейблил бы
     * экран (баг ручной проверки Фазы 4: вечный спиннер «Сохранить» после повторного открытия группы).
     * Следствие (осознанно, как busyUid во Friends): guard isSaving не защищает от действия,
     * оставшегося в полёте с прошлого показа, — корутина живёт не дольше AWAIT_TIMEOUT_SECONDS
     * (30 c); завершение старой корутины после сброса может дать внезапный Closed (успех) либо
     * возврат isSaving + баннер (ошибка). Для update это безопасно (merge-set идемпотентен),
     * для create возможен дубль группы только при осознанном повторном сохранении после перезахода.
     */
    private fun onScreenShown(groupId: String?) {
        selectedUids = emptySet()
        _uiState.update { state ->
            state.copy(
                groupId = groupId,
                isLoading = true,
                isLoadFailed = false,
                loadError = null,
                actionError = null,
                name = "",
                isSaving = false,
                isDeleting = false,
                isDeleteDialogVisible = false,
            )
        }
        refreshTrigger.value += 1
        if (groupId != null) {
            loadGroup(groupId)
        }
    }

    /** Одноразовая перечитка группы: имя — в поле ввода, состав — в локальный выбор экрана. */
    private fun loadGroup(groupId: String) {
        viewModelScope.launch {
            groupsInteractor.getGroup(groupId).fold(
                onSuccess = { group ->
                    selectedUids = group.memberUids.toSet()
                    _uiState.update { state ->
                        state.copy(
                            isLoading = false,
                            isLoadFailed = false,
                            loadError = null,
                            name = group.name,
                            friends = state.friends.map { friend ->
                                friend.copy(isChecked = friend.uid in selectedUids)
                            },
                        )
                    }
                },
                onFailure = { throwable -> onLoadFailed(throwable) },
            )
        }
    }

    /**
     * Экран-ошибка загрузки. Причина уточняется только для ошибок группы (в частности
     * GroupNotFound); падение realtime-потока друзей даёт generic-текст (loadError = null).
     */
    private fun onLoadFailed(throwable: Throwable) {
        Log.w(TAG, "Не удалось загрузить группу или список друзей", throwable)
        val error = (throwable as? GroupsException)?.error
        _uiState.update { state ->
            state.copy(
                isLoading = false,
                isLoadFailed = true,
                loadError = error?.let(groupEditorUiConverter::convertError),
            )
        }
    }

    private fun observeFriends() {
        viewModelScope.launch {
            refreshTrigger
                .flatMapLatest {
                    friendsInteractor.observeFriends()
                        .catch { throwable ->
                            // Пассивное падение — экран-ошибка с «Повторить», не баннер действия.
                            onLoadFailed(throwable)
                        }
                }
                .collect { users ->
                    friendUids = users.map(User::uid).toSet()
                    _uiState.update { state ->
                        state.copy(
                            isLoading = false,
                            friends = users.map { user ->
                                groupEditorUiConverter.convertFriend(
                                    user,
                                    isChecked = user.uid in selectedUids,
                                )
                            },
                        )
                    }
                }
        }
    }

    private fun toggleMember(uid: String) {
        selectedUids = if (uid in selectedUids) selectedUids - uid else selectedUids + uid
        _uiState.update { state ->
            state.copy(
                friends = state.friends.map { friend ->
                    if (friend.uid == uid) friend.copy(isChecked = !friend.isChecked) else friend
                },
            )
        }
    }

    /**
     * Сохранение: создание — createGroup (имя + выбранный состав; uid берутся из чекбоксов
     * актуальных друзей, поэтому ⊆ друзей по построению; пустой состав допустим), редактирование —
     * updateGroup (имя + весь memberUids). Успех закрывает экран: список групп обновится
     * realtime-слушателем вкладки, колбэки «результата» не используются.
     */
    private fun save() {
        val state = _uiState.value
        if (state.isSaving || state.isLoading) return
        val groupId = state.groupId
        val name = state.name
        // Состав записи всегда ⊆ актуальных друзей: «протухшие» uid отсекаются здесь.
        val memberUids = (selectedUids intersect friendUids).toList()
        _uiState.update { it.copy(isSaving = true) }
        viewModelScope.launch {
            val result = if (groupId == null) {
                // groupId созданной группы не нужен: экран закрывается, список обновится сам.
                groupsInteractor.createGroup(name, selectedUids.toList()).map { }
            } else {
                groupsInteractor.updateGroup(groupId, name, memberUids)
            }
            result.fold(
                onSuccess = { _effects.send(GroupEditorEffect.Closed) },
                onFailure = { throwable -> onActionError(throwable) },
            )
        }
    }

    private fun delete() {
        val state = _uiState.value
        if (state.isDeleting || state.isSaving) return
        val groupId = state.groupId ?: return
        _uiState.update { it.copy(isDeleting = true, isDeleteDialogVisible = false) }
        viewModelScope.launch {
            groupsInteractor.deleteGroup(groupId).fold(
                onSuccess = { _effects.send(GroupEditorEffect.Closed) },
                onFailure = { throwable -> onActionError(throwable) },
            )
        }
    }

    /** Ошибки, не завёрнутые в GroupsException, считаем Unexpected. */
    private fun onActionError(throwable: Throwable) {
        Log.w(TAG, "Действие с группой не удалось", throwable)
        val error = (throwable as? GroupsException)?.error
            ?: GroupsError.Unexpected(throwable.message)
        _uiState.update { state ->
            state.copy(
                isSaving = false,
                isDeleting = false,
                actionError = groupEditorUiConverter.convertError(error),
            )
        }
    }

    private companion object {
        const val TAG = "GroupEditorViewModel"
    }
}
