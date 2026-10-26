package ru.tyupik.sharemylocation.ui.common.composable

import androidx.compose.foundation.BorderStroke
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.ColumnScope
import ru.tyupik.sharemylocation.ui.theme.AuroraAccentBorder
import ru.tyupik.sharemylocation.ui.theme.AuroraGlass
import ru.tyupik.sharemylocation.ui.theme.AuroraGlassBorder
import ru.tyupik.sharemylocation.ui.theme.AuroraSelectedGlass

/**
 * Стеклянная карточка AURORA: полупрозрачная заливка и рамка, крупное скругление (22dp).
 * [isSelected] — выбранная опция: циановая заливка и акцентная рамка.
 * Паддинг не задаёт — его оставляют вызывающие (внутри карточек).
 */
@Composable
internal fun GlassCard(
    onClick: (() -> Unit)? = null,
    isSelected: Boolean = false,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    val color = if (isSelected) AuroraSelectedGlass else AuroraGlass
    val border = BorderStroke(1.dp, if (isSelected) AuroraAccentBorder else AuroraGlassBorder)
    if (onClick != null) {
        Surface(
            onClick = onClick,
            modifier = modifier,
            color = color,
            contentColor = MaterialTheme.colorScheme.onSurface,
            border = border,
            shape = MaterialTheme.shapes.large,
            content = content,
        )
    } else {
        Surface(
            modifier = modifier,
            color = color,
            contentColor = MaterialTheme.colorScheme.onSurface,
            border = border,
            shape = MaterialTheme.shapes.large,
            content = content,
        )
    }
}
