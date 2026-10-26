package ru.tyupik.sharemylocation.ui.friends.composable

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.Badge
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import ru.tyupik.sharemylocation.R
import ru.tyupik.sharemylocation.ui.common.composable.GlassIconButton

/**
 * Кнопка-«колокольчик» заявок в друзья: открывает bottom-sheet со списком заявок.
 * Бейдж с количеством показывается только при [requestCount] > 0 (как бейдж вкладки
 * друзей в MainBottomBar): нулевой бейдж визуального шума не добавляет.
 */
@Composable
internal fun FriendsRequestsBellButton(
    requestCount: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val contentDescription = stringResource(R.string.friends_requests_bell)
    GlassIconButton(
        icon = Icons.Outlined.Notifications,
        contentDescription = contentDescription,
        onClick = onClick,
        modifier = modifier,
        badge = if (requestCount > 0) {
            { Badge { Text(text = requestCount.toString()) } }
        } else {
            null
        },
    )
}
