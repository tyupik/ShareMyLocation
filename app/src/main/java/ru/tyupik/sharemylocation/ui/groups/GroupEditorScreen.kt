package ru.tyupik.sharemylocation.ui.groups

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import ru.tyupik.sharemylocation.ui.groups.composable.GroupDeleteConfirmDialog
import ru.tyupik.sharemylocation.ui.groups.composable.GroupEditorDeleteButton
import ru.tyupik.sharemylocation.ui.groups.composable.GroupEditorErrorBanner
import ru.tyupik.sharemylocation.ui.groups.composable.GroupEditorLoadError
import ru.tyupik.sharemylocation.ui.groups.composable.GroupEditorNameField
import ru.tyupik.sharemylocation.ui.groups.composable.GroupEditorSaveButton
import ru.tyupik.sharemylocation.ui.groups.composable.GroupMemberCheckRow
import ru.tyupik.sharemylocation.ui.theme.ShareMyLocationTheme

/**
 * Экран управления группой: [groupId] == null — создание, иначе — редактирование существующей.
 * Навигацию выполняет вызывающая сторона через [onClosed]: экран сам не знает про back stack,
 * лямбда вызывается только из коллекции Effect. Перечитывание при показе экрана — через
 * [GroupEditorIntent.ScreenShown].
 */
@Composable
internal fun GroupEditorScreen(
    groupId: String?,
    onClosed: () -> Unit,
    viewModel: GroupEditorViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) { viewModel.onIntent(GroupEditorIntent.ScreenShown(groupId)) }

    LaunchedEffect(Unit) {
        viewModel.effects.collect { effect ->
            when (effect) {
                GroupEditorEffect.Closed -> onClosed()
            }
        }
    }

    GroupEditorContent(uiState = uiState, onIntent = viewModel::onIntent)
}

@Composable
private fun GroupEditorContent(
    uiState: GroupEditorUiState,
    onIntent: (GroupEditorIntent) -> Unit,
) {
    if (uiState.isLoading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }
    if (uiState.isLoadFailed) {
        GroupEditorLoadError(
            text = loadErrorText(uiState.loadError),
            onRetry = { onIntent(GroupEditorIntent.ScreenShown(uiState.groupId)) },
        )
        return
    }

    // Производные флаги экрана: чистый UI-расчёт, не бизнес-состояние. Пустой состав допустим.
    val isCreateMode = uiState.groupId == null
    val selectedCount = uiState.friends.count { it.isChecked }
    val canSave = uiState.name.isNotBlank() && !uiState.isSaving && !uiState.isLoading

    Column(
        modifier = Modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .imePadding()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = stringResource(
                if (isCreateMode) {
                    R.string.groups_editor_title_create
                } else {
                    R.string.groups_editor_title_edit
                },
            ),
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(top = 24.dp),
        )

        uiState.actionError?.let { error ->
            GroupEditorErrorBanner(
                text = actionErrorText(error),
                onDismiss = { onIntent(GroupEditorIntent.ErrorDismissed) },
            )
        }

        GroupEditorNameField(
            value = uiState.name,
            isEnabled = !uiState.isSaving,
            onValueChange = { onIntent(GroupEditorIntent.NameChanged(it)) },
        )

        Text(
            text = stringResource(R.string.groups_editor_selected_count, selectedCount),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            items(uiState.friends, key = { it.uid }) { member ->
                GroupMemberCheckRow(
                    member = member,
                    isEnabled = !uiState.isSaving,
                    onToggle = { onIntent(GroupEditorIntent.MemberToggled(member.uid)) },
                )
            }
        }

        GroupEditorSaveButton(
            isEnabled = canSave,
            isLoading = uiState.isSaving,
            onClick = { onIntent(GroupEditorIntent.SaveClicked) },
        )
        if (!isCreateMode) {
            GroupEditorDeleteButton(
                isEnabled = !uiState.isSaving && !uiState.isDeleting,
                onClick = { onIntent(GroupEditorIntent.DeleteClicked) },
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
    }

    if (uiState.isDeleteDialogVisible) {
        GroupDeleteConfirmDialog(
            groupName = uiState.name,
            isBusy = uiState.isDeleting,
            onConfirm = { onIntent(GroupEditorIntent.DeleteConfirmed) },
            onDismiss = { onIntent(GroupEditorIntent.DeleteDialogDismissed) },
        )
    }
}

/** Текст экрана-ошибки: GroupNotFound/сеть показываются явно, остальное — generic. */
@Composable
private fun loadErrorText(error: GroupsUiError?): String = when (error) {
    GroupsUiError.Network -> stringResource(R.string.groups_error_network)
    GroupsUiError.GroupNotFound -> stringResource(R.string.groups_error_group_not_found)
    is GroupsUiError.Unexpected -> stringResource(R.string.groups_error_unexpected)
    GroupsUiError.EmptyName, GroupsUiError.NameTooLong, null ->
        stringResource(R.string.groups_load_error)
}

@Composable
private fun actionErrorText(error: GroupsUiError): String = when (error) {
    GroupsUiError.Network -> stringResource(R.string.groups_error_network)
    GroupsUiError.EmptyName -> stringResource(R.string.groups_error_empty_name)
    GroupsUiError.NameTooLong -> stringResource(R.string.groups_error_name_too_long)
    GroupsUiError.GroupNotFound -> stringResource(R.string.groups_error_group_not_found)
    is GroupsUiError.Unexpected -> stringResource(R.string.groups_error_unexpected)
}

@Preview(showBackground = true)
@Composable
private fun GroupEditorContentPreview() {
    ShareMyLocationTheme {
        GroupEditorContent(uiState = GroupEditorUiState(isLoading = false), onIntent = {})
    }
}
