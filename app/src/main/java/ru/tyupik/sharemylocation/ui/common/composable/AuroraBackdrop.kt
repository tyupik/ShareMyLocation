package ru.tyupik.sharemylocation.ui.common.composable

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ru.tyupik.sharemylocation.ui.theme.AuroraGlowCyan
import ru.tyupik.sharemylocation.ui.theme.AuroraGlowIndigo

/**
 * Фон-свечение AURORA: два круга радиального градиента (индиго у верхнего правого угла,
 * циан у нижнего левого), уходящие за край экрана. Свечение — спад альфы радиального
 * градиента; [androidx.compose.ui.draw.blur] не используется (API 31+, minSdk 24).
 * Контент рисуется поверх.
 */
@Composable
internal fun AuroraBackdrop(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    Box(modifier = modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .offset(x = 120.dp, y = (-140).dp)
                .size(INDIGO_GLOW_SIZE)
                .background(brush = AuroraGlowIndigo),
        )
        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .offset(x = (-150).dp, y = 140.dp)
                .size(CYAN_GLOW_SIZE)
                .background(brush = AuroraGlowCyan),
        )
        content()
    }
}

/** Диаметр индигового свечения. */
private val INDIGO_GLOW_SIZE = 420.dp

/** Диаметр цианового свечения. */
private val CYAN_GLOW_SIZE = 460.dp
