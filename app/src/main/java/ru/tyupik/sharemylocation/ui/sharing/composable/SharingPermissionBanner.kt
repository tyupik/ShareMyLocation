package ru.tyupik.sharemylocation.ui.sharing.composable

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import ru.tyupik.sharemylocation.ui.common.composable.ErrorBanner

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
    ErrorBanner(text = text, onDismiss = null, modifier = modifier)
}
