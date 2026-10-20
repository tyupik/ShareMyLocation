package ru.tyupik.sharemylocation.ui.main

import javax.inject.Inject
import ru.tyupik.sharemylocation.business.common.User

/** Ui-модель временного главного экрана. */
data class MainUiModel(
    val userEmail: String?,
)

/** Конвертер бизнес-модели пользователя в Ui-модель главного экрана. */
class MainUiConverter @Inject constructor() {

    fun convert(user: User?): MainUiModel = MainUiModel(userEmail = user?.email)
}
