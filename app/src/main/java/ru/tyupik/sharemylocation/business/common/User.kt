package ru.tyupik.sharemylocation.business.common

/**
 * Доменная модель пользователя, общая для нескольких экранов.
 * Наполняется из FirebaseAuth и Firestore в data-слое, поэтому не тянет за собой Firebase-типы.
 */
data class User(
    val uid: String,
    val email: String?,
    val displayName: String?,
    /** Нормализованный телефон: только цифры с ведущим «+», если он был. */
    val phone: String? = null,
    /**
     * Версия аватара: инкрементируется при каждой загрузке нового JPEG в avatars/{uid}.
     * 0 — аватара нет (legacy-документы с avatarUrl и без avatarVersion читаются как 0).
     */
    val avatarVersion: Long = 0L,
    /** Прошёл ли пользователь онбординг ProfileSetup. */
    val hasCompletedOnboarding: Boolean = false,
)
