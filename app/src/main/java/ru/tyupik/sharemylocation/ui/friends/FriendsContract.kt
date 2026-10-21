package ru.tyupik.sharemylocation.ui.friends

/**
 * SSOT вкладки «Друзья». Секции «Поиск», «Заявки», «Список» живут в одном состоянии:
 * списки приходят realtime-потоками, [searchState] и [busyUid] не зависят друг от друга.
 * Аватары резолвятся асинхронно после основного контента: ошибка аватара не ломает экран,
 * остаётся плейсхолдер с инициалом.
 */
data class FriendsUiState(
    val friends: List<FriendUiModel> = emptyList(),
    val isFriendsLoading: Boolean = true,
    val incomingRequests: List<FriendRequestUiModel> = emptyList(),
    /** Текущий запрос поиска; вход на вкладку = чистый лист поиска (сбрасывается ScreenShown). */
    val searchQuery: String = "",
    val searchState: FriendsSearchUiState = FriendsSearchUiState.Idle,
    /** uid, с которым сейчас выполняется действие (кнопки показывают прогресс). */
    val busyUid: String? = null,
    /**
     * Ошибка явного действия (поиск/добавить/принять/отклонить/удалить) для баннера;
     * ошибки пассивной загрузки секций сюда не попадают. Сбрасывается по ErrorDismissed
     * и при каждом ScreenShown.
     */
    val actionError: FriendsUiError? = null,
    /** Пассивная недоступность секции «Друзья» (упал realtime-поток); восстанавливается ScreenShown/Повторить. */
    val isFriendsLoadFailed: Boolean = false,
    /** Пассивная недоступность секции «Заявки» (упал realtime-поток); восстанавливается ScreenShown/Повторить. */
    val isRequestsLoadFailed: Boolean = false,
    /** Секции «Группы»: realtime-список групп владельца (имя + число участников). */
    val groups: List<GroupUiModel> = emptyList(),
    val isGroupsLoading: Boolean = true,
    /** Пассивная недоступность секции «Группы» (упал realtime-поток); восстанавливается ScreenShown/Повторить. */
    val isGroupsLoadFailed: Boolean = false,
)

/** Состояние секции поиска. */
sealed interface FriendsSearchUiState {
    data object Idle : FriendsSearchUiState

    data object Loading : FriendsSearchUiState

    data class Found(val user: FriendUiModel) : FriendsSearchUiState

    data object NotFound : FriendsSearchUiState

    data class Error(val error: FriendsUiError) : FriendsSearchUiState
}

/** Действия пользователя на вкладке «Друзья». */
sealed class FriendsIntent {
    /** Вкладка показана: перезапускаем realtime-потоки при каждом входе на вкладку. */
    data object ScreenShown : FriendsIntent()

    data class SearchQueryChanged(val value: String) : FriendsIntent()

    data object SearchClicked : FriendsIntent()

    data object SearchCleared : FriendsIntent()

    data class AddFriendClicked(val uid: String) : FriendsIntent()

    data class AcceptRequestClicked(val fromUid: String) : FriendsIntent()

    data class DeclineRequestClicked(val fromUid: String) : FriendsIntent()

    data class RemoveFriendClicked(val uid: String) : FriendsIntent()

    /** Нажата кнопка «Создать» в секции групп — открыть редактор в режиме создания. */
    data object CreateGroupClicked : FriendsIntent()

    /** Нажата карточка группы [groupId] — открыть редактор этой группы. */
    data class GroupClicked(val groupId: String) : FriendsIntent()

    data object ErrorDismissed : FriendsIntent()
}

/** One-off события экрана. */
sealed class FriendsEffect {
    /** Заявка в друзья успешно отправлена — показываем Snackbar. */
    data object FriendRequestSent : FriendsEffect()

    /** Открыть редактор группы поверх Main; [groupId] == null — режим создания. */
    data class OpenGroupEditor(val groupId: String?) : FriendsEffect()
}
