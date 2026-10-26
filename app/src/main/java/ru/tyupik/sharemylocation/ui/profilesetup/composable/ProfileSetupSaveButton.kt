package ru.tyupik.sharemylocation.ui.profilesetup.composable

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import ru.tyupik.sharemylocation.ui.common.composable.GradientPrimaryButton

/** Кнопка сохранения профиля; в [isLoading] вместо текста показывает индикатор загрузки. */
@Composable
internal fun ProfileSetupSaveButton(
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
