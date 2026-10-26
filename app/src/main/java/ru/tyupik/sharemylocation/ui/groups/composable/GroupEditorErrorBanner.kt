package ru.tyupik.sharemylocation.ui.groups.composable

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import ru.tyupik.sharemylocation.ui.common.composable.ErrorBanner

/** Баннер ошибки действия над группой (сохранить/удалить) над формой редактора. */
@Composable
internal fun GroupEditorErrorBanner(
    text: String,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ErrorBanner(text = text, onDismiss = onDismiss, modifier = modifier)
}
