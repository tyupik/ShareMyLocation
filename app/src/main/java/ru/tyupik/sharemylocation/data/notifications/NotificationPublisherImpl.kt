package ru.tyupik.sharemylocation.data.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import ru.tyupik.sharemylocation.R
import ru.tyupik.sharemylocation.business.notifications.AppNotification
import ru.tyupik.sharemylocation.business.notifications.NotificationPublisher

/**
 * Публикация локальных уведомлений о заявках в друзья и шаринге геопозиции друзей.
 * Живёт в data-слое: держит [ApplicationContext] и знает про платформенные
 * NotificationManager/PendingIntent. Право POST_NOTIFICATIONS (SDK ≥ 33) проверяется здесь:
 * без него публикация молча пропускается (повторный запрос права — не здесь).
 */
@Singleton
class NotificationPublisherImpl @Inject constructor(
    @ApplicationContext private val context: Context,
) : NotificationPublisher {

    override fun publish(notification: AppNotification) {
        if (!isNotificationsPermissionGranted()) {
            Log.i(TAG, "publish: нет разрешения POST_NOTIFICATIONS — уведомление пропущено")
            return
        }
        ensureChannels()
        when (notification) {
            is AppNotification.FriendRequestReceived -> publishFriendRequest(notification)
            is AppNotification.FriendStartedSharing -> publishFriendSharing(notification)
        }
    }

    private fun publishFriendRequest(notification: AppNotification.FriendRequestReceived) {
        val text = if (notification.fromName.isBlank()) {
            context.getString(R.string.notification_friend_request_text_no_name)
        } else {
            context.getString(
                R.string.notification_friend_request_text,
                notification.fromName,
            )
        }
        publishNotification(
            notification = notification,
            channelId = FRIEND_REQUESTS_CHANNEL_ID,
            title = context.getString(R.string.notification_friend_request_title),
            text = text,
            idPrefix = FRIEND_REQUEST_NOTIFICATION_ID_PREFIX,
            uid = notification.fromUid,
        )
    }

    private fun publishFriendSharing(notification: AppNotification.FriendStartedSharing) {
        val text = if (notification.displayName.isBlank()) {
            context.getString(R.string.notification_friend_sharing_text_no_name)
        } else {
            context.getString(
                R.string.notification_friend_sharing_text,
                notification.displayName,
            )
        }
        publishNotification(
            notification = notification,
            channelId = FRIEND_SHARING_CHANNEL_ID,
            title = context.getString(R.string.notification_friend_sharing_title),
            text = text,
            idPrefix = FRIEND_SHARING_NOTIFICATION_ID_PREFIX,
            uid = notification.uid,
        )
    }

    /** Публикует уведомление; тап открывает приложение (launch-intent пакета), свайп закрывает. */
    private fun publishNotification(
        notification: AppNotification,
        channelId: String,
        title: String,
        text: String,
        idPrefix: String,
        uid: String,
    ) {
        val contentIntent = context.packageManager.getLaunchIntentForPackage(context.packageName)
        val pendingIntent = contentIntent?.let { intent ->
            PendingIntent.getActivity(
                context,
                notification.hashCode(),
                intent,
                PendingIntent.FLAG_IMMUTABLE,
            )
        }
        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(text)
            .setAutoCancel(true)
        if (pendingIntent != null) {
            builder.setContentIntent(pendingIntent)
        }
        val notificationId = "$idPrefix$uid".hashCode()
        getSystemServiceNotificationManager().notify(notificationId, builder.build())
        Log.d(TAG, "publish: id=$notificationId")
    }

    private fun isNotificationsPermissionGranted(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED

    /** Каналы уведомлений: createNotificationChannel идемпотентен, повторный вызов безопасен. */
    private fun ensureChannels() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = getSystemServiceNotificationManager()
        manager.createNotificationChannel(
            NotificationChannel(
                FRIEND_REQUESTS_CHANNEL_ID,
                context.getString(R.string.notification_channel_friend_requests_name),
                NotificationManager.IMPORTANCE_DEFAULT,
            ).apply {
                description = context.getString(
                    R.string.notification_channel_friend_requests_description,
                )
            },
        )
        manager.createNotificationChannel(
            NotificationChannel(
                FRIEND_SHARING_CHANNEL_ID,
                context.getString(R.string.notification_channel_friend_sharing_name),
                NotificationManager.IMPORTANCE_DEFAULT,
            ).apply {
                description = context.getString(
                    R.string.notification_channel_friend_sharing_description,
                )
            },
        )
    }

    private fun getSystemServiceNotificationManager(): NotificationManager =
        context.getSystemService(NotificationManager::class.java)

    private companion object {
        const val TAG = "NotificationPublisher"

        const val FRIEND_REQUESTS_CHANNEL_ID = "friend_requests"
        const val FRIEND_SHARING_CHANNEL_ID = "friend_sharing"

        /** Префиксы id уведомлений: uid + префикс исключают коллизии типов уведомлений. */
        const val FRIEND_REQUEST_NOTIFICATION_ID_PREFIX = "friend_request_"
        const val FRIEND_SHARING_NOTIFICATION_ID_PREFIX = "friend_sharing_"
    }
}
