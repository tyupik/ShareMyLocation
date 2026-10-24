package ru.tyupik.sharemylocation.business.notifications

/**
 * Проверка права POST_NOTIFICATIONS и учёт факта его запроса. Системный компонент,
 * поэтому живёт в data-слое (см. NotificationPermissionControllerImpl).
 */
interface NotificationPermissionController {

    /** Право на показ уведомлений выдано (на SDK < 33 право не существует — всегда true). */
    fun isGranted(): Boolean

    /** Системный диалог права ещё не показывался — его стоит запросить. */
    fun shouldRequest(): Boolean

    /** Результат системного диалога; вызывается при любом исходе. */
    fun onRequestResult(granted: Boolean)
}
