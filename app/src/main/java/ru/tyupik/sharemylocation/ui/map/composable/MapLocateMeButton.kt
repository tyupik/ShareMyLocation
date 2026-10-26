package ru.tyupik.sharemylocation.ui.map.composable

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import ru.tyupik.sharemylocation.R
import ru.tyupik.sharemylocation.ui.theme.AuroraGlass

/**
 * Круглая кнопка центровки (иконка геолокации вместо текста), размещается над
 * zoom-контролами Google на call-site: центровка камеры на свою точку
 * (или запрос прав геолокации). Стеклянный стиль AURORA.
 */
@Composable
internal fun MapLocateMeButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    SmallFloatingActionButton(
        onClick = onClick,
        containerColor = AuroraGlass,
        contentColor = MaterialTheme.colorScheme.onSurface,
        shape = MaterialTheme.shapes.medium,
        modifier = modifier,
    ) {
        Icon(
            imageVector = Icons.Filled.MyLocation,
            contentDescription = stringResource(R.string.map_locate_me),
        )
    }
}
