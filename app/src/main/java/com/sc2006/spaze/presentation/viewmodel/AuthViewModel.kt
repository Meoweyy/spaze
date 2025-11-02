package com.sc2006.spaze.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sc2006.spaze.data.local.entity.UserEntity
import com.sc2006.spaze.data.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    init {
        // Check if user is already logged in (session restoration)
        viewModelScope.launch {
            checkAuthStatus()
        }
    }

    /**
     * Check if user is authenticated (restore session)
     */
    private suspend fun checkAuthStatus() {
        val user = authRepository.getCurrentUser()
        _uiState.update { it.copy(isAuthenticated = user != null, currentUser = user) }
    }

    // ═══════════════════════════════════════════════════
    // VALIDATION METHODS
    // ═══════════════════════════════════════════════════

    /**
     * Validate email format
     */
    private fun validateEmail(email: String): String? {
        if (email.isBlank()) return "Email is required"
        if (!email.matches(Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$"))) {
            return "Invalid email format"
        }
        return null
    }

    /**
     * Validate password strength
     */
    private fun validatePassword(password: String): String? {
        if (password.isBlank()) return "Password is required"
        if (password.length < 6) return "Password must be at least 6 characters"
        if (password.length > 128) return "Password is too long"
        return null
    }

    /**
     * Validate username
     */
    private fun validateUserName(userName: String): String? {
        if (userName.isBlank()) return "Username is required"
        if (userName.length < 3) return "Username must be at least 3 characters"
        if (userName.length > 50) return "Username is too long"
        if (!userName.matches(Regex("^[a-zA-Z0-9_]+$"))) {
            return "Username can only contain letters, numbers, and underscores"
        }
        return null
    }

    /**
     * Validate password confirmation
     */
    private fun validatePasswordConfirmation(password: String, confirmPassword: String): String? {
        if (password != confirmPassword) return "Passwords do not match"
        return null
    }

    // ═══════════════════════════════════════════════════
    // AUTHENTICATION METHODS
    // ═══════════════════════════════════════════════════

    /**
     * Sign up with email (with validation)
     */
    fun signUpWithEmail(userName: String, email: String, password: String, confirmPassword: String? = null) {
        // Validate inputs
        validateUserName(userName)?.let { error ->
            _uiState.update { it.copy(error = error) }
            return
        }
        validateEmail(email)?.let { error ->
            _uiState.update { it.copy(error = error) }
            return
        }
        validatePassword(password)?.let { error ->
            _uiState.update { it.copy(error = error) }
            return
        }
        // Validate password confirmation if provided
        confirmPassword?.let { confirm ->
            validatePasswordConfirmation(password, confirm)?.let { error ->
                _uiState.update { it.copy(error = error) }
                return
            }
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            val result = authRepository.signUpWithEmail(userName, email, password)
            result.fold(
                onSuccess = { user ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isAuthenticated = true,
                            currentUser = user
                        )
                    }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(isLoading = false, error = error.message)
                    }
                }
            )
        }
    }

    /**
     * Sign in with email (with validation)
     */
    fun signInWithEmail(email: String, password: String) {
        // Validate inputs
        validateEmail(email)?.let { error ->
            _uiState.update { it.copy(error = error) }
            return
        }
        validatePassword(password)?.let { error ->
            _uiState.update { it.copy(error = error) }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            val result = authRepository.signInWithEmail(email, password)
            result.fold(
                onSuccess = { user ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isAuthenticated = true,
                            currentUser = user
                        )
                    }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(isLoading = false, error = error.message)
                    }
                }
            )
        }
    }

    /**
     * Sign in with Google
     */
    fun signInWithGoogle(idToken: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            val result = authRepository.signInWithGoogle(idToken)
            result.fold(
                onSuccess = { user ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isAuthenticated = true,
                            currentUser = user
                        )
                    }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(isLoading = false, error = error.message)
                    }
                }
            )
        }
    }

    /**
     * Reset password (with validation)
     */
    fun resetPassword(email: String) {
        // Validate email
        validateEmail(email)?.let { error ->
            _uiState.update { it.copy(error = error) }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            val result = authRepository.resetPassword(email)
            result.fold(
                onSuccess = {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            resetPasswordSuccess = true
                        )
                    }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(isLoading = false, error = error.message)
                    }
                }
            )
        }
    }

    /**
     * Sign out (with loading state)
     */
    fun signOut() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            val result = authRepository.signOut()
            result.fold(
                onSuccess = {
                    _uiState.update {
                        it.copy(
                            isAuthenticated = false,
                            currentUser = null,
                            error = null,
                            isLoading = false
                        )
                    }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(error = error.message, isLoading = false)
                    }
                }
            )
        }
    }

    /**
     * Clear error
     */
    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    /**
     * Clear reset password success flag
     */
    fun clearResetPasswordSuccess() {
        _uiState.update { it.copy(resetPasswordSuccess = false) }
    }

    /**
     * Clear session (for debugging)
     */
    fun clearSession() {
        viewModelScope.launch {
            authRepository.signOut()
            _uiState.update { AuthUiState() }
        }
    }
}

data class AuthUiState(
    val isLoading: Boolean = false,
    val isAuthenticated: Boolean = false,
    val currentUser: UserEntity? = null,
    val error: String? = null,
    val resetPasswordSuccess: Boolean = false
)