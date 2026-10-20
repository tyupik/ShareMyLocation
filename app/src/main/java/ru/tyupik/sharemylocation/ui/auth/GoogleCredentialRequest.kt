package ru.tyupik.sharemylocation.ui.auth

import android.content.Context
import android.util.Log
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import kotlin.coroutines.cancellation.CancellationException
import ru.tyupik.sharemylocation.R

/** Тег для диагностики Google-флоу в logcat. */
private const val TAG = "GoogleCredential"

/** Результат запроса Google-креденшела через Credential Manager. */
internal sealed interface GoogleCredentialOutcome {
    data class Success(val idToken: String) : GoogleCredentialOutcome
    data object Cancelled : GoogleCredentialOutcome
    data class Failed(val reason: String?) : GoogleCredentialOutcome
}

/**
 * Запрашивает Google idToken через Credential Manager.
 * Должна вызываться с контекстом ComponentActivity: открывает системный диалог выбора аккаунта.
 * Обмен idToken на Firebase-аккаунт происходит дальше, в data-слое.
 */
internal suspend fun requestGoogleCredential(context: Context): GoogleCredentialOutcome = try {
    val googleIdOption = GetGoogleIdOption.Builder()
        .setServerClientId(context.getString(R.string.default_web_client_id))
        .setFilterByAuthorizedAccounts(false)
        .setAutoSelectEnabled(false)
        .build()
    val request = GetCredentialRequest.Builder()
        .addCredentialOption(googleIdOption)
        .build()
    Log.d(TAG, "Requesting google credential")
    val response = CredentialManager.create(context).getCredential(context, request)
    val credential = response.credential
    // Credential Manager возвращает Google-креденшел как CustomCredential с типом
    // TYPE_GOOGLE_ID_TOKEN_CREDENTIAL — разворачиваем через createFrom(data).
    if (credential is CustomCredential &&
        credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
    ) {
        val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
        Log.i(TAG, "Google credential received")
        GoogleCredentialOutcome.Success(googleIdTokenCredential.idToken)
    } else {
        Log.w(TAG, "Unexpected credential type: ${credential.type}")
        GoogleCredentialOutcome.Failed("Неожиданный тип креденшела: ${credential.type}")
    }
} catch (cancellation: GetCredentialCancellationException) {
    Log.i(TAG, "Google credential cancelled by user (GetCredentialCancellationException)")
    GoogleCredentialOutcome.Cancelled
} catch (exception: GetCredentialException) {
    Log.w(TAG, "Google credential failed: ${exception.type}", exception)
    GoogleCredentialOutcome.Failed(exception.type)
} catch (cancellation: CancellationException) {
    throw cancellation
} catch (exception: Exception) {
    Log.w(TAG, "Google credential failed unexpectedly", exception)
    GoogleCredentialOutcome.Failed(exception.message)
}
