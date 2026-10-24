package ru.tyupik.sharemylocation

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject
import ru.tyupik.sharemylocation.business.notifications.NotificationWatcher

/** Точка инициализации Hilt и запуска наблюдателя локальных уведомлений. */
@HiltAndroidApp
class ShareMyLocationApp : Application() {

    @Inject
    lateinit var notificationWatcher: NotificationWatcher

    override fun onCreate() {
        super.onCreate()
        // Наблюдатель живёт весь срок процесса (идемпотентный старт) и сам следит за аутентификацией.
        notificationWatcher.start()
    }
}
