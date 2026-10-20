package ru.tyupik.sharemylocation.data.auth

import java.util.concurrent.ExecutionException
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import ru.tyupik.sharemylocation.business.auth.AuthError
import ru.tyupik.sharemylocation.business.auth.AuthException
import ru.tyupik.sharemylocation.business.auth.AuthRepository
import ru.tyupik.sharemylocation.business.common.User
import ru.tyupik.sharemylocation.data.common.mapFailure
import ru.tyupik.sharemylocation.data.common.runSuspendCatching

/**
 * Реализация аутентификации поверх [FirebaseBackend].
 * Вся работа с Firebase вынесена в Java-класс: пайплайн компиляции AGP 9.4 не может
 * резолвить firebase-артефакты из Kotlin-источников (см. комментарий в build.gradle.kts),
 * а javac читает те же jar без проблем. Kotlin-слой видит только наш тип [AuthRepository].
 */
@Singleton
class AuthRepositoryImpl @Inject constructor(
    private val backend: FirebaseBackend,
) : AuthRepository {

    override suspend fun signInWithEmail(email: String, password: String): Result<Unit> =
        runSuspendCatching {
            withContext(Dispatchers.IO) { backend.signInWithEmail(email, password) }
        }.mapFailure { toAuthException(it) }

    override suspend fun signUpWithEmail(email: String, password: String, displayName: String): Result<Unit> =
        runSuspendCatching {
            withContext(Dispatchers.IO) { backend.signUpWithEmail(email, password, displayName) }
        }.mapFailure { toAuthException(it) }

    override suspend fun signInWithGoogle(idToken: String): Result<Unit> =
        runSuspendCatching {
            withContext(Dispatchers.IO) { backend.signInWithGoogle(idToken) }
        }.mapFailure { toAuthException(it) }

    override suspend fun getCurrentUser(): User? =
        withContext(Dispatchers.IO) { backend.getCurrentUser() }

    override suspend fun signOut() {
        withContext(Dispatchers.IO) { backend.signOut() }
    }

    /** AuthException от backend проходит как есть, остальное заворачивается в Unexpected. */
    private fun toAuthException(throwable: Throwable): AuthException {
        val cause = (throwable as? ExecutionException)?.cause ?: throwable
        return cause as? AuthException
            ?: AuthException(AuthError.Unexpected(cause.message), cause)
    }
}
