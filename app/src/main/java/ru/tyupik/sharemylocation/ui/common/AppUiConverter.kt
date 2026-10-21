package ru.tyupik.sharemylocation.ui.common

import javax.inject.Inject
import ru.tyupik.sharemylocation.business.common.User

/** Ui-модель корневой навигации. */
data class AppUiModel(
    val isAuthenticated: Boolean,
    /** Вошедший пользователь ещё не прошёл онбординг ProfileSetup. */
    val requiresOnboarding: Boolean,
)

/** Конвертер бизнес-моделей в Ui-модель корневой навигации. */
class AppUiConverter @Inject constructor() {

    /**
     * @param user пользователь сохранённой сессии либо null.
     * @param profile его документ users/{uid} либо null (нет сессии или ошибка чтения).
     */
    fun convert(user: User?, profile: User?): AppUiModel = AppUiModel(
        isAuthenticated = user != null,
        requiresOnboarding = user != null && profile != null && !profile.hasCompletedOnboarding,
    )
}
