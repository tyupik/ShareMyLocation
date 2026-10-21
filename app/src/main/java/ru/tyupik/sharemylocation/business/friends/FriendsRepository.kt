package ru.tyupik.sharemylocation.business.friends

import kotlinx.coroutines.flow.Flow
import ru.tyupik.sharemylocation.business.common.User

/**
 * Контракт работы с друзьями и заявками: коллекции users и friendRequests в Firestore.
 * Текущий пользователь подразумевается неявно — как в [ru.tyupik.sharemylocation.business.auth.AuthRepository].
 */
interface FriendsRepository {

    /** Поиск по точному совпадению нормализованного телефона; пустой список — совпадений нет. */
    suspend fun findUsersByPhone(phone: String): Result<List<User>>

    /**
     * Отправляет PENDING-заявку пользователю [toUid]. Проверки AlreadyFriends,
     * RequestAlreadySent и RequestAlreadyIncoming выполняются внутри по свежим данным Firestore.
     */
    suspend fun sendFriendRequest(toUid: String): Result<Unit>

    /** Принимает заявку [request]: статус ACCEPTED + friendUids обоих участников (транзакция). */
    suspend fun acceptFriendRequest(request: FriendRequest): Result<Unit>

    /** Отклоняет заявку [request]: удаление её документа (статус DECLINED не персистится). */
    suspend fun declineFriendRequest(request: FriendRequest): Result<Unit>

    /** Удаляет друга: arrayRemove(friendUids) у обоих участников (транзакция). */
    suspend fun removeFriend(friendUid: String): Result<Unit>

    /** Профили пользователей по uid; чанкование списками по 10 выполняется внутри. */
    suspend fun getUsersByUids(uids: List<String>): Result<List<User>>

    /**
     * Realtime-поток входящих PENDING-заявок текущего пользователя.
     * Без orderBy на сервере: сортировка «новые сверху» выполняется на клиенте (в интеракторе),
     * композитные индексы для фичи не требуются.
     */
    fun observeIncomingFriendRequests(): Flow<List<FriendRequest>>

    /** Realtime-поток списка друзей текущего пользователя (поле friendUids документа users). */
    fun observeFriendUids(): Flow<List<String>>
}
