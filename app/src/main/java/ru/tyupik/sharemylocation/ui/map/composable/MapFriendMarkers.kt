package ru.tyupik.sharemylocation.ui.map.composable

import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.rememberUpdatedMarkerState
import ru.tyupik.sharemylocation.ui.map.VisibleFriendUiModel

/**
 * Маркеры видимых друзей: круглые аватар-иконки (без аватара — круг с инициалом),
 * anchor = центр круга — координата друга в его середине, а не в нижней вершине.
 * Ключ композиции маркера — uid: тап по маркеру и тап по карточке списка шлют один Intent,
 * а маркеры не «переезжают» между uid при изменении списка.
 * [rememberUpdatedMarkerState] двигает маркер при каждом тике координат без пересоздания;
 * иконка же пересоздаётся только при смене (avatarPath, displayName) — позиция и иконка
 * независимые параметры Marker, тики без мигания иконки.
 */
@Composable
internal fun MapFriendMarkers(
    friends: List<VisibleFriendUiModel>,
    onFriendClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    friends.forEach { friend ->
        key(friend.uid) {
            val markerState = rememberUpdatedMarkerState(LatLng(friend.latitude, friend.longitude))
            val icon = rememberFriendMarkerIcon(friend.uid, friend.avatarPath, friend.displayName)
            Marker(
                state = markerState,
                title = friend.displayName,
                icon = icon,
                anchor = Offset(0.5f, 0.5f),
                onClick = {
                    onFriendClick(friend.uid)
                    true
                },
            )
        }
    }
}
