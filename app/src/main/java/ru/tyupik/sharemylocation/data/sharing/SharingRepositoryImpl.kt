package ru.tyupik.sharemylocation.data.sharing

import java.util.concurrent.ExecutionException
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import ru.tyupik.sharemylocation.business.sharing.SharingError
import ru.tyupik.sharemylocation.business.sharing.SharingException
import ru.tyupik.sharemylocation.business.sharing.SharingRepository
import ru.tyupik.sharemylocation.business.sharing.SharingSettings
import ru.tyupik.sharemylocation.data.auth.FirebaseBackend
import ru.tyupik.sharemylocation.data.common.mapFailure
import ru.tyupik.sharemylocation.data.common.runSuspendCatching

/**
 * Реализация настроек шаринга поверх [FirebaseBackend].
 * Вся работа с Firestore вынесена в Java-класс: пайплайн компиляции AGP 9.4 не может
 * резолвить firebase-артефакты из Kotlin-источников, поэтому Kotlin-слой видит
 * только наш тип [SharingRepository] (см. KDoc FriendsRepositoryImpl).
 */
@Singleton
class SharingRepositoryImpl @Inject constructor(
    private val backend: FirebaseBackend,
) : SharingRepository {

    override suspend fun getSettings(): Result<SharingSettings> =
        runSuspendCatching {
            withContext(Dispatchers.IO) { backend.getSharingSettings() }
        }.mapFailure { toSharingException(it) }

    override suspend fun saveSettings(settings: SharingSettings): Result<Unit> =
        runSuspendCatching {
            withContext(Dispatchers.IO) {
                backend.saveSharingSettings(
                    settings.isEnabled,
                    settings.audience.name,
                    settings.allowedUids,
                    settings.allowedGroupIds,
                )
            }
        }.mapFailure { toSharingException(it) }

    /** SharingException от backend проходит как есть, остальное заворачивается в Unexpected. */
    private fun toSharingException(throwable: Throwable): SharingException {
        val cause = (throwable as? ExecutionException)?.cause ?: throwable
        return cause as? SharingException
            ?: SharingException(SharingError.Unexpected(cause.message), cause)
    }
}
