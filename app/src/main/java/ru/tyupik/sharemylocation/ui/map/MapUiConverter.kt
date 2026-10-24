package ru.tyupik.sharemylocation.ui.map

import android.content.Context
import android.text.format.DateUtils
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import ru.tyupik.sharemylocation.R
import ru.tyupik.sharemylocation.business.map.VisibleFriend
import ru.tyupik.sharemylocation.business.sharing.SharedLocation

/**
 * Ui-модель видимого друга: точка карты + аватар (резолвится асинхронно) + «как давно».
 */
data class VisibleFriendUiModel(
    val uid: String,
    val displayName: String,
    val avatarVersion: Long,
    /** Путь к файлу аватара в кэше приложения; null — плейсхолдер с инициалом. */
    val avatarPath: String?,
    val latitude: Double,
    val longitude: Double,
    val accuracyMeters: Double,
    val updatedAtMillis: Long,
    /** «как давно»: «Только что» (≤ 30 с), «30 секунд назад» (30–60 с), минуты (≥ 60 с);
     * обновляется тиками 5 с и ScreenShown.
     */
    val lastSeenLabel: String,
)

/**
 * Последняя известная точка текущего пользователя для центровки камеры (без maps-типов
 * в контракте). [updatedAtMillis] — момент получения фикса, а не данных Firestore:
 * StateFlow дедуплицирует равные data-классы, и без «свежего» поля повторный фикс по тем
 * же координатам (неподвижный пользователь) не перезапустил бы camera-эффект — не сработали
 * бы ни кнопка «Показать меня», ни автоцентровка при входе на вкладку.
 */
data class MyLocationUiModel(
    val latitude: Double,
    val longitude: Double,
    val updatedAtMillis: Long,
)

/** Конвертер бизнес-моделей карты в Ui-модели вкладки «Карта». */
class MapUiConverter @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    /**
     * [avatarPath] заполняется ViewModel после дозагрузки аватара; «как давно» считается
     * [lastSeenLabel]: «Только что» (≤ 30 с), «30 секунд назад» (30–60 с), далее минуты через
     * DateUtils.getRelativeTimeSpanString (гранулярность MINUTE); для остановившейся точки
     * метка «замораживается» до тика/ScreenShown.
     */
    fun convertVisibleFriend(friend: VisibleFriend, nowMillis: Long): VisibleFriendUiModel =
        VisibleFriendUiModel(
            uid = friend.uid,
            displayName = friend.displayName,
            avatarVersion = friend.avatarVersion,
            avatarPath = null,
            latitude = friend.latitude,
            longitude = friend.longitude,
            accuracyMeters = friend.accuracyMeters,
            updatedAtMillis = friend.updatedAtMillis,
            lastSeenLabel = lastSeenLabel(friend.updatedAtMillis, nowMillis),
        )

    /**
     * Метка «как давно» по [updatedAtMillis] относительно [nowMillis], три ветки:
     * - ≤ [JUST_NOW_THRESHOLD_MS] (включая точки из будущего, elapsed < 0) — «Только что»;
     * - < [DateUtils.MINUTE_IN_MILLIS] — фиксированная «30 секунд назад» (без плюрализации);
     * - иначе — DateUtils с гранулярностью MINUTE (следует локали устройства).
     * Границы: ровно 30 000 мс → «Только что», ровно 60 000 мс → минуты; из-за тикера 5 с
     * пограничные состояния могут держаться до 5 с дольше — приемлемо.
     */
    private fun lastSeenLabel(updatedAtMillis: Long, nowMillis: Long): String {
        val elapsedMillis = nowMillis - updatedAtMillis
        return when {
            elapsedMillis <= JUST_NOW_THRESHOLD_MS -> context.getString(R.string.map_last_seen_just_now)
            elapsedMillis < DateUtils.MINUTE_IN_MILLIS -> context.getString(R.string.map_last_seen_30s)
            else -> DateUtils
                .getRelativeTimeSpanString(updatedAtMillis, nowMillis, DateUtils.MINUTE_IN_MILLIS)
                .toString()
        }
    }

    /**
     * Фикс собственной локации ([SharedLocation] из business.sharing переиспользуется
     * провайдером) в Ui-модель центровки; точность карте для центровки не нужна.
     */
    fun convertMyLocation(location: SharedLocation, nowMillis: Long): MyLocationUiModel =
        MyLocationUiModel(
            latitude = location.latitude,
            longitude = location.longitude,
            updatedAtMillis = nowMillis,
        )

    private companion object {
        /** Порог «Только что»: elapsed ≤ 30 с (верхняя граница минутной ветки — MINUTE_IN_MILLIS). */
        const val JUST_NOW_THRESHOLD_MS = 30_000L
    }
}
