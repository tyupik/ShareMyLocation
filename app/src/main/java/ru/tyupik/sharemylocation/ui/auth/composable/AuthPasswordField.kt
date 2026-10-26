package ru.tyupik.sharemylocation.ui.auth.composable

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import ru.tyupik.sharemylocation.R
import ru.tyupik.sharemylocation.ui.common.composable.auroraOutlinedTextFieldColors

/** Поле ввода пароля с переключателем видимости; [errorText] подсвечивает поле и выводится под ним. */
@Composable
internal fun AuthPasswordField(
    value: String,
    isEnabled: Boolean,
    errorText: String?,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var isVisible by rememberSaveable { mutableStateOf(false) }
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.fillMaxWidth(),
        label = { Text(stringResource(R.string.auth_password_label)) },
        singleLine = true,
        enabled = isEnabled,
        isError = errorText != null,
        supportingText = errorText?.let { text -> { Text(text) } },
        colors = auroraOutlinedTextFieldColors(),
        visualTransformation = if (isVisible) VisualTransformation.None else PasswordVisualTransformation(),
        trailingIcon = {
            PasswordVisibilityToggle(isVisible = isVisible, onToggle = { isVisible = !isVisible })
        },
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Password,
            imeAction = ImeAction.Next,
        ),
    )
}

/** Кнопка-глаз: переключает видимость пароля. */
@Composable
internal fun PasswordVisibilityToggle(
    isVisible: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    IconButton(onClick = onToggle, modifier = modifier) {
        Icon(
            imageVector = if (isVisible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
            contentDescription = stringResource(
                if (isVisible) R.string.auth_hide_password else R.string.auth_show_password
            ),
        )
    }
}
