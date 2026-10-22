package ru.tyupik.sharemylocation.business.map

/**
 * Маркер отклонённого правилами запроса locations: whereIn-чанк с get()-правилом друга
 * не provable («rules are not filters») — Firestore отклоняет весь запрос целиком
 * PERMISSION_DENIED вместо молчаливой фильтрации. Служит сигналом для fallback
 * на по-документные слушатели locations/{uid} в MapRepositoryImpl (по-документное чтение
 * оценивается правилами по конкретному документу и работает под тем же правилом).
 */
class MapQueryDeniedException(cause: Throwable) : Exception(cause)
