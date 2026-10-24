package ru.tyupik.sharemylocation.ui.profilesetup

import javax.inject.Inject
import ru.tyupik.sharemylocation.business.common.User
import ru.tyupik.sharemylocation.business.profile.ProfileError

/** Ui-модель профиля, в которую конвертер переводит бизнес-модель User. */
data class ProfileSetupUiModel(
    val displayName: String,
    val phone: String,
    /**
     * Путь к кэш-файлу уже сохранённого аватара; конвертер всегда возвращает null —
     * путь резолвится асинхронно в [ProfileSetupViewModel] через интерактор.
     */
    val savedAvatarPath: String?,
)

/** Ошибки экрана настройки/редактирования профиля в терминах Ui-слоя. */
sealed class ProfileSetupUiError {
    data object InvalidPhone : ProfileSetupUiError()

    data object Network : ProfileSetupUiError()

    /** Фото слишком большое для документа Firestore даже после сжатия. */
    data object AvatarTooLarge : ProfileSetupUiError()

    /** Камеру открыть не удалось: на устройстве нет приложения камеры. */
    data object CameraUnavailable : ProfileSetupUiError()

    data class Unexpected(val reason: String?) : ProfileSetupUiError()
}

/** Конвертер бизнес-моделей профиля в Ui-модели экрана настройки. */
class ProfileSetupUiConverter @Inject constructor() {

    fun convert(user: User): ProfileSetupUiModel = ProfileSetupUiModel(
        displayName = user.displayName.orEmpty(),
        phone = user.phone.orEmpty(),
        savedAvatarPath = null,
    )

    fun convertError(error: ProfileError): ProfileSetupUiError = when (error) {
        ProfileError.InvalidPhone -> ProfileSetupUiError.InvalidPhone
        ProfileError.NetworkUnavailable -> ProfileSetupUiError.Network
        ProfileError.AvatarTooLarge -> ProfileSetupUiError.AvatarTooLarge
        is ProfileError.Unexpected -> ProfileSetupUiError.Unexpected(error.reason)
    }
}
