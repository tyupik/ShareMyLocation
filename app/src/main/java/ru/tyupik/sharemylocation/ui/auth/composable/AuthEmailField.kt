package ru.tyupik.sharemylocation.ui.auth.composable

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import ru.tyupik.sharemylocation.R

/** Поле ввода email; [errorText] подсвечивает поле и выводится под ним. */
@Composable
internal fun AuthEmailField(
    value: String,
    isEnabled: Boolean,
    errorText: String?,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.fillMaxWidth(),
        label = { Text(stringResource(R.string.auth_email_label)) },
        singleLine = true,
        enabled = isEnabled,
        isError = errorText != null,
        supportingText = errorText?.let { text -> { Text(text) } },
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Email,
            imeAction = ImeAction.Next,
        ),
    )
}
