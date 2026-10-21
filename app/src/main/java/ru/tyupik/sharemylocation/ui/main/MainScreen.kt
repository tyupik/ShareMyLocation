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
import ru.tyupik.sharemylocation.ui.main.composable.FriendsTabPlaceholder
import ru.tyupik.sharemylocation.ui.main.composable.MainBottomBar
import ru.tyupik.sharemylocation.ui.main.composable.MapTabPlaceholder
import ru.tyupik.sharemylocation.ui.profile.ProfileScreen
import ru.tyupik.sharemylocation.ui.theme.ShareMyLocationTheme

/**
 * Главный экран-контейнер: нижняя навигация с вкладками Карта, Друзья, Профиль.
 * Навигацию выполняет вызывающая сторона через [onSignedOut] и [onEditProfile]:
 * экран сам не знает про back stack, лямбды вызываются только из коллекций Effect.
 */
@Composable
internal fun MainScreen(
    onSignedOut: () -> Unit,
    onEditProfile: () -> Unit,
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
        onIntent = viewModel::onIntent,
        onEditProfile = onEditProfile,
    )
}

@Composable
private fun MainContent(
    selectedTab: MainTab,
    onIntent: (MainIntent) -> Unit,
    onEditProfile: () -> Unit,
) {
    Scaffold(
        bottomBar = {
            MainBottomBar(
                selectedTab = selectedTab,
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
                MainTab.FRIENDS -> FriendsTabPlaceholder(modifier = Modifier.fillMaxSize())
                MainTab.PROFILE -> ProfileScreen(
                    onEditProfile = onEditProfile,
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
        MainContent(selectedTab = MainTab.MAP, onIntent = {}, onEditProfile = {})
    }
}
