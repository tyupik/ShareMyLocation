package ru.tyupik.sharemylocation.ui.main.composable

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
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import ru.tyupik.sharemylocation.R
import ru.tyupik.sharemylocation.ui.main.MainTab

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

/** Нижняя навигация главного экрана: вкладки Карта, Друзья, Профиль. */
@Composable
internal fun MainBottomBar(
    selectedTab: MainTab,
    friendsBadgeCount: Int,
    onTabSelected: (MainTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    NavigationBar(modifier = modifier) {
        MAIN_TAB_ITEMS.forEach { item ->
            NavigationBarItem(
                selected = selectedTab == item.tab,
                onClick = { onTabSelected(item.tab) },
                icon = {
                    val icon = if (selectedTab == item.tab) item.selectedIcon else item.unselectedIcon
                    if (item.tab == MainTab.FRIENDS && friendsBadgeCount > 0) {
                        BadgedBox(badge = { Badge { Text(text = friendsBadgeCount.toString()) } }) {
                            Icon(
                                imageVector = icon,
                                contentDescription = stringResource(item.labelRes),
                            )
                        }
                    } else {
                        Icon(
                            imageVector = icon,
                            contentDescription = stringResource(item.labelRes),
                        )
                    }
                },
                label = { Text(stringResource(item.labelRes)) },
            )
        }
    }
}
