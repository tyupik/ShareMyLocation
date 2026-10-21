package ru.tyupik.sharemylocation.ui.main

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
import ru.tyupik.sharemylocation.business.auth.AuthInteractor
import ru.tyupik.sharemylocation.business.friends.FriendsInteractor

/**
 * ViewModel главного экрана-контейнера: хранит выбранную вкладку, badge входящих заявок
 * в друзья и выполняет выход из аккаунта. Единственная публичная точка входа для UI — [onIntent].
 */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class MainViewModel @Inject constructor(
    private val authInteractor: AuthInteractor,
    private val friendsInteractor: FriendsInteractor,
) : ViewModel() {

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    private val _effects = Channel<MainEffect>(Channel.BUFFERED)
    val effects: Flow<MainEffect> = _effects.receiveAsFlow()

    /** Триггер перезапуска badge-слушателя: счётчик инкрементируется при каждой смене вкладки. */
    private val badgeTrigger = MutableStateFlow(0)

    init {
        observePendingRequests()
    }

    fun onIntent(intent: MainIntent) {
        when (intent) {
            is MainIntent.TabSelected -> {
                _uiState.update { it.copy(selectedTab = intent.tab) }
                badgeTrigger.value += 1
            }
            MainIntent.SignOutClicked -> signOut()
        }
    }

    /**
     * Badge входящих заявок. ViewModel живёт в переиспользуемом store Nav-entry Main
     * и переживает logout→login, поэтому слушатель перезапускается по триггеру-счётчику:
     * старый поток после смены аккаунта умирает (например, PERMISSION_DENIED), и без
     * перезапуска badge второго аккаунта остался бы мёртвым навсегда.
     * Ошибка глотается НАМЕРЕННО (контракт MainUiState не содержит error-поля,
     * расширять его запрещено планом): badge остаётся с последним значением, job жив —
     * catch стоит внутри flatMapLatest, поэтому внешний поток продолжает реагировать
     * на триггер; surfaced-обработка — при появлении error-поля в MainUiState.
     */
    private fun observePendingRequests() {
        viewModelScope.launch {
            badgeTrigger
                .flatMapLatest {
                    friendsInteractor.observeIncomingFriendRequests()
                        .catch { throwable ->
                            Log.w(TAG, "Не удалось слушать входящие заявки в друзья", throwable)
                        }
                }
                .collect { requests ->
                    _uiState.update { it.copy(pendingRequestsCount = requests.size) }
                }
        }
    }

    private fun signOut() {
        viewModelScope.launch {
            authInteractor.signOut()
            _effects.send(MainEffect.NavigateToAuth)
        }
    }

    private companion object {
        const val TAG = "MainViewModel"
    }
}
