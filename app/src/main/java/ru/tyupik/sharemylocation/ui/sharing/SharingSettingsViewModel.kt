package ru.tyupik.sharemylocation.ui.sharing

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
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
import ru.tyupik.sharemylocation.business.groups.GroupsInteractor
import ru.tyupik.sharemylocation.business.sharing.SharingError
import ru.tyupik.sharemylocation.business.sharing.SharingException
import ru.tyupik.sharemylocation.business.sharing.SharingInteractor
import ru.tyupik.sharemylocation.business.sharing.SharingSettings

/**
 * ViewModel экрана настроек шаринга. Ключевое решение — автосохранение без кнопки «Сохранить»:
 * каждый change-intent (1) обновляет UiState сразу (optimistic), (2) собирает полный снимок
 * всех пяти полей и (3) записывает его через интерактор; снимок идемпотентен. Полёт записи
 * отменяется новым изменением, поэтому записи не конкурируют. При неудаче — оптимистичный
 * откат к [lastSaved], если состояние экрана всё ещё соответствует записанному снимку,
 * и inline-баннер ошибки в любом случае.
 *
 * Локальный выбор ([rawSelectedUids]/[rawSelectedGroupIds]) отделён от Firestore: хранится
 * без пересечения и пересекается с актуальными друзьями/группами при отображении и записи —
 * «протухшие» id (друг/группа удалены) не попадают ни на экран, ни в документ
 * (паттерн GroupEditorViewModel). При ALL_FRIENDS списки выбора сохраняются как есть —
 * возврат в CUSTOM восстанавливает выбор.
 *
 * Чтение документа одноразовое, НЕ realtime: документ редактирует только владелец с этого
 * экрана, realtime-слушатель конкурировал бы с optimistic-состоянием автосейва.
 * Realtime-потоки друзей и групп перезапускаются при каждом ScreenShown (flatMapLatest
 * на триггер); их падение — inline-состояние секции, не экран-ошибка.
 * Единственная публичная точка входа для UI — [onIntent].
 */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class SharingSettingsViewModel @Inject constructor(
    private val sharingInteractor: SharingInteractor,
    private val friendsInteractor: FriendsInteractor,
    private val groupsInteractor: GroupsInteractor,
    private val sharingSettingsUiConverter: SharingSettingsUiConverter,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SharingSettingsUiState())
    val uiState: StateFlow<SharingSettingsUiState> = _uiState.asStateFlow()

    private val _effects = Channel<SharingSettingsEffect>(Channel.BUFFERED)
    val effects: Flow<SharingSettingsEffect> = _effects.receiveAsFlow()

    /** Триггер перезапуска realtime-потоков: счётчик, инкрементируемый при каждом ScreenShown. */
    private val refreshTrigger = MutableStateFlow(0)

    /** Последний успешно записанный снимок; дефолт после чтения. Откат автосейва — к нему. */
    private var lastSaved: SharingSettings? = null

    /** Raw-выбор «Люди»: списки из документа + переключения; пересечение с друзьями — при записи. */
    private var rawSelectedUids: Set<String> = emptySet()

    /** Raw-выбор «Группы»: id из документа + переключения; пересечение с группами — при записи. */
    private var rawSelectedGroupIds: Set<String> = emptySet()

    /** Актуальные друзья владельца по последней эмиссии потока; защита от «протухших» uid. */
    private var friendUids: Set<String> = emptySet()

    /** Актуальные группы владельца по последней эмиссии потока. */
    private var groupIds: Set<String> = emptySet()

    /** Полёт автосохранения; новое изменение отменяет предыдущую запись (снимок идемпотентен). */
    private var saveJob: Job? = null

    init {
        observeFriends()
        observeGroups()
    }

    fun onIntent(intent: SharingSettingsIntent) {
        when (intent) {
            SharingSettingsIntent.ScreenShown -> onScreenShown()
            is SharingSettingsIntent.EnabledToggled -> onEnabledToggled(intent.value)
            is SharingSettingsIntent.AudienceSelected -> onAudienceSelected(intent.audience)
            is SharingSettingsIntent.FriendToggled -> onFriendToggled(intent.uid)
            is SharingSettingsIntent.GroupToggled -> onGroupToggled(intent.groupId)
            SharingSettingsIntent.SaveErrorDismissed ->
                _uiState.update { it.copy(saveError = null) }
        }
    }

    /**
     * Вход на экран = чистый лист: сброс транзиентов прошлой сессии (isSaving, saveError,
     * экран-ошибка, флаги секций) — ViewModel переживает пересоздание Nav-entry, унаследованная
     * занятость навсегда дизейблила бы экран (урок Фазы 4) — плюс одноразовая перечитка
     * документа и перезапуск realtime-потоков.
     */
    private fun onScreenShown() {
        saveJob?.cancel()
        refreshTrigger.value += 1
        _uiState.update { state ->
            state.copy(
                isLoading = true,
                isLoadFailed = false,
                loadError = null,
                isSaving = false,
                saveError = null,
                isFriendsLoadFailed = false,
                isGroupsLoadFailed = false,
            )
        }
        loadSettings()
    }

    /** Одноразовая перечитка документа; отсутствующий документ — дефолт от интерактора, не ошибка. */
    private fun loadSettings() {
        viewModelScope.launch {
            sharingInteractor.getSettings().fold(
                onSuccess = { settings ->
                    lastSaved = settings
                    applySettings(settings)
                    _uiState.update { state ->
                        state.copy(isLoading = false, isLoadFailed = false, loadError = null)
                    }
                },
                onFailure = { throwable ->
                    Log.w(TAG, "Не удалось загрузить настройки шаринга", throwable)
                    val error = (throwable as? SharingException)?.error
                    _uiState.update { state ->
                        state.copy(
                            isLoading = false,
                            isLoadFailed = true,
                            loadError = error?.let(sharingSettingsUiConverter::convertError),
                        )
                    }
                },
            )
        }
    }

    /** Применяет прочитанный/откаченный снимок к raw-выбору и строкам секций. */
    private fun applySettings(settings: SharingSettings) {
        rawSelectedUids = settings.allowedUids.toSet()
        rawSelectedGroupIds = settings.allowedGroupIds.toSet()
        _uiState.update { state ->
            state.copy(
                isEnabled = settings.isEnabled,
                audience = sharingSettingsUiConverter.convertAudience(settings.audience),
                friends = state.friends.map { friend ->
                    friend.copy(isChecked = friend.id in rawSelectedUids)
                },
                groups = state.groups.map { group ->
                    group.copy(isChecked = group.id in rawSelectedGroupIds)
                },
            )
        }
    }

    private fun observeFriends() {
        viewModelScope.launch {
            refreshTrigger
                .flatMapLatest {
                    friendsInteractor.observeFriends()
                        .catch { throwable ->
                            // Пассивное падение секции — inline-состояние, не баннер и не экран-ошибка.
                            Log.w(TAG, "Realtime-поток друзей прервался", throwable)
                            _uiState.update { state -> state.copy(isFriendsLoadFailed = true) }
                        }
                }
                .collect { users ->
                    friendUids = users.map(User::uid).toSet()
                    _uiState.update { state ->
                        state.copy(
                            friends = users.map { user ->
                                sharingSettingsUiConverter.convertFriend(
                                    user,
                                    isChecked = user.uid in rawSelectedUids,
                                )
                            },
                        )
                    }
                }
        }
    }

    private fun observeGroups() {
        viewModelScope.launch {
            refreshTrigger
                .flatMapLatest {
                    groupsInteractor.observeGroups()
                        .catch { throwable ->
                            // Пассивное падение секции — inline-состояние, не баннер и не экран-ошибка.
                            Log.w(TAG, "Realtime-поток групп прервался", throwable)
                            _uiState.update { state -> state.copy(isGroupsLoadFailed = true) }
                        }
                }
                .collect { groups ->
                    groupIds = groups.map { group -> group.groupId }.toSet()
                    _uiState.update { state ->
                        state.copy(
                            groups = groups.map { group ->
                                sharingSettingsUiConverter.convertGroup(
                                    group,
                                    isChecked = group.groupId in rawSelectedGroupIds,
                                )
                            },
                        )
                    }
                }
        }
    }

    private fun onEnabledToggled(value: Boolean) {
        _uiState.update { it.copy(isEnabled = value) }
        // Включение при CUSTOM с пустым выбором = «локацию не увидит никто» — подсказка.
        if (value && _uiState.value.audience == SharingAudienceUi.CUSTOM && isSelectionEmpty()) {
            sendHint()
        }
        persist()
    }

    private fun onAudienceSelected(audience: SharingAudienceUi) {
        if (audience == _uiState.value.audience) return
        _uiState.update { it.copy(audience = audience) }
        // Переход в CUSTOM с пустым выбором = «локацию не увидит никто» — подсказка.
        if (audience == SharingAudienceUi.CUSTOM && isSelectionEmpty()) {
            sendHint()
        }
        persist()
    }

    private fun onFriendToggled(uid: String) {
        rawSelectedUids = if (uid in rawSelectedUids) rawSelectedUids - uid else rawSelectedUids + uid
        _uiState.update { state ->
            state.copy(
                friends = state.friends.map { friend ->
                    if (friend.id == uid) friend.copy(isChecked = !friend.isChecked) else friend
                },
            )
        }
        persist()
    }

    private fun onGroupToggled(groupId: String) {
        rawSelectedGroupIds = if (groupId in rawSelectedGroupIds) {
            rawSelectedGroupIds - groupId
        } else {
            rawSelectedGroupIds + groupId
        }
        _uiState.update { state ->
            state.copy(
                groups = state.groups.map { group ->
                    if (group.id == groupId) group.copy(isChecked = !group.isChecked) else group
                },
            )
        }
        persist()
    }

    /**
     * Автосохранение: сборка и запись полного снимка всех пяти полей (идемпотентная merge-запись).
     * «Протухшие» id отсекаются пересечением с актуальными друзьями/группами; при ALL_FRIENDS
     * списки выбора сохраняются как есть. Предыдущий полёт записи отменяется — записи
     * не конкурируют и не приходят в Firestore не по порядку.
     */
    private fun persist() {
        if (lastSaved == null) return
        val snapshot = buildSnapshot()
        _uiState.update { it.copy(isSaving = true) }
        saveJob?.cancel()
        saveJob = viewModelScope.launch {
            sharingInteractor.saveSettings(snapshot).fold(
                onSuccess = {
                    lastSaved = snapshot
                    _uiState.update { it.copy(isSaving = false) }
                },
                onFailure = { throwable -> onSaveFailed(snapshot, throwable) },
            )
        }
    }

    /**
     * Полный снимок настроек для записи: isEnabled, аудитория и пересечённые с актуальностью
     * списки выбора — «протухшие» id (друг/группа удалены) не попадают в документ.
     */
    private fun buildSnapshot(): SharingSettings {
        val state = _uiState.value
        return SharingSettings(
            uid = lastSaved?.uid.orEmpty(),
            isEnabled = state.isEnabled,
            audience = sharingSettingsUiConverter.toBusiness(state.audience),
            allowedUids = (rawSelectedUids intersect friendUids).toList(),
            allowedGroupIds = (rawSelectedGroupIds intersect groupIds).toList(),
        )
    }

    /**
     * Неудача автосохранения: если состояние экрана всё ещё соответствует записанному снимку
     * (новых изменений не было) — оптимистичный откат к [lastSaved]; в любом случае — баннер.
     */
    private fun onSaveFailed(snapshot: SharingSettings, throwable: Throwable) {
        Log.w(TAG, "Автосохранение настроек шаринга не удалось", throwable)
        if (isStateUnchangedSince(snapshot)) {
            rollbackToLastSaved()
        }
        val error = (throwable as? SharingException)?.error
            ?: SharingError.Unexpected(throwable.message)
        _uiState.update { state ->
            state.copy(
                isSaving = false,
                saveError = sharingSettingsUiConverter.convertError(error),
            )
        }
    }

    /**
     * Состояние не изменилось с момента снимка, если тумблер/аудитория те же и отмеченные
     * строки совпадают с его списками: отмеченные на экране строки = raw-выбор ∩ актуальные,
     * как и списки снимка, поэтому равенство означает «переключений не было».
     */
    private fun isStateUnchangedSince(snapshot: SharingSettings): Boolean {
        val state = _uiState.value
        return state.isEnabled == snapshot.isEnabled &&
            state.audience == sharingSettingsUiConverter.convertAudience(snapshot.audience) &&
            state.friends.filter { it.isChecked }.map { it.id }.toSet() ==
            snapshot.allowedUids.toSet() &&
            state.groups.filter { it.isChecked }.map { it.id }.toSet() ==
            snapshot.allowedGroupIds.toSet()
    }

    /** Оптимистичный откат: тумблер, аудитория, raw-выбор и чекбоксы — к последнему успешному снимку. */
    private fun rollbackToLastSaved() {
        val saved = lastSaved ?: return
        rawSelectedUids = saved.allowedUids.toSet()
        rawSelectedGroupIds = saved.allowedGroupIds.toSet()
        _uiState.update { state ->
            state.copy(
                isEnabled = saved.isEnabled,
                audience = sharingSettingsUiConverter.convertAudience(saved.audience),
                friends = state.friends.map { it.copy(isChecked = it.id in rawSelectedUids) },
                groups = state.groups.map { it.copy(isChecked = it.id in rawSelectedGroupIds) },
            )
        }
    }

    /** Пустой выбор = нет ни одного отмеченного человека и группы (raw, без пересечения). */
    private fun isSelectionEmpty(): Boolean =
        rawSelectedUids.isEmpty() && rawSelectedGroupIds.isEmpty()

    private fun sendHint() {
        viewModelScope.launch { _effects.send(SharingSettingsEffect.ShowEmptyAudienceHint) }
    }

    private companion object {
        const val TAG = "SharingSettingsViewModel"
    }
}
