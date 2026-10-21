package ru.tyupik.sharemylocation.business.profile

/** Типизированные ошибки работы с профилем; замаплены из Firebase в data-слое. */
sealed class ProfileError {
    /** Телефон не соответствует правилу 10–15 цифр после нормализации. */
    data object InvalidPhone : ProfileError()

    /** Нет сети или таймаут (FirebaseNetworkException, IOException). */
    data object NetworkUnavailable : ProfileError()

    /** JPEG аватара превышает лимит документа Firestore (1 МБ) даже после сжатия. */
    data object AvatarTooLarge : ProfileError()

    /** Всё, что не удалось сопоставить с конкретной ошибкой. */
    data class Unexpected(val reason: String?) : ProfileError()
}

/**
 * Доменная ошибка профиля: несёт типизированный [ProfileError] для UiState
 * и исходный Throwable для логирования.
 */
class ProfileException(
    val error: ProfileError,
    cause: Throwable? = null,
) : Exception(error.toString(), cause)
