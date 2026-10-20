package ru.tyupik.sharemylocation.ui.common

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import ru.tyupik.sharemylocation.business.auth.AuthInteractor

/**
 * Состояние корневой навигации: пока [isSessionResolved] равно false,
 * стартовый экран не определён и UI показывает индикатор загрузки.
 */
data class AppUiState(
    val isSessionResolved: Boolean = false,
    val isAuthenticated: Boolean = false,
)

/**
 * Корневая ViewModel приложения: один раз при старте через интерактор проверяет
 * сохранённую сессию (Firebase Auth персистит её сам), чтобы навигация выбрала стартовый экран.
 * Пользовательских действий нет, поэтому Intent/Effect у экрана отсутствуют.
 */
@HiltViewModel
class AppViewModel @Inject constructor(
    private val authInteractor: AuthInteractor,
    private val appUiConverter: AppUiConverter,
) : ViewModel() {

    private val _uiState = MutableStateFlow(AppUiState())
    val uiState: StateFlow<AppUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val uiModel = appUiConverter.convert(authInteractor.getCurrentUser())
            _uiState.value = AppUiState(
                isSessionResolved = true,
                isAuthenticated = uiModel.isAuthenticated,
            )
        }
    }
}
