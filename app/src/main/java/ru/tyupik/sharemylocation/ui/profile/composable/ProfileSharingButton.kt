package ru.tyupik.sharemylocation.ui.profile.composable

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import ru.tyupik.sharemylocation.R
import ru.tyupik.sharemylocation.ui.common.composable.GradientPrimaryButton

/** Кнопка «Делиться геопозицией»: открывает настройки шаринга (градиентная AURORA-кнопка). */
@Composable
internal fun ProfileSharingButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    GradientPrimaryButton(
        text = stringResource(R.string.profile_sharing_button),
        isEnabled = true,
        isLoading = false,
        onClick = onClick,
        modifier = modifier,
    )
}
