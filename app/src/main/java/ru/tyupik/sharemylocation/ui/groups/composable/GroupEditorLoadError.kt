package ru.tyupik.sharemylocation.ui.groups.composable

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import ru.tyupik.sharemylocation.R
import ru.tyupik.sharemylocation.ui.common.composable.LoadError

/**
 * Экран-ошибка первичной загрузки редактора (группа не прочитана или упал поток друзей):
 * текст причины и «Повторить» — повторный ScreenShown.
 */
@Composable
internal fun GroupEditorLoadError(
    text: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    LoadError(
        text = text,
        retryText = stringResource(R.string.groups_retry),
        onRetry = onRetry,
        modifier = modifier,
        isCentered = true,
    )
}
