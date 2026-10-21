package ru.tyupik.sharemylocation.ui.groups

import javax.inject.Inject
import ru.tyupik.sharemylocation.business.common.User
import ru.tyupik.sharemylocation.business.groups.GroupsError

/** Ui-модель друга в списке чекбоксов редактора группы. */
data class GroupMemberUiModel(
    val uid: String,
    val displayName: String,
    /** Вхождение друга в группу на экране; запись в Firestore — по SaveClicked. */
    val isChecked: Boolean,
)

/** Ошибки фичи «Группы» в терминах Ui-слоя. */
sealed class GroupsUiError {
    data object Network : GroupsUiError()

    data object EmptyName : GroupsUiError()

    data object NameTooLong : GroupsUiError()

    data object GroupNotFound : GroupsUiError()

    data class Unexpected(val reason: String?) : GroupsUiError()
}

/** Конвертер бизнес-моделей в Ui-модели редактора группы. */
class GroupEditorUiConverter @Inject constructor() {

    fun convertFriend(user: User, isChecked: Boolean): GroupMemberUiModel = GroupMemberUiModel(
        uid = user.uid,
        displayName = user.displayName.orEmpty(),
        isChecked = isChecked,
    )

    fun convertError(error: GroupsError): GroupsUiError = when (error) {
        GroupsError.NetworkUnavailable -> GroupsUiError.Network
        GroupsError.EmptyName -> GroupsUiError.EmptyName
        GroupsError.NameTooLong -> GroupsUiError.NameTooLong
        GroupsError.GroupNotFound -> GroupsUiError.GroupNotFound
        is GroupsError.Unexpected -> GroupsUiError.Unexpected(error.reason)
    }
}
