package ru.tyupik.sharemylocation.business.sharing

/**
 * Бизнес-модель точки геолокации владельца: документ locations/{uid} в Firestore (Фаза 6).
 * Записывается foreground-сервисом каждые ~5 секунд merge-set'ом {geo, accuracy, updatedAt};
 * получателей сервис не вычисляет — свежесть (updatedAt) и адресатов определяет читатель (Фаза 7).
 */
data class SharedLocation(
    val latitude: Double,
    val longitude: Double,
    /** Радиус точности в метрах по данным провайдера; в правилах не валидируется. */
    val accuracyMeters: Double,
)
