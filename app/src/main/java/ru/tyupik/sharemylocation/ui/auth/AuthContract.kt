package ru.tyupik.sharemylocation.ui.auth

/** Режим экрана аутентификации: вход или регистрация. */
enum class AuthMode {
    SIGN_IN,
    SIGN_UP,
}

/**
 * SSOT для экрана входа/регистрации.
 * Ошибки показываются инлайн на форме и переживают поворот; сбрасываются при вводе,
 * переключении режима и новом сабмите.
 */
data class AuthUiState(
    val mode: AuthMode = AuthMode.SIGN_IN,
    val email: String = "",
    val password: String = "",
    /** Заполняется только в режиме SIGN_UP: второе поле для подтверждения пароля. */
    val repeatedPassword: String = "",
    /** Заполняется только в режиме SIGN_UP и попадает в users/{uid}.displayName. */
    val displayName: String = "",
    val isLoading: Boolean = false,
    val error: AuthUiError? = null,
)

/** Действия пользователя и системные события Credential Manager. */
sealed class AuthIntent {
    // Ввод текста
    data class EmailChanged(val value: String) : AuthIntent()
    data class PasswordChanged(val value: String) : AuthIntent()
    data class RepeatedPasswordChanged(val value: String) : AuthIntent()
    data class DisplayNameChanged(val value: String) : AuthIntent()

    // Переключение режима: сбрасывает пароли и ошибку, email сохраняет
    data class ModeChanged(val mode: AuthMode) : AuthIntent()

    // Email/Password
    data object SignInWithEmailClicked : AuthIntent()
    data object SignUpWithEmailClicked : AuthIntent()

    // Google через Credential Manager
    data object GoogleSignInClicked : AuthIntent()

    /** UI-слой получил idToken от Credential Manager и передаёт его на обмен в интерактор. */
    data class GoogleCredentialReceived(val idToken: String) : AuthIntent()

    /** Пользователь закрыл системный диалог выбора аккаунта. */
    data object GoogleSignInCancelled : AuthIntent()

    /** Credential Manager вернул ошибку (нет аккаунтов, нет Play Services и т.п.). */
    data class GoogleSignInFailed(val reason: String?) : AuthIntent()

    /** Пользователь закрыл баннер с ошибкой. */
    data object ErrorDismissed : AuthIntent()
}

/** One-off события экрана. */
sealed class AuthEffect {
    /** Аутентификация успешна, документ users/{uid} создан или обновлён. */
    data object NavigateToMain : AuthEffect()
}
