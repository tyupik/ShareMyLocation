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
