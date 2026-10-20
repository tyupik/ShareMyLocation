package ru.tyupik.sharemylocation.business.auth

import ru.tyupik.sharemylocation.business.common.User

/**
 * Интерактор аутентификации: оркестрирует [AuthRepository] и возвращает бизнес-модели.
 * Успешным считается только результат, после которого документ users/{uid} создан или обновлён.
 */
interface AuthInteractor {
    suspend fun signInWithEmail(email: String, password: String): AuthResult
    suspend fun signUpWithEmail(email: String, password: String, displayName: String): AuthResult
    suspend fun signInWithGoogle(idToken: String): AuthResult

    /** Текущий пользователь сохранённой сессии либо null. */
    suspend fun getCurrentUser(): User?

    suspend fun signOut()
}
