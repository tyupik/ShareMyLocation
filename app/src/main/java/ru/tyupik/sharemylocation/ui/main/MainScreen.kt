package ru.tyupik.sharemylocation.ui.main

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import ru.tyupik.sharemylocation.ui.map.MapScreen
import ru.tyupik.sharemylocation.ui.profile.ProfileScreen
import ru.tyupik.sharemylocation.ui.theme.ShareMyLocationTheme

/**
 * Главный экран-контейнер: нижняя навигация с вкладками Карта, Друзья, Профиль.
 * Навигацию выполняет вызывающая сторона через [onSignedOut], [onEditProfile],
 * [onOpenSharingSettings] и [onOpenGroupEditor]: экран сам не знает про back stack,
 * лямбды вызываются только из коллекций Effect.
 *
 * Сбор Effect включает разовый запрос права POST_NOTIFICATIONS: системный диалог
 * запускается Effect'ом [MainEffect.RequestNotificationsPermission], результат —
 * intent'ом [MainIntent.NotificationsPermissionResult].
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

    // Системный диалог права на уведомления: запускается Effect'ом, результат — intent'ом.
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { granted ->
        viewModel.onIntent(MainIntent.NotificationsPermissionResult(granted = granted))
    }

    LaunchedEffect(Unit) {
        viewModel.effects.collect { effect ->
            when (effect) {
                MainEffect.NavigateToAuth -> onSignedOut()
                MainEffect.RequestNotificationsPermission -> notificationPermissionLauncher.launch(
                    Manifest.permission.POST_NOTIFICATIONS,
                )
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
                MainTab.MAP -> MapScreen(modifier = Modifier.fillMaxSize())
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
