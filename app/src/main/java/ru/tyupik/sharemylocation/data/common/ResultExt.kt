package ru.tyupik.sharemylocation.data.common

import kotlin.coroutines.cancellation.CancellationException

/**
 * Result-обёртка для suspend-вызовов, которая в отличие от runCatching
 * не глотает CancellationException и не ломает отмену корутины.
 */
inline fun <T> runSuspendCatching(block: () -> T): Result<T> = try {
    Result.success(block())
} catch (cancellation: CancellationException) {
    throw cancellation
} catch (throwable: Throwable) {
    Result.failure(throwable)
}

/** Заменяет Throwable в неуспешном Result, успех проходит как есть. */
inline fun <T> Result<T>.mapFailure(transform: (Throwable) -> Throwable): Result<T> =
    fold({ Result.success(it) }, { Result.failure(transform(it)) })
