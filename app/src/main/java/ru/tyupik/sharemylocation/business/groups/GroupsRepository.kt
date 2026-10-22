package ru.tyupik.sharemylocation.business.groups

import kotlinx.coroutines.flow.Flow

/**
 * Контракт работы с группами: коллекция groups в Firestore.
 * Текущий пользователь подразумевается неявно — он владелец всех операций,
 * как в [ru.tyupik.sharemylocation.business.auth.AuthRepository].
 */
interface GroupsRepository {

    /**
     * Realtime-поток групп текущего пользователя (поле ownerId == его uid).
     * Без orderBy на сервере: сортировка по имени выполняется на клиенте (в интеракторе),
     * композитные индексы для фичи не требуются.
     */
    fun observeMyGroups(): Flow<List<Group>>

    /**
     * Одноразовое чтение группы [groupId]; несуществующая группа, а также группа, где текущий
     * пользователь не владелец и не участник (memberUids), — GroupsError.GroupNotFound.
     * С Фазы 7 участник группы может читать её документ (проверка видимости карты);
     * пишут группу по-прежнему только владельцы.
     */
    suspend fun getGroup(groupId: String): Result<Group>

    /** Создаёт группу с именем [name] и составом [memberUids]; возвращает её groupId. */
    suspend fun createGroup(name: String, memberUids: List<String>): Result<String>

    /** Обновляет группу [groupId]: имя и полный состав [memberUids]; ownerId/createdAt не меняются. */
    suspend fun updateGroup(groupId: String, name: String, memberUids: List<String>): Result<Unit>

    /** Удаляет группу [groupId]; id приходит только из realtime-списка — документ существует. */
    suspend fun deleteGroup(groupId: String): Result<Unit>
}
