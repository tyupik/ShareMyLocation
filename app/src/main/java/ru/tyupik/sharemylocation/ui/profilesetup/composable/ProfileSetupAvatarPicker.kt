package ru.tyupik.sharemylocation.ui.profilesetup.composable

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TextButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import ru.tyupik.sharemylocation.R
import ru.tyupik.sharemylocation.ui.common.ProfileAvatar

/** Пикер аватара: круглое фото с бейджем камеры и подписью-кнопкой выбора изображения. */
@Composable
internal fun ProfileSetupAvatarPicker(
    displayName: String,
    savedAvatarPath: String?,
    localPath: String?,
    isEnabled: Boolean,
    isEditMode: Boolean,
    onPickRequest: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = modifier) {
        Box {
            ProfileAvatar(
                displayName = displayName,
                localPath = localPath ?: savedAvatarPath,
            )
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .size(BADGE_SIZE_DP.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary)
                    .clickable(enabled = isEnabled, onClick = onPickRequest),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Filled.PhotoCamera,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(BADGE_ICON_SIZE_DP.dp),
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        TextButton(
            onClick = onPickRequest,
            enabled = isEnabled,
        ) {
            Text(
                text = stringResource(
                    if (isEditMode) R.string.profile_setup_change_photo else R.string.profile_setup_add_photo,
                ),
            )
        }
    }
}

private const val BADGE_SIZE_DP = 32
private const val BADGE_ICON_SIZE_DP = 18
