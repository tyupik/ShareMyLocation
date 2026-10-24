package ru.tyupik.sharemylocation.business.contacts

/**
 * Контракт чтения телефонной книги через ContactsProvider.
 * READ_CONTACTS не требуется: репозиторий работает только с Uri, полученным от системного
 * контакт-пикера (ACTION_PICK), — вместе с результатом пикера выдаётся временный read-грант
 * на этот конкретный контакт.
 */
interface ContactsRepository {

    /**
     * Читает первый телефон контакта [contactUri] «как есть», без нормализации.
     * Приоритет номера: IS_SUPER_PRIMARY, затем IS_PRIMARY; null в успехе — у контакта
     * нет телефона (или контакт по Uri не найден). Любой сбой чтения — [ContactsError.ReadFailed].
     */
    suspend fun readFirstPhone(contactUri: String): Result<String?>
}
