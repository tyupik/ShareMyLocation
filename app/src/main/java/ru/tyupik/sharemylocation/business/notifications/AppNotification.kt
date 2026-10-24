package ru.tyupik.sharemylocation.business.notifications

/** Уведомления приложения, публикуемые локально из realtime-потоков Firestore. */
sealed class AppNotification {

    /** Входящая заявка в друзья от пользователя [fromUid] (снапшот имени — [fromName]). */
    data class FriendRequestReceived(
        val fromUid: String,
        val fromName: String,
    ) : AppNotification()

    /** Друг [uid] начал делиться геопозицией (появился в наборе видимых друзей). */
    data class FriendStartedSharing(
        val uid: String,
        val displayName: String,
    ) : AppNotification()
}
