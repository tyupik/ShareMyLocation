package ru.tyupik.sharemylocation.business.profile

/**
 * Бизнес-модель аватара пользователя.
 * [cachedPath] — путь к файлу JPEG в кэше приложения; null — аватара нет
 * (документ avatars/{uid} отсутствует при avatarVersion > 0 — рассинхрон, не ошибка).
 */
data class AvatarImage(
    val uid: String,
    val version: Long,
    val cachedPath: String?,
)
