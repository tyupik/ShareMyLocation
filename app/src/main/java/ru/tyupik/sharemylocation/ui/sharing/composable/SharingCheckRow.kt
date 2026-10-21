package ru.tyupik.sharemylocation.ui.sharing.composable

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import ru.tyupik.sharemylocation.ui.sharing.SharingCheckRowUiModel

/**
 * Строка чекбокса секций «Люди»/«Группы» настроек шаринга: чекбокс и имя; клик по всей строке
 * переключает чекбокс. Осознанный локальный аналог GroupMemberCheckRow без аватара: фичу groups
 * не трогаем, в ui.common не выносим.
 */
@Composable
internal fun SharingCheckRow(
    row: SharingCheckRowUiModel,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.small)
            .clickable(onClick = onToggle)
            .padding(horizontal = 4.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(
            checked = row.isChecked,
            onCheckedChange = { onToggle() },
        )
        Text(
            text = row.displayName,
            style = MaterialTheme.typography.bodyLarge,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
