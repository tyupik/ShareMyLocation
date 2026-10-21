package ru.tyupik.sharemylocation.business.sharing

/**
 * Контракт работы с настройками шаринга: коллекция sharingSettings в Firestore.
 * Текущий пользователь подразумевается неявно — документ ключуется его uid,
 * как в [ru.tyupik.sharemylocation.business.auth.AuthRepository].
 */
interface SharingRepository {

    /**
     * Одноразовое чтение настроек текущего пользователя. Отсутствующий документ — не ошибка:
     * возвращается [SharingSettings.default] (до первого сохранения документа нет).
     */
    suspend fun getSettings(): Result<SharingSettings>

    /**
     * Записывает полный снимок настроек (merge всех пяти полей, updatedAt = serverTimestamp).
     * Идемпотентна: повторная запись того же снимка не меняет смысл документа.
     */
    suspend fun saveSettings(settings: SharingSettings): Result<Unit>
}
