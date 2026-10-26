package ru.tyupik.sharemylocation.ui.auth.composable

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import ru.tyupik.sharemylocation.ui.common.composable.GradientPrimaryButton

/** Кнопка сабмита формы; в [isLoading] вместо текста показывает индикатор загрузки. */
@Composable
internal fun AuthSubmitButton(
    text: String,
    isEnabled: Boolean,
    isLoading: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    GradientPrimaryButton(
        text = text,
        isEnabled = isEnabled,
        isLoading = isLoading,
        onClick = onClick,
        modifier = modifier,
    )
}
