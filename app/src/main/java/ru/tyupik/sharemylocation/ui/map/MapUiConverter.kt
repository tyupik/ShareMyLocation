package ru.tyupik.sharemylocation.ui.map

import android.text.format.DateUtils
import javax.inject.Inject
import ru.tyupik.sharemylocation.business.map.VisibleFriend

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
    /** «как давно» — относительное время последней точки; обновляется тиками 5 с и ScreenShown. */
    val lastSeenLabel: String,
)

/** Конвертер бизнес-моделей карты в Ui-модели вкладки «Карта». */
class MapUiConverter @Inject constructor() {

    /**
     * [avatarPath] заполняется ViewModel после дозагрузки аватара; [lastSeenLabel] считается
     * DateUtils.getRelativeTimeSpanString с гранулярностью MINUTE — без новых зависимостей;
     * для остановившейся точки метка «замораживается» до тика/ScreenShown.
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
            lastSeenLabel = DateUtils
                .getRelativeTimeSpanString(friend.updatedAtMillis, nowMillis, DateUtils.MINUTE_IN_MILLIS)
                .toString(),
        )
}
