package ru.tyupik.sharemylocation.ui.map

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.CameraPositionState
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.rememberCameraPositionState
import ru.tyupik.sharemylocation.BuildConfig
import ru.tyupik.sharemylocation.ui.map.composable.MapFriendMarkers
import ru.tyupik.sharemylocation.ui.map.composable.MapFriendsListSheet
import ru.tyupik.sharemylocation.ui.map.composable.MapLoadError
import ru.tyupik.sharemylocation.ui.map.composable.MapLocateMeButton
import ru.tyupik.sharemylocation.ui.map.composable.MapOpenListButton
import ru.tyupik.sharemylocation.ui.map.composable.MapUnavailableNotice

/**
 * Вкладка «Карта»: GoogleMap с аватар-маркерами видимых друзей, синей точкой своего
 * местоположения (isMyLocationEnabled — только при фактических правах), кнопкой центровки
 * и кнопкой списка с bottom-sheet'ом. Перечитывание при показе вкладки — через
 * [MapIntent.ScreenShown]. Сбор Effect: запрос системного диалога прав геолокации,
 * результат — intent'ом [MapIntent.LocationPermissionsResult].
 *
 * Слежение камеры — производная состояния в едином LaunchedEffect: без выбранного друга
 * камера центруется на свою точку ([MapUiState.myLocation] — каждый свежий фикс даёт новый
 * инстанс, StateFlow-дедупликации нет), при выборе — следует за тиками координат друга.
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

    // Системный диалог прав геолокации: запускается Effect'ом, результат — intent'ом.
    // POST_NOTIFICATIONS карте не нужен — запрашиваем только локацию; выданным считаем
    // любой грант локации (как в SharingSettingsScreen).
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
    ) { permissions ->
        val isLocationGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        viewModel.onIntent(MapIntent.LocationPermissionsResult(granted = isLocationGranted))
    }

    LaunchedEffect(Unit) { viewModel.onIntent(MapIntent.ScreenShown) }

    LaunchedEffect(Unit) {
        viewModel.effects.collect { effect ->
            when (effect) {
                MapEffect.RequestLocationPermissions -> permissionLauncher.launch(
                    arrayOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION,
                    ),
                )
            }
        }
    }

    val selectedFriend = uiState.visibleFriends.firstOrNull { it.uid == uiState.selectedUid }
    val cameraPositionState = rememberCameraPositionState()
    val selectedUid = uiState.selectedUid
    val myLocation = uiState.myLocation

    LaunchedEffect(selectedUid, myLocation, selectedFriend?.latitude, selectedFriend?.longitude) {
        when {
            selectedUid == null && myLocation != null ->
                cameraPositionState.animate(
                    CameraUpdateFactory.newCameraPosition(
                        CameraPosition.fromLatLngZoom(
                            LatLng(myLocation.latitude, myLocation.longitude),
                            MY_LOCATION_ZOOM,
                        ),
                    ),
                )

            else -> selectedFriend?.let { friend ->
                cameraPositionState.animate(
                    CameraUpdateFactory.newCameraPosition(
                        CameraPosition.fromLatLngZoom(
                            LatLng(friend.latitude, friend.longitude),
                            SELECTED_ZOOM,
                        ),
                    ),
                )
            }
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
                // Синяя точка своего местоположения — только при фактических грантах:
                // isMyLocationEnabled без прав — SecurityException Maps SDK. Встроенная
                // кнопка выключена — центровка через MapLocateMeButton (снимает слежение).
                properties = MapProperties(isMyLocationEnabled = uiState.hasLocationPermission),
                uiSettings = MapUiSettings(myLocationButtonEnabled = false),
            ) {
                MapFriendMarkers(
                    friends = uiState.visibleFriends,
                    onFriendClick = { uid -> onIntent(MapIntent.FriendSelected(uid)) },
                )
            }
            MapLocateMeButton(
                onClick = { onIntent(MapIntent.LocateMeClicked) },
                // end = 8.dp — выровнено с правым отступом нативных zoom-контролов Google;
                // bottom = 132.dp — над стеком зума (~48dp x 2 от низа ~10–12dp, верх ≈ 106–116dp)
                // с зазором ≥ 16dp.
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 8.dp, bottom = 132.dp),
            )
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

/** Zoom камеры центровки на свою точку. */
private const val MY_LOCATION_ZOOM = 16f
