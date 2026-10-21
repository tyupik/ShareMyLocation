package ru.tyupik.sharemylocation.business.groups

import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** Реализация интерактора групп поверх репозитория. */
@Singleton
class GroupsInteractorImpl @Inject constructor(
    private val groupsRepository: GroupsRepository,
) : GroupsInteractor {

    /** Сортировка групп по имени на клиенте — композитные индексы не требуются. */
    override fun observeGroups(): Flow<List<Group>> = groupsRepository.observeMyGroups()
        .map { groups -> groups.sortedBy { group -> group.name.lowercase() } }

    override suspend fun getGroup(groupId: String): Result<Group> = groupsRepository.getGroup(groupId)

    override suspend fun createGroup(rawName: String, memberUids: List<String>): Result<String> {
        val name = validatedName(rawName)
            .getOrElse { throwable -> return Result.failure(throwable) }
        // Состав валидируется в ViewModel (⊆ актуальных друзей) и авторитетно — create-правилами.
        return groupsRepository.createGroup(name, memberUids)
    }

    override suspend fun updateGroup(
        groupId: String,
        rawName: String,
        memberUids: List<String>,
    ): Result<Unit> {
        val name = validatedName(rawName)
            .getOrElse { throwable -> return Result.failure(throwable) }
        return groupsRepository.updateGroup(groupId, name, memberUids)
    }

    override suspend fun deleteGroup(groupId: String): Result<Unit> = groupsRepository.deleteGroup(groupId)

    /** Trim + валидация имени; неуспех — [GroupsError.EmptyName] либо [GroupsError.NameTooLong]. */
    private fun validatedName(rawName: String): Result<String> {
        val name = rawName.trim()
        if (name.isEmpty()) {
            return Result.failure(GroupsException(GroupsError.EmptyName))
        }
        if (name.length > MAX_GROUP_NAME_LENGTH) {
            return Result.failure(GroupsException(GroupsError.NameTooLong))
        }
        return Result.success(name)
    }

    private companion object {
        /** Лимит имени группы; продублирован литералом 50 в firestore.rules (isGroupName). */
        const val MAX_GROUP_NAME_LENGTH = 50
    }
}
