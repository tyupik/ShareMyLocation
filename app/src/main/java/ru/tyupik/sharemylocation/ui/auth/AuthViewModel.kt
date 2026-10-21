package ru.tyupik.sharemylocation.ui.auth

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
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import ru.tyupik.sharemylocation.business.auth.AuthInteractor
import ru.tyupik.sharemylocation.business.auth.AuthResult

/**
 * ViewModel экрана входа/регистрации.
 * Получает от интерактора бизнес-результат, через [AuthUiConverter] переводит его в Ui-модель
 * и кладёт в состояние. Единственная публичная точка входа для UI — [onIntent].
 */
@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authInteractor: AuthInteractor,
    private val authUiConverter: AuthUiConverter,
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    private val _effects = Channel<AuthEffect>(Channel.BUFFERED)
    val effects: Flow<AuthEffect> = _effects.receiveAsFlow()

    /** Признак завершённой в этой сессии VM успешной аутентификации: сброс формы на повторный показ. */
    private var wasAuthenticated = false

    fun onIntent(intent: AuthIntent) {
        when (intent) {
            AuthIntent.ScreenShown -> onScreenShown()
            is AuthIntent.EmailChanged -> onEmailChanged(intent.value)
            is AuthIntent.PasswordChanged -> onPasswordChanged(intent.value)
            is AuthIntent.RepeatedPasswordChanged -> onRepeatedPasswordChanged(intent.value)
            is AuthIntent.DisplayNameChanged -> onDisplayNameChanged(intent.value)
            is AuthIntent.ModeChanged -> onModeChanged(intent.mode)
            AuthIntent.SignInWithEmailClicked -> signInWithEmail()
            AuthIntent.SignUpWithEmailClicked -> signUpWithEmail()
            AuthIntent.GoogleSignInClicked -> onGoogleSignInClicked()
            is AuthIntent.GoogleCredentialReceived -> onGoogleCredentialReceived(intent.idToken)
            AuthIntent.GoogleSignInCancelled -> onGoogleSignInCancelled()
            is AuthIntent.GoogleSignInFailed -> onGoogleSignInFailed(intent.reason)
            AuthIntent.ErrorDismissed -> _uiState.update { it.copy(error = null) }
        }
    }

    /**
     * Повторный показ экрана после успешной аутентификации (выход из аккаунта) — полный
     * чистый лист: форма, isLoading и остаточная ошибка сбрасываются. На каждый показ
     * сбрасывать нельзя: LaunchedEffect(Unit) перезапускается при повороте, и форма
     * потеряла бы ввод.
     */
    private fun onScreenShown() {
        if (!wasAuthenticated) return
        wasAuthenticated = false
        _uiState.value = AuthUiState()
    }

    private fun onEmailChanged(value: String) = _uiState.update { it.copy(email = value, error = null) }

    private fun onPasswordChanged(value: String) = _uiState.update { it.copy(password = value, error = null) }

    private fun onRepeatedPasswordChanged(value: String) =
        _uiState.update { it.copy(repeatedPassword = value, error = null) }

    private fun onDisplayNameChanged(value: String) =
        _uiState.update { it.copy(displayName = value, error = null) }

    private fun onModeChanged(mode: AuthMode) = _uiState.update {
        if (it.mode == mode) {
            it
        } else {
            it.copy(mode = mode, password = "", repeatedPassword = "", error = null)
        }
    }

    private fun signInWithEmail() {
        val state = _uiState.value
        if (state.isLoading) return
        _uiState.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            handleUiModel(authUiConverter.convert(authInteractor.signInWithEmail(state.email.trim(), state.password)))
        }
    }

    private fun signUpWithEmail() {
        val state = _uiState.value
        if (state.isLoading) return
        _uiState.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            handleUiModel(
                authUiConverter.convert(
                    authInteractor.signUpWithEmail(state.email.trim(), state.password, state.displayName.trim())
                )
            )
        }
    }

    /** Клик по кнопке Google: включаем загрузку и ждём системное событие от Credential Manager. */
    private fun onGoogleSignInClicked() {
        if (_uiState.value.isLoading) return
        _uiState.update { it.copy(isLoading = true, error = null) }
    }

    private fun onGoogleCredentialReceived(idToken: String) {
        if (!_uiState.value.isLoading) return
        viewModelScope.launch {
            handleUiModel(authUiConverter.convert(authInteractor.signInWithGoogle(idToken)))
        }
    }

    /** Отмена диалога Google — не ошибка: тихо возвращаем форму в исходное состояние. */
    private fun onGoogleSignInCancelled() {
        if (!_uiState.value.isLoading) return
        Log.i(TAG, "Google sign-in sheet cancelled by user")
        _uiState.update { it.copy(isLoading = false) }
    }

    private fun onGoogleSignInFailed(reason: String?) {
        if (!_uiState.value.isLoading) return
        Log.w(TAG, "Google Sign-In failed: $reason")
        handleUiModel(AuthUiModel.Error(AuthUiError.Unexpected(reason)))
    }

    private fun handleUiModel(uiModel: AuthUiModel) {
        when (uiModel) {
            is AuthUiModel.Success -> onAuthSuccess()
            is AuthUiModel.Error -> onAuthError(uiModel.error)
        }
    }

    private fun onAuthSuccess() {
        wasAuthenticated = true
        _uiState.update { it.copy(isLoading = false) }
        viewModelScope.launch { _effects.send(AuthEffect.NavigateToMain) }
    }

    private fun onAuthError(error: AuthUiError) {
        Log.w(TAG, "Auth failed: $error")
        _uiState.update { it.copy(isLoading = false, error = error) }
    }

    private companion object {
        const val TAG = "AuthViewModel"
    }
}
