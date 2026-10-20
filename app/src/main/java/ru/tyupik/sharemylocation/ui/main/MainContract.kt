package ru.tyupik.sharemylocation.ui.main

/** Состояние временного главного экрана. */
data class MainUiState(
    /** Email вошедшего пользователя; null, если сессия не найдена. */
    val userEmail: String? = null,
)

/** Действия пользователя. */
sealed class MainIntent {
    data object SignOutClicked : MainIntent()

    /** Экран показан (в том числе повторно) — перечитать текущего пользователя. */
    data object ScreenShown : MainIntent()
}

/** One-off события экрана. */
sealed class MainEffect {
    /** Выход выполнен — возвращаемся на экран аутентификации. */
    data object NavigateToAuth : MainEffect()
}
