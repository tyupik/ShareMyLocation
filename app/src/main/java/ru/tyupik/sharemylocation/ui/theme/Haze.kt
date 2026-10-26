package ru.tyupik.sharemylocation.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint

/**
 * Стиль backdrop-блюра нижней навигации AURORA: тонированное стекло поверх
 * размытого контента.
 *
 * backgroundColor прозрачный намеренно: на вкладке «Карта» GoogleMap — SurfaceView
 * и Haze её не захватывает (issue dev.chrisbanes.haze#798), поэтому панель остаётся
 * полупрозрачным тонированным стеклом, а не заливается непрозрачным surface.
 * Настоящий блюр работает на вкладках «Друзья» и «Профиль».
 */
internal fun auroraHazeStyle(): HazeStyle = HazeStyle(
    backgroundColor = Color.Transparent,
    tint = HazeTint(AuroraBackground.copy(alpha = 0.55f)),
    blurRadius = 20.dp,
    noiseFactor = 0.1f,
)
