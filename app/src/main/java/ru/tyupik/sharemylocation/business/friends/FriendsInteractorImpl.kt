package ru.tyupik.sharemylocation.business.friends

import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import ru.tyupik.sharemylocation.business.auth.AuthInteractor
import ru.tyupik.sharemylocation.business.common.User
import ru.tyupik.sharemylocation.business.profile.PhoneNormalizer

/** Реализация интерактора друзей поверх репозитория. */
@OptIn(ExperimentalCoroutinesApi::class)
@Singleton
class FriendsInteractorImpl @Inject constructor(
    private val friendsRepository: FriendsRepository,
    private val authInteractor: AuthInteractor,
) : FriendsInteractor {

    /** Кэш профилей друзей по uid: исключает повторные запросы для уже известных uid. */
    private val friendsCache = ConcurrentHashMap<String, User>()

    override suspend fun searchByPhone(rawPhone: String): Result<User> {
        val normalizedPhone = PhoneNormalizer.normalize(rawPhone)
        if (!PhoneNormalizer.isValid(normalizedPhone)) {
            return Result.failure(FriendsException(FriendsError.InvalidPhone))
        }
        val candidates = friendsRepository.findUsersByPhone(normalizedPhone)
            .getOrElse { throwable -> return Result.failure(throwable) }
        // При коллизии телефонов берём первого из списка (допущение фичи).
        val user = candidates.firstOrNull()
            ?: return Result.failure(FriendsException(FriendsError.UserNotFound))
        if (user.uid == authInteractor.getCurrentUser()?.uid) {
            return Result.failure(FriendsException(FriendsError.RequestToSelf))
        }
        return Result.success(user)
    }

    override suspend fun sendFriendRequest(toUid: String): Result<Unit> =
        friendsRepository.sendFriendRequest(toUid)

    override suspend fun acceptFriendRequest(request: FriendRequest): Result<Unit> =
        friendsRepository.acceptFriendRequest(request)

    override suspend fun declineFriendRequest(request: FriendRequest): Result<Unit> =
        friendsRepository.declineFriendRequest(request)

    override suspend fun removeFriend(friendUid: String): Result<Unit> =
        friendsRepository.removeFriend(friendUid)

    /** Realtime-поток входящих PENDING-заявок; сортировка новые сверху — на клиенте (без индекса). */
    override fun observeIncomingFriendRequests(): Flow<List<FriendRequest>> =
        friendsRepository.observeIncomingFriendRequests()
            .map { requests -> requests.sortedByDescending(FriendRequest::createdAt) }

    override fun observeFriends(): Flow<List<User>> = friendsRepository.observeFriendUids()
        .mapLatest { uids ->
            val missing = uids.filter { uid -> !friendsCache.containsKey(uid) }
            if (missing.isNotEmpty()) {
                friendsRepository.getUsersByUids(missing).getOrThrow()
                    .forEach { user -> friendsCache[user.uid] = user }
            }
            uids.mapNotNull(friendsCache::get)
                .sortedBy { user -> user.displayName.orEmpty() }
        }
        .distinctUntilChanged()
}
