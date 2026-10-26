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
import ru.tyupik.sharemylocation.ui.theme.AuroraGlass
import ru.tyupik.sharemylocation.ui.theme.AuroraGlassBorder

/** Кнопка «Редактировать»: стеклянная AURORA-кнопка, открывает ProfileSetup в режиме редактирования. */
@Composable
internal fun ProfileEditButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = onClick,
        color = AuroraGlass,
        contentColor = MaterialTheme.colorScheme.onSurface,
        border = BorderStroke(1.dp, AuroraGlassBorder),
        shape = MaterialTheme.shapes.medium,
        modifier = modifier.fillMaxWidth(),
    ) {
        Text(
            text = stringResource(R.string.profile_edit),
            style = MaterialTheme.typography.labelLarge,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
        )
    }
}
