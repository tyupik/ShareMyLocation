package ru.tyupik.sharemylocation.ui.groups.composable

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import ru.tyupik.sharemylocation.R

/** Кнопка запроса удаления группы; доступна только в режиме редактирования. */
@Composable
internal fun GroupEditorDeleteButton(
    isEnabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    TextButton(
        onClick = onClick,
        enabled = isEnabled,
        modifier = modifier.fillMaxWidth(),
    ) {
        Text(
            text = stringResource(R.string.groups_editor_delete_button),
            color = MaterialTheme.colorScheme.error,
        )
    }
}
