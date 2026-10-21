package ru.tyupik.sharemylocation.ui.main

/** Вкладки главного экрана. */
enum class MainTab {
    MAP,
    FRIENDS,
    PROFILE,
}

/**
 * SSOT главного экрана-контейнера.
 * Содержимое вкладок сами управляют своим состоянием, поэтому здесь только выбранная вкладка.
 */
data class MainUiState(
    val selectedTab: MainTab = MainTab.MAP,
)

/** Действия пользователя. */
sealed class MainIntent {
    /** Пользователь выбрал вкладку в нижней навигации. */
    data class TabSelected(val tab: MainTab) : MainIntent()

    /** Пользователь нажал «Выход» на вкладке «Профиль». */
    data object SignOutClicked : MainIntent()
}

/** One-off события экрана. */
sealed class MainEffect {
    /** Выход выполнен — возвращаемся на экран аутентификации. */
    data object NavigateToAuth : MainEffect()
}
