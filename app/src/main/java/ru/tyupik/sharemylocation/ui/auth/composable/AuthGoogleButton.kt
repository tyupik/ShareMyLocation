package ru.tyupik.sharemylocation.ui.auth.composable

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import ru.tyupik.sharemylocation.R
import ru.tyupik.sharemylocation.ui.theme.AuroraGlass
import ru.tyupik.sharemylocation.ui.theme.AuroraGlassBorder

/** Кнопка входа через Google (Credential Manager): стеклянная AURORA-кнопка. */
@Composable
internal fun AuthGoogleButton(
    isEnabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    OutlinedButton(
        onClick = onClick,
        enabled = isEnabled,
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = AuroraGlass,
            contentColor = MaterialTheme.colorScheme.onSurface,
        ),
        border = BorderStroke(1.dp, AuroraGlassBorder),
        modifier = modifier.fillMaxWidth(),
    ) {
        // Цветная четырёхцветная «G» (фигуративный знак Google), без описания —
        // смысл кнопки несёт текст.
        Icon(
            painter = painterResource(R.drawable.ic_google),
            contentDescription = null,
            modifier = Modifier.size(18.dp),
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(stringResource(R.string.auth_google_button))
    }
}
