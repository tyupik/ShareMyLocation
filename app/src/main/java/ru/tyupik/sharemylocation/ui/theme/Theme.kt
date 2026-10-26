package ru.tyupik.sharemylocation.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Тёмная схема AURORA «ночной неон»: единственная тема приложения.
// Контейнеры — непрозрачные эквиваленты полупрозрачных наложений макета над #0B1020.
private val AuroraColorScheme = darkColorScheme(
    primary = Color(0xFF818CF8),
    onPrimary = Color(0xFF041018),
    primaryContainer = Color(0xFF2A3352),
    onPrimaryContainer = Color(0xFFDDE3FF),
    inversePrimary = Color(0xFF6366F1),
    secondary = Color(0xFF22D3EE),
    onSecondary = Color(0xFF041018),
    secondaryContainer = Color(0xFF0E293B),
    onSecondaryContainer = Color(0xFFA5F3FC),
    tertiary = Color(0xFF34D399),
    onTertiary = Color(0xFF04281B),
    tertiaryContainer = Color(0xFF123A2C),
    onTertiaryContainer = Color(0xFFA7F3D0),
    background = Color(0xFF0B1020),
    onBackground = Color(0xFFF1F5F9),
    surface = Color(0xFF0B1020),
    onSurface = Color(0xFFF1F5F9),
    surfaceVariant = Color(0xFF1B2440),
    onSurfaceVariant = Color(0xFF8A94AD),
    surfaceDim = Color(0xFF070B16),
    surfaceBright = Color(0xFF232C4C),
    surfaceContainerLowest = Color(0xFF05070F),
    surfaceContainerLow = Color(0xFF101628),
    surfaceContainer = Color(0xFF141B30),
    surfaceContainerHigh = Color(0xFF1B2440),
    surfaceContainerHighest = Color(0xFF253052),
    outline = Color(0xFF3D4663),
    outlineVariant = Color(0xFF262F4D),
    error = Color(0xFFF87171),
    onError = Color(0xFF4A0E0E),
    errorContainer = Color(0xFF2A1518),
    onErrorContainer = Color(0xFFFDA4A4),
    inverseSurface = Color(0xFFF1F5F9),
    inverseOnSurface = Color(0xFF141B30),
    scrim = Color(0xFF02030A),
)

/** Тема приложения: всегда тёмная AURORA, шрифт Manrope, крупные скругления. */
@Composable
fun ShareMyLocationTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = AuroraColorScheme,
        typography = Typography,
        shapes = AurShapes,
        content = content,
    )
}
