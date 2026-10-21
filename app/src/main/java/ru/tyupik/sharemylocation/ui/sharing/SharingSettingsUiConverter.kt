package ru.tyupik.sharemylocation.ui.sharing

import javax.inject.Inject
import ru.tyupik.sharemylocation.business.common.User
import ru.tyupik.sharemylocation.business.groups.Group
import ru.tyupik.sharemylocation.business.sharing.SharingAudience
import ru.tyupik.sharemylocation.business.sharing.SharingError

/** Ui-модель строки чекбокса секций «Люди»/«Группы» настроек шаринга. */
data class SharingCheckRowUiModel(
    /** uid друга либо groupId группы. */
    val id: String,
    val displayName: String,
    /** Вхождение в выбор шаринга на экране; запись в Firestore — автосохранением. */
    val isChecked: Boolean,
)

/** Ошибки фичи «Шаринг» в терминах Ui-слоя. */
sealed class SharingUiError {
    data object Network : SharingUiError()

    data class Unexpected(val reason: String?) : SharingUiError()
}

/** Конвертер бизнес-моделей в Ui-модели экрана настроек шаринга. */
class SharingSettingsUiConverter @Inject constructor() {

    fun convertFriend(user: User, isChecked: Boolean): SharingCheckRowUiModel =
        SharingCheckRowUiModel(
            id = user.uid,
            displayName = user.displayName.orEmpty(),
            isChecked = isChecked,
        )

    fun convertGroup(group: Group, isChecked: Boolean): SharingCheckRowUiModel =
        SharingCheckRowUiModel(
            id = group.groupId,
            displayName = group.name,
            isChecked = isChecked,
        )

    fun convertAudience(audience: SharingAudience): SharingAudienceUi = when (audience) {
        SharingAudience.ALL_FRIENDS -> SharingAudienceUi.ALL_FRIENDS
        SharingAudience.CUSTOM -> SharingAudienceUi.CUSTOM
    }

    /** Обратное преобразование аудитории для снимка записи. */
    fun toBusiness(audience: SharingAudienceUi): SharingAudience = when (audience) {
        SharingAudienceUi.ALL_FRIENDS -> SharingAudience.ALL_FRIENDS
        SharingAudienceUi.CUSTOM -> SharingAudience.CUSTOM
    }

    fun convertError(error: SharingError): SharingUiError = when (error) {
        SharingError.NetworkUnavailable -> SharingUiError.Network
        is SharingError.Unexpected -> SharingUiError.Unexpected(error.reason)
    }
}
