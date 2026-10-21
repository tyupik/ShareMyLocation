package ru.tyupik.sharemylocation.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import java.io.File

/**
 * Круглый аватар с плейсхолдером (первая буква имени) — общий элемент экранов профиля.
 * [localPath] — путь к файлу JPEG в кэше приложения; null — плейсхолдер.
 * [size] — сторона аватара; по умолчанию 112dp для совместимости с ProfileHeader.
 */
@Composable
fun ProfileAvatar(
    displayName: String,
    localPath: String?,
    modifier: Modifier = Modifier,
    size: Dp = DEFAULT_AVATAR_SIZE,
) {
    if (localPath != null) {
        AsyncImage(
            model = File(localPath),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = modifier
                .size(size)
                .clip(CircleShape),
        )
    } else {
        Box(
            modifier = modifier
                .size(size)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = displayName.trim().take(1).uppercase().ifEmpty { "?" },
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private val DEFAULT_AVATAR_SIZE = 112.dp
