package ru.tyupik.sharemylocation.ui.friends

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ru.tyupik.sharemylocation.R
import ru.tyupik.sharemylocation.ui.friends.composable.FriendCard
import ru.tyupik.sharemylocation.ui.friends.composable.FriendRemoveConfirmDialog
import ru.tyupik.sharemylocation.ui.friends.composable.FriendsEmptyState
import ru.tyupik.sharemylocation.ui.friends.composable.FriendsErrorBanner
import ru.tyupik.sharemylocation.ui.friends.composable.FriendsLoadError
import ru.tyupik.sharemylocation.ui.friends.composable.FriendsRequestCard
import ru.tyupik.sharemylocation.ui.friends.composable.FriendsSearchField
import ru.tyupik.sharemylocation.ui.friends.composable.FriendsSearchResultCard
import ru.tyupik.sharemylocation.ui.friends.composable.GroupCard
import ru.tyupik.sharemylocation.ui.friends.composable.GroupsHeader
import ru.tyupik.sharemylocation.ui.theme.ShareMyLocationTheme

/**
 * Вкладка «Друзья»: поиск по номеру телефона, входящие заявки, список друзей и группы.
 * Realtime-обновления приходят потоками из интеракторов; перечитывание при показе
 * вкладки выполняется через [FriendsIntent.ScreenShown]. Эффекты: [FriendsEffect.FriendRequestSent]
 * показывается Snackbar'ом, [FriendsEffect.OpenGroupEditor] открывает редактор группы
 * через [onOpenGroupEditor].
 */
@Composable
internal fun FriendsScreen(
    onOpenGroupEditor: (String?) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: FriendsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    LaunchedEffect(Unit) { viewModel.onIntent(FriendsIntent.ScreenShown) }

    LaunchedEffect(Unit) {
        viewModel.effects.collect { effect ->
            when (effect) {
                FriendsEffect.FriendRequestSent ->
                    snackbarHostState.showSnackbar(context.getString(R.string.friends_request_sent))
                is FriendsEffect.OpenGroupEditor -> onOpenGroupEditor(effect.groupId)
            }
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        FriendsContent(uiState = uiState, onIntent = viewModel::onIntent)
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 16.dp),
        )
        uiState.removeDialogFriend?.let { friend ->
            FriendRemoveConfirmDialog(
                friendName = friend.displayName,
                isBusy = uiState.busyUid == friend.uid,
                onConfirm = { viewModel.onIntent(FriendsIntent.RemoveFriendConfirmed) },
                onDismiss = { viewModel.onIntent(FriendsIntent.RemoveFriendDialogDismissed) },
            )
        }
    }
}

