package ru.tyupik.sharemylocation.ui.map

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.CameraPositionState
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.rememberCameraPositionState
import ru.tyupik.sharemylocation.BuildConfig
import ru.tyupik.sharemylocation.R
import ru.tyupik.sharemylocation.ui.map.composable.MapEmptyState
import ru.tyupik.sharemylocation.ui.map.composable.MapFriendMarkers
import ru.tyupik.sharemylocation.ui.map.composable.MapFriendsListSheet
import ru.tyupik.sharemylocation.ui.map.composable.MapLoadError
import ru.tyupik.sharemylocation.ui.map.composable.MapOpenListButton
import ru.tyupik.sharemylocation.ui.map.composable.MapUnavailableNotice

/**
 * Вкладка «Карта»: GoogleMap с маркерами видимых друзей, кнопкой списка и bottom-sheet'ом.
 * Перечитывание при показе вкладки — через [MapIntent.ScreenShown]. Навигаций и one-off
 * событий в фиче нет ([MapEffect] пуст), поэтому сбор Effect — заготовка под будущие.
 *
 * Слежение камеры за выбранным другом — производная состояния: тап ставит [MapUiState.selectedUid],
 * LaunchedEffect анимирует камеру к точке и продолжает следовать за тиками координат.
 * CameraPositionState — imperative-API composable-инстанса, Effect не смог бы адресовать его
 * без утечки UI во ViewModel, поэтому камера не Effect. Ручной сдвиг камеры слежение
 * НЕ отменяет: следующий тик координат выбранного друга вернёт камеру (жест отмены —
 * граница фазы).
 */
@Composable
internal fun MapScreen(
    modifier: Modifier = Modifier,
    viewModel: MapViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) { viewModel.onIntent(MapIntent.ScreenShown) }

    LaunchedEffect(Unit) {
        // MapEffect пуст: one-off событий в фиче нет (заготовка под будущие события,
        // обработка — как в FriendsScreen: collect { when (effect) { ... } }).
        viewModel.effects.collect { }
    }

    val selectedFriend = uiState.visibleFriends.firstOrNull { it.uid == uiState.selectedUid }
    val cameraPositionState = rememberCameraPositionState()

    LaunchedEffect(uiState.selectedUid, selectedFriend?.latitude, selectedFriend?.longitude) {
        selectedFriend?.let { friend ->
            cameraPositionState.animate(
                CameraUpdateFactory.newCameraPosition(
                    CameraPosition.fromLatLngZoom(LatLng(friend.latitude, friend.longitude), SELECTED_ZOOM),
                ),
            )
        }
    }

    MapContent(
        uiState = uiState,
        cameraPositionState = cameraPositionState,
        onIntent = viewModel::onIntent,
        modifier = modifier,
    )
}

@Composable
private fun MapContent(
    uiState: MapUiState,
    cameraPositionState: CameraPositionState,
    onIntent: (MapIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxSize()) {
        if (BuildConfig.MAPS_API_KEY.isBlank()) {
            // Без ключа карту не инициализируем вовсе: Maps SDK упал бы на старте фрагмента.
            MapUnavailableNotice(modifier = Modifier.fillMaxSize())
        } else {
            GoogleMap(
                modifier = Modifier.matchParentSize(),
                cameraPositionState = cameraPositionState,
            ) {
                MapFriendMarkers(
                    friends = uiState.visibleFriends,
                    onFriendClick = { uid -> onIntent(MapIntent.FriendSelected(uid)) },
                )
            }
        }
        if (uiState.isMapLoading && !uiState.isMapLoadFailed) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
        }
        if (uiState.isMapLoadFailed) {
            MapLoadError(
                onRetry = { onIntent(MapIntent.ScreenShown) },
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(horizontal = 24.dp),
            )
        }
        val isEmpty = !uiState.isMapLoading && !uiState.isMapLoadFailed && uiState.visibleFriends.isEmpty()
        if (isEmpty) {
            MapEmptyState(
                text = stringResource(
                    if (uiState.friendsCount == 0) {
                        R.string.map_empty_no_friends
                    } else {
                        R.string.map_empty_nobody_sharing
                    },
                ),
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(horizontal = 24.dp),
            )
        }
        if (uiState.visibleFriends.isNotEmpty()) {
            MapOpenListButton(
                visibleCount = uiState.visibleFriends.size,
                onClick = { onIntent(MapIntent.ShowListClicked) },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 16.dp),
            )
        }
    }
    if (uiState.isListVisible) {
        MapFriendsListSheet(
            friends = uiState.visibleFriends,
            onFriendClick = { uid -> onIntent(MapIntent.FriendSelected(uid)) },
            onDismiss = { onIntent(MapIntent.ListDismissed) },
        )
    }
}

/** Zoom камеры слежения за выбранным другом. */
private const val SELECTED_ZOOM = 16f
