package ru.tyupik.sharemylocation.ui.map.composable

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ru.tyupik.sharemylocation.ui.map.VisibleFriendUiModel

/**
 * Список видимых друзей в ModalBottomSheet: карточка на друга, клик — слежение камеры.
 * Закрытие любым способом (свайп, тап вне, назад) — [onDismiss] → ListDismissed.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun MapFriendsListSheet(
    friends: List<VisibleFriendUiModel>,
    onFriendClick: (String) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ModalBottomSheet(onDismissRequest = onDismiss, modifier = modifier) {
        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(friends, key = { it.uid }) { friend ->
                MapFriendListCard(
                    friend = friend,
                    onClick = { onFriendClick(friend.uid) },
                )
            }
        }
    }
}
