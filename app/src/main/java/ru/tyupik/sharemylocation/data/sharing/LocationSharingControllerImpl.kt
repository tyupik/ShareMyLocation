package ru.tyupik.sharemylocation.data.sharing

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import androidx.core.content.ContextCompat.startForegroundService
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import ru.tyupik.sharemylocation.business.sharing.LocationSharingController
import ru.tyupik.sharemylocation.data.auth.FirebaseBackend

/**
 * Реализация контроллера сервиса отправки геолокации. Живёт в data-слое, потому что держит
 * [ApplicationContext] и знает про [FirebaseBackend]: права и сессия проверяются здесь,
 * чтобы «включено без прав» никогда не доходило до старта сервиса (SecurityException на
 * requestLocationUpdates / startForeground с типом location).
 */
@Singleton
class LocationSharingControllerImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val backend: FirebaseBackend,
) : LocationSharingController {

    override fun setSharingEnabled(enabled: Boolean) {
        if (enabled && hasLocationPermission() && backend.getCurrentUser() != null) {
            startForegroundService(
                context,
                Intent(context, LocationSharingService::class.java),
            )
        } else {
            // stopService по не запущенному сервису — no-op; повторный старт, когда включение
            // не подтверждено правами/сессией, подавляем (fail-safe).
            context.stopService(Intent(context, LocationSharingService::class.java))
        }
    }

    override fun hasLocationPermission(): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED
}
