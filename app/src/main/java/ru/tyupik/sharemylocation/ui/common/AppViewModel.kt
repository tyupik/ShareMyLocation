package ru.tyupik.sharemylocation.ui.common

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
import kotlinx.coroutines.launch
import ru.tyupik.sharemylocation.business.auth.AuthInteractor
import ru.tyupik.sharemylocation.business.common.User
import ru.tyupik.sharemylocation.business.profile.ProfileInteractor
import ru.tyupik.sharemylocation.business.sharing.LocationSharingController
import ru.tyupik.sharemylocation.business.sharing.SharingInteractor

/**
 * Состояние корневой навигации: пока [isSessionResolved] равно false,
 * стартовый экран не определён и UI показывает индикатор загрузки.
 * [requiresOnboarding] — вошедший пользователь ещё не прошёл онбординг ProfileSetup.
 */
data class AppUiState(
    val isSessionResolved: Boolean = false,
    val isAuthenticated: Boolean = false,
    val requiresOnboarding: Boolean = false,
)

/** Системные события корневой навигации. */
sealed class AppIntent {
    /** Аутентификация выполнена — нужно решить, куда вести дальше: онбординг или Main. */
    data object AuthCompleted : AppIntent()
}

/** One-off события корневой навигации: маршрутизацией занимается AppRoot. */
sealed class AppEffect {
    data object NavigateToMain : AppEffect()

    data object NavigateToProfileSetup : AppEffect()
}

/**
 * Корневая ViewModel приложения: при старте и после входа через интеракторы проверяет сессию
 * и документ users/{uid}, чтобы навигация выбрала стартовый экран (Auth / ProfileSetup / Main).
 *
 * Дополнительно (Фаза 6) — корневой оркестратор foreground-сервиса отправки геолокации:
 * после успешного resolveSession с isAuthenticated == true подписывается на realtime-флаг
 * isEnabled и на каждую эмиссию стартует/останавливает сервис через контроллер. Закрывает оба
 * сценария: при старте процесса сервис поднимается, если isEnabled и права (проверка внутри
 * контроллера); тумблер стоп/стартит сервис даже с закрытым экраном настроек (latency
 * compensation Firestore делает реакцию мгновенной). Падение потока (сеть, PERMISSION_DENIED
 * после logout, смерть слушателя) — fail-safe: лог + остановка сервиса.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class AppViewModel @Inject constructor(
    private val authInteractor: AuthInteractor,
    private val profileInteractor: ProfileInteractor,
    private val sharingInteractor: SharingInteractor,
    private val locationSharingController: LocationSharingController,
    private val appUiConverter: AppUiConverter,
) : ViewModel() {

    private val _uiState = MutableStateFlow(AppUiState())
    val uiState: StateFlow<AppUiState> = _uiState.asStateFlow()

    private val _effects = Channel<AppEffect>(Channel.BUFFERED)
    val effects: Flow<AppEffect> = _effects.receiveAsFlow()

    /**
     * Триггер перезапуска слушателя isEnabled: счётчик инкрементируется после каждого
     * успешного resolveSession вошедшего пользователя (init и AuthCompleted) — паттерн
     * badgeTrigger MainViewModel: ViewModel переживает logout→login, старый поток после
     * смены аккаунта умирает, без перезапуска сервис второго аккаунта остался бы мёртвым.
     */
    private val sharingTrigger = MutableStateFlow(0)

    init {
        viewModelScope.launch {
            resolveSession(sendEffect = false)
        }
        observeSharingEnabled()
    }

    fun onIntent(intent: AppIntent) {
        when (intent) {
            AppIntent.AuthCompleted -> viewModelScope.launch { resolveSession(sendEffect = true) }
        }
    }

    /**
     * Читает сессию и документ профиля, обновляет [AppUiState];
     * после входа ([sendEffect] = true) дополнительно отправляет Effect с целевым экраном.
     * Ошибка чтения профиля не блокирует вход: считается, что онбординг не требуется.
     */
    private suspend fun resolveSession(sendEffect: Boolean) {
        val user = authInteractor.getCurrentUser()
        val profile: User? = user?.let { profileInteractor.getProfile().getOrNull() }
        if (user != null && profile == null) {
            Log.w(TAG, "Не удалось прочитать users/{uid}, пропускаем экран онбординга")
        }
        val uiModel = appUiConverter.convert(user, profile)
        _uiState.value = AppUiState(
            isSessionResolved = true,
            isAuthenticated = uiModel.isAuthenticated,
            requiresOnboarding = uiModel.requiresOnboarding,
        )
        if (uiModel.isAuthenticated) {
            sharingTrigger.value += 1
        }
        if (sendEffect) {
            val effect =
                if (uiModel.requiresOnboarding) AppEffect.NavigateToProfileSetup else AppEffect.NavigateToMain
            _effects.send(effect)
        }
    }

    /**
     * Realtime-флаг isEnabled → старт/стоп сервиса. Подписка перезапускается по триггеру
     * (flatMapLatest); ошибка потока глотается НАМЕРЕННО: контракт [AppUiState] не содержит
     * error-полей, а сервис обязан останавливаться при обрыве (fail-safe), job при этом жив —
     * catch внутри flatMapLatest, внешний поток продолжает реагировать на триггер.
     */
    private fun observeSharingEnabled() {
        viewModelScope.launch {
            sharingTrigger
                .flatMapLatest {
                    sharingInteractor.observeIsEnabled()
                        .catch { throwable ->
                            Log.w(TAG, "Realtime-поток флага isEnabled прервался — стоп сервиса", throwable)
                            locationSharingController.setSharingEnabled(false)
                        }
                }
                .collect { isEnabled ->
                    locationSharingController.setSharingEnabled(isEnabled)
                }
        }
    }

    private companion object {
        const val TAG = "AppViewModel"
    }
}
