package ru.tyupik.sharemylocation.data.sharing

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.location.Location
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlin.concurrent.Volatile
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import ru.tyupik.sharemylocation.R
import ru.tyupik.sharemylocation.business.sharing.SharedLocation
import ru.tyupik.sharemylocation.business.sharing.SharingError
import ru.tyupik.sharemylocation.business.sharing.SharingException
import ru.tyupik.sharemylocation.business.sharing.SharingRepository
import ru.tyupik.sharemylocation.data.auth.FirebaseBackend

/**
 * Foreground-сервис отправки геолокации (Фаза 6): пока документ sharingSettings/{uid} хранит
 * isEnabled = true, каждые [LOCATION_INTERVAL_MS] пишет последнюю координату FusedLocationProvider
 * в locations/{uid} (merge-set). Получателей не вычисляет — только пишет свою точку; выбор
 * адресатов и свежесть (updatedAt) определяет читатель (Фаза 7).
 *
 * Жизненный цикл: старт/стоп — только через [LocationSharingController] (корневой оркестратор
 * AppViewModel по realtime-флагу isEnabled, явный стоп при выходе из аккаунта) и кнопка
 * «Остановить» в уведомлении (ACTION_STOP — персистит isEnabled = false, иначе наблюдатель
 * оживит сервис). После kill процесса система перезапускает сервис (START_STICKY) — каждый
 * onStartCommand проходит ревалидацию прав/сессии/настройки.
 *
 * [Service] вместо LifecycleService: не подключаем lifecycle-service — свой
 * [serviceScope], отменяемый в [onDestroy]. FusedLocationProviderClient создаётся прямо здесь:
 * play-services-location — обычный maven-артефакт, резолвится из Kotlin (в отличие от
 * firebase-auth, см. FirebaseBackend).
 */
@AndroidEntryPoint
class LocationSharingService : Service() {

    @Inject
    lateinit var sharingRepository: SharingRepository

    /** Локальная проверка сессии при ревалидации (без сети, до startForeground). */
    @Inject
    lateinit var backend: FirebaseBackend

    /** Собственный scope сервиса; все записи Firestore и стоп-логика живут в нём. */
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private lateinit var fusedLocationClient: FusedLocationProviderClient

    /** Защита от повторных startForegroundService: подписка на обновления оформляется один раз. */
    @Volatile
    private var isSubscribed = false

