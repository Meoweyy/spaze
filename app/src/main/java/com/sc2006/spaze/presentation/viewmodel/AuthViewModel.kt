package com.sc2006.spaze.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sc2006.spaze.data.local.entity.UserEntity
import com.sc2006.spaze.data.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Authentication ViewModel
 * Manages user authentication state
 * Implements: Account and Authentication requirements
 *
 * Week 2 - Person 3: Added currentUserId StateFlow for ProfileScreen
 */
@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    /**
     * Expose current user ID as StateFlow
     * Used by ProfileScreen to load profile without hardcoding user ID
     */
    val currentUserId: StateFlow<String?> = uiState.map { state ->
        state.currentUser?.userID
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = null
    )

    init {
        // ✅ launch a coroutine to call the suspend function
        viewModelScope.launch {
            checkAuthStatus()
            // TODO - Person 4: Restore session from DataStore
            // authRepository.restoreSession()
            // checkAuthStatus()
        }
    }

    /**
     * Check if user is authenticated
     *
     * TODO - Person 4: After DataStore integration, this should check persisted auth state
     */
    private fun checkAuthStatus() {
        viewModelScope.launch {
            val user = authRepository.restoreSession()
            _uiState.update {
                it.copy(
                    isAuthenticated = user != null,
                    currentUser = user
                )
            }
        }
    }

    /**
     * Sign up with email
     */
    fun signUpWithEmail(userName: String, email: String, password: String) {
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
     * Sign in with email
     */
    fun signInWithEmail(email: String, password: String) {
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
     * Sign out
     */
    fun signOut() {
        viewModelScope.launch {
            authRepository.signOut()
            _uiState.update {
                it.copy(
                    isAuthenticated = false,
                    currentUser = null
                )
            }
        }
    }

    /**
     * Change password
     */
    fun changePassword(oldPassword: String, newPassword: String) {
        viewModelScope.launch {
            val userId = _uiState.value.currentUser?.userID ?: return@launch

            _uiState.update { it.copy(isLoading = true, error = null) }

            val result = authRepository.changePassword(userId, oldPassword, newPassword)
            result.fold(
                onSuccess = {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            passwordChangeSuccess = true
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
}

data class AuthUiState(
    val isLoading: Boolean = false,
    val isAuthenticated: Boolean = false,
    val currentUser: UserEntity? = null,
    val error: String? = null,
    val passwordResetEmailSent: Boolean = false,
    val passwordChangeSuccess: Boolean = false
)