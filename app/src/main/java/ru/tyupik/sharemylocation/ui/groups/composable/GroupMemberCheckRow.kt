package ru.tyupik.sharemylocation.ui.groups.composable

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
import ru.tyupik.sharemylocation.ui.groups.GroupMemberUiModel

/**
 * Строка участника группы: чекбокс и имя друга; клик по всей строке переключает чекбокс.
 * Аватары в чекбоксах сознательно не показываются.
 */
@Composable
internal fun GroupMemberCheckRow(
    member: GroupMemberUiModel,
    isEnabled: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.small)
            .clickable(enabled = isEnabled, onClick = onToggle)
            .padding(horizontal = 4.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(
            checked = member.isChecked,
            onCheckedChange = { onToggle() },
            enabled = isEnabled,
        )
        Text(
            text = member.displayName,
            style = MaterialTheme.typography.bodyLarge,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
