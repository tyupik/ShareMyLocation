package ru.tyupik.sharemylocation.ui.common

import javax.inject.Inject
import ru.tyupik.sharemylocation.business.common.User

/** Ui-модель корневой навигации. */
data class AppUiModel(
    val isAuthenticated: Boolean,
)

/** Конвертер бизнес-модели пользователя в Ui-модель корневой навигации. */
class AppUiConverter @Inject constructor() {

    fun convert(user: User?): AppUiModel = AppUiModel(isAuthenticated = user != null)
}
