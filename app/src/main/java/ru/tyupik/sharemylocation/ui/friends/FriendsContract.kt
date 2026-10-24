package ru.tyupik.sharemylocation.ui.friends

/**
 * SSOT вкладки «Друзья». Секции «Поиск», «Список» живут в одном состоянии, заявки — в
 * bottom-sheet'е по колокольчику ([isRequestsSheetVisible]); списки приходят realtime-потоками,
 * [searchState] и [busyUid] не зависят друг от друга. Аватары резолвятся асинхронно после
 * основного контента: ошибка аватара не ломает экран, остаётся плейсхолдер с инициалом.
 * Контакт из системного пикера ([FriendsIntent.ContactPicked]) нового состояния не требует:
 * его телефон подставляется в [searchQuery] и переиспользует обычный поиск ([searchState]).
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
    /**
     * Снимок друга, для которого открыт диалог подтверждения удаления; null — диалог скрыт.
     * Транзиентное: вход на вкладку = без диалога (сбрасывается ScreenShown).
     */
    val removeDialogFriend: FriendUiModel? = null,
    /** Видимость bottom-sheet'а заявок. Транзиентное: вход на вкладку = лист закрыт (сбрасывается ScreenShown). */
    val isRequestsSheetVisible: Boolean = false,
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

    /** Нажат колокольчик заявок — открыть bottom-sheet со списком заявок. */
    data object RequestsBellClicked : FriendsIntent()

    /** Bottom-sheet заявок закрыт (свайп, тап вне, назад). */
    data object RequestsSheetDismissed : FriendsIntent()

    data class SearchQueryChanged(val value: String) : FriendsIntent()

    data object SearchClicked : FriendsIntent()

    data object SearchCleared : FriendsIntent()

    /**
     * Системный контакт-пикер вернул результат: [contactUri] — Uri выбранного контакта
     * (читаем первый телефон и запускаем поиск по нему), null — пикер закрыт без выбора,
     * реакция не нужна.
     */
    data class ContactPicked(val contactUri: String?) : FriendsIntent()

    /** Контакт-пикер открыть не удалось (нет приложения контактов) — ошибка в баннер действия. */
    data object ContactPickerUnavailable : FriendsIntent()

    data class AddFriendClicked(val uid: String) : FriendsIntent()

    data class AcceptRequestClicked(val fromUid: String) : FriendsIntent()

    data class DeclineRequestClicked(val fromUid: String) : FriendsIntent()

    /** Нажата кнопка «Удалить из друзей» у друга [uid] — открыть диалог подтверждения. */
    data class RemoveFriendClicked(val uid: String) : FriendsIntent()

    /** Подтверждение удаления в диалоге: удаляем друга из снимка [FriendsUiState.removeDialogFriend]. */
    data object RemoveFriendConfirmed : FriendsIntent()

    /** Диалог подтверждения удаления закрыт без подтверждения. */
    data object RemoveFriendDialogDismissed : FriendsIntent()

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
