package ru.tyupik.sharemylocation.data.map

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.util.Log
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import ru.tyupik.sharemylocation.business.map.MyLocationProvider
import ru.tyupik.sharemylocation.business.sharing.SharedLocation

/**
 * Реализация провайдера собственной локации поверх FusedLocationProviderClient
 * (прецедент LocationSharingService: play-services-location резолвится из Kotlin).
 * Все отказы (нет прав, SecurityException, ошибка/таймаут API) преобразуются в null —
 * «фикса нет», вызывающий не трогает камеру; отмена корутины пробрасывается как есть.
 * Битмапы/IO-стратегия не нужны: lastLocation читает кэш, getCurrentLocation —
 * one-shot PRIORITY_BALANCED_POWER_ACCURACY (точность достаточна для центровки карты).
 */
@Singleton
class MyLocationProviderImpl @Inject constructor(
    @ApplicationContext private val context: Context,
) : MyLocationProvider {

    /** Клиент создаётся лениво: первый запрос локации может случиться и не скоро. */
    private val fusedLocationClient: FusedLocationProviderClient by lazy {
        LocationServices.getFusedLocationProviderClient(context)
    }

    override fun hasLocationPermission(): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED

    override suspend fun getLastKnownLocation(): SharedLocation? {
        if (!hasLocationPermission()) return null
        return try {
            withContext(Dispatchers.IO) {
                fusedLocationClient.lastLocation.await()?.toSharedLocation()
            }
        } catch (throwable: CancellationException) {
            throw throwable
        } catch (throwable: Exception) {
            Log.w(TAG, "getLastKnownLocation не удался — считаю фикса нет", throwable)
            null
        }
    }

    override suspend fun getCurrentLocation(): SharedLocation? {
        if (!hasLocationPermission()) return null
        return try {
            withContext(Dispatchers.IO) {
                fusedLocationClient
                    .getCurrentLocation(
                        Priority.PRIORITY_BALANCED_POWER_ACCURACY,
                        CancellationTokenSource().token,
                    )
                    .await()
                    ?.toSharedLocation()
            }
        } catch (throwable: CancellationException) {
            throw throwable
        } catch (throwable: Exception) {
            Log.w(TAG, "getCurrentLocation не удался — считаю фикса нет", throwable)
            null
        }
    }

    private fun Location.toSharedLocation() = SharedLocation(
        latitude = latitude,
        longitude = longitude,
        accuracyMeters = accuracy.toDouble(),
    )

    private companion object {
        const val TAG = "MyLocationProvider"
    }
}
