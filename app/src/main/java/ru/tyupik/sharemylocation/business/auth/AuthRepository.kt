package ru.tyupik.sharemylocation.business.auth

import kotlinx.coroutines.flow.Flow
import ru.tyupik.sharemylocation.business.common.User

/**
 * Доменная ошибка аутентификации: несёт типизированный [AuthError] для UiState
 * и исходный Throwable для логирования.
 */
class AuthException(
    val error: AuthError,
    cause: Throwable? = null,
) : Exception(error.toString(), cause)

/**
 * Контракт аутентификации: Firebase Auth + документ users/{uid} в Firestore.
 * Успешным считается только результат, после которого документ пользователя
 * создан или обновлён (merge по uid).
 */
interface AuthRepository {
    suspend fun signInWithEmail(email: String, password: String): Result<Unit>
    suspend fun signUpWithEmail(email: String, password: String, displayName: String): Result<Unit>
    suspend fun signInWithGoogle(idToken: String): Result<Unit>

    /** Текущий пользователь сохранённой сессии либо null. */
    suspend fun getCurrentUser(): User?

    suspend fun signOut()

    /**
     * Realtime-поток состояния аутентификации: первая эмиссия — сохранённая сессия
     * (или null), далее — при каждом sign-in/sign-out.
     */
    fun observeAuthState(): Flow<User?>
}
