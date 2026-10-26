package ru.tyupik.sharemylocation.ui.common.composable

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

/**
 * Состояние ошибки загрузки AURORA: текст причины и кнопка повтора.
 * [isCentered] — экран-ошибка целиком (GroupEditor/SharingSettings): fillMaxSize + центр;
 * иначе inline-состояние секции (Friends): fillMaxWidth + вертикальный отступ.
 * [retryText] — строка повтора, у фич разные ресурсы.
 */
@Composable
internal fun LoadError(
    text: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
    isCentered: Boolean = false,
    retryText: String,
) {
    Column(
        modifier = if (isCentered) {
            modifier
                .fillMaxSize()
                .padding(24.dp)
        } else {
            modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
        },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = if (isCentered) Arrangement.Center else Arrangement.Top,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = if (isCentered) Modifier.fillMaxWidth() else Modifier,
        )
        TextButton(onClick = onRetry) {
            Text(retryText)
        }
    }
}
