package ru.tyupik.sharemylocation.data.groups

import java.util.concurrent.ExecutionException
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.withContext
import ru.tyupik.sharemylocation.business.groups.Group
import ru.tyupik.sharemylocation.business.groups.GroupsError
import ru.tyupik.sharemylocation.business.groups.GroupsException
import ru.tyupik.sharemylocation.business.groups.GroupsRepository
import ru.tyupik.sharemylocation.data.auth.FirebaseBackend
import ru.tyupik.sharemylocation.data.common.mapFailure
import ru.tyupik.sharemylocation.data.common.runSuspendCatching

/**
 * Реализация групп поверх [FirebaseBackend].
 * Вся работа с Firestore вынесена в Java-класс: пайплайн компиляции AGP 9.4 не может
 * резолвить firebase-артефакты из Kotlin-источников, поэтому Kotlin-слой видит
 * только наш тип [GroupsRepository] (см. KDoc FriendsRepositoryImpl).
 *
 * Realtime-слушатель backend'а проброшен наружу через callbackFlow: данные уходят
 * в канал через trySend, ошибка слушателя закрывает flow — перезапуск возможен
 * по повторной подписке (например, по ScreenShown).
 */
@Singleton
class GroupsRepositoryImpl @Inject constructor(
    private val backend: FirebaseBackend,
) : GroupsRepository {

    override fun observeMyGroups(): Flow<List<Group>> = callbackFlow {
        val registration = try {
            backend.listenMyGroups { groups, error ->
                if (error != null) {
                    close(toGroupsException(error))
                } else {
                    trySend(groups.orEmpty())
                }
            }
        } catch (throwable: Throwable) {
            close(toGroupsException(throwable))
            null
        }
        awaitClose { registration?.remove() }
    }

    override suspend fun getGroup(groupId: String): Result<Group> =
        runSuspendCatching {
            withContext(Dispatchers.IO) { backend.getGroup(groupId) }
        }.mapFailure { toGroupsException(it) }

    override suspend fun createGroup(name: String, memberUids: List<String>): Result<String> =
        runSuspendCatching {
            withContext(Dispatchers.IO) { backend.createGroup(name, memberUids) }
        }.mapFailure { toGroupsException(it) }

    override suspend fun updateGroup(
        groupId: String,
        name: String,
        memberUids: List<String>,
    ): Result<Unit> = runSuspendCatching {
        withContext(Dispatchers.IO) { backend.updateGroup(groupId, name, memberUids) }
    }.mapFailure { toGroupsException(it) }

    override suspend fun deleteGroup(groupId: String): Result<Unit> =
        runSuspendCatching {
            withContext(Dispatchers.IO) { backend.deleteGroup(groupId) }
        }.mapFailure { toGroupsException(it) }

    /** GroupsException от backend проходит как есть, остальное заворачивается в Unexpected. */
    private fun toGroupsException(throwable: Throwable): GroupsException {
        val cause = (throwable as? ExecutionException)?.cause ?: throwable
        return cause as? GroupsException
            ?: GroupsException(GroupsError.Unexpected(cause.message), cause)
    }
}
