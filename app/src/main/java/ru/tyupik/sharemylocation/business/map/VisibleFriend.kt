package ru.tyupik.sharemylocation.business.map

/**
 * Друг, чья локация сейчас видна текущему пользователю: точка [FriendLocation],
 * дополненная профилем (имя, версия аватара) из realtime-списка друзей.
 * Видимость определяется семантикой Фазы 5 (см. KDoc MapInteractor).
 */
data class VisibleFriend(
    val uid: String,
    val displayName: String,
    val avatarVersion: Long,
    val latitude: Double,
    val longitude: Double,
    val accuracyMeters: Double,
    val updatedAtMillis: Long,
)
