package ru.tyupik.sharemylocation.data.profile

import android.util.Log
import java.io.File
import java.util.concurrent.ExecutionException
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import ru.tyupik.sharemylocation.business.common.User
import ru.tyupik.sharemylocation.business.profile.ProfileError
import ru.tyupik.sharemylocation.business.profile.ProfileException
import ru.tyupik.sharemylocation.business.profile.ProfileRepository
import ru.tyupik.sharemylocation.data.auth.FirebaseBackend
import ru.tyupik.sharemylocation.data.common.mapFailure
import ru.tyupik.sharemylocation.data.common.runSuspendCatching

/**
 * Реализация профиля поверх [FirebaseBackend], [AvatarCompressor] и [AvatarCache].
 * Вся работа с Firebase вынесена в Java-класс: пайплайн компиляции AGP 9.4 не может
 * резолвить firebase-артефакты из Kotlin-источников (см. комментарий в build.gradle.kts),
 * поэтому Kotlin-слой видит только наш тип [ProfileRepository].
 */
@Singleton
class ProfileRepositoryImpl @Inject constructor(
    private val backend: FirebaseBackend,
    private val avatarCompressor: AvatarCompressor,
    private val avatarCache: AvatarCache,
    private val cameraAvatarSource: CameraAvatarSource,
    private val avatarCropOutputSource: AvatarCropOutputSource,
) : ProfileRepository {

    override suspend fun getProfile(): Result<User> =
        runSuspendCatching {
            withContext(Dispatchers.IO) { backend.getUserProfile() }
        }.mapFailure { toProfileException(it) }

    override suspend fun prepareAvatar(localUri: String): Result<String> =
        runSuspendCatching {
            withContext(Dispatchers.IO) { avatarCompressor.compressToCache(localUri) }
        }.mapFailure { toProfileException(it) }

    /** Создаёт временный файл для снимка камеры и возвращает его content-Uri. */
    override suspend fun createCameraAvatarUri(): Result<String> =
        runSuspendCatching {
            withContext(Dispatchers.IO) { cameraAvatarSource.createCaptureUri() }
        }.mapFailure { toProfileException(it) }

    /** Создаёт пустой файл-результат кропа в кэше и возвращает его file-Uri. */
    override suspend fun createCropAvatarUri(): Result<String> =
        runSuspendCatching {
            withContext(Dispatchers.IO) { avatarCropOutputSource.createOutputUri() }
        }.mapFailure { toProfileException(it) }

    /** Best-effort удаляет временный файл-результат кропа по его file-Uri. */
    override suspend fun deleteCropAvatar(uri: String): Result<Unit> =
        runSuspendCatching {
            withContext(Dispatchers.IO) {
                avatarCropOutputSource.deleteOutput(uri)
                Unit
            }
        }.mapFailure { toProfileException(it) }

    /** Best-effort удаляет временный снимок камеры; сбой удаления не пробрасывается как успех. */
    override suspend fun deleteCameraAvatar(uri: String): Result<Unit> =
        runSuspendCatching {
            withContext(Dispatchers.IO) {
                cameraAvatarSource.deleteCapturedImage(uri)
                Unit
            }
        }.mapFailure { toProfileException(it) }

    override suspend fun uploadAvatar(cachedPath: String): Result<Long> =
        runSuspendCatching {
            withContext(Dispatchers.IO) {
                val newVersion = backend.uploadAvatar(avatarCompressor.readBytes(cachedPath))
                deleteTempFile(cachedPath)
                newVersion
            }
        }.mapFailure { toProfileException(it) }

    override suspend fun getAvatar(uid: String, version: Long): Result<String?> =
        runSuspendCatching {
            withContext(Dispatchers.IO) {
                avatarCache.getCachedPath(uid, version)
                    ?: backend.getAvatarBytes(uid)
                        ?.let { bytes -> avatarCache.cacheBytes(uid, version, bytes) }
            }
        }.mapFailure { toProfileException(it) }

    override suspend fun saveProfile(
        displayName: String,
        phone: String,
        hasCompletedOnboarding: Boolean,
    ): Result<Unit> =
        runSuspendCatching {
            withContext(Dispatchers.IO) {
                backend.saveProfile(displayName, phone, hasCompletedOnboarding)
            }
        }.mapFailure { toProfileException(it) }

    /**
     * Удаляет временный файл из [AvatarCompressor.compressToCache] после успешной загрузки;
     * при неудачной загрузке файл остаётся — нужен для повторного сохранения.
     * Ошибка удаления не влияет на результат загрузки.
     */
    private fun deleteTempFile(cachedPath: String) {
        if (!File(cachedPath).delete()) {
            Log.w(TAG, "Не удалось удалить временный файл аватара: $cachedPath")
        }
    }

    /** ProfileException от backend проходит как есть, остальное заворачивается в Unexpected. */
    private fun toProfileException(throwable: Throwable): ProfileException {
        val cause = (throwable as? ExecutionException)?.cause ?: throwable
        return cause as? ProfileException
            ?: ProfileException(ProfileError.Unexpected(cause.message), cause)
    }

    private companion object {
        const val TAG = "ProfileRepositoryImpl"
    }
}
