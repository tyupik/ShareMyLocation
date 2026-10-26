package ru.tyupik.sharemylocation.ui.friends.composable

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import ru.tyupik.sharemylocation.R
import ru.tyupik.sharemylocation.ui.common.ProfileAvatar
import ru.tyupik.sharemylocation.ui.common.composable.GlassCard
import ru.tyupik.sharemylocation.ui.friends.FriendUiModel

/** Карточка найденного по телефону пользователя с кнопкой «Добавить в друзья». */
@Composable
internal fun FriendsSearchResultCard(
    user: FriendUiModel,
    isBusy: Boolean,
    onAddClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    GlassCard(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            ProfileAvatar(
                displayName = user.displayName,
                localPath = user.avatarPath,
                size = CARD_AVATAR_SIZE,
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = user.displayName,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                user.phone?.let { phone ->
                    Text(
                        text = phone,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            if (isBusy) {
                CircularProgressIndicator(
                    modifier = Modifier.size(BUTTON_PROGRESS_SIZE),
                    strokeWidth = BUTTON_PROGRESS_STROKE,
                )
            } else {
                Button(onClick = onAddClick) {
                    Text(stringResource(R.string.friends_add_button))
                }
            }
        }
    }
}

private val CARD_AVATAR_SIZE = 56.dp
private val BUTTON_PROGRESS_SIZE = 24.dp
private val BUTTON_PROGRESS_STROKE = 2.dp
