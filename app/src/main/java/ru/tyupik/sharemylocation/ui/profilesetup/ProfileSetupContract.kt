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
    /** Показан диалог выбора источника аватара (галерея или камера). */
    val isAvatarSourceDialogVisible: Boolean = false,
    /** Первичная загрузка профиля с сервера. */
    val isLoading: Boolean = false,
    /** Подготовка аватара (сжатие) или сохранение профиля. */
    val isSaving: Boolean = false,
    val error: ProfileSetupUiError? = null,
)

/** Действия пользователя экрана настройки/редактирования профиля. */
sealed class ProfileSetupIntent {
    /** Экран показан: перечитываем профиль, чтобы предзаполнить поля. */
    data object ScreenShown : ProfileSetupIntent()

    data class DisplayNameChanged(val value: String) : ProfileSetupIntent()

    data class PhoneChanged(val value: String) : ProfileSetupIntent()

    /** UI-слой получил результат photo picker или камеры; null — пользователь отменил выбор. */
    data class AvatarSelected(val localUri: String?) : ProfileSetupIntent()

    /**
     * CropImageActivity вернула успешный результат: кроп-файл сжимается
     * и становится новым отложенным аватаром.
     */
    data class AvatarCropConfirmed(val croppedImageUri: String) : ProfileSetupIntent()

    /** Кроп отменён или завершился ошибкой: выбор фото отменяется целиком, temp-файлы убираются. */
    data object AvatarCropCancelled : ProfileSetupIntent()

    /** Пользователь нажал на бейдж камеры / кнопку выбора фото: показать диалог источника. */
    data object AvatarPickRequested : ProfileSetupIntent()

    /** Пользователь закрыл диалог выбора источника, не выбрав вариант. */
    data object AvatarSourceDialogDismissed : ProfileSetupIntent()

    /** В диалоге выбрана галерея: экран запускает photo picker. */
    data object AvatarSourceGallerySelected : ProfileSetupIntent()

    /** В диалоге выбрана камера: VM создаёт временный Uri и шлёт [ProfileSetupEffect.LaunchCamera]. */
    data object AvatarSourceCameraSelected : ProfileSetupIntent()

    /** Камеру открыть не удалось (нет приложения камеры): показать ошибку и убрать temp-файл. */
    data object AvatarCaptureUnavailable : ProfileSetupIntent()

    /** Пользователь отменил съёмку (TakePicture вернул false): тихо убрать temp-файл. */
    data class AvatarCaptureCancelled(val uri: String) : ProfileSetupIntent()

    data object SaveClicked : ProfileSetupIntent()

    /** Пользователь закрыл баннер с ошибкой. */
    data object ErrorDismissed : ProfileSetupIntent()
}

/** One-off события экрана. */
sealed class ProfileSetupEffect {
    /** Профиль сохранён — навигацию выполняет вызывающая сторона через onCompleted. */
    data object Completed : ProfileSetupEffect()

    /** Запустить системную камеру на съёмку аватара в content-Uri временного файла. */
    data class LaunchCamera(val imageUri: String) : ProfileSetupEffect()

    /** Открыть CropImageActivity: [sourceUri] — выбранное фото, [outputUri] — temp-файл результата. */
    data class LaunchAvatarCrop(val sourceUri: String, val outputUri: String) : ProfileSetupEffect()
}
