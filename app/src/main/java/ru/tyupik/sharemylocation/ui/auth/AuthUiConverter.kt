package ru.tyupik.sharemylocation.ui.auth

import javax.inject.Inject
import ru.tyupik.sharemylocation.business.auth.AuthError
import ru.tyupik.sharemylocation.business.auth.AuthResult

/** Модели Ui-слоя, в которые конвертер переводит бизнес-результат аутентификации. */
sealed class AuthUiModel {
    data object Success : AuthUiModel()

    data class Error(val error: AuthUiError) : AuthUiModel()
}

/** Ошибки аутентификации в терминах Ui-слоя. */
sealed class AuthUiError {
    data object InvalidCredentials : AuthUiError()

    data object EmailAlreadyInUse : AuthUiError()

    data object NetworkUnavailable : AuthUiError()

    data class Unexpected(val reason: String?) : AuthUiError()
}

/** Конвертер бизнес-результата аутентификации в Ui-модель. */
class AuthUiConverter @Inject constructor() {

    fun convert(result: AuthResult): AuthUiModel = when (result) {
        is AuthResult.Success -> AuthUiModel.Success
        is AuthResult.Error -> AuthUiModel.Error(result.error.toUiError())
    }

    private fun AuthError.toUiError(): AuthUiError = when (this) {
        AuthError.InvalidCredentials -> AuthUiError.InvalidCredentials
        AuthError.EmailAlreadyInUse -> AuthUiError.EmailAlreadyInUse
        AuthError.NetworkUnavailable -> AuthUiError.NetworkUnavailable
        is AuthError.Unexpected -> AuthUiError.Unexpected(reason)
    }
}
