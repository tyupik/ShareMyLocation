package ru.tyupik.sharemylocation.business.auth

/** Типизированные ошибки аутентификации; замаплены из Firebase/Credential Manager в data-слое. */
sealed class AuthError {
    /** Неверный email или пароль при входе либо некорректные данные при регистрации. */
    data object InvalidCredentials : AuthError()

    /** Email уже занят (FirebaseAuthUserCollisionException). */
    data object EmailAlreadyInUse : AuthError()

    /** Нет сети или таймаут (FirebaseNetworkException, IOException). */
    data object NetworkUnavailable : AuthError()

    /** Всё, что не удалось сопоставить с конкретной ошибкой. */
    data class Unexpected(val reason: String?) : AuthError()
}
