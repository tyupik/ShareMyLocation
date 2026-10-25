package ru.tyupik.sharemylocation.business.contacts

/**
 * Интерактор телефонной книги: оркестрирует [ContactsRepository] для сценария
 * «выбрать друга из контактов и найти его по телефону».
 */
interface ContactsInteractor {

    /**
     * Телефон выбранного контакта «как есть» (нормализацию выполняет поиск друзей).
     * Телефона нет — [ContactsError.NoPhone], сбой чтения — [ContactsError.ReadFailed].
     */
    suspend fun getPhoneForSearch(contactUri: String): Result<String>
}
