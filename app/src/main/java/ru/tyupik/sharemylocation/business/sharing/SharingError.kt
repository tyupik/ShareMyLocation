package ru.tyupik.sharemylocation.business.sharing

/** Типизированные ошибки фичи «Шаринг»; замаплены из Firebase в data-слое. */
sealed class SharingError {

    /** Нет сети или таймаут (FirebaseNetworkException, IOException). */
    data object NetworkUnavailable : SharingError()

    /** Всё, что не удалось сопоставить с конкретной ошибкой. */
    data class Unexpected(val reason: String?) : SharingError()

    // «Not found» нет: отсутствие документа sharingSettings/{uid} — не ошибка,
    // а дефолтное состояние до первого сохранения (см. SharingSettings.default).
}

/**
 * Доменная ошибка фичи «Шаринг»: несёт типизированный [SharingError] для UiState
 * и исходный Throwable для логирования.
 */
class SharingException(
    val error: SharingError,
    cause: Throwable? = null,
) : Exception(error.toString(), cause)
