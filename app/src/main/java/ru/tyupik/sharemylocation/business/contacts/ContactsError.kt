package ru.tyupik.sharemylocation.business.contacts

/** Типизированные ошибки чтения телефонной книги; замаплены из ContactsProvider в data-слое. */
sealed class ContactsError {

    /** У выбранного контакта нет ни одного телефона (или контакт не найден). */
    data object NoPhone : ContactsError()

    /** ContactsProvider не ответил или упал при чтении (SecurityException, IOException и т.п.). */
    data class ReadFailed(val reason: String?) : ContactsError()
}

/**
 * Доменная ошибка чтения телефонной книги: несёт типизированный [ContactsError] для UiState
 * и исходный Throwable для логирования.
 */
class ContactsException(
    val error: ContactsError,
    cause: Throwable? = null,
) : Exception(error.toString(), cause)
