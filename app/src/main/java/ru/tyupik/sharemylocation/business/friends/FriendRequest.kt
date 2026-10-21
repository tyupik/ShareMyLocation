package ru.tyupik.sharemylocation.business.friends

/**
 * Статусы заявки в друзья.
 * DECLINED не используется: отклонение заявки — удаление её документа в Firestore.
 */
enum class FriendRequestStatus {
    PENDING,
    ACCEPTED,
}

/**
 * Бизнес-модель заявки в друзья из коллекции friendRequests Firestore.
 * [fromName] и [fromAvatarVersion] — снапшот профиля отправителя на момент отправки.
 * [createdAt] — millis времени создания; 0 — поле отсутствует/рассинхрон,
 * такие заявки при клиентской сортировке оказываются внизу.
 */
data class FriendRequest(
    val fromUid: String,
    val toUid: String,
    val fromName: String,
    val fromAvatarVersion: Long,
    val status: FriendRequestStatus,
    val createdAt: Long = 0L,
)
