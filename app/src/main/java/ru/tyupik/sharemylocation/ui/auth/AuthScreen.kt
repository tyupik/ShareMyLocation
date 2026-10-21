package ru.tyupik.sharemylocation.ui.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import ru.tyupik.sharemylocation.R
import ru.tyupik.sharemylocation.ui.auth.composable.AuthConfirmPasswordField
import ru.tyupik.sharemylocation.ui.auth.composable.AuthDisplayNameField
import ru.tyupik.sharemylocation.ui.auth.composable.AuthEmailField
import ru.tyupik.sharemylocation.ui.auth.composable.AuthErrorBanner
import ru.tyupik.sharemylocation.ui.auth.composable.AuthGoogleButton
import ru.tyupik.sharemylocation.ui.auth.composable.AuthModeSwitcher
import ru.tyupik.sharemylocation.ui.auth.composable.AuthOrDivider
import ru.tyupik.sharemylocation.ui.auth.composable.AuthPasswordField
import ru.tyupik.sharemylocation.ui.auth.composable.AuthSubmitButton
import ru.tyupik.sharemylocation.ui.theme.ShareMyLocationTheme

/** Минимальная длина пароля Firebase Auth. */
private const val MIN_PASSWORD_LENGTH = 6

/**
 * Экран входа/регистрации: собирает UI-элементы из composable-пакета и маппит состояние в их параметры.
 * Навигацию выполняет вызывающая сторона через [onSuccess]: экран сам не знает про back stack,
 * лямбда вызывается только из коллекции Effect.
 */
@Composable
internal fun AuthScreen(
    onSuccess: () -> Unit,
    viewModel: AuthViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // На каждый показ (в т.ч. при повороте) — no-op: форма сбрасывается только
    // при повторном показе после успешной аутентификации (см. AuthViewModel.onScreenShown).
    LaunchedEffect(Unit) { viewModel.onIntent(AuthIntent.ScreenShown) }

    LaunchedEffect(Unit) {
        viewModel.effects.collect { effect ->
            when (effect) {
                AuthEffect.NavigateToMain -> onSuccess()
            }
        }
    }

    AuthContent(
        uiState = uiState,
        onIntent = viewModel::onIntent,
        onGoogleSignInClick = {
            // Сначала включаем состояние загрузки, затем идём за креденшелом:
            // без этого интента isLoading не поднимется и результат будет отброшен guard'ом.
            viewModel.onIntent(AuthIntent.GoogleSignInClicked)
            scope.launch {
                when (val outcome = requestGoogleCredential(context)) {
                    is GoogleCredentialOutcome.Success ->
                        viewModel.onIntent(AuthIntent.GoogleCredentialReceived(outcome.idToken))

                    GoogleCredentialOutcome.Cancelled ->
                        viewModel.onIntent(AuthIntent.GoogleSignInCancelled)

                    is GoogleCredentialOutcome.Failed ->
                        viewModel.onIntent(AuthIntent.GoogleSignInFailed(outcome.reason))
                }
            }
        },
    )
}

@Composable
private fun AuthContent(
    uiState: AuthUiState,
    onIntent: (AuthIntent) -> Unit,
    onGoogleSignInClick: () -> Unit,
) {
    val isSignUp = uiState.mode == AuthMode.SIGN_UP
    val isLoading = uiState.isLoading
    // Чистый UI-расчёт валидности, не бизнес-состояние: кнопка активна, только если форма заполнена,
    // а в SIGN_UP подтверждение пароля совпадает с паролем.
    val isFormValid = uiState.email.contains('@') &&
        uiState.password.length >= MIN_PASSWORD_LENGTH &&
        (!isSignUp || uiState.repeatedPassword == uiState.password) &&
        (!isSignUp || uiState.displayName.isNotBlank())

    val error = uiState.error
    val emailErrorText = if (error is AuthUiError.EmailAlreadyInUse) {
        stringResource(R.string.auth_error_email_in_use)
    } else {
        null
    }
    val passwordErrorText = if (error is AuthUiError.InvalidCredentials && !isSignUp) {
        stringResource(R.string.auth_error_invalid_credentials)
    } else {
        null
    }
    val confirmErrorText = if (isSignUp &&
        uiState.repeatedPassword.isNotEmpty() &&
        uiState.repeatedPassword != uiState.password
    ) {
        stringResource(R.string.auth_error_passwords_mismatch)
    } else {
        null
    }
    val bannerErrorText = when (error) {
        AuthUiError.NetworkUnavailable -> stringResource(R.string.auth_error_network)
        is AuthUiError.Unexpected -> stringResource(R.string.auth_error_unexpected)
        is AuthUiError.InvalidCredentials ->
            if (isSignUp) stringResource(R.string.auth_error_invalid_sign_up_data) else null

        AuthUiError.EmailAlreadyInUse, null -> null
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Spacer(modifier = Modifier.height(32.dp))

        if (bannerErrorText != null) {
            AuthErrorBanner(text = bannerErrorText, onDismiss = { onIntent(AuthIntent.ErrorDismissed) })
        }

        Text(
            text = stringResource(if (isSignUp) R.string.auth_title_sign_up else R.string.auth_title_sign_in),
            style = MaterialTheme.typography.headlineMedium,
        )

        AuthModeSwitcher(
            mode = uiState.mode,
            isEnabled = !isLoading,
            onModeChanged = { onIntent(AuthIntent.ModeChanged(it)) },
        )

        AuthEmailField(
            value = uiState.email,
            isEnabled = !isLoading,
            errorText = emailErrorText,
            onValueChange = { onIntent(AuthIntent.EmailChanged(it)) },
        )

        if (isSignUp) {
            AuthDisplayNameField(
                value = uiState.displayName,
                isEnabled = !isLoading,
                onValueChange = { onIntent(AuthIntent.DisplayNameChanged(it)) },
            )
        }

        AuthPasswordField(
            value = uiState.password,
            isEnabled = !isLoading,
            errorText = passwordErrorText,
            onValueChange = { onIntent(AuthIntent.PasswordChanged(it)) },
        )

        if (isSignUp) {
            AuthConfirmPasswordField(
                value = uiState.repeatedPassword,
                isEnabled = !isLoading,
                errorText = confirmErrorText,
                onValueChange = { onIntent(AuthIntent.RepeatedPasswordChanged(it)) },
            )
        }

        AuthSubmitButton(
            text = stringResource(if (isSignUp) R.string.auth_sign_up_button else R.string.auth_sign_in_button),
            isEnabled = isFormValid,
            isLoading = isLoading,
            onClick = {
                onIntent(if (isSignUp) AuthIntent.SignUpWithEmailClicked else AuthIntent.SignInWithEmailClicked)
            },
        )

        AuthOrDivider()

        AuthGoogleButton(isEnabled = !isLoading, onClick = onGoogleSignInClick)
    }
}

@Preview(showBackground = true)
@Composable
private fun AuthContentPreview() {
    ShareMyLocationTheme {
        AuthContent(
            uiState = AuthUiState(mode = AuthMode.SIGN_IN),
            onIntent = {},
            onGoogleSignInClick = {},
        )
    }
}
