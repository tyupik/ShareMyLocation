package ru.tyupik.sharemylocation.ui.profilesetup.composable

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
import ru.tyupik.sharemylocation.ui.common.composable.auroraOutlinedTextFieldColors

/** Поле ввода телефона; [errorText] показывается под полем и подсвечивает его как ошибочное. */
@Composable
internal fun ProfileSetupPhoneField(
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
        label = { Text(stringResource(R.string.profile_setup_phone_label)) },
        singleLine = true,
        enabled = isEnabled,
        colors = auroraOutlinedTextFieldColors(),
        isError = errorText != null,
        supportingText = if (errorText != null) {
            { Text(errorText) }
        } else {
            null
        },
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Phone,
            imeAction = ImeAction.Done,
        ),
    )
}
