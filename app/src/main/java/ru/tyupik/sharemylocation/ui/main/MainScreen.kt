package ru.tyupik.sharemylocation.ui.main

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ru.tyupik.sharemylocation.ui.friends.FriendsScreen
import ru.tyupik.sharemylocation.ui.main.composable.MainBottomBar
import ru.tyupik.sharemylocation.ui.main.composable.MapTabPlaceholder
import ru.tyupik.sharemylocation.ui.profile.ProfileScreen
import ru.tyupik.sharemylocation.ui.theme.ShareMyLocationTheme

/**
 * Главный экран-контейнер: нижняя навигация с вкладками Карта, Друзья, Профиль.
 * Навигацию выполняет вызывающая сторона через [onSignedOut], [onEditProfile],
 * [onOpenSharingSettings] и [onOpenGroupEditor]: экран сам не знает про back stack,
 * лямбды вызываются только из коллекций Effect.
 */
@Composable
internal fun MainScreen(
    onSignedOut: () -> Unit,
    onEditProfile: () -> Unit,
    onOpenSharingSettings: () -> Unit,
    onOpenGroupEditor: (String?) -> Unit,
    viewModel: MainViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.effects.collect { effect ->
            when (effect) {
                MainEffect.NavigateToAuth -> onSignedOut()
            }
        }
    }

    MainContent(
        selectedTab = uiState.selectedTab,
        pendingRequestsCount = uiState.pendingRequestsCount,
        onIntent = viewModel::onIntent,
        onEditProfile = onEditProfile,
        onOpenSharingSettings = onOpenSharingSettings,
        onOpenGroupEditor = onOpenGroupEditor,
    )
}

@Composable
private fun MainContent(
    selectedTab: MainTab,
    pendingRequestsCount: Int,
    onIntent: (MainIntent) -> Unit,
    onEditProfile: () -> Unit,
    onOpenSharingSettings: () -> Unit,
    onOpenGroupEditor: (String?) -> Unit,
) {
    Scaffold(
        bottomBar = {
            MainBottomBar(
                selectedTab = selectedTab,
                friendsBadgeCount = pendingRequestsCount,
                onTabSelected = { onIntent(MainIntent.TabSelected(it)) },
            )
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            when (selectedTab) {
                MainTab.MAP -> MapTabPlaceholder(modifier = Modifier.fillMaxSize())
                MainTab.FRIENDS -> FriendsScreen(
                    onOpenGroupEditor = onOpenGroupEditor,
                    modifier = Modifier.fillMaxSize(),
                )
                MainTab.PROFILE -> ProfileScreen(
                    onEditProfile = onEditProfile,
                    onOpenSharingSettings = onOpenSharingSettings,
                    onSignOutClick = { onIntent(MainIntent.SignOutClicked) },
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun MainContentPreview() {
    ShareMyLocationTheme {
        MainContent(
            selectedTab = MainTab.MAP,
            pendingRequestsCount = 0,
            onIntent = {},
            onEditProfile = {},
            onOpenSharingSettings = {},
            onOpenGroupEditor = {},
        )
    }
}
