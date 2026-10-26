package ru.tyupik.sharemylocation.ui.sharing.composable

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import ru.tyupik.sharemylocation.R
import ru.tyupik.sharemylocation.ui.common.composable.LoadError

/**
 * Экран-ошибка первичного чтения настроек шаринга: текст причины и «Повторить» —
 * повторный ScreenShown (паттерн GroupEditorLoadError).
 */
@Composable
internal fun SharingSettingsLoadError(
    text: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    LoadError(
        text = text,
        retryText = stringResource(R.string.sharing_retry),
        onRetry = onRetry,
        modifier = modifier,
        isCentered = true,
    )
}
