package ru.tyupik.sharemylocation.ui.map.composable

import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.rememberUpdatedMarkerState
import ru.tyupik.sharemylocation.ui.map.VisibleFriendUiModel

/**
 * Маркеры видимых друзей: дефолтный маркер (иконки-аватары — граница фазы) с title = имя.
 * Ключ композиции маркера — uid: тап по маркеру и тап по карточке списка шлют один Intent,
 * а маркеры не «переезжают» между uid при изменении списка.
 * [rememberUpdatedMarkerState] двигает маркер при каждом тике координат без пересоздания.
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
            Marker(
                state = markerState,
                title = friend.displayName,
                onClick = {
                    onFriendClick(friend.uid)
                    true
                },
            )
        }
    }
}
