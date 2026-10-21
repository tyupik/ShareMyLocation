package ru.tyupik.sharemylocation.ui.groups.composable

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import ru.tyupik.sharemylocation.R

/** Кнопка сохранения группы; в [isLoading] вместо текста показывает индикатор загрузки. */
@Composable
internal fun GroupEditorSaveButton(
    isEnabled: Boolean,
    isLoading: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Button(
        onClick = onClick,
        enabled = isEnabled && !isLoading,
        modifier = modifier.fillMaxWidth(),
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                strokeWidth = 2.dp,
                color = LocalContentColor.current,
            )
        } else {
            Text(text = stringResource(R.string.groups_editor_save_button))
        }
    }
}
