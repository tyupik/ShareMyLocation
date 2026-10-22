package ru.tyupik.sharemylocation.business.groups

import kotlinx.coroutines.flow.Flow

/**
 * Интерактор групп: оркестрирует [GroupsRepository] и возвращает бизнес-модели.
 * Валидирует имя группы при создании и переименовании: trim; пустое после trim —
 * [GroupsError.EmptyName], длиннее 50 символов — [GroupsError.NameTooLong].
 */
interface GroupsInteractor {

    /** Realtime-поток групп текущего пользователя, отсортированный по имени (lowercase). */
    fun observeGroups(): Flow<List<Group>>

    /**
     * Одноразовое чтение группы; несуществующая, а также группа, где текущий пользователь
     * не владелец и не участник (memberUids), — [GroupsError.GroupNotFound]. С Фазы 7 участник
     * группы может читать её документ (проверка видимости карты).
     */
    suspend fun getGroup(groupId: String): Result<Group>

    /** Создание группы с именем [rawName] и составом [memberUids]; возвращает groupId созданной группы. */
    suspend fun createGroup(rawName: String, memberUids: List<String>): Result<String>

    /** Обновление группы: имя [rawName] (trim, валидация) и полный состав [memberUids]. */
    suspend fun updateGroup(groupId: String, rawName: String, memberUids: List<String>): Result<Unit>

    /** Удаление группы. */
    suspend fun deleteGroup(groupId: String): Result<Unit>
}
