package ru.tyupik.sharemylocation.ui.main.composable

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
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

/** Вертикальный зазор пилюли активного таба от краёв навбара: стекло видно сверху и снизу. */
private val PILL_VERTICAL_INSET = 8.dp

/** Коэффициент затухания пружины смещения пилюли: лёгкий overshoot даёт «перекатывание». */
private const val PILL_SPRING_DAMPING_RATIO = 0.75f

/** Начальный масштаб пилюли при смене таба: поджатие перед упругим возвратом к полному размеру. */
private const val PILL_PULSE_START_SCALE = 0.94f

/**
 * Нижняя навигация главного экрана: плавающая пилюля AURORA с вкладками Карта, Друзья,
 * Профиль. Фон — backdrop-блюр захваченного контента через [hazeEffect] (стиль
 * [auroraHazeStyle]); на вкладке «Карта» карта — SurfaceView и блюром не захватывается,
 * поэтому панель там остаётся полупрозрачным тонированным стеклом.
 * Активный таб — одна градиентная пилюля: «перекатывается» пружиной к выбранному табу
 * и пульсирует в такт переключению; цвет контента плавно перетекает следом.
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
        // Стадионная форма (радиус = половина высоты) — единый язык скругления с пилюлей
        // активного таба: «пилюля в пилюле».
        shape = CircleShape,
        modifier = modifier
            .padding(horizontal = 16.dp)
            .navigationBarsPadding()
            .padding(bottom = 12.dp),
    ) {
        // hazeEffect на внутреннем контейнере, а не на модификаторе Surface: блюр-регион
        // должен повторять форму панели (клип по CircleShape), а не тянуться прямоугольником.
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .clip(CircleShape)
                .hazeEffect(state = hazeState, style = auroraHazeStyle()),
        ) {
            val tabIndex = MAIN_TAB_ITEMS.indexOfFirst { it.tab == selectedTab }.coerceAtLeast(0)
            // Ширина таба равная (weight(1f) у всех вкладок): вся ширина навбара минус
            // отступы ряда контента, делённая на число вкладок.
            val tabWidth = (maxWidth - TABS_ROW_HORIZONTAL_PADDING * 2) / MAIN_TAB_ITEMS.size
            val pillX by animateDpAsState(
                targetValue = TABS_ROW_HORIZONTAL_PADDING + tabWidth * tabIndex,
                animationSpec = spring(dampingRatio = PILL_SPRING_DAMPING_RATIO),
                label = "pillX",
            )
            // Скромный «пульс в такт» при смене таба: пилюля слегка поджимается и
            // упруго возвращается к полному размеру.
            val pillPulse = remember { Animatable(1f) }
            LaunchedEffect(selectedTab) {
                pillPulse.snapTo(PILL_PULSE_START_SCALE)
                pillPulse.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy))
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    // Пилюля активного таба: одна на весь навбар, рисуется за контентом
                    // вкладок. Высота — вся область отрисовки (badge вкладки учтётся сам),
                    // пульс применяется как инсет пилюли.
                    .drawBehind {
                        val pillWidthPx = tabWidth.toPx()
                        val insetX = pillWidthPx * (1f - pillPulse.value) / 2f
                        val insetY = PILL_VERTICAL_INSET.toPx() + size.height * (1f - pillPulse.value) / 2f
                        drawRoundRect(
                            brush = AuroraButtonBrush,
                            topLeft = Offset(pillX.toPx() + insetX, insetY),
                            size = Size(pillWidthPx - insetX * 2f, size.height - insetY * 2f),
                            cornerRadius = CornerRadius((size.height - insetY * 2f) / 2f),
                        )
                    }
                    .padding(
                        horizontal = TABS_ROW_HORIZONTAL_PADDING,
                        vertical = TABS_ROW_VERTICAL_PADDING,
                    ),
            ) {
                MAIN_TAB_ITEMS.forEach { item ->
                    val isSelected = selectedTab == item.tab
                    val contentColor by animateColorAsState(
                        targetValue = if (isSelected) {
                            AuroraOnGradient
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        label = "tabContentColor",
                    )
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
