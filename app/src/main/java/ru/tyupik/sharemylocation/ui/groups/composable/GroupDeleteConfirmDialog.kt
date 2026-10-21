package ru.tyupik.sharemylocation.ui.groups.composable

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import ru.tyupik.sharemylocation.R

/** Диалог подтверждения удаления группы (Material 3). */
@Composable
internal fun GroupDeleteConfirmDialog(
    groupName: String,
    isBusy: Boolean,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.groups_delete_dialog_title)) },
        text = { Text(stringResource(R.string.groups_delete_dialog_text, groupName)) },
        confirmButton = {
            TextButton(onClick = onConfirm, enabled = !isBusy) {
                Text(
                    text = stringResource(R.string.groups_delete_dialog_confirm),
                    color = MaterialTheme.colorScheme.error,
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.groups_delete_dialog_cancel))
            }
        },
    )
}
