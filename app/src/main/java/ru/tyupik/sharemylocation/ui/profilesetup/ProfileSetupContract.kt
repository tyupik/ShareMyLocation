package ru.tyupik.sharemylocation.ui.profilesetup

/**
 * SSOT экрана настройки/редактирования профиля.
 * Режим (онбординг или редактирование) в состояние не входит: он влияет только на тексты
 * и приходит параметром [ProfileSetupScreen.isEditMode] от корневой навигации.
 * [pendingAvatarPath] — путь к сжатой копии нового аватара в кэше приложения,
 * [savedAvatarPath] — путь к кэш-файлу уже сохранённого аватара
 * (показывается, пока новое фото не выбрано).
 */
data class ProfileSetupUiState(
    val displayName: String = "",
    val phone: String = "",
    val pendingAvatarPath: String? = null,
    val savedAvatarPath: String? = null,
    /** Первичная загрузка профиля с сервера. */
    val isLoading: Boolean = false,
    /** Сжатие аватара или сохранение профиля. */
    val isSaving: Boolean = false,
    val error: ProfileSetupUiError? = null,
)

/** Действия пользователя экрана настройки/редактирования профиля. */
sealed class ProfileSetupIntent {
    /** Экран показан: перечитываем профиль, чтобы предзаполнить поля. */
    data object ScreenShown : ProfileSetupIntent()

    data class DisplayNameChanged(val value: String) : ProfileSetupIntent()

    data class PhoneChanged(val value: String) : ProfileSetupIntent()

    /** UI-слой получил результат photo picker; null — пользователь отменил выбор. */
    data class AvatarSelected(val localUri: String?) : ProfileSetupIntent()

    data object SaveClicked : ProfileSetupIntent()

    /** Пользователь закрыл баннер с ошибкой. */
    data object ErrorDismissed : ProfileSetupIntent()
}

/** One-off события экрана. */
sealed class ProfileSetupEffect {
    /** Профиль сохранён — навигацию выполняет вызывающая сторона через onCompleted. */
    data object Completed : ProfileSetupEffect()
}
