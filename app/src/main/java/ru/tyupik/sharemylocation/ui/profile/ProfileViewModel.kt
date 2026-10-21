package ru.tyupik.sharemylocation.ui.profile

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
import kotlinx.coroutines.launch
import ru.tyupik.sharemylocation.business.common.User
import ru.tyupik.sharemylocation.business.profile.ProfileError
import ru.tyupik.sharemylocation.business.profile.ProfileException
import ru.tyupik.sharemylocation.business.profile.ProfileInteractor

/**
 * ViewModel вкладки «Профиль»: перечитывает профиль при каждом показе вкладки.
 * Аватар резолвится асинхронно после основного контента: вкладка сразу показывает
 * плейсхолдер с инициалом, ошибка аватара не ломает вкладку.
 * Через [ProfileUiConverter] переводит бизнес-модель в Ui-модель.
 * Единственная публичная точка входа для UI — [onIntent].
 */
@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val profileInteractor: ProfileInteractor,
    private val profileUiConverter: ProfileUiConverter,
) : ViewModel() {

    private val _uiState = MutableStateFlow<ProfileUiState>(ProfileUiState.Loading)
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    private val _effects = Channel<ProfileEffect>(Channel.BUFFERED)
    val effects: Flow<ProfileEffect> = _effects.receiveAsFlow()

    fun onIntent(intent: ProfileIntent) {
        when (intent) {
            ProfileIntent.ScreenShown -> loadProfile()
            ProfileIntent.EditClicked -> onEditClicked()
            ProfileIntent.SharingClicked -> onSharingClicked()
        }
    }

    private fun loadProfile() {
        viewModelScope.launch {
            profileInteractor.getProfile().fold(
                onSuccess = { user ->
                    _uiState.value =
                        ProfileUiState.Content(profileUiConverter.convert(user, avatarPath = null))
                    loadAvatar(user)
                },
                onFailure = { throwable ->
                    Log.w(TAG, "Не удалось загрузить профиль", throwable)
                    _uiState.value = ProfileUiState.Error(toUiError(throwable))
                },
            )
        }
    }

    /** Неуспех загрузки аватара не показываем: остаёмся на плейсхолдере с инициалом. */
    private fun loadAvatar(user: User) {
        if (user.avatarVersion <= 0L) return
        viewModelScope.launch {
            profileInteractor.getAvatar(user.uid, user.avatarVersion).fold(
                onSuccess = { avatar ->
                    val state = _uiState.value
                    if (state is ProfileUiState.Content) {
                        _uiState.value = state.copy(
                            profile = state.profile.copy(avatarPath = avatar.cachedPath),
                        )
                    }
                },
                onFailure = { throwable ->
                    Log.w(TAG, "Не удалось загрузить аватар", throwable)
                },
            )
        }
    }

    private fun onEditClicked() {
        viewModelScope.launch { _effects.send(ProfileEffect.NavigateToEditProfile) }
    }

    private fun onSharingClicked() {
        viewModelScope.launch { _effects.send(ProfileEffect.NavigateToSharingSettings) }
    }

    /** Ошибки, не завёрнутые в ProfileException, считаем Unexpected. */
    private fun toUiError(throwable: Throwable): ProfileUiError {
        val error = (throwable as? ProfileException)?.error
            ?: ProfileError.Unexpected(throwable.message)
        return profileUiConverter.convertError(error)
    }

    private companion object {
        const val TAG = "ProfileViewModel"
    }
}
