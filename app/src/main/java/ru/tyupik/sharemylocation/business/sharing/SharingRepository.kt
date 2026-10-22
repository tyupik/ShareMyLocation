package ru.tyupik.sharemylocation.business.sharing

import kotlinx.coroutines.flow.Flow

/**
 * Контракт работы с настройками шаринга и точкой геолокации: коллекции sharingSettings
 * и locations в Firestore. Текущий пользователь подразумевается неявно — документ ключуется
 * его uid, как в [ru.tyupik.sharemylocation.business.auth.AuthRepository].
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

    /**
     * Записывает точку геолокации текущего пользователя merge-set'ом locations/{uid}
     * (только {geo, accuracy, updatedAt}); настройки и получателей не трогает.
     */
    suspend fun updateLocation(location: SharedLocation): Result<Unit>

    /**
     * Меняет только {isEnabled, updatedAt} в sharingSettings/{uid} (merge): частичная запись,
     * чтобы сервис и уведомление не перезаписали audience/allowedUids снимком автосейва.
     */
    suspend fun saveEnabled(isEnabled: Boolean): Result<Unit>

    /**
     * Realtime-поток флага isEnabled текущего пользователя; отсутствующий документ — false.
     * Ошибка слушателя (сеть, PERMISSION_DENIED после logout, смерть слушателя) закрывает flow.
     */
    fun observeIsEnabled(): Flow<Boolean>
}
