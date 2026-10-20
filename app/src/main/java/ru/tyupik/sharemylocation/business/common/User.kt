package ru.tyupik.sharemylocation.business.common

/**
 * Доменная модель пользователя, общая для нескольких экранов.
 * Наполняется из FirebaseAuth в data-слое, поэтому не тянет за собой Firebase-типы.
 */
data class User(
    val uid: String,
    val email: String?,
    val displayName: String?,
)
