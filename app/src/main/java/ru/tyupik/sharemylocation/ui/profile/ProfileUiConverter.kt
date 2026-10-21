package ru.tyupik.sharemylocation.ui.profile

import javax.inject.Inject
import ru.tyupik.sharemylocation.business.common.User
import ru.tyupik.sharemylocation.business.profile.ProfileError

/** Ui-модель профиля, в которую конвертер переводит бизнес-модель User. */
data class ProfileUiModel(
    val displayName: String,
    val email: String?,
    val phone: String?,
    /** Путь к файлу аватара в кэше приложения; null — плейсхолдер с инициалом. */
    val avatarPath: String?,
)

/** Ошибки загрузки профиля в терминах Ui-слоя. */
sealed class ProfileUiError {
    data object Network : ProfileUiError()

    data class Unexpected(val reason: String?) : ProfileUiError()
}

/** Конвертер бизнес-модели пользователя в Ui-модель вкладки «Профиль». */
class ProfileUiConverter @Inject constructor() {

    fun convert(user: User, avatarPath: String?): ProfileUiModel = ProfileUiModel(
        displayName = user.displayName.orEmpty(),
        email = user.email,
        phone = user.phone,
        avatarPath = avatarPath,
    )

    fun convertError(error: ProfileError): ProfileUiError = when (error) {
        ProfileError.InvalidPhone -> ProfileUiError.Unexpected(error.toString())
        ProfileError.NetworkUnavailable -> ProfileUiError.Network
        ProfileError.AvatarTooLarge -> ProfileUiError.Unexpected(error.toString())
        is ProfileError.Unexpected -> ProfileUiError.Unexpected(error.reason)
    }
}
