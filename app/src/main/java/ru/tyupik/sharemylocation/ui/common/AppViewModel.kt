package ru.tyupik.sharemylocation.ui.common

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import ru.tyupik.sharemylocation.business.auth.AuthInteractor
import ru.tyupik.sharemylocation.business.common.User
import ru.tyupik.sharemylocation.business.profile.ProfileInteractor

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
 */
@HiltViewModel
class AppViewModel @Inject constructor(
    private val authInteractor: AuthInteractor,
    private val profileInteractor: ProfileInteractor,
    private val appUiConverter: AppUiConverter,
) : ViewModel() {

    private val _uiState = MutableStateFlow(AppUiState())
    val uiState: StateFlow<AppUiState> = _uiState.asStateFlow()

    private val _effects = Channel<AppEffect>(Channel.BUFFERED)
    val effects: Flow<AppEffect> = _effects.receiveAsFlow()

    init {
        viewModelScope.launch {
            resolveSession(sendEffect = false)
        }
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
        if (sendEffect) {
            val effect =
                if (uiModel.requiresOnboarding) AppEffect.NavigateToProfileSetup else AppEffect.NavigateToMain
            _effects.send(effect)
        }
    }

    private companion object {
        const val TAG = "AppViewModel"
    }
}
