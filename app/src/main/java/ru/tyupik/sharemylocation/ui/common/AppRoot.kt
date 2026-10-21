package ru.tyupik.sharemylocation.ui.common

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import ru.tyupik.sharemylocation.ui.groups.GroupEditorScreen
import ru.tyupik.sharemylocation.ui.main.MainScreen
import ru.tyupik.sharemylocation.ui.profilesetup.ProfileSetupScreen

/** Экраны корневой навигации приложения. */
@Serializable
sealed interface AppDestination : NavKey {
    @Serializable
    data object Auth : AppDestination

    /** Онбординг и редактирование профиля: один экран, [isEditMode] меняет только тексты. */
    @Serializable
    data class ProfileSetup(val isEditMode: Boolean) : AppDestination

    @Serializable
    data object Main : AppDestination

    /** Редактор группы поверх Main: [groupId] == null — создание, иначе — редактирование. */
    @Serializable
    data class GroupEditor(val groupId: String?) : AppDestination
}

/**
 * Корень приложения: пока сессия не проверена, показывает индикатор,
 * затем строит back stack от стартового экрана (Auth / ProfileSetup / Main по сессии и онбордингу).
 * После входа целевой экран приходит Effect'ом из [AppViewModel].
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

    val startDestination = when {
        !state.isAuthenticated -> AppDestination.Auth
        state.requiresOnboarding -> AppDestination.ProfileSetup(isEditMode = false)
        else -> AppDestination.Main
    }
    val backStack = rememberNavBackStack(startDestination)

    LaunchedEffect(Unit) {
        appViewModel.effects.collect { effect ->
            when (effect) {
                AppEffect.NavigateToMain -> {
                    backStack.clear()
                    backStack.add(AppDestination.Main)
                }

                AppEffect.NavigateToProfileSetup -> {
                    backStack.clear()
                    backStack.add(AppDestination.ProfileSetup(isEditMode = false))
                }
            }
        }
    }

    NavDisplay(
        backStack = backStack,
        onBack = { backStack.removeLastOrNull() },
        entryProvider = entryProvider {
            entry<AppDestination.Auth> {
                AuthScreen(
                    onSuccess = { appViewModel.onIntent(AppIntent.AuthCompleted) },
                )
            }
            entry<AppDestination.ProfileSetup> { key ->
                ProfileSetupScreen(
                    isEditMode = key.isEditMode,
                    onCompleted = {
                        if (key.isEditMode) {
                            backStack.removeLastOrNull()
                        } else {
                            backStack.clear()
                            backStack.add(AppDestination.Main)
                        }
                    },
                )
            }
            entry<AppDestination.Main> {
                MainScreen(
                    onSignedOut = {
                        backStack.clear()
                        backStack.add(AppDestination.Auth)
                    },
                    onEditProfile = { backStack.add(AppDestination.ProfileSetup(isEditMode = true)) },
                    // Пуш поверх Main: Main остаётся в стеке, вкладка «Друзья» переживает возврат,
                    // свежий состав приходит realtime-слушателем.
                    onOpenGroupEditor = { groupId ->
                        backStack.add(AppDestination.GroupEditor(groupId))
                    },
                )
            }
            entry<AppDestination.GroupEditor> { key ->
                GroupEditorScreen(
                    groupId = key.groupId,
                    onClosed = { backStack.removeLastOrNull() },
                )
            }
        },
    )
}
