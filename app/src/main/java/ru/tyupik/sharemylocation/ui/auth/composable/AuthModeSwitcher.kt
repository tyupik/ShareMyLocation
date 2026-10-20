package ru.tyupik.sharemylocation.ui.auth.composable

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import ru.tyupik.sharemylocation.R
import ru.tyupik.sharemylocation.ui.auth.AuthMode

/** Переключатель режима Вход/Регистрация. */
@Composable
internal fun AuthModeSwitcher(
    mode: AuthMode,
    isEnabled: Boolean,
    onModeChanged: (AuthMode) -> Unit,
    modifier: Modifier = Modifier,
) {
    SingleChoiceSegmentedButtonRow(modifier = modifier.fillMaxWidth()) {
        SegmentedButton(
            selected = mode == AuthMode.SIGN_IN,
            onClick = { onModeChanged(AuthMode.SIGN_IN) },
            shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
            enabled = isEnabled,
        ) {
            Text(stringResource(R.string.auth_title_sign_in))
        }
        SegmentedButton(
            selected = mode == AuthMode.SIGN_UP,
            onClick = { onModeChanged(AuthMode.SIGN_UP) },
            shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
            enabled = isEnabled,
        ) {
            Text(stringResource(R.string.auth_title_sign_up))
        }
    }
}
