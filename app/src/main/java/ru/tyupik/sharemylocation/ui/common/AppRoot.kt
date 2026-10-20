package ru.tyupik.sharemylocation.ui.common

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import kotlinx.serialization.Serializable
import ru.tyupik.sharemylocation.ui.auth.AuthScreen
import ru.tyupik.sharemylocation.ui.main.MainScreen

/** Экраны корневой навигации приложения. */
@Serializable
sealed interface AppDestination : NavKey {
    @Serializable
    data object Auth : AppDestination

    @Serializable
    data object Main : AppDestination
}

/**
 * Корень приложения: пока сессия не проверена, показывает индикатор,
 * затем строит back stack от стартового экрана (Auth или Main по сохранённой сессии).
 */
@Composable
fun AppRoot(appViewModel: AppViewModel = hiltViewModel()) {
    val state by appViewModel.uiState.collectAsStateWithLifecycle()
    if (!state.isSessionResolved) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    val backStack = rememberNavBackStack(
        if (state.isAuthenticated) AppDestination.Main else AppDestination.Auth,
    )

    NavDisplay(
        backStack = backStack,
        onBack = { backStack.removeLastOrNull() },
        entryProvider = entryProvider {
            entry<AppDestination.Auth> {
                AuthScreen(
                    onSuccess = {
                        backStack.clear()
                        backStack.add(AppDestination.Main)
                    },
                )
            }
            entry<AppDestination.Main> {
                MainScreen(
                    onSignedOut = {
                        backStack.clear()
                        backStack.add(AppDestination.Auth)
                    },
                )
            }
        },
    )
}
