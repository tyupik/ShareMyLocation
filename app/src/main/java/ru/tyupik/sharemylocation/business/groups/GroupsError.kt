package ru.tyupik.sharemylocation.business.groups

/** Типизированные ошибки фичи «Группы»; замаплены из Firebase в data-слое. */
sealed class GroupsError {

    /** Нет сети или таймаут (FirebaseNetworkException, IOException). */
    data object NetworkUnavailable : GroupsError()

    /** Имя группы после trim пустое. */
    data object EmptyName : GroupsError()

    /** Имя группы длиннее 50 символов. */
    data object NameTooLong : GroupsError()

    /**
     * Группа не найдена или не принадлежит текущему пользователю: правило groups читает
     * resource.data, поэтому несуществующий документ и чужая группа дают PERMISSION_DENIED,
     * NOT_FOUND мапится туда же — для клиента это одно состояние.
     */
    data object GroupNotFound : GroupsError()

    /** Всё, что не удалось сопоставить с конкретной ошибкой. */
    data class Unexpected(val reason: String?) : GroupsError()
}

/**
 * Доменная ошибка фичи «Группы»: несёт типизированный [GroupsError] для UiState
 * и исходный Throwable для логирования.
 */
class GroupsException(
    val error: GroupsError,
    cause: Throwable? = null,
) : Exception(error.toString(), cause)
