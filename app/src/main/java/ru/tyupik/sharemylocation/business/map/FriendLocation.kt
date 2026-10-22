package ru.tyupik.sharemylocation.business.map

/**
 * Точка геолокации пользователя: документ locations/{uid} в Firestore.
 * Наполняется из Firestore в data-слое, поэтому не тянет за собой Firebase-типы
 * (см. FirebaseBackend.listenLocationsByUids).
 */
data class FriendLocation(
    val uid: String,
    val latitude: Double,
    val longitude: Double,
    /** Точность точки в метрах; 0.0 — поле отсутствует в документе. */
    val accuracyMeters: Double,
    /** millis последней отправки точки (updatedAt); 0 — поле отсутствует. */
    val updatedAtMillis: Long,
)
