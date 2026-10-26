package ru.tyupik.sharemylocation.ui.main.composable

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.Group
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import ru.tyupik.sharemylocation.R
import ru.tyupik.sharemylocation.ui.main.MainTab
import ru.tyupik.sharemylocation.ui.theme.AuroraButtonBrush
import ru.tyupik.sharemylocation.ui.theme.AuroraGlass
import ru.tyupik.sharemylocation.ui.theme.AuroraGlassBorder
import ru.tyupik.sharemylocation.ui.theme.AuroraOnGradient

/** Описание одной вкладки нижней навигации: подпись и пара иконок выбранное/невыбранное. */
private data class MainTabItem(
    val tab: MainTab,
    val labelRes: Int,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
)

private val MAIN_TAB_ITEMS = listOf(
    MainTabItem(MainTab.MAP, R.string.main_tab_map, Icons.Filled.Map, Icons.Outlined.Map),
    MainTabItem(MainTab.FRIENDS, R.string.main_tab_friends, Icons.Filled.Group, Icons.Outlined.Group),
    MainTabItem(MainTab.PROFILE, R.string.main_tab_profile, Icons.Filled.Person, Icons.Outlined.Person),
)

/**
 * Нижняя навигация главного экрана: плавающая стеклянная пилюля AURORA с вкладками
 * Карта, Друзья, Профиль. Выбранный таб — градиентная пилюля, невыбранный — прозрачный.
 */
@Composable
internal fun MainBottomBar(
    selectedTab: MainTab,
    friendsBadgeCount: Int,
    onTabSelected: (MainTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        color = AuroraGlass,
        contentColor = MaterialTheme.colorScheme.onSurface,
        border = BorderStroke(1.dp, AuroraGlassBorder),
        shape = MaterialTheme.shapes.extraLarge,
        modifier = modifier
            .padding(horizontal = 16.dp)
            .navigationBarsPadding()
            .padding(bottom = 12.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp),
        ) {
            MAIN_TAB_ITEMS.forEach { item ->
                val isSelected = selectedTab == item.tab
                val contentColor = if (isSelected) {
                    AuroraOnGradient
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(CircleShape)
                        .background(
                            brush = if (isSelected) AuroraButtonBrush else AuroraButtonBrush.copy(alpha = 0f),
                            shape = CircleShape,
                        )
                        .clickable { onTabSelected(item.tab) }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        val icon = if (isSelected) item.selectedIcon else item.unselectedIcon
                        if (item.tab == MainTab.FRIENDS && friendsBadgeCount > 0) {
                            BadgedBox(badge = { Badge { Text(text = friendsBadgeCount.toString()) } }) {
                                Icon(
                                    imageVector = icon,
                                    contentDescription = stringResource(item.labelRes),
                                    tint = contentColor,
                                )
                            }
                        } else {
                            Icon(
                                imageVector = icon,
                                contentDescription = stringResource(item.labelRes),
                                tint = contentColor,
                            )
                        }
                        Text(
                            text = stringResource(item.labelRes),
                            style = MaterialTheme.typography.labelMedium,
                            color = contentColor,
                        )
                    }
                }
            }
        }
    }
}
