package ru.tyupik.sharemylocation.ui.main.composable

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeEffect
import ru.tyupik.sharemylocation.R
import ru.tyupik.sharemylocation.ui.main.MainTab
import ru.tyupik.sharemylocation.ui.theme.AuroraButtonBrush
import ru.tyupik.sharemylocation.ui.theme.AuroraGlassBorder
import ru.tyupik.sharemylocation.ui.theme.AuroraOnGradient
import ru.tyupik.sharemylocation.ui.theme.auroraHazeStyle

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

/** Горизонтальный отступ контента вкладок внутри навбара; с него начинается пилюля активного таба. */
private val TABS_ROW_HORIZONTAL_PADDING = 8.dp

/** Вертикальный отступ контента вкладок внутри навбара. */
private val TABS_ROW_VERTICAL_PADDING = 8.dp

/**
 * Нижняя навигация главного экрана: плавающая пилюля AURORA с вкладками Карта, Друзья,
 * Профиль. Фон — backdrop-блюр захваченного контента через [hazeEffect] (стиль
 * [auroraHazeStyle]); на вкладке «Карта» карта — SurfaceView и блюром не захватывается,
 * поэтому панель там остаётся полупрозрачным тонированным стеклом.
 * Активный таб — одна градиентная пилюля, отрисованная за контентом вкладок.
 */
@Composable
internal fun MainBottomBar(
    selectedTab: MainTab,
    friendsBadgeCount: Int,
    hazeState: HazeState,
    onTabSelected: (MainTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        color = Color.Transparent,
        contentColor = MaterialTheme.colorScheme.onSurface,
        border = BorderStroke(1.dp, AuroraGlassBorder),
        shape = MaterialTheme.shapes.extraLarge,
        modifier = modifier
            .padding(horizontal = 16.dp)
            .navigationBarsPadding()
            .padding(bottom = 12.dp),
    ) {
        // hazeEffect на внутреннем Box, а не на модификаторе Surface: блюр-регион должен
        // повторять форму пилюли (клип по shapes.extraLarge), а не тянуться прямоугольником.
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(MaterialTheme.shapes.extraLarge)
                .hazeEffect(state = hazeState, style = auroraHazeStyle()),
        ) {
            val tabIndex = MAIN_TAB_ITEMS.indexOfFirst { it.tab == selectedTab }.coerceAtLeast(0)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    // Пилюля активного таба: одна на весь навбар, рисуется за контентом
                    // вкладок. Ширина таба равная (weight(1f)), высота пилюли — вся
                    // область отрисовки (badge вкладки учтётся сам).
                    .drawBehind {
                        val pillWidth = (size.width - 2 * TABS_ROW_HORIZONTAL_PADDING.toPx()) /
                            MAIN_TAB_ITEMS.size
                        val pillLeft = TABS_ROW_HORIZONTAL_PADDING.toPx() + pillWidth * tabIndex
                        drawRoundRect(
                            brush = AuroraButtonBrush,
                            topLeft = Offset(pillLeft, 0f),
                            size = Size(pillWidth, size.height),
                            cornerRadius = CornerRadius(size.height / 2f),
                        )
                    }
                    .padding(
                        horizontal = TABS_ROW_HORIZONTAL_PADDING,
                        vertical = TABS_ROW_VERTICAL_PADDING,
                    ),
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
}
