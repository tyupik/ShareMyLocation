package ru.tyupik.sharemylocation.ui.auth.composable

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import ru.tyupik.sharemylocation.R

/** Кнопка входа через Google (Credential Manager). */
@Composable
internal fun AuthGoogleButton(
    isEnabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    OutlinedButton(
        onClick = onClick,
        enabled = isEnabled,
        modifier = modifier.fillMaxWidth(),
    ) {
        Text(stringResource(R.string.auth_google_button))
    }
}
