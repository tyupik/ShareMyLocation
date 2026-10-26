package ru.tyupik.sharemylocation.ui.common.composable

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.TextFieldColors
import androidx.compose.runtime.Composable
import ru.tyupik.sharemylocation.ui.theme.AuroraGlass
import ru.tyupik.sharemylocation.ui.theme.AuroraGlassBorder

/**
 * Единый источник стиля полей ввода AURORA: стеклянная подложка, приглушённая рамка,
 * акцентные фокус/курсор/метка. Для всех OutlinedTextField проекта.
 */
@Composable
internal fun auroraOutlinedTextFieldColors(): TextFieldColors {
    val colorScheme = MaterialTheme.colorScheme
    return OutlinedTextFieldDefaults.colors(
        focusedContainerColor = AuroraGlass,
        unfocusedContainerColor = AuroraGlass,
        disabledContainerColor = AuroraGlass,
        focusedBorderColor = colorScheme.primary,
        unfocusedBorderColor = AuroraGlassBorder,
        disabledBorderColor = AuroraGlassBorder,
        cursorColor = colorScheme.secondary,
        focusedLabelColor = colorScheme.secondary,
        unfocusedLabelColor = colorScheme.onSurfaceVariant,
        disabledLabelColor = colorScheme.onSurfaceVariant,
        focusedSupportingTextColor = colorScheme.onSurfaceVariant,
        unfocusedSupportingTextColor = colorScheme.onSurfaceVariant,
    )
}
