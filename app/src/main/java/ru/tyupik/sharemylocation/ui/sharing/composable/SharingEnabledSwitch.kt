package ru.tyupik.sharemylocation.ui.sharing.composable

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import ru.tyupik.sharemylocation.R
import ru.tyupik.sharemylocation.ui.common.composable.GlassCard

/**
 * Строка главного переключателя шаринга: стеклянная карточка, подпись и Switch;
 * клик по всей строке переключает. Выключенное состояние — обещание приватности
 * «уже выключено», поэтому без задержек.
 */
@Composable
internal fun SharingEnabledSwitch(
    isChecked: Boolean,
    onToggle: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    GlassCard(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onToggle(!isChecked) }
                .padding(horizontal = 12.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.sharing_enabled_label),
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            Switch(checked = isChecked, onCheckedChange = onToggle)
        }
    }
}
