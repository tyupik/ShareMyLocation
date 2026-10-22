package ru.tyupik.sharemylocation.ui.map

import android.text.format.DateUtils
import javax.inject.Inject
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
    /** «как давно» — относительное время последней точки; обновляется тиками 5 с и ScreenShown. */
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
}
