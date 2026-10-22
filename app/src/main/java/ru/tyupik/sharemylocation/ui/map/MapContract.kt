package ru.tyupik.sharemylocation.ui.map

/**
 * SSOT вкладки «Карта». Список видимых друзей приходит realtime-потоком (тики 5 с
 * старят «как давно»), [selectedUid] и [isListVisible] переживают рекомпозиции;
 * аватары резолвятся асинхронно, ошибка аватара остаётся плейсхолдером с инициалом.
 */
data class MapUiState(
    /** Друзья, чья локация сейчас видна (маркеры карты и список). */
    val visibleFriends: List<VisibleFriendUiModel> = emptyList(),
    /** Всего друзей. Поле резервируется: после выноса empty-состояния в bottom-sheet списка UI его не читает. */
    val friendsCount: Int = 0,
    /** Карта ещё не получила первую эмиссию realtime-потока точек. */
    val isMapLoading: Boolean = true,
    /** Пассивное падение realtime-потока; восстанавливается ScreenShown/Повторить. */
    val isMapLoadFailed: Boolean = false,
    /** Друг, за которым следует камера; повторный тап по нему снимает слежение. */
    val selectedUid: String? = null,
    /** Видимость списка видимых друзей (ModalBottomSheet) — состояние экрана, не remember. */
    val isListVisible: Boolean = false,
    /**
     * Реальное состояние рантайм-прав геолокации: проверка провайдером на ScreenShown
     * и результат диалога прав. Гейтит MapProperties(isMyLocationEnabled = ...) —
     * без фактических грантов Maps SDK бросит SecurityException, поэтому флаг только
     * из фактических грантов, никогда не «оптимистично».
     */
    val hasLocationPermission: Boolean = false,
    /**
     * Последняя известная моя точка для центровки камеры; null — фикса нет, камеру не трогаем.
     * Каждый свежий фикс несёт новый [MyLocationUiModel.updatedAtMillis], поэтому повторный
     * фикс по тем же координатам перезапускает camera-эффект (автоцентровка/кнопка).
     */
    val myLocation: MyLocationUiModel? = null,
)

/** Действия пользователя на вкладке «Карта». */
sealed class MapIntent {

    /** Вкладка показана: инкремент refreshTrigger — перезапуск потоков и перечитка настроек/групп. */
    data object ScreenShown : MapIntent()

    /**
     * Тап по карточке списка ИЛИ по маркеру: toggle слежения камеры за другом [uid]
     * (повторный тап по выбранному снимает слежение).
     */
    data class FriendSelected(val uid: String) : MapIntent()

    /** Открыт список видимых друзей. */
    data object ShowListClicked : MapIntent()

    /** Список закрыт (свайп, тап вне, назад). */
    data object ListDismissed : MapIntent()

    /**
     * Тап по кнопке центровки «Показать меня»: без прав — Effect
     * [MapEffect.RequestLocationPermissions] и отложенное желание центроваться;
     * с правами — снятие слежения (selectedUid = null, иначе камеру на тике вернёт к другу)
     * и центровка на свежий фикс.
     */
    data object LocateMeClicked : MapIntent()

    /**
     * Результат системного диалога прав геолокации: выдан хотя бы один грант локации.
     * granted = true — обновить [MapUiState.hasLocationPermission] и получить фикс;
     * отказ — hasLocationPermission = false.
     */
    data class LocationPermissionsResult(val granted: Boolean) : MapIntent()
}

/**
 * One-off события экрана. Слежение камеры — производная состояния
 * [MapUiState.selectedUid]/[MapUiState.myLocation] (CameraPositionState — imperative-API
 * composable-инстанса, Effect не смог бы адресовать его без утечки UI во ViewModel),
 * поэтому в Effect попадает только запрос системного диалога прав.
 */
sealed class MapEffect {

    /** Экран запускает лаунчер прав: ACCESS_FINE_LOCATION + ACCESS_COARSE_LOCATION. */
    data object RequestLocationPermissions : MapEffect()
}
