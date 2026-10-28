package com.example.minibooklibrary.ui.auth

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.minibooklibrary.R
import com.example.minibooklibrary.data.repository.AuthResult
import com.example.minibooklibrary.data.repository.UserRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Backs both the login and register screens. Keeping them in one ViewModel is
 * intentional — they share validation rules and a single "authenticating" loading
 * indicator, and switching between the two is a navigation tab on a single screen
 * rather than two graphs.
 */
class AuthViewModel(private val userRepository: UserRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<AuthEvent>(extraBufferCapacity = 1)
    val events: SharedFlow<AuthEvent> = _events.asSharedFlow()

    fun login(username: String, password: String) {
        if (_uiState.value.isLoading) return
        _uiState.value = _uiState.value.copy(isLoading = true, error = null)
        viewModelScope.launch {
            when (val result = userRepository.login(username, password)) {
                is AuthResult.Success -> {
                    _uiState.value = _uiState.value.copy(isLoading = false)
                    _events.emit(AuthEvent.LoggedIn(result.userId, result.username))
                }
                is AuthResult.Failure -> {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = result.reason.toAuthError()
                    )
                }
            }
        }
    }

    fun register(username: String, email: String, password: String, confirmPassword: String) {
        if (_uiState.value.isLoading) return
        if (password != confirmPassword) {
            _uiState.value = _uiState.value.copy(
                error = AuthError(AuthField.CONFIRM_PASSWORD, R.string.auth_error_password_mismatch)
            )
            return
        }
        _uiState.value = _uiState.value.copy(isLoading = true, error = null)
        viewModelScope.launch {
            when (val result = userRepository.register(username, email, password)) {
                is AuthResult.Success -> {
                    _uiState.value = _uiState.value.copy(isLoading = false)
                    _events.emit(AuthEvent.Registered(result.username))
                }
                is AuthResult.Failure -> {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = result.reason.toAuthError()
                    )
                }
            }
        }
    }

    fun consumeError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    /** Sign-out — clears persisted session and emits a navigation event. */
    fun logout() {
        userRepository.logout()
        viewModelScope.launch { _events.emit(AuthEvent.LoggedOut) }
    }
}

data class AuthUiState(
    val isLoading: Boolean = false,
    val error: AuthError? = null
)

enum class AuthField { USERNAME, EMAIL, PASSWORD, CONFIRM_PASSWORD }

/** An error to show inline under [field]. */
data class AuthError(val field: AuthField, @StringRes val messageRes: Int)

internal fun AuthResult.FailureReason.toAuthError(): AuthError = when (this) {
    AuthResult.FailureReason.MISSING_FIELDS ->
        AuthError(AuthField.USERNAME, R.string.auth_error_username_required)
    AuthResult.FailureReason.USERNAME_TOO_SHORT ->
        AuthError(AuthField.USERNAME, R.string.auth_error_username_short)
    AuthResult.FailureReason.USERNAME_TAKEN ->
        AuthError(AuthField.USERNAME, R.string.auth_error_username_taken)
    AuthResult.FailureReason.INVALID_EMAIL ->
        AuthError(AuthField.EMAIL, R.string.auth_error_email_invalid)
    AuthResult.FailureReason.PASSWORD_TOO_SHORT ->
        AuthError(AuthField.PASSWORD, R.string.auth_error_password_short)
    // Deliberately not saying which half was wrong.
    AuthResult.FailureReason.INVALID_CREDENTIALS ->
        AuthError(AuthField.PASSWORD, R.string.auth_error_invalid_credentials)
}

sealed class AuthEvent {
    data class LoggedIn(val userId: Long, val username: String) : AuthEvent()
    data class Registered(val username: String) : AuthEvent()
    data object LoggedOut : AuthEvent()
}
