package ru.tyupik.sharemylocation.ui.profile

/**
 * SSOT вкладки «Профиль». Состояния взаимоисключающие: загрузка, данные или ошибка.
 * Переходы на другие экраны через Effect; выход из аккаунта остаётся в контракте MainScreen.
 */
sealed interface ProfileUiState {
    data object Loading : ProfileUiState

    data class Content(val profile: ProfileUiModel) : ProfileUiState

    data class Error(val error: ProfileUiError) : ProfileUiState
}

/** Действия пользователя на вкладке «Профиль». */
sealed class ProfileIntent {
    /** Вкладка показана: перечитываем профиль при каждом входе на вкладку. */
    data object ScreenShown : ProfileIntent()

    /** Пользователь нажал «Редактировать». */
    data object EditClicked : ProfileIntent()

    /** Пользователь нажал «Делиться геопозицией». */
    data object SharingClicked : ProfileIntent()
}

/** One-off события экрана. */
sealed class ProfileEffect {
    /** Открыть ProfileSetup в режиме редактирования; навигацию выполняет вызывающая сторона. */
    data object NavigateToEditProfile : ProfileEffect()

    /** Открыть настройки шаринга; навигацию выполняет вызывающая сторона. */
    data object NavigateToSharingSettings : ProfileEffect()
}
