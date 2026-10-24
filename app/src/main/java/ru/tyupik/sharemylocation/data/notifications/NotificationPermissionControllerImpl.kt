package ru.tyupik.sharemylocation.data.notifications

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import ru.tyupik.sharemylocation.business.notifications.NotificationPermissionController

/**
 * Проверка права POST_NOTIFICATIONS и флаг «право уже запрашивали».
 * Живёт в data-слое: держит [ApplicationContext] и SharedPreferences.
 * На SDK < 33 право не существует — считается выданным всегда.
 */
@Singleton
class NotificationPermissionControllerImpl @Inject constructor(
    @ApplicationContext private val context: Context,
) : NotificationPermissionController {

    private val preferences by lazy {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    override fun isGranted(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED

    override fun shouldRequest(): Boolean = !preferences.getBoolean(KEY_ASKED, false)

    override fun onRequestResult(granted: Boolean) {
        Log.d(TAG, "onRequestResult: granted=$granted")
        // Флаг ставится при любом результате: после первого показа системного диалога
        // запрос больше не навязывается (отказ — пользователь включит в настройках).
        preferences.edit().putBoolean(KEY_ASKED, true).apply()
    }

    private companion object {
        const val TAG = "NotificationPermCtl"
        const val PREFS_NAME = "notifications"
        const val KEY_ASKED = "notifications_permission_asked"
    }
}
