package ru.tyupik.sharemylocation.ui.profilesetup.composable

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import ru.tyupik.sharemylocation.R

/** Диалог выбора источника аватара: галерея или камера (Material 3). */
@Composable
internal fun ProfileSetupAvatarSourceDialog(
    onGallerySelected: () -> Unit,
    onCameraSelected: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AlertDialog(
        modifier = modifier,
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.profile_setup_avatar_source_title)) },
        text = {
            Column {
                ProfileSetupAvatarSourceOption(
                    icon = Icons.Filled.PhotoLibrary,
                    label = stringResource(R.string.profile_setup_avatar_source_gallery),
                    onClick = onGallerySelected,
                )
                ProfileSetupAvatarSourceOption(
                    icon = Icons.Filled.PhotoCamera,
                    label = stringResource(R.string.profile_setup_avatar_source_camera),
                    onClick = onCameraSelected,
                )
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.profile_setup_avatar_source_cancel))
            }
        },
    )
}

/** Одна опция диалога: иконка источника и подпись, вся строка кликабельна. */
@Composable
private fun ProfileSetupAvatarSourceOption(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
) {
    TextButton(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(OPTION_ICON_SIZE_DP.dp),
            )
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(start = 12.dp),
            )
        }
    }
}

private const val OPTION_ICON_SIZE_DP = 20