    /** Подряд идущие несетевые ошибки записи; серия — признак отзыва прав (см. onPersistFailed). */
    private var consecutiveUnexpectedFailures = 0

    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult) {
            val location = result.lastLocation ?: return
            serviceScope.launch { persistLocation(location) }
        }
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            serviceScope.launch { stopByUser() }
            return START_STICKY
        }
        // Локальная ревалидация (права/сессия, без сети) ДО startForeground: на API 34+
        // startForeground с типом location без прав кидает SecurityException — например,
        // START_STICKY-рестарт после отзыва разрешений.
        if (!hasLocationPermission() || backend.getCurrentUser() == null) {
            Log.w(TAG, "Ревалидация не пройдена: нет прав геолокации или пользователя — стоп")
            stopSelf()
            return START_STICKY
        }
        // Обязательный startForeground сразу после старта: на API 34+ жёсткий таймаут,
        // сетевые проверки — только после него.
        ServiceCompat.startForeground(
            this,
            NOTIFICATION_ID,
            buildNotification(),
            ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION,
        )
        if (!isSubscribed) {
            isSubscribed = true
            serviceScope.launch { validateAndSubscribe() }
        }
        return START_STICKY
    }

    override fun onDestroy() {
        if (this::fusedLocationClient.isInitialized) {
            fusedLocationClient.removeLocationUpdates(locationCallback)
        }
        serviceScope.cancel()
        super.onDestroy()
    }

    /**
     * Ревалидация настройки по свежим данным: ошибка чтения или isEnabled = false — стоп.
     * Сервис доверяет только isEnabled: получателей (allowedUids/группы) вычисляет читатель.
     */
    private suspend fun validateAndSubscribe() {
        val settings = sharingRepository.getSettings().getOrNull()
        if (settings == null || !settings.isEnabled) {
            Log.w(TAG, "Ревалидация не пройдена: настройка выключена или недоступна — стоп")
            isSubscribed = false
            stopSelf()
            return
        }
        startLocationUpdates()
    }

    /** Подписка на обновления локации; HIGH_ACCURACY раз в [LOCATION_INTERVAL_MS] (компромисс плана). */
    private fun startLocationUpdates() {
        val request = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY,
            LOCATION_INTERVAL_MS,
        ).build()
        try {
            fusedLocationClient.requestLocationUpdates(request, locationCallback, mainLooper)
            Log.i(TAG, "Обновления локации оформлены: интервал=$LOCATION_INTERVAL_MS мс")
        } catch (throwable: SecurityException) {
            Log.w(TAG, "Нет прав на обновления локации — останавливаюсь", throwable)
            stopSelf()
        }
    }

    private suspend fun persistLocation(location: Location) {
        val result = sharingRepository.updateLocation(
            SharedLocation(
                latitude = location.latitude,
                longitude = location.longitude,
                accuracyMeters = location.accuracy.toDouble(),
            ),
        )
        result.fold(
            onSuccess = { consecutiveUnexpectedFailures = 0 },
            onFailure = { throwable -> onPersistFailed(throwable) },
        )
    }

    /**
     * Ошибка записи: сеть недоступна — ждём следующего тика; повторная несанкционированная
     * (PERMISSION_DENIED в Unexpected — отозвали права или разлогинили) — стоп, иначе сервис
     * писал бы в никуда каждые 5 секунд.
     */
    private fun onPersistFailed(throwable: Throwable) {
        val error = (throwable as? SharingException)?.error
        if (error is SharingError.NetworkUnavailable) {
            Log.i(TAG, "Не удалось записать локацию: сеть недоступна — ждём следующего тика")
            return
        }
        consecutiveUnexpectedFailures += 1
        Log.w(TAG, "Не удалось записать локацию (подряд: $consecutiveUnexpectedFailures)", throwable)
        val isUnauthorized = error is SharingError.Unexpected &&
            error.reason?.contains(PERMISSION_DENIED_MARKER) == true
        if (isUnauthorized && consecutiveUnexpectedFailures >= MAX_CONSECUTIVE_UNEXPECTED_FAILURES) {
            Log.w(TAG, "Повторная несанкционированная ошибка записи — останавливаю сервис")
            stopSelf()
        }
    }

    /** Кнопка «Остановить»: isEnabled = false персистим обязательно, иначе наблюдатель оживит сервис. */
    private suspend fun stopByUser() {
        try {
            sharingRepository.saveEnabled(false).onFailure { throwable ->
                Log.w(TAG, "Не удалось записать isEnabled=false, но сервис останавливается", throwable)
            }
        } finally {
            stopSelf()
        }
    }

    private fun hasLocationPermission(): Boolean =
        checkSelfPermission(android.Manifest.permission.ACCESS_FINE_LOCATION) ==
            android.content.pm.PackageManager.PERMISSION_GRANTED ||
            checkSelfPermission(android.Manifest.permission.ACCESS_COARSE_LOCATION) ==
                android.content.pm.PackageManager.PERMISSION_GRANTED

    private fun buildNotification(): Notification {
        val stopIntent = PendingIntent.getService(
            this,
            0,
            Intent(this, LocationSharingService::class.java).setAction(ACTION_STOP),
            PendingIntent.FLAG_IMMUTABLE,
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(getString(R.string.sharing_notification_title))
            .setOngoing(true)
            .addAction(0, getString(R.string.sharing_notification_stop_action), stopIntent)
            .build()
    }

    /** Канал уведомления: IMPORTANCE_LOW — постоянное уведомление не должно звучать/вибрировать. */
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val channel = NotificationChannel(
            CHANNEL_ID,
            getString(R.string.sharing_notification_channel_name),
            NotificationManager.IMPORTANCE_LOW,
        ).apply {
            description = getString(R.string.sharing_notification_channel_description)
        }
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    private companion object {
        const val TAG = "LocationSharingService"

        /** Интервал обновлений: 5 c ≈ 17 200 записей/день — безопасно для квот Spark. */
        const val LOCATION_INTERVAL_MS = 5_000L
        const val NOTIFICATION_ID = 1001
        const val CHANNEL_ID = "location_sharing"
        const val ACTION_STOP = "ru.tyupik.sharemylocation.action.STOP_SHARING"

        /** Признак несанкционированности в reason у SharingError.Unexpected (описание кода Firestore). */
        const val PERMISSION_DENIED_MARKER = "PERMISSION_DENIED"

        /** Сколько подряд несанкционированных ошибок пережидаем, прежде чем остановить сервис. */
        const val MAX_CONSECUTIVE_UNEXPECTED_FAILURES = 2
    }
}
