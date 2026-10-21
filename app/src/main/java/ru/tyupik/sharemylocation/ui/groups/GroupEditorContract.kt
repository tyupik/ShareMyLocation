package ru.tyupik.sharemylocation.ui.groups

/**
 * SSOT экрана управления группой: один экран на создание ([GroupEditorUiState.groupId] == null)
 * и редактирование. Состав выбирается чекбоксами из друзей владельца; запись в Firestore —
 * только по SaveClicked/DeleteConfirmed.
 */
data class GroupEditorUiState(
    /** null — создание группы, иначе — id редактируемой группы. */
    val groupId: String? = null,
    /** Первичная загрузка (группа + друзья); при каждом ScreenShown снова true. */
    val isLoading: Boolean = true,
    /**
     * Экран-ошибка загрузки (группа не прочитана или упал realtime-поток друзей);
     * «Повторить» — повторный ScreenShown. Причина уточняется [loadError].
     */
    val isLoadFailed: Boolean = false,
    /** Конкретная причина экрана-ошибки; null — generic-текст (падение потока друзей). */
    val loadError: GroupsUiError? = null,
    val name: String = "",
    /** Все друзья владельца; [GroupMemberUiModel.isChecked] — вхождение в группу на экране. */
    val friends: List<GroupMemberUiModel> = emptyList(),
    /** Сбрасывается при каждом ScreenShown — вход на экран не должен наследовать занятость пережившей ViewModel. */
    val isSaving: Boolean = false,
    /** Сбрасывается при каждом ScreenShown — вход на экран не должен наследовать занятость пережившей ViewModel. */
    val isDeleting: Boolean = false,
    /** Транзиентное UI-состояние, вход = без диалога. */
    val isDeleteDialogVisible: Boolean = false,
    /**
     * Ошибка явного действия (сохранить/удалить) для баннера; ошибки загрузки сюда
     * не попадают (isLoadFailed). Сбрасывается по ErrorDismissed и при каждом ScreenShown.
     */
    val actionError: GroupsUiError? = null,
)

/** Действия пользователя на экране управления группой. */
sealed class GroupEditorIntent {

    /**
     * Экран показан: в режиме редактирования — одноразовая перечитка группы, в обоих режимах —
     * перезапуск realtime-потока друзей. Повтор по «Повторить» — тот же intent с тем же groupId.
     */
    data class ScreenShown(val groupId: String?) : GroupEditorIntent()

    data class NameChanged(val value: String) : GroupEditorIntent()

    /** Локальное переключение чекбокса участника; запись в Firestore — только по SaveClicked. */
    data class MemberToggled(val uid: String) : GroupEditorIntent()

    /**
     * Сохранение: создание — createGroup(имя + выбранный состав),
     * редактирование — updateGroup(имя + весь memberUids).
     */
    data object SaveClicked : GroupEditorIntent()

    /** Запрос удаления: показывает диалог подтверждения. */
    data object DeleteClicked : GroupEditorIntent()

    /** Подтверждение удаления в диалоге. */
    data object DeleteConfirmed : GroupEditorIntent()

    data object DeleteDialogDismissed : GroupEditorIntent()

    data object ErrorDismissed : GroupEditorIntent()
}

/** One-off события экрана. */
sealed class GroupEditorEffect {

    /** Сохранение или удаление завершены — навигация назад (список обновится realtime-слушателем). */
    data object Closed : GroupEditorEffect()
}
