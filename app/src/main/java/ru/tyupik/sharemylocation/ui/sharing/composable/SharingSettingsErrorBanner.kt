package ru.tyupik.sharemylocation.ui.sharing.composable

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import ru.tyupik.sharemylocation.ui.common.composable.ErrorBanner

/** Баннер ошибки автосохранения настроек шаринга над формой (паттерн GroupEditorErrorBanner). */
@Composable
internal fun SharingSettingsErrorBanner(
    text: String,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ErrorBanner(text = text, onDismiss = onDismiss, modifier = modifier)
}
