package ru.tyupik.sharemylocation.ui.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import ru.tyupik.sharemylocation.business.auth.AuthInteractor

/**
 * ViewModel главного экрана-контейнера: хранит выбранную вкладку и выполняет выход из аккаунта.
 * Единственная публичная точка входа для UI — [onIntent].
 */
@HiltViewModel
class MainViewModel @Inject constructor(
    private val authInteractor: AuthInteractor,
) : ViewModel() {

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    private val _effects = Channel<MainEffect>(Channel.BUFFERED)
    val effects: Flow<MainEffect> = _effects.receiveAsFlow()

    fun onIntent(intent: MainIntent) {
        when (intent) {
            is MainIntent.TabSelected -> _uiState.update { it.copy(selectedTab = intent.tab) }
            MainIntent.SignOutClicked -> signOut()
        }
    }

    private fun signOut() {
        viewModelScope.launch {
            authInteractor.signOut()
            _effects.send(MainEffect.NavigateToAuth)
        }
    }
}
