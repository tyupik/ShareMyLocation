package ru.tyupik.sharemylocation.ui.profilesetup

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import ru.tyupik.sharemylocation.business.common.User
import ru.tyupik.sharemylocation.business.profile.ProfileError
import ru.tyupik.sharemylocation.business.profile.ProfileException
import ru.tyupik.sharemylocation.business.profile.ProfileInteractor

/**
 * ViewModel экрана настройки/редактирования профиля.
 * Через интерактор читает и сохраняет профиль, через [ProfileSetupUiConverter] переводит
 * бизнес-модели в Ui-модели. Сохранённый аватар резолвится асинхронно после предзаполнения:
 * ошибка аватара не ломает экран, остаётся плейсхолдер с инициалом.
 * Единственная публичная точка входа для UI — [onIntent].
 */
@HiltViewModel
class ProfileSetupViewModel @Inject constructor(
    private val profileInteractor: ProfileInteractor,
    private val profileSetupUiConverter: ProfileSetupUiConverter,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileSetupUiState())
    val uiState: StateFlow<ProfileSetupUiState> = _uiState.asStateFlow()

    private val _effects = Channel<ProfileSetupEffect>(Channel.BUFFERED)
    val effects: Flow<ProfileSetupEffect> = _effects.receiveAsFlow()

    /** Uri временного файла снимка камеры, ожидающего результата TakePicture; нужен для очистки. */
    private var pendingCaptureUri: String? = null

    /** Uri фото, отправленного в CropImageActivity; нужен для очистки temp-снимка камеры. */
    private var pendingCropSourceUri: String? = null

    /** Uri temp-файла-результата кропа, ожидающего результата CropImageActivity; нужен для очистки. */
    private var pendingCropOutputUri: String? = null

    fun onIntent(intent: ProfileSetupIntent) {
        when (intent) {
            ProfileSetupIntent.ScreenShown -> loadProfile()
            is ProfileSetupIntent.DisplayNameChanged -> onDisplayNameChanged(intent.value)
            is ProfileSetupIntent.PhoneChanged -> onPhoneChanged(intent.value)
            is ProfileSetupIntent.AvatarSelected -> onAvatarSelected(intent.localUri)
            is ProfileSetupIntent.AvatarCropConfirmed -> onCropConfirmed(intent.croppedImageUri)
            ProfileSetupIntent.AvatarCropCancelled -> onCropCancelled()
            ProfileSetupIntent.AvatarPickRequested -> _uiState.update {
                it.copy(isAvatarSourceDialogVisible = true)
            }
            ProfileSetupIntent.AvatarSourceDialogDismissed -> _uiState.update {
                it.copy(isAvatarSourceDialogVisible = false)
            }
            ProfileSetupIntent.AvatarSourceGallerySelected -> _uiState.update {
                it.copy(isAvatarSourceDialogVisible = false)
            }
            ProfileSetupIntent.AvatarSourceCameraSelected -> onCameraSelected()
            ProfileSetupIntent.AvatarCaptureUnavailable -> onCaptureUnavailable()
            is ProfileSetupIntent.AvatarCaptureCancelled -> onCaptureCancelled(intent.uri)
            ProfileSetupIntent.SaveClicked -> saveProfile()
            ProfileSetupIntent.ErrorDismissed -> _uiState.update { it.copy(error = null) }
        }
    }

    private fun onDisplayNameChanged(value: String) =
        _uiState.update { it.copy(displayName = value, error = null) }

    private fun onPhoneChanged(value: String) =
        _uiState.update { it.copy(phone = value, error = null) }

    private fun loadProfile() {
        if (_uiState.value.isLoading) return
        _uiState.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            profileInteractor.getProfile().fold(
                onSuccess = { user ->
                    val uiModel = profileSetupUiConverter.convert(user)
                    _uiState.update {
                        it.copy(
                            displayName = uiModel.displayName,
                            phone = uiModel.phone,
                            savedAvatarPath = uiModel.savedAvatarPath,
                            isLoading = false,
                        )
                    }
                    loadSavedAvatar(user)
                },
                onFailure = { throwable ->
                    Log.w(TAG, "Не удалось загрузить профиль", throwable)
                    _uiState.update { it.copy(isLoading = false, error = toUiError(throwable)) }
                },
            )
        }
    }

    /** Неуспех загрузки сохранённого аватара не показываем: остаёмся на плейсхолдере с инициалом. */
    private fun loadSavedAvatar(user: User) {
        if (user.avatarVersion <= 0L) return
        viewModelScope.launch {
            profileInteractor.getAvatar(user.uid, user.avatarVersion).fold(
                onSuccess = { avatar ->
                    _uiState.update { it.copy(savedAvatarPath = avatar.cachedPath) }
                },
                onFailure = { throwable ->
                    Log.w(TAG, "Не удалось загрузить сохранённый аватар", throwable)
                },
            )
        }
    }

    /**
     * Выбрано фото: создаём temp-файл-результат кропа и просим экран открыть CropImageActivity;
     * EXIF и down-sampling выполняет библиотека, результат обрабатывается в [onCropConfirmed].
     * Отмена photo picker (null) — не ошибка: просто оставляем предыдущее фото.
     */
    private fun onAvatarSelected(localUri: String?) {
        if (localUri == null) return
        if (_uiState.value.isSaving) return
        viewModelScope.launch {
            profileInteractor.createCropAvatarUri().fold(
                onSuccess = { outputUri ->
                    // Повторный выбор фото до завершения кропа предыдущего:
                    // старый temp-результат больше не нужен, освобождаем перед новым запуском.
                    pendingCropOutputUri?.let { staleUri -> deleteCropOutput(staleUri) }
                    pendingCropSourceUri = localUri
                    pendingCropOutputUri = outputUri
                    _effects.send(ProfileSetupEffect.LaunchAvatarCrop(localUri, outputUri))
                },
                onFailure = { throwable ->
                    Log.w(TAG, "Не удалось создать файл для результата кадрирования", throwable)
                    releaseCameraCaptureIfMatches(localUri)
                    _uiState.update { it.copy(error = toUiError(throwable)) }
                },
            )
        }
    }

    /**
     * Кроп завершён успешно: сжимаем результат из temp-файла кропа и делаем его
     * отложенным аватаром. Неуспех — ошибка показывается существующим баннером,
     * temp-файлы (кроп и, если был, снимок камеры) убираются, прежний аватар остаётся.
     */
    private fun onCropConfirmed(croppedImageUri: String) {
        val sourceUri = pendingCropSourceUri ?: return
        val outputUri = pendingCropOutputUri ?: return
        if (_uiState.value.isSaving) return
        _uiState.update { it.copy(isSaving = true, error = null) }
        viewModelScope.launch {
            profileInteractor.prepareAvatar(croppedImageUri).fold(
                onSuccess = { path ->
                    cleanupCropTemp(sourceUri, outputUri)
                    _uiState.update { it.copy(isSaving = false, pendingAvatarPath = path) }
                },
                onFailure = { throwable ->
                    Log.w(TAG, "Не удалось подготовить аватар", throwable)
                    cleanupCropTemp(sourceUri, outputUri)
                    _uiState.update { it.copy(isSaving = false, error = toUiError(throwable)) }
                },
            )
        }
    }

    /** Кроп отменён (или завершился ошибкой): выбор фото отменяется целиком, temp-файлы убираются. */
    private fun onCropCancelled() {
        val sourceUri = pendingCropSourceUri ?: return
        val outputUri = pendingCropOutputUri ?: return
        cleanupCropTemp(sourceUri, outputUri)
    }

    /** Убирает оба temp-файла кропа: снимок камеры (если источник — камера) и результат кропа. */
    private fun cleanupCropTemp(sourceUri: String, outputUri: String) {
        releaseCameraCaptureIfMatches(sourceUri)
        pendingCropSourceUri = null
        pendingCropOutputUri = null
        deleteCropOutput(outputUri)
    }

    /** Fire-and-forget очистка temp-результата кропа; сбой удаления не влияет на UI. */
    private fun deleteCropOutput(uri: String) {
        viewModelScope.launch {
            profileInteractor.deleteCropAvatar(uri).onFailure { throwable ->
                Log.w(TAG, "Не удалось удалить временный файл кадрирования: $uri", throwable)
            }
        }
    }

    /** Камера выбрана: создаём временный Uri и просим экран запустить съёмку. */
    private fun onCameraSelected() {
        _uiState.update { it.copy(isAvatarSourceDialogVisible = false, error = null) }
        viewModelScope.launch {
            profileInteractor.createCameraAvatarUri().fold(
                onSuccess = { uri ->
                    // Повторный выбор камеры до завершения prepareAvatar предыдущего снимка:
                    // старый temp-файл больше не нужен, освобождаем перед перезаписью Uri.
                    pendingCaptureUri?.let { staleUri -> deleteCameraCapture(staleUri) }
                    pendingCaptureUri = uri
                    _effects.send(ProfileSetupEffect.LaunchCamera(uri))
                },
                onFailure = { throwable ->
                    Log.w(TAG, "Не удалось создать файл для снимка камеры", throwable)
                    _uiState.update { it.copy(error = toUiError(throwable)) }
                },
            )
        }
    }

    /** Камеру открыть не удалось: показываем ошибку и убираем ненужный temp-файл. */
    private fun onCaptureUnavailable() {
        pendingCaptureUri?.let { uri ->
            pendingCaptureUri = null
            deleteCameraCapture(uri)
        }
        _uiState.update { it.copy(error = ProfileSetupUiError.CameraUnavailable) }
    }

    /** Пользователь отменил съёмку: тихо убираем temp-файл, если Uri совпадает с ожидаемым. */
    private fun onCaptureCancelled(uri: String) {
        if (uri != pendingCaptureUri) return
        pendingCaptureUri = null
        deleteCameraCapture(uri)
    }

    /** Если сжатый Uri был снимком камеры — temp-оригинал больше не нужен. */
    private fun releaseCameraCaptureIfMatches(localUri: String) {
        if (localUri == pendingCaptureUri) {
            pendingCaptureUri = null
            deleteCameraCapture(localUri)
        }
    }

    /** Fire-and-forget очистка temp-снимка камеры; сбой удаления не влияет на UI. */
    private fun deleteCameraCapture(uri: String) {
        viewModelScope.launch {
            profileInteractor.deleteCameraAvatar(uri).onFailure { throwable ->
                Log.w(TAG, "Не удалось удалить временный снимок камеры: $uri", throwable)
            }
        }
    }

    private fun saveProfile() {
        val state = _uiState.value
        if (state.isSaving || state.isLoading) return
        _uiState.update { it.copy(isSaving = true, error = null) }
        viewModelScope.launch {
            profileInteractor.saveProfile(
                displayName = state.displayName.trim(),
                phone = state.phone,
                pendingAvatarPath = state.pendingAvatarPath,
            ).fold(
                onSuccess = {
                    _uiState.update { it.copy(isSaving = false) }
                    _effects.send(ProfileSetupEffect.Completed)
                },
                onFailure = { throwable ->
                    Log.w(TAG, "Не удалось сохранить профиль", throwable)
                    _uiState.update { it.copy(isSaving = false, error = toUiError(throwable)) }
                },
            )
        }
    }

    /** Ошибки, не завёрнутые в ProfileException (например, сбой сжатия), считаем Unexpected. */
    private fun toUiError(throwable: Throwable): ProfileSetupUiError {
        val error = (throwable as? ProfileException)?.error
            ?: ProfileError.Unexpected(throwable.message)
        return profileSetupUiConverter.convertError(error)
    }

    private companion object {
        const val TAG = "ProfileSetupViewModel"
    }
}
