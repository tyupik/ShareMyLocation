package ru.tyupik.sharemylocation.ui.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.CircularProgressIndicator
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
import ru.tyupik.sharemylocation.ui.common.composable.LoadError
import ru.tyupik.sharemylocation.ui.profile.composable.ProfileEditButton
import ru.tyupik.sharemylocation.ui.profile.composable.ProfileHeader
import ru.tyupik.sharemylocation.ui.profile.composable.ProfileInfoRow
import ru.tyupik.sharemylocation.ui.profile.composable.ProfileSharingButton
import ru.tyupik.sharemylocation.ui.profile.composable.ProfileSignOutButton
import ru.tyupik.sharemylocation.ui.theme.ShareMyLocationTheme

/**
 * Вкладка «Профиль»: показывает аватар, имя, email и телефон, кнопки «Редактировать»,
 * «Делиться геопозицией» и «Выход». Навигацию выполняет вызывающая сторона: [onEditProfile]
 * и [onOpenSharingSettings] вызываются только из коллекции Effect, [onSignOutClick] передаёт
 * действие в контракт MainScreen (выход живёт в MainViewModel).
 */
@Composable
internal fun ProfileScreen(
    onEditProfile: () -> Unit,
    onOpenSharingSettings: () -> Unit,
    onSignOutClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ProfileViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) { viewModel.onIntent(ProfileIntent.ScreenShown) }

    LaunchedEffect(Unit) {
        viewModel.effects.collect { effect ->
            when (effect) {
                ProfileEffect.NavigateToEditProfile -> onEditProfile()
                ProfileEffect.NavigateToSharingSettings -> onOpenSharingSettings()
            }
        }
    }

    when (val state = uiState) {
        ProfileUiState.Loading -> Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }

        is ProfileUiState.Content -> ProfileContent(
            profile = state.profile,
            onIntent = viewModel::onIntent,
            onSignOutClick = onSignOutClick,
            modifier = modifier,
        )

        is ProfileUiState.Error -> ProfileErrorContent(
            error = state.error,
            onRetry = { viewModel.onIntent(ProfileIntent.ScreenShown) },
            modifier = modifier,
        )
    }
}

@Composable
private fun ProfileContent(
    profile: ProfileUiModel,
    onIntent: (ProfileIntent) -> Unit,
    onSignOutClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        ProfileHeader(
            displayName = profile.displayName,
            avatarPath = profile.avatarPath,
        )

        profile.email?.let { email ->
            ProfileInfoRow(
                icon = Icons.Filled.Email,
                text = email,
            )
        }

        profile.phone?.let { phone ->
            ProfileInfoRow(
                icon = Icons.Filled.Phone,
                text = phone,
            )
        }

        ProfileEditButton(onClick = { onIntent(ProfileIntent.EditClicked) })

        ProfileSharingButton(onClick = { onIntent(ProfileIntent.SharingClicked) })

        ProfileSignOutButton(onClick = onSignOutClick)
    }
}

@Composable
private fun ProfileErrorContent(
    error: ProfileUiError,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    LoadError(
        text = when (error) {
            ProfileUiError.Network -> stringResource(R.string.profile_error_network)
            is ProfileUiError.Unexpected -> stringResource(R.string.profile_error_unexpected)
        },
        retryText = stringResource(R.string.profile_retry),
        onRetry = onRetry,
        modifier = modifier,
        isCentered = true,
    )
}

@Preview(showBackground = true)
@Composable
private fun ProfileContentPreview() {
    ShareMyLocationTheme {
        ProfileContent(
            profile = ProfileUiModel(
                displayName = "Иван",
                email = "ivan@example.com",
                phone = "+79991234567",
                avatarPath = null,
            ),
            onIntent = {},
            onSignOutClick = {},
        )
    }
}
