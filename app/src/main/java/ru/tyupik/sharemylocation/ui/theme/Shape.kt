package ru.tyupik.sharemylocation.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

// Крупные скругления AURORA: карточки — 22dp (large), поля/кнопки — 16dp (medium),
// нижняя навигация — 28dp (extraLarge), мелкие элементы — 12dp (small).
val AurShapes = Shapes(
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp),
)
