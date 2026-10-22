package ru.tyupik.sharemylocation.ui.sharing.composable

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Inline-баннер «нет доступа к геолокации» (Фаза 6, паттерн SharingSettingsErrorBanner).
 * Без кнопки закрытия: состояние isPermissionMissing сбрасывается не пользователем, а
 * фактом выдачи прав (результат системного диалога) или выключением шаринга (ScreenShown).
 */
@Composable
internal fun SharingPermissionBanner(
    text: String,
    modifier: Modifier = Modifier,
) {
    Surface(
        color = MaterialTheme.colorScheme.errorContainer,
        contentColor = MaterialTheme.colorScheme.onErrorContainer,
        shape = MaterialTheme.shapes.medium,
        modifier = modifier.fillMaxWidth(),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
        )
    }
}
