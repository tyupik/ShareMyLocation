package ru.tyupik.sharemylocation.ui.auth.composable

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import ru.tyupik.sharemylocation.ui.common.composable.ErrorBanner

/** Баннер ошибки над формой: для ошибок без привязки к конкретному полю. */
@Composable
internal fun AuthErrorBanner(
    text: String,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ErrorBanner(text = text, onDismiss = onDismiss, modifier = modifier)
}
