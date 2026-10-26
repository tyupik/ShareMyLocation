package ru.tyupik.sharemylocation.ui.common.composable

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import ru.tyupik.sharemylocation.ui.theme.AuroraButtonBrush
import ru.tyupik.sharemylocation.ui.theme.AuroraOnGradient

/**
 * Градиентная primary-кнопка AURORA (индиго → циан): единый стиль submit-кнопок.
 * В [isLoading] вместо текста показывает индикатор загрузки; disabled — приглушённый градиент.
 */
@Composable
internal fun GradientPrimaryButton(
    text: String,
    isEnabled: Boolean,
    isLoading: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val brush: Brush = if (isEnabled && !isLoading) {
        AuroraButtonBrush
    } else {
        AuroraButtonBrush.copy(alpha = DISABLED_GRADIENT_ALPHA)
    }
    Button(
        onClick = onClick,
        enabled = isEnabled && !isLoading,
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.Transparent,
            contentColor = AuroraOnGradient,
            disabledContainerColor = Color.Transparent,
            disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        ),
        modifier = modifier
            .fillMaxWidth()
            .background(brush = brush, shape = MaterialTheme.shapes.medium),
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                strokeWidth = 2.dp,
                color = LocalContentColor.current,
            )
        } else {
            Text(text)
        }
    }
}

/** Альфа градиента в disabled-состоянии. */
private const val DISABLED_GRADIENT_ALPHA = 0.38f
