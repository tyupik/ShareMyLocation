package ru.tyupik.sharemylocation.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// Градиенты AURORA: кнопки/активные пилюли — горизонталь (96° макета ≈ горизонталь),
// кольца аватаров — диагональ (135°), акцентный текст — горизонталь светлых акцентов,
// свечение фона — радиальный спад альфы к прозрачному.
val AuroraButtonBrush = Brush.horizontalGradient(listOf(AuroraIndigo, AuroraCyan))
val AuroraAvatarBrush = Brush.linearGradient(listOf(AuroraIndigo, AuroraCyan))
val AuroraAccentBrush = Brush.horizontalGradient(listOf(AuroraIndigoLight, AuroraCyan))
val AuroraGlowIndigo = Brush.radialGradient(listOf(AuroraIndigo.copy(alpha = 0.67f), Color.Transparent))
val AuroraGlowCyan = Brush.radialGradient(listOf(AuroraCyan.copy(alpha = 0.53f), Color.Transparent))
