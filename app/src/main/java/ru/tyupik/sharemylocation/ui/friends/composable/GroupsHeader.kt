package ru.tyupik.sharemylocation.ui.friends.composable

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import ru.tyupik.sharemylocation.R

/** Заголовок секции «Группы» с кнопкой «Создать» (открывает редактор в режиме создания). */
@Composable
internal fun GroupsHeader(
    onCreateClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(R.string.groups_section),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.weight(1f),
        )
        TextButton(onClick = onCreateClick) {
            Text(stringResource(R.string.groups_create_button))
        }
    }
}
