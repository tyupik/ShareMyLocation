package ru.tyupik.sharemylocation.ui.groups.composable

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

/** Поле ввода названия группы. */
@Composable
internal fun GroupEditorNameField(
    value: String,
    isEnabled: Boolean,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.fillMaxWidth(),
        label = { Text(stringResource(R.string.groups_editor_name_label)) },
        singleLine = true,
        enabled = isEnabled,
        colors = auroraOutlinedTextFieldColors(),
        keyboardOptions = KeyboardOptions(
            capitalization = KeyboardCapitalization.Words,
            imeAction = ImeAction.Done,
        ),
    )
}
