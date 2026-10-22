package ru.tyupik.sharemylocation.ui.map

/**
 * SSOT вкладки «Карта». Список видимых друзей приходит realtime-потоком (тики 5 с
 * старят «как давно»), [selectedUid] и [isListVisible] переживают рекомпозиции;
 * аватары резолвятся асинхронно, ошибка аватара остаётся плейсхолдером с инициалом.
 */
data class MapUiState(
    /** Друзья, чья локация сейчас видна (маркеры карты и список). */
    val visibleFriends: List<VisibleFriendUiModel> = emptyList(),
    /** Всего друзей — для выбора текста пустого состояния («друзей нет» / «никто не делится»). */
    val friendsCount: Int = 0,
    /** Карта ещё не получила первую эмиссию realtime-потока точек. */
    val isMapLoading: Boolean = true,
    /** Пассивное падение realtime-потока; восстанавливается ScreenShown/Повторить. */
    val isMapLoadFailed: Boolean = false,
    /** Друг, за которым следует камера; повторный тап по нему снимает слежение. */
    val selectedUid: String? = null,
    /** Видимость списка видимых друзей (ModalBottomSheet) — состояние экрана, не remember. */
    val isListVisible: Boolean = false,
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
}

/**
 * One-off события экрана. Пустой по построению: навигаций в фиче нет, а слежение камеры —
 * производная состояния [MapUiState.selectedUid] (CameraPositionState — imperative-API
 * composable-инстанса, Effect не смог бы адресовать его без утечки UI во ViewModel).
 */
sealed class MapEffect
