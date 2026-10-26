package ru.tyupik.sharemylocation.ui.friends.composable

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import ru.tyupik.sharemylocation.R
import ru.tyupik.sharemylocation.ui.common.composable.LoadError

/** Inline-состояние упавшей секции: строка ошибки и «Повторить» (перезапускает потоки по ScreenShown). */
@Composable
internal fun FriendsLoadError(
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    LoadError(
        text = stringResource(R.string.friends_load_error),
        retryText = stringResource(R.string.friends_retry),
        onRetry = onRetry,
        modifier = modifier,
    )
}
