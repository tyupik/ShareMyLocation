package ru.tyupik.sharemylocation.business.map

import kotlinx.coroutines.flow.Flow

/**
 * Контракт чтения точек геолокаций: коллекция locations в Firestore.
 * Документ ключуется uid владельца точки; текущий пользователь подразумевается неявно,
 * как в [ru.tyupik.sharemylocation.business.sharing.SharingRepository].
 */
interface MapRepository {

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
