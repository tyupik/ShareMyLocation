package ru.tyupik.sharemylocation.ui.map.composable

import androidx.compose.material3.AssistChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import ru.tyupik.sharemylocation.R

/** Чип «N на карте»: открывает список видимых друзей (ModalBottomSheet). */
@Composable
internal fun MapOpenListButton(
    visibleCount: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AssistChip(
        onClick = onClick,
        modifier = modifier,
        label = { Text(stringResource(R.string.map_open_list, visibleCount)) },
    )
}
