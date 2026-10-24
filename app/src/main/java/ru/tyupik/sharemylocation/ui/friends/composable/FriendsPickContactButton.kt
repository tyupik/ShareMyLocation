package ru.tyupik.sharemylocation.ui.friends.composable

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.PersonSearch
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import ru.tyupik.sharemylocation.R

/**
 * Кнопка «Выбрать из контактов» рядом с полем поиска: открывает системный контакт-пикер,
 * выбранный телефон подставляется в поиск. Как и поле поиска, гасится, пока выполняется
 * действие с другом ([isEnabled]).
 */
@Composable
internal fun FriendsPickContactButton(
    isEnabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val contentDescription = stringResource(R.string.friends_search_pick_contact)
    IconButton(onClick = onClick, enabled = isEnabled, modifier = modifier) {
        Icon(imageVector = Icons.Outlined.PersonSearch, contentDescription = contentDescription)
    }
}
