package ru.tyupik.sharemylocation.ui.friends.composable

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import ru.tyupik.sharemylocation.R

/**
 * Заголовок вкладки «Друзья» с колокольчиком заявок справа: заявки живут в bottom-sheet'е,
 * поэтому отдельной секции в теле вкладки больше нет.
 */
@Composable
internal fun FriendsHeader(
    requestCount: Int,
    onBellClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(R.string.friends_screen_title),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.weight(1f),
        )
        FriendsRequestsBellButton(
            requestCount = requestCount,
            onClick = onBellClick,
        )
    }
}
