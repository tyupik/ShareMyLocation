package ru.tyupik.sharemylocation.ui.profile.composable

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import ru.tyupik.sharemylocation.R

/** Кнопка «Выход»: инициирует выход из аккаунта в контракте MainScreen. */
@Composable
internal fun ProfileSignOutButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
    ) {
        Text(
            text = stringResource(R.string.profile_sign_out),
            color = MaterialTheme.colorScheme.error,
        )
    }
}
