package ru.tyupik.sharemylocation.ui.main

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ru.tyupik.sharemylocation.R
import ru.tyupik.sharemylocation.ui.theme.ShareMyLocationTheme

/**
 * Временный главный экран: показывает email вошедшего пользователя и кнопку выхода.
 * Будет заменён реальным экраном, сейчас нужен только для проверки аутентификации.
 */
@Composable
internal fun MainScreen(
    onSignedOut: () -> Unit,
    viewModel: MainViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // Экран может показаться повторно поверх живой ViewModel — данные перечитываются каждый раз.
    LaunchedEffect(Unit) {
        viewModel.onIntent(MainIntent.ScreenShown)
        viewModel.effects.collect { effect ->
            when (effect) {
                MainEffect.NavigateToAuth -> onSignedOut()
            }
        }
    }

    MainContent(
        userEmail = uiState.userEmail,
        onIntent = viewModel::onIntent,
    )
}

@Composable
private fun MainContent(
    userEmail: String?,
    onIntent: (MainIntent) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = stringResource(
                R.string.main_signed_in_as,
                userEmail ?: stringResource(R.string.main_unknown_user),
            ),
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = { onIntent(MainIntent.SignOutClicked) }) {
            Text(stringResource(R.string.main_sign_out))
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun MainContentPreview() {
    ShareMyLocationTheme {
        MainContent(userEmail = "user@example.com", onIntent = {})
    }
}
