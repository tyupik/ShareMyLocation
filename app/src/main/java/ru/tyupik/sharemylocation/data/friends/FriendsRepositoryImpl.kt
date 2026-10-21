package ru.tyupik.sharemylocation.data.friends

import java.util.concurrent.ExecutionException
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.withContext
import ru.tyupik.sharemylocation.business.common.User
import ru.tyupik.sharemylocation.business.friends.FriendRequest
import ru.tyupik.sharemylocation.business.friends.FriendRequestStatus
import ru.tyupik.sharemylocation.business.friends.FriendsError
import ru.tyupik.sharemylocation.business.friends.FriendsException
import ru.tyupik.sharemylocation.business.friends.FriendsRepository
import ru.tyupik.sharemylocation.data.auth.FirebaseBackend
import ru.tyupik.sharemylocation.data.common.mapFailure
import ru.tyupik.sharemylocation.data.common.runSuspendCatching

/**
 * Реализация друзей поверх [FirebaseBackend].
 * Вся работа с Firestore вынесена в Java-класс: пайплайн компиляции AGP 9.4 не может
 * резолвить firebase-артефакты из Kotlin-источников, поэтому Kotlin-слой видит
 * только наш тип [FriendsRepository].
 *
 * Realtime-слушатели backend'а проброшены наружу через callbackFlow: данные уходят
 * в канал через trySend, ошибка слушателя закрывает flow — перезапуск возможен
 * по повторной подписке (например, по ScreenShown).
 */
@Singleton
class FriendsRepositoryImpl @Inject constructor(
    private val backend: FirebaseBackend,
) : FriendsRepository {

    override suspend fun findUsersByPhone(phone: String): Result<List<User>> =
        runSuspendCatching {
            withContext(Dispatchers.IO) { backend.findUsersByPhone(phone) }
        }.mapFailure { toFriendsException(it) }

    /**
     * Отправляет PENDING-заявку пользователю [toUid]. Препроверки — по свежим данным Firestore,
     * не по кэшам UI: уже друзья → AlreadyFriends; висящая PENDING-заявка (своя или встречная)
     * → RequestAlreadySent/RequestAlreadyIncoming.
     * Существующий ACCEPTED-документ пары — история последней заявки, барьером НЕ является:
     * перед create stale-документы пары удаляются, затем заявка создаётся чистым create
     * (set() на существующий документ оценивается правилами как update и был бы запрещён).
     * Последовательность delete→create не атомарна: разрыв между операциями оставляет
     * «нет документа» — повторная попытка чисто создаёт его заново. Приемлемо.
     * История пары не сохраняется: документ заявки отражает только последнюю заявку
     * (осознанная цена).
     */
    override suspend fun sendFriendRequest(toUid: String): Result<Unit> = runSuspendCatching {
        withContext(Dispatchers.IO) {
            val myUid = requireMyUid()
            if (toUid in backend.getMyFriendUids()) {
                throw FriendsException(FriendsError.AlreadyFriends)
            }
            val outgoing = backend.getFriendRequest(myUid, toUid)
            if (outgoing != null && outgoing.status == FriendRequestStatus.PENDING) {
                throw FriendsException(FriendsError.RequestAlreadySent)
            }
            val incoming = backend.getFriendRequest(toUid, myUid)
            if (incoming != null && incoming.status == FriendRequestStatus.PENDING) {
                throw FriendsException(FriendsError.RequestAlreadyIncoming)
            }
            // Stale ACCEPTED-документы удаляем, иначе create по тому же id стал бы update.
            if (outgoing != null) {
                backend.deleteFriendRequest(myUid, toUid)
            }
            if (incoming != null) {
                backend.deleteFriendRequest(toUid, myUid)
            }
            backend.sendFriendRequest(toUid)
        }
    }.mapFailure { toFriendsException(it) }

    override suspend fun acceptFriendRequest(request: FriendRequest): Result<Unit> =
        runSuspendCatching {
            withContext(Dispatchers.IO) { backend.acceptFriendRequest(request.fromUid) }
        }.mapFailure { toFriendsException(it) }

    override suspend fun declineFriendRequest(request: FriendRequest): Result<Unit> =
        runSuspendCatching {
            withContext(Dispatchers.IO) { backend.declineFriendRequest(request.fromUid) }
        }.mapFailure { toFriendsException(it) }

    override suspend fun removeFriend(friendUid: String): Result<Unit> =
        runSuspendCatching {
            withContext(Dispatchers.IO) { backend.removeFriend(friendUid) }
        }.mapFailure { toFriendsException(it) }

    override suspend fun getUsersByUids(uids: List<String>): Result<List<User>> =
        runSuspendCatching {
            withContext(Dispatchers.IO) {
                // whereIn Firestore принимает максимум 10 идентификаторов на запрос.
                uids.chunked(MAX_WHERE_IN_UIDS).flatMap { chunk -> backend.getUsersByUids(chunk) }
            }
        }.mapFailure { toFriendsException(it) }

    override fun observeIncomingFriendRequests(): Flow<List<FriendRequest>> = callbackFlow {
        val registration = try {
            backend.listenIncomingFriendRequests { requests, error ->
                if (error != null) {
                    close(toFriendsException(error))
                } else {
                    trySend(requests.orEmpty())
                }
            }
        } catch (throwable: Throwable) {
            close(toFriendsException(throwable))
            null
        }
        awaitClose { registration?.remove() }
    }

    override fun observeFriendUids(): Flow<List<String>> = callbackFlow {
        val registration = try {
            backend.listenMyFriendUids { uids, error ->
                if (error != null) {
                    close(toFriendsException(error))
                } else {
                    trySend(uids.orEmpty())
                }
            }
        } catch (throwable: Throwable) {
            close(toFriendsException(throwable))
            null
        }
        awaitClose { registration?.remove() }
    }

    private fun requireMyUid(): String =
        backend.getCurrentUser()?.uid
            ?: throw FriendsException(FriendsError.Unexpected("Нет текущего пользователя Firebase"))

    /** FriendsException от backend проходит как есть, остальное заворачивается в Unexpected. */
    private fun toFriendsException(throwable: Throwable): FriendsException {
        val cause = (throwable as? ExecutionException)?.cause ?: throwable
        return cause as? FriendsException
            ?: FriendsException(FriendsError.Unexpected(cause.message), cause)
    }

    private companion object {
        /** Лимит Firestore на whereIn по documentId. */
        const val MAX_WHERE_IN_UIDS = 10
    }
}
