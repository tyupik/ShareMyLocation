package ru.tyupik.sharemylocation.ui.profile.composable

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ru.tyupik.sharemylocation.ui.common.ProfileAvatar
import ru.tyupik.sharemylocation.ui.theme.AuroraAvatarBrush

/** Шапка профиля: круглый аватар в градиентном кольце AURORA и имя пользователя. */
@Composable
internal fun ProfileHeader(
    displayName: String,
    avatarPath: String?,
    modifier: Modifier = Modifier,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = modifier) {
        Box(
            modifier = Modifier
                .background(brush = AuroraAvatarBrush, shape = CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            ProfileAvatar(
                displayName = displayName,
                localPath = avatarPath,
                modifier = Modifier.padding(4.dp),
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = displayName,
            style = MaterialTheme.typography.titleLarge,
        )
    }
}