@Composable
private fun FriendsContent(
    uiState: FriendsUiState,
    onIntent: (FriendsIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item {
            Text(
                text = stringResource(R.string.friends_search_hint),
                style = MaterialTheme.typography.titleMedium,
            )
        }
        item {
            FriendsSearchField(
                value = uiState.searchQuery,
                isEnabled = uiState.busyUid == null,
                onValueChange = { onIntent(FriendsIntent.SearchQueryChanged(it)) },
                onSearch = { onIntent(FriendsIntent.SearchClicked) },
                onClear = { onIntent(FriendsIntent.SearchCleared) },
            )
        }
        uiState.actionError?.let { error ->
            item {
                FriendsErrorBanner(
                    text = errorText(error),
                    onDismiss = { onIntent(FriendsIntent.ErrorDismissed) },
                )
            }
        }
        when (val searchState = uiState.searchState) {
            FriendsSearchUiState.Idle -> Unit

            FriendsSearchUiState.Loading -> item {
                Box(modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp)) {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
            }

            is FriendsSearchUiState.Found -> item {
                FriendsSearchResultCard(
                    user = searchState.user,
                    isBusy = uiState.busyUid == searchState.user.uid,
                    onAddClick = { onIntent(FriendsIntent.AddFriendClicked(searchState.user.uid)) },
                )
            }

            FriendsSearchUiState.NotFound -> item {
                FriendsEmptyState(text = stringResource(R.string.friends_error_user_not_found))
            }

            is FriendsSearchUiState.Error -> item {
                Text(
                    text = errorText(searchState.error),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        item {
            Text(
                text = stringResource(R.string.friends_section_requests),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
        if (uiState.isRequestsLoadFailed) {
            // Падение секции заявок не скрывает других секций: inline-состояние + «Повторить».
            item { FriendsLoadError(onRetry = { onIntent(FriendsIntent.ScreenShown) }) }
        } else if (uiState.incomingRequests.isEmpty()) {
            item { FriendsEmptyState(text = stringResource(R.string.friends_empty_requests)) }
        } else {
            items(uiState.incomingRequests, key = { it.fromUid }) { request ->
                FriendsRequestCard(
                    request = request,
                    isBusy = uiState.busyUid == request.fromUid,
                    onAcceptClick = { onIntent(FriendsIntent.AcceptRequestClicked(request.fromUid)) },
                    onDeclineClick = { onIntent(FriendsIntent.DeclineRequestClicked(request.fromUid)) },
                )
            }
        }
        item {
            Text(
                text = stringResource(R.string.friends_section_friends),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
        if (uiState.isFriendsLoadFailed) {
            // Падение секции друзей не скрывает других секций: inline-состояние + «Повторить».
            item { FriendsLoadError(onRetry = { onIntent(FriendsIntent.ScreenShown) }) }
        } else if (uiState.isFriendsLoading) {
            item {
                Box(modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp)) {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
            }
        } else if (uiState.friends.isEmpty()) {
            item { FriendsEmptyState(text = stringResource(R.string.friends_empty_friends)) }
        } else {
            items(uiState.friends, key = { it.uid }) { friend ->
                FriendCard(
                    friend = friend,
                    isBusy = uiState.busyUid == friend.uid,
                    onRemoveClick = { onIntent(FriendsIntent.RemoveFriendClicked(friend.uid)) },
                )
            }
        }
        item { GroupsHeader(onCreateClick = { onIntent(FriendsIntent.CreateGroupClicked) }) }
        if (uiState.isGroupsLoadFailed) {
            // Падение секции групп не скрывает других секций: inline-состояние + «Повторить».
            item { FriendsLoadError(onRetry = { onIntent(FriendsIntent.ScreenShown) }) }
        } else if (uiState.isGroupsLoading) {
            item {
                Box(modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp)) {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
            }
        } else if (uiState.groups.isEmpty()) {
            item { FriendsEmptyState(text = stringResource(R.string.groups_empty)) }
        } else {
            items(uiState.groups, key = { it.groupId }) { group ->
                GroupCard(
                    group = group,
                    onGroupClick = { onIntent(FriendsIntent.GroupClicked(group.groupId)) },
                )
            }
        }
    }
}

@Composable
private fun errorText(error: FriendsUiError): String = when (error) {
    FriendsUiError.Network -> stringResource(R.string.friends_error_network)
    FriendsUiError.InvalidPhone -> stringResource(R.string.friends_error_invalid_phone)
    FriendsUiError.RequestToSelf -> stringResource(R.string.friends_error_request_to_self)
    FriendsUiError.AlreadyFriends -> stringResource(R.string.friends_error_already_friends)
    FriendsUiError.RequestAlreadySent -> stringResource(R.string.friends_error_request_already_sent)
    FriendsUiError.RequestAlreadyIncoming ->
        stringResource(R.string.friends_error_request_already_incoming)
    is FriendsUiError.Unexpected -> stringResource(R.string.friends_error_unexpected)
}

@Preview(showBackground = true)
@Composable
private fun FriendsContentPreview() {
    ShareMyLocationTheme {
        FriendsContent(uiState = FriendsUiState(isFriendsLoading = false), onIntent = {})
    }
}
