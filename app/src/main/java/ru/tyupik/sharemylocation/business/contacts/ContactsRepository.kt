package ru.tyupik.sharemylocation.business.contacts

/**
 * Контракт чтения телефонной книги через ContactsProvider.
 * READ_CONTACTS не требуется: репозиторий работает только с Uri, полученным от системного
 * контакт-пикера (ACTION_PICK на таблицу телефонов) — вместе с результатом пикера выдаётся
 * временный read-грант на Uri конкретного номера.
 */
interface ContactsRepository {

    /**
     * Читает телефон по [contactUri] (Uri конкретного номера от пикера) «как есть»,
     * без нормализации. Null в успехе — курсора нет или номера пусты; любой сбой чтения —
     * [ContactsError.ReadFailed].
     */
    suspend fun readFirstPhone(contactUri: String): Result<String?>
}
