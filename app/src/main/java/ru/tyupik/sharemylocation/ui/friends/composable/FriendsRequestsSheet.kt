package ru.tyupik.sharemylocation.ui.friends.composable

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import ru.tyupik.sharemylocation.R
import ru.tyupik.sharemylocation.ui.friends.FriendRequestUiModel

/**
 * Bottom-sheet входящих заявок в друзья: открывается по колокольчику в [FriendsHeader].
 * Ошибка действия (принять/отклонить) показывается inline над списком; упавший realtime-поток —
 * как [FriendsLoadError] с «Повторить». Пустое состояние обязательно: realtime-поток может
 * обнулить [requests] при открытом листе (последнюю заявку приняли/отклонили) — как в
 * MapFriendsListSheet. Закрытие любым способом (свайп, тап вне, назад) — [onDismiss].
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun FriendsRequestsSheet(
    requests: List<FriendRequestUiModel>,
    busyUid: String?,
    isRequestsLoadFailed: Boolean,
    errorText: String?,
    onAcceptClick: (String) -> Unit,
    onDeclineClick: (String) -> Unit,
    onRetry: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ModalBottomSheet(onDismissRequest = onDismiss, modifier = modifier) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = stringResource(R.string.friends_section_requests),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(horizontal = 16.dp),
            )
            errorText?.let { text ->
                Text(
                    text = text,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                )
            }
            when {
                isRequestsLoadFailed -> FriendsLoadError(onRetry = onRetry)

                requests.isEmpty() -> FriendsEmptyState(text = stringResource(R.string.friends_empty_requests))

                else -> LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(requests, key = { it.fromUid }) { request ->
                        FriendsRequestCard(
                            request = request,
                            isBusy = busyUid == request.fromUid,
                            onAcceptClick = { onAcceptClick(request.fromUid) },
                            onDeclineClick = { onDeclineClick(request.fromUid) },
                        )
                    }
                }
            }
        }
    }
}
