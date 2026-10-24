package ru.tyupik.sharemylocation.ui.friends

import javax.inject.Inject
import ru.tyupik.sharemylocation.business.common.User
import ru.tyupik.sharemylocation.business.contacts.ContactsError
import ru.tyupik.sharemylocation.business.friends.FriendRequest
import ru.tyupik.sharemylocation.business.friends.FriendsError
import ru.tyupik.sharemylocation.business.groups.Group

/** Ui-модель друга/найденного пользователя: [avatarPath] резолвится асинхронно. */
data class FriendUiModel(
    val uid: String,
    val displayName: String,
    val phone: String?,
    val avatarVersion: Long,
    val avatarPath: String?,
)

/** Ui-модель входящей заявки: [avatarPath] резолвится асинхронно. */
data class FriendRequestUiModel(
    val fromUid: String,
    val fromName: String,
    val fromAvatarVersion: Long,
    val avatarPath: String?,
)

/** Ui-модель группы в секции «Группы»: карточка показывает имя и число участников. */
data class GroupUiModel(
    val groupId: String,
    val name: String,
    val memberCount: Int,
)

/** Ошибки фичи «Друзья» в терминах Ui-слоя. */
sealed class FriendsUiError {
    data object Network : FriendsUiError()

    data object InvalidPhone : FriendsUiError()

    data object RequestToSelf : FriendsUiError()

    data object AlreadyFriends : FriendsUiError()

    data object RequestAlreadySent : FriendsUiError()

    data object RequestAlreadyIncoming : FriendsUiError()

    /** У выбранного из контактов друга нет телефона. */
    data object ContactNoPhone : FriendsUiError()

    /** Не удалось прочитать выбранный контакт (или пикер контактов недоступен). */
    data object ContactReadFailed : FriendsUiError()

    data class Unexpected(val reason: String?) : FriendsUiError()
}

/** Конвертер бизнес-моделей друзей в Ui-модели вкладки «Друзья». */
class FriendsUiConverter @Inject constructor() {

    /** [avatarPath] заполняется ViewModel после дозагрузки аватара. */
    fun convertFriend(user: User): FriendUiModel = FriendUiModel(
        uid = user.uid,
        displayName = user.displayName.orEmpty(),
        phone = user.phone,
        avatarVersion = user.avatarVersion,
        avatarPath = null,
    )

    /** [avatarPath] заполняется ViewModel после дозагрузки аватара. */
    fun convertRequest(request: FriendRequest): FriendRequestUiModel = FriendRequestUiModel(
        fromUid = request.fromUid,
        fromName = request.fromName,
        fromAvatarVersion = request.fromAvatarVersion,
        avatarPath = null,
    )

    /** Имена участников на вкладке не нужны — только их число. */
    fun convertGroup(group: Group): GroupUiModel = GroupUiModel(
        groupId = group.groupId,
        name = group.name,
        memberCount = group.memberUids.size,
    )

    fun convertError(error: FriendsError): FriendsUiError = when (error) {
        FriendsError.NetworkUnavailable -> FriendsUiError.Network
        FriendsError.InvalidPhone -> FriendsUiError.InvalidPhone
        FriendsError.RequestToSelf -> FriendsUiError.RequestToSelf
        FriendsError.AlreadyFriends -> FriendsUiError.AlreadyFriends
        FriendsError.RequestAlreadySent -> FriendsUiError.RequestAlreadySent
        FriendsError.RequestAlreadyIncoming -> FriendsUiError.RequestAlreadyIncoming
        is FriendsError.Unexpected -> FriendsUiError.Unexpected(error.reason)
        FriendsError.UserNotFound -> FriendsUiError.Unexpected(error.toString())
    }

    /** Ошибки чтения телефонной книги попадают в тот же баннер действия, что и ошибки друзей. */
    fun convertContactError(error: ContactsError): FriendsUiError = when (error) {
        ContactsError.NoPhone -> FriendsUiError.ContactNoPhone
        is ContactsError.ReadFailed -> FriendsUiError.ContactReadFailed
    }
}
