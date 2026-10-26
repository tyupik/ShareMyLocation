package ru.tyupik.sharemylocation.ui.map.composable

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import ru.tyupik.sharemylocation.R
import ru.tyupik.sharemylocation.ui.common.composable.GlassCard
import ru.tyupik.sharemylocation.ui.common.composable.LoadError

/**
 * Inline-состояние упавшего realtime-потока карты: стеклянная карточка со строкой ошибки
 * и «Повторить» (= ScreenShown — перезапуск потока и перечитка настроек/групп).
 */
@Composable
internal fun MapLoadError(
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    GlassCard(modifier = modifier.fillMaxWidth()) {
        LoadError(
            text = stringResource(R.string.map_load_error),
            retryText = stringResource(R.string.map_retry),
            onRetry = onRetry,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
        )
    }
}
