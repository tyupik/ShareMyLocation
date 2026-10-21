package ru.tyupik.sharemylocation.business.friends

import kotlinx.coroutines.flow.Flow
import ru.tyupik.sharemylocation.business.common.User

/**
 * Интерактор друзей: оркестрирует [FriendsRepository] и возвращает бизнес-модели.
 * Валидирует телефон при поиске и решает, можно ли отправить заявку.
 */
interface FriendsInteractor {

    /**
     * Поиск пользователя по введённому телефону: нормализация и валидация ([FriendsError.InvalidPhone]),
     * пустой результат — [FriendsError.UserNotFound], собственный uid — [FriendsError.RequestToSelf].
     * При коллизии телефонов возвращается первый из списка.
     */
    suspend fun searchByPhone(rawPhone: String): Result<User>

    /** Отправка PENDING-заявки; проверки дубликатов выполняются в репозитории по свежим данным. */
    suspend fun sendFriendRequest(toUid: String): Result<Unit>

    /** Принятие входящей заявки. */
    suspend fun acceptFriendRequest(request: FriendRequest): Result<Unit>

    /** Отклонение входящей заявки (удаление её документа). */
    suspend fun declineFriendRequest(request: FriendRequest): Result<Unit>

    /**
     * Удаление друга из своего списка (и себя из списка друга).
     * TODO(фаза карты/шеринга): каскадно чистить groups/sharingSettings удалённого друга.
     */
    suspend fun removeFriend(friendUid: String): Result<Unit>

    /** Realtime-поток входящих PENDING-заявок, новые сверху. */
    fun observeIncomingFriendRequests(): Flow<List<FriendRequest>>

    /**
     * Realtime-поток списка друзей (по uid из friendUids), отсортированный по displayName.
     * Внутренний кэш профилей живёт, пока жив синглтон-интерактор: перезапрашиваются
     * только новые uid, поэтому смена имени/аватара уже известного друга подтянется
     * только после перезапуска процесса (компромисс плана фичи).
     */
    fun observeFriends(): Flow<List<User>>
}
