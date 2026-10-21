package ru.tyupik.sharemylocation.business.profile

import ru.tyupik.sharemylocation.business.common.User

/**
 * Интерактор профиля: оркестрирует [ProfileRepository] и возвращает бизнес-модели.
 * Валидирует телефон до записи и решает, нужен ли upload аватара.
 */
interface ProfileInteractor {

    /** Профиль текущего пользователя. */
    suspend fun getProfile(): Result<User>

    /** Сжимает выбранное изображение и возвращает путь к копии в кэше приложения. */
    suspend fun prepareAvatar(localUri: String): Result<String>

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
