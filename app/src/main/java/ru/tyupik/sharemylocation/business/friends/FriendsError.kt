package ru.tyupik.sharemylocation.business.friends

/** Типизированные ошибки фичи «Друзья»; замаплены из Firebase в data-слое. */
sealed class FriendsError {

    /** Нет сети или таймаут (FirebaseNetworkException, IOException). */
    data object NetworkUnavailable : FriendsError()

    /** Телефон не соответствует правилу 10–15 цифр после нормализации. */
    data object InvalidPhone : FriendsError()

    /** Пользователь с таким телефоном не найден. */
    data object UserNotFound : FriendsError()

    /** Попытка добавить в друзья самого себя. */
    data object RequestToSelf : FriendsError()

    /** Пользователь уже в списке друзей. */
    data object AlreadyFriends : FriendsError()

    /** Заявка этому пользователю уже отправлена и ещё в статусе PENDING. */
    data object RequestAlreadySent : FriendsError()

    /** От этого пользователя уже есть встречная заявка. */
    data object RequestAlreadyIncoming : FriendsError()

    /** Всё, что не удалось сопоставить с конкретной ошибкой. */
    data class Unexpected(val reason: String?) : FriendsError()
}

/**
 * Доменная ошибка фичи «Друзья»: несёт типизированный [FriendsError] для UiState
 * и исходный Throwable для логирования.
 */
class FriendsException(
    val error: FriendsError,
    cause: Throwable? = null,
) : Exception(error.toString(), cause)
