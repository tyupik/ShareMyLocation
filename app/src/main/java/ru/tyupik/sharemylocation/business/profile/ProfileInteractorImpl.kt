package ru.tyupik.sharemylocation.business.profile

import javax.inject.Inject
import javax.inject.Singleton
import ru.tyupik.sharemylocation.business.common.User

/** Реализация интерактора профиля поверх репозитория. */
@Singleton
class ProfileInteractorImpl @Inject constructor(
    private val profileRepository: ProfileRepository,
) : ProfileInteractor {

    override suspend fun getProfile(): Result<User> = profileRepository.getProfile()

    override suspend fun prepareAvatar(localUri: String): Result<String> =
        profileRepository.prepareAvatar(localUri)

    override suspend fun createCameraAvatarUri(): Result<String> =
        profileRepository.createCameraAvatarUri()

    override suspend fun deleteCameraAvatar(uri: String): Result<Unit> =
        profileRepository.deleteCameraAvatar(uri)

    override suspend fun createCropAvatarUri(): Result<String> =
        profileRepository.createCropAvatarUri()

    override suspend fun deleteCropAvatar(uri: String): Result<Unit> =
        profileRepository.deleteCropAvatar(uri)

    override suspend fun getAvatar(uid: String, version: Long): Result<AvatarImage> {
        if (version <= 0L) {
            return Result.success(AvatarImage(uid = uid, version = version, cachedPath = null))
        }
        return profileRepository.getAvatar(uid, version).map { cachedPath ->
            AvatarImage(uid = uid, version = version, cachedPath = cachedPath)
        }
    }

    override suspend fun saveProfile(
        displayName: String,
        phone: String,
        pendingAvatarPath: String?,
    ): Result<Unit> {
        val normalizedPhone = PhoneNormalizer.normalize(phone)
        if (!PhoneNormalizer.isValid(normalizedPhone)) {
            return Result.failure(ProfileException(ProfileError.InvalidPhone))
        }

        pendingAvatarPath?.let { path ->
            profileRepository.uploadAvatar(path).getOrElse { return Result.failure(it) }
        }
        return profileRepository.saveProfile(
            displayName = displayName.trim(),
            phone = normalizedPhone,
            hasCompletedOnboarding = true,
        )
    }
}
