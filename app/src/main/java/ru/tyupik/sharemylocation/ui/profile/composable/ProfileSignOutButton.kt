package ru.tyupik.sharemylocation.ui.profile.composable

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import ru.tyupik.sharemylocation.R
import ru.tyupik.sharemylocation.ui.theme.AuroraDangerBorder
import ru.tyupik.sharemylocation.ui.theme.AuroraDangerGlass

/** Кнопка «Выход»: ghost-danger AURORA (danger-стекло), инициирует выход из аккаунта в контракте MainScreen. */
@Composable
internal fun ProfileSignOutButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = onClick,
        color = AuroraDangerGlass,
        contentColor = MaterialTheme.colorScheme.error,
        border = BorderStroke(1.dp, AuroraDangerBorder),
        shape = MaterialTheme.shapes.medium,
        modifier = modifier.fillMaxWidth(),
    ) {
        Text(
            text = stringResource(R.string.profile_sign_out),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.error,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
        )
    }
}
