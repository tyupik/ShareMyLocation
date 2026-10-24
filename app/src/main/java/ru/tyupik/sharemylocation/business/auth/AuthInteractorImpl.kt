package ru.tyupik.sharemylocation.business.auth

import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import ru.tyupik.sharemylocation.business.common.User

/** Реализация интерактора аутентификации поверх репозитория. */
@Singleton
class AuthInteractorImpl @Inject constructor(
    private val authRepository: AuthRepository,
) : AuthInteractor {

    override suspend fun signInWithEmail(email: String, password: String): AuthResult =
        authRepository.signInWithEmail(email, password).toAuthResult()

    override suspend fun signUpWithEmail(email: String, password: String, displayName: String): AuthResult =
        authRepository.signUpWithEmail(email, password, displayName).toAuthResult()

    override suspend fun signInWithGoogle(idToken: String): AuthResult =
        authRepository.signInWithGoogle(idToken).toAuthResult()

    override suspend fun getCurrentUser(): User? = authRepository.getCurrentUser()

    override suspend fun signOut() = authRepository.signOut()

    override fun observeAuthState(): Flow<User?> = authRepository.observeAuthState()

    private fun Result<Unit>.toAuthResult(): AuthResult = fold(
        onSuccess = { AuthResult.Success },
        onFailure = { throwable ->
            val error = (throwable as? AuthException)?.error ?: AuthError.Unexpected(throwable.message)
            AuthResult.Error(error)
        },
    )
}
