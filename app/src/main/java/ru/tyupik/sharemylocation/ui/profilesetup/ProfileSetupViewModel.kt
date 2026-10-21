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

    fun onIntent(intent: ProfileSetupIntent) {
        when (intent) {
            ProfileSetupIntent.ScreenShown -> loadProfile()
            is ProfileSetupIntent.DisplayNameChanged -> onDisplayNameChanged(intent.value)
            is ProfileSetupIntent.PhoneChanged -> onPhoneChanged(intent.value)
            is ProfileSetupIntent.AvatarSelected -> onAvatarSelected(intent.localUri)
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

    /** Отмена photo picker (null) — не ошибка: просто оставляем предыдущее фото. */
    private fun onAvatarSelected(localUri: String?) {
        if (localUri == null) return
        if (_uiState.value.isSaving) return
        _uiState.update { it.copy(isSaving = true, error = null) }
        viewModelScope.launch {
            profileInteractor.prepareAvatar(localUri).fold(
                onSuccess = { path ->
                    _uiState.update { it.copy(isSaving = false, pendingAvatarPath = path) }
                },
                onFailure = { throwable ->
                    Log.w(TAG, "Не удалось подготовить аватар", throwable)
                    _uiState.update { it.copy(isSaving = false, error = toUiError(throwable)) }
                },
            )
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
