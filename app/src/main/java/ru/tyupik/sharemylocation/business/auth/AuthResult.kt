package ru.tyupik.sharemylocation.business.auth

/** Бизнес-модель результата аутентификации. */
sealed class AuthResult {
    data object Success : AuthResult()

    data class Error(val error: AuthError) : AuthResult()
}
