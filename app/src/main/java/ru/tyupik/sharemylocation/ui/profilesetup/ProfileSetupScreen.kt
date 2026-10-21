package ru.tyupik.sharemylocation.ui.profilesetup

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ru.tyupik.sharemylocation.R
import ru.tyupik.sharemylocation.ui.profilesetup.composable.ProfileSetupAvatarPicker
import ru.tyupik.sharemylocation.ui.profilesetup.composable.ProfileSetupErrorBanner
import ru.tyupik.sharemylocation.ui.profilesetup.composable.ProfileSetupNameField
import ru.tyupik.sharemylocation.ui.profilesetup.composable.ProfileSetupPhoneField
import ru.tyupik.sharemylocation.ui.profilesetup.composable.ProfileSetupSaveButton
import ru.tyupik.sharemylocation.ui.theme.ShareMyLocationTheme

/** Допустимое число цифр в телефоне: экранное правило для доступности кнопки (авторитетное — в интеракторе). */
private const val MIN_PHONE_DIGITS = 10
private const val MAX_PHONE_DIGITS = 15

/**
 * Экран настройки (онбординг) и редактирования профиля: один экран, [isEditMode] меняет только тексты.
 * Навигацию выполняет вызывающая сторона через [onCompleted]: экран сам не знает про back stack,
 * лямбда вызывается только из коллекции Effect.
 */
@Composable
internal fun ProfileSetupScreen(
    isEditMode: Boolean,
    onCompleted: () -> Unit,
    viewModel: ProfileSetupViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) { viewModel.onIntent(ProfileSetupIntent.ScreenShown) }

    LaunchedEffect(Unit) {
        viewModel.effects.collect { effect ->
            when (effect) {
                ProfileSetupEffect.Completed -> onCompleted()
            }
        }
    }

    val pickerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        viewModel.onIntent(ProfileSetupIntent.AvatarSelected(uri?.toString()))
    }

    ProfileSetupContent(
        uiState = uiState,
        isEditMode = isEditMode,
        onIntent = viewModel::onIntent,
        onPickAvatar = {
            pickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        },
    )
}

@Composable
private fun ProfileSetupContent(
    uiState: ProfileSetupUiState,
    isEditMode: Boolean,
    onIntent: (ProfileSetupIntent) -> Unit,
    onPickAvatar: () -> Unit,
) {
    if (uiState.isLoading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    // Чистый UI-расчёт валидности, не бизнес-состояние: кнопка активна, только если форма заполнена.
    val isPhoneValid = uiState.phone.count { it.isDigit() } in MIN_PHONE_DIGITS..MAX_PHONE_DIGITS
    val isFormValid = uiState.displayName.isNotBlank() && isPhoneValid

    val bannerErrorText = when (val error = uiState.error) {
        ProfileSetupUiError.Network -> stringResource(R.string.profile_setup_error_network)
        ProfileSetupUiError.AvatarTooLarge -> stringResource(R.string.profile_setup_error_avatar_too_large)
        is ProfileSetupUiError.Unexpected -> stringResource(R.string.profile_setup_error_unexpected)
        ProfileSetupUiError.InvalidPhone, null -> null
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Spacer(modifier = Modifier.height(24.dp))

        if (bannerErrorText != null) {
            ProfileSetupErrorBanner(
                text = bannerErrorText,
                onDismiss = { onIntent(ProfileSetupIntent.ErrorDismissed) },
            )
        }

        Text(
            text = stringResource(
                if (isEditMode) R.string.profile_setup_edit_title else R.string.profile_setup_title,
            ),
            style = MaterialTheme.typography.headlineMedium,
        )

        ProfileSetupAvatarPicker(
            displayName = uiState.displayName,
            savedAvatarPath = uiState.savedAvatarPath,
            localPath = uiState.pendingAvatarPath,
            isEnabled = !uiState.isSaving,
            isEditMode = isEditMode,
            onPickRequest = onPickAvatar,
        )

        ProfileSetupNameField(
            value = uiState.displayName,
            isEnabled = !uiState.isSaving,
            onValueChange = { onIntent(ProfileSetupIntent.DisplayNameChanged(it)) },
        )

        ProfileSetupPhoneField(
            value = uiState.phone,
            isEnabled = !uiState.isSaving,
            errorText = if (uiState.error is ProfileSetupUiError.InvalidPhone) {
                stringResource(R.string.profile_setup_error_phone)
            } else {
                null
            },
            onValueChange = { onIntent(ProfileSetupIntent.PhoneChanged(it)) },
        )

        ProfileSetupSaveButton(
            text = stringResource(
                if (isEditMode) R.string.profile_setup_save_button else R.string.profile_setup_done_button,
            ),
            isEnabled = isFormValid,
            isLoading = uiState.isSaving,
            onClick = { onIntent(ProfileSetupIntent.SaveClicked) },
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ProfileSetupContentPreview() {
    ShareMyLocationTheme {
        ProfileSetupContent(
            uiState = ProfileSetupUiState(),
            isEditMode = false,
            onIntent = {},
            onPickAvatar = {},
        )
    }
}
