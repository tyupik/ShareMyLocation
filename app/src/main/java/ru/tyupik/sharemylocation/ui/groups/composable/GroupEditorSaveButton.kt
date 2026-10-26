package ru.tyupik.sharemylocation.ui.groups.composable

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import ru.tyupik.sharemylocation.R
import ru.tyupik.sharemylocation.ui.common.composable.GradientPrimaryButton

/** Кнопка сохранения группы; в [isLoading] вместо текста показывает индикатор загрузки. */
@Composable
internal fun GroupEditorSaveButton(
    isEnabled: Boolean,
    isLoading: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    GradientPrimaryButton(
        text = stringResource(R.string.groups_editor_save_button),
        isEnabled = isEnabled,
        isLoading = isLoading,
        onClick = onClick,
        modifier = modifier,
    )
}
