package ru.tyupik.sharemylocation.ui.friends.composable

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import ru.tyupik.sharemylocation.ui.common.composable.ErrorBanner

/** Баннер ошибки действия над друзьями/заявками над списком секций. */
@Composable
internal fun FriendsErrorBanner(
    text: String,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ErrorBanner(text = text, onDismiss = onDismiss, modifier = modifier)
}
