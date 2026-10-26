package ru.tyupik.sharemylocation.ui.common.composable

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import ru.tyupik.sharemylocation.ui.theme.AuroraGlass
import ru.tyupik.sharemylocation.ui.theme.AuroraGlassBorder

/**
 * Квадратная стеклянная кнопка-иконка AURORA (стекло + рамка, скругление 16dp):
 * колокольчик заявок в друзьях, кнопки-иконки рядом с полем поиска.
 * [badge] — необязательный бейдж поверх стеклянного квадрата (правый верхний угол).
 */
@Composable
internal fun GlassIconButton(
    icon: ImageVector,
    contentDescription: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isEnabled: Boolean = true,
    badge: (@Composable () -> Unit)? = null,
) {
    Surface(
        color = AuroraGlass,
        contentColor = MaterialTheme.colorScheme.onSurface,
        border = BorderStroke(1.dp, AuroraGlassBorder),
        shape = MaterialTheme.shapes.medium,
        modifier = modifier.size(GLASS_ICON_BUTTON_SIZE),
    ) {
        Box(contentAlignment = Alignment.Center) {
            IconButton(onClick = onClick, enabled = isEnabled) {
                Icon(imageVector = icon, contentDescription = contentDescription)
            }
            badge?.let { badgeContent ->
                Box(modifier = Modifier.align(Alignment.TopEnd)) {
                    badgeContent()
                }
            }
        }
    }
}

/** Сторона квадратной стеклянной кнопки. */
private val GLASS_ICON_BUTTON_SIZE = 44.dp
