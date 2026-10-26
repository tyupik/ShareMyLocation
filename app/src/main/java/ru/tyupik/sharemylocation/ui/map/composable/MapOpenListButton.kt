package ru.tyupik.sharemylocation.ui.map.composable

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import ru.tyupik.sharemylocation.R
import ru.tyupik.sharemylocation.ui.theme.AuroraGlass
import ru.tyupik.sharemylocation.ui.theme.AuroraGlassBorder

/** Чип «N на карте»: открывает список видимых друзей (ModalBottomSheet). */
@Composable
internal fun MapOpenListButton(
    visibleCount: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        color = AuroraGlass,
        contentColor = MaterialTheme.colorScheme.onSurface,
        border = BorderStroke(1.dp, AuroraGlassBorder),
        shape = CircleShape,
        modifier = modifier,
    ) {
        Text(
            text = stringResource(R.string.map_open_list, visibleCount),
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier
                .clickable(onClick = onClick)
                .padding(horizontal = 20.dp, vertical = 10.dp),
        )
    }
}
