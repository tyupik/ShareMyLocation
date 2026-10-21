package ru.tyupik.sharemylocation.ui.profile.composable

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import ru.tyupik.sharemylocation.R

/** Кнопка «Делиться геопозицией»: открывает настройки шаринга (в стиле ProfileEditButton). */
@Composable
internal fun ProfileSharingButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Button(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
    ) {
        Text(stringResource(R.string.profile_sharing_button))
    }
}
