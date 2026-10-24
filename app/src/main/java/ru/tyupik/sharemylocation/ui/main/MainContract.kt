package ru.tyupik.sharemylocation.ui.main

/** Вкладки главного экрана. */
enum class MainTab {
    MAP,
    FRIENDS,
    PROFILE,
}

/**
 * SSOT главного экрана-контейнера.
 * Содержимое вкладок сами управляют своим состоянием, поэтому здесь только выбранная вкладка
 * и badge-счётчик входящих заявок в друзья для нижней панели.
 */
data class MainUiState(
    val selectedTab: MainTab = MainTab.MAP,
    /** Число входящих PENDING-заявок в друзья; badge на вкладке FRIENDS при значении > 0. */
    val pendingRequestsCount: Int = 0,
)

/** Действия пользователя. */
sealed class MainIntent {
    /** Пользователь выбрал вкладку в нижней навигации. */
    data class TabSelected(val tab: MainTab) : MainIntent()

    /** Пользователь нажал «Выход» на вкладке «Профиль». */
    data object SignOutClicked : MainIntent()

    /** Системный диалог права POST_NOTIFICATIONS закрыт — результат передаётся в контроллер. */
    data class NotificationsPermissionResult(val granted: Boolean) : MainIntent()
}

/** One-off события экрана. */
sealed class MainEffect {
    /** Выход выполнен — возвращаемся на экран аутентификации. */
    data object NavigateToAuth : MainEffect()

    /** Показать системный диалог права на уведомления (один раз за установку). */
    data object RequestNotificationsPermission : MainEffect()
}
