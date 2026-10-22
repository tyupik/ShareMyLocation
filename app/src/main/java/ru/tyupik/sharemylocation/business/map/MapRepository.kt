package ru.tyupik.sharemylocation.business.map

import kotlinx.coroutines.flow.Flow
import ru.tyupik.sharemylocation.business.sharing.SharingSettings

/**
 * Контракт чтения точек геолокаций и настроек шаринга друзей: коллекции locations
 * и sharingSettings в Firestore. Документ ключуется uid владельца; текущий пользователь
 * подразумевается неявно, как в [ru.tyupik.sharemylocation.business.sharing.SharingRepository].
 */
interface MapRepository {

    /**
     * Realtime-настройки шаринга пользователей [uids]: документ sharingSettings/{uid} на uid,
     * ключ карты — uid. Отсутствующий или запрещённый правилами документ приходит как
     * SharingSettings.default («не шарит»), не ошибка; ошибка слушателя закрывает flow —
     * перезапуск возможен по повторной подписке (каждый ScreenShown).
     */
    fun observeSharingSettingsByUids(uids: List<String>): Flow<Map<String, SharingSettings>>

    /**
     * Realtime-поток точек локаций пользователей [uids]: ключ карты — uid.
     * Запрос whereIn по documentId чанкуется по 10 (лимит Firestore); чанки сливаются
     * в одну карту, обновление любого чанка переэмитирует объединённый результат.
     * Правило чтения locations использует get(users/...), такой whereIn не provable
     * (rules are not filters), поэтому отклонённый PERMISSION_DENIED чанк автоматически
     * переходит на по-документные слушатели locations/{uid} — контракт от этого не меняется.
     * Иная ошибка любого чанка закрывает flow — перезапуск возможен по повторной подписке
     * (каждый ScreenShown); отсутствующие/запрещённые правилами документы молча
     * не попадают в результат.
     */
    fun observeLocationsByUids(uids: List<String>): Flow<Map<String, FriendLocation>>
}
