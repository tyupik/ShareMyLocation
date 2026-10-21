package ru.tyupik.sharemylocation.ui.sharing

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ru.tyupik.sharemylocation.R
import ru.tyupik.sharemylocation.ui.sharing.composable.SharingAudienceOption
import ru.tyupik.sharemylocation.ui.sharing.composable.SharingCheckRow
import ru.tyupik.sharemylocation.ui.sharing.composable.SharingEnabledSwitch
import ru.tyupik.sharemylocation.ui.sharing.composable.SharingSettingsErrorBanner
import ru.tyupik.sharemylocation.ui.sharing.composable.SharingSettingsLoadError
import ru.tyupik.sharemylocation.ui.theme.ShareMyLocationTheme

/**
 * Экран настроек шаринга геолокации: главный тумблер, выбор аудитории и секции чекбоксов
 * «Люди»/«Группы». Автосохранение без кнопки «Сохранить»: каждое изменение немедленно пишется
 * ViewModel'ю, [SharingSettingsUiState.isSaving] показывает индикатор «Сохранение…»,
 * ошибка записи — inline-баннер. Секции выбора видны только при [SharingAudienceUi.CUSTOM]
 * и остаются редактируемыми при выключенном тумблере. Подсказка о пустой аудитории —
 * снекбар из Effect. Перечитывание при показе — через [SharingSettingsIntent.ScreenShown].
 */
@Composable
internal fun SharingSettingsScreen(
    modifier: Modifier = Modifier,
    viewModel: SharingSettingsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    LaunchedEffect(Unit) { viewModel.onIntent(SharingSettingsIntent.ScreenShown) }

    LaunchedEffect(Unit) {
        viewModel.effects.collect { effect ->
            when (effect) {
                SharingSettingsEffect.ShowEmptyAudienceHint ->
                    snackbarHostState.showSnackbar(
                        context.getString(R.string.sharing_hint_empty_audience),
                    )
            }
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        SharingSettingsContent(uiState = uiState, onIntent = viewModel::onIntent)
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 16.dp),
        )
    }
}

@Composable
private fun SharingSettingsContent(
    uiState: SharingSettingsUiState,
    onIntent: (SharingSettingsIntent) -> Unit,
) {
    if (uiState.isLoading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }
    if (uiState.isLoadFailed) {
        SharingSettingsLoadError(
            text = loadErrorText(uiState.loadError),
            onRetry = { onIntent(SharingSettingsIntent.ScreenShown) },
        )
        return
    }

    // Производный флаг: секции выбора видны только в CUSTOM (при выключенном тумблере тоже).
    val isCustomAudience = uiState.audience == SharingAudienceUi.CUSTOM

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .safeDrawingPadding()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = stringResource(R.string.sharing_title),
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(top = 24.dp),
        )

        if (uiState.isSaving) {
            // Индикатор «Сохранение…»: контролы при автосохранении не дизейблятся.
            Text(
                text = stringResource(R.string.sharing_saving),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        uiState.saveError?.let { error ->
            SharingSettingsErrorBanner(
                text = errorText(error),
                onDismiss = { onIntent(SharingSettingsIntent.SaveErrorDismissed) },
            )
        }

        SharingEnabledSwitch(
            isChecked = uiState.isEnabled,
            onToggle = { onIntent(SharingSettingsIntent.EnabledToggled(it)) },
        )

        SharingAudienceOption(
            label = stringResource(R.string.sharing_audience_all_friends),
            isSelected = uiState.audience == SharingAudienceUi.ALL_FRIENDS,
            onClick = {
                onIntent(SharingSettingsIntent.AudienceSelected(SharingAudienceUi.ALL_FRIENDS))
            },
        )
        SharingAudienceOption(
            label = stringResource(R.string.sharing_audience_custom),
            isSelected = isCustomAudience,
            onClick = { onIntent(SharingSettingsIntent.AudienceSelected(SharingAudienceUi.CUSTOM)) },
        )

        if (isCustomAudience) {
            SharingSelectionSections(uiState = uiState, onIntent = onIntent)
        }
    }
}

/** Секции «Люди»/«Группы»: падение секции — inline-ошибка, пустая — плейсхолдер (паттерн Friends). */
@Composable
private fun SharingSelectionSections(
    uiState: SharingSettingsUiState,
    onIntent: (SharingSettingsIntent) -> Unit,
) {
    Text(
        text = stringResource(R.string.sharing_section_people),
        style = MaterialTheme.typography.titleMedium,
    )
    if (uiState.isFriendsLoadFailed) {
        // Падение секции людей не скрывает других секций: inline-состояние + «Повторить».
        SharingSectionLoadError(onRetry = { onIntent(SharingSettingsIntent.ScreenShown) })
    } else if (uiState.friends.isEmpty()) {
        SharingSectionPlaceholder(stringResource(R.string.sharing_empty_friends))
    } else {
        uiState.friends.forEach { friend ->
            SharingCheckRow(
                row = friend,
                onToggle = { onIntent(SharingSettingsIntent.FriendToggled(friend.id)) },
            )
        }
    }

    Text(
        text = stringResource(R.string.sharing_section_groups),
        style = MaterialTheme.typography.titleMedium,
    )
    if (uiState.isGroupsLoadFailed) {
        // Падение секции групп не скрывает других секций: inline-состояние + «Повторить».
        SharingSectionLoadError(onRetry = { onIntent(SharingSettingsIntent.ScreenShown) })
    } else if (uiState.groups.isEmpty()) {
        SharingSectionPlaceholder(stringResource(R.string.sharing_empty_groups))
    } else {
        uiState.groups.forEach { group ->
            SharingCheckRow(
                row = group,
                onToggle = { onIntent(SharingSettingsIntent.GroupToggled(group.id)) },
            )
        }
    }
}

/** Плейсхолдер пустой секции выбора: «Друзей пока нет» / «Групп пока нет». */
@Composable
private fun SharingSectionPlaceholder(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
    )
}

/** Inline-состояние упавшей realtime-секции: текст ошибки и «Повторить» (= ScreenShown). */
@Composable
private fun SharingSectionLoadError(onRetry: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(R.string.sharing_load_error),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        TextButton(onClick = onRetry) {
            Text(stringResource(R.string.sharing_retry))
        }
    }
}

/** Текст экрана-ошибки: сеть показывается явно, остальное — generic. */
@Composable
private fun loadErrorText(error: SharingUiError?): String = when (error) {
    SharingUiError.Network -> stringResource(R.string.sharing_error_network)
    is SharingUiError.Unexpected -> stringResource(R.string.sharing_error_unexpected)
    null -> stringResource(R.string.sharing_load_error)
}

@Composable
private fun errorText(error: SharingUiError): String = when (error) {
    SharingUiError.Network -> stringResource(R.string.sharing_error_network)
    is SharingUiError.Unexpected -> stringResource(R.string.sharing_error_unexpected)
}

@Preview(showBackground = true)
@Composable
private fun SharingSettingsContentPreview() {
    ShareMyLocationTheme {
        SharingSettingsContent(
            uiState = SharingSettingsUiState(
                isLoading = false,
                isEnabled = true,
                audience = SharingAudienceUi.CUSTOM,
                friends = listOf(
                    SharingCheckRowUiModel(id = "uid-1", displayName = "Иван", isChecked = true),
                    SharingCheckRowUiModel(id = "uid-2", displayName = "Мария", isChecked = false),
                ),
                groups = listOf(
                    SharingCheckRowUiModel(id = "group-1", displayName = "Семья", isChecked = false),
                ),
            ),
            onIntent = {},
        )
    }
}
