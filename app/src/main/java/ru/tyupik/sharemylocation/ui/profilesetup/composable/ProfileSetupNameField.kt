package ru.tyupik.sharemylocation.ui.profilesetup.composable

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import ru.tyupik.sharemylocation.R
import ru.tyupik.sharemylocation.ui.common.composable.auroraOutlinedTextFieldColors

/** Поле ввода имени профиля. */
@Composable
internal fun ProfileSetupNameField(
    value: String,
    isEnabled: Boolean,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.fillMaxWidth(),
        label = { Text(stringResource(R.string.profile_setup_name_label)) },
        singleLine = true,
        enabled = isEnabled,
        colors = auroraOutlinedTextFieldColors(),
        keyboardOptions = KeyboardOptions(
            capitalization = KeyboardCapitalization.Words,
            imeAction = ImeAction.Next,
        ),
    )
}
