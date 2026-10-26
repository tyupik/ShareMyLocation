package ru.tyupik.sharemylocation.ui.friends.composable

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import ru.tyupik.sharemylocation.R
import ru.tyupik.sharemylocation.ui.common.composable.GlassCard
import ru.tyupik.sharemylocation.ui.friends.GroupUiModel

/**
 * Карточка группы: имя и подпись «N участников»; клик открывает редактор группы.
 * Счётчик может завышаться, если из друзей недавно удалили участника группы
 * (чистка memberUids отложена — TODO Фаза 5); редактор показывает актуальный состав.
 */
@Composable
internal fun GroupCard(
    group: GroupUiModel,
    onGroupClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    GlassCard(
        onClick = onGroupClick,
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
        ) {
            Text(
                text = group.name,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = pluralStringResource(
                    R.plurals.groups_member_count,
                    group.memberCount,
                    group.memberCount,
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
