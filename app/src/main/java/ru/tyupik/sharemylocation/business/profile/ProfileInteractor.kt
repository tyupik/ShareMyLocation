package ru.tyupik.sharemylocation.business.profile

import ru.tyupik.sharemylocation.business.common.User

/**
 * Интерактор профиля: оркестрирует [ProfileRepository] и возвращает бизнес-модели.
 * Валидирует телефон до записи и решает, нужен ли upload аватара.
 */
interface ProfileInteractor {

    /** Профиль текущего пользователя. */
    suspend fun getProfile(): Result<User>

    /**
     * Сжимает выбранное изображение и возвращает путь к копии в кэше приложения.
     */
    suspend fun prepareAvatar(localUri: String): Result<String>

    /**
     * Создаёт временный файл для снимка камеры и возвращает его content-Uri
     * (FileProvider) для запуска TakePicture; удаляется через [deleteCameraAvatar].
     */
    suspend fun createCameraAvatarUri(): Result<String>

    /** Best-effort удаляет временный снимок камеры по его content-Uri. */
    suspend fun deleteCameraAvatar(uri: String): Result<Unit>

    /**
     * Формирует путь к файлу-результату кругового кадрирования в кэше и возвращает его
     * file-Uri (customOutputUri для CropImageActivity); удаляется через [deleteCropAvatar].
     */
    suspend fun createCropAvatarUri(): Result<String>

    /** Best-effort удаляет временный файл-результат кадрирования по его file-Uri. */
    suspend fun deleteCropAvatar(uri: String): Result<Unit>

    /**
     * Резолвит аватар пользователя как [AvatarImage]; версия 0 и меньше означает,
     * что аватара нет — вернётся модель с cachedPath = null без похода в репозиторий.
     */
    suspend fun getAvatar(uid: String, version: Long): Result<AvatarImage>

    /**
     * Сохраняет профиль: нормализует и валидирует телефон, при необходимости загружает
     * аватар из [pendingAvatarPath]; завершает онбординг (hasCompletedOnboarding = true).
     */
    suspend fun saveProfile(
        displayName: String,
        phone: String,
        pendingAvatarPath: String?,
    ): Result<Unit>
}
