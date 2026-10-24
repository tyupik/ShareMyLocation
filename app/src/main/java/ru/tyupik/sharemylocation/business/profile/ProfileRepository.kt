package ru.tyupik.sharemylocation.business.profile

import ru.tyupik.sharemylocation.business.common.User

/**
 * Контракт работы с профилем: документы users/{uid} и avatars/{uid} в Firestore.
 * Текущий пользователь подразумевается неявно — как в [ru.tyupik.sharemylocation.business.auth.AuthRepository].
 */
interface ProfileRepository {

    /** Профиль текущего пользователя: данные сессии, дополненные документом users/{uid}. */
    suspend fun getProfile(): Result<User>

    /**
     * Сжимает локальное изображение (максимальная сторона ~1024px, JPEG ~80%)
     * и кладёт копию в кэш приложения; возвращает путь к файлу.
     */
    suspend fun prepareAvatar(localUri: String): Result<String>

    /**
     * Создаёт временный файл в кэше для снимка камеры и возвращает его content-Uri
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

    /** Загружает подготовленный JPEG в документ avatars/{uid}; возвращает новую версию аватара. */
    suspend fun uploadAvatar(cachedPath: String): Result<Long>

    /**
     * Путь к кэшированному файлу аватара пользователя; при промахе кэша байты читаются
     * из avatars/{uid} и складываются в кэш. null — аватара нет
     * (рассинхрон avatarVersion и avatars/{uid}, не ошибка).
     */
    suspend fun getAvatar(uid: String, version: Long): Result<String?>

    /**
     * Сохраняет поля профиля в users/{uid}: displayName, phone, hasCompletedOnboarding
     * и updatedAt. Аватар загружается отдельно через [uploadAvatar].
     */
    suspend fun saveProfile(
        displayName: String,
        phone: String,
        hasCompletedOnboarding: Boolean,
    ): Result<Unit>
}
