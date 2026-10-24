package ru.tyupik.sharemylocation.business.notifications

/**
 * Публикация системных уведомлений. Системный компонент, поэтому живёт в data-слое
 * (см. NotificationPublisherImpl): право POST_NOTIFICATIONS и каналы — его забота.
 */
interface NotificationPublisher {

    /** Публикует уведомление; без права на SDK ≥ 33 реализация молча пропускает публикацию. */
    fun publish(notification: AppNotification)
}
