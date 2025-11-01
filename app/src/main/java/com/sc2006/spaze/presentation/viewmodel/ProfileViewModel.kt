package com.sc2006.spaze.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sc2006.spaze.data.local.entity.UserEntity
import com.sc2006.spaze.data.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Profile ViewModel - Person 3 (ViewModel Layer)
 * Week 2 Tasks:
 *  - Task 5.1: Implement updateProfile() ✅
 *  - Task 5.2: Implement changePassword() ✅
 *  - Task 5.3: Add error handling to loadProfile() ✅
 *  - Add logout() method ✅
 */
@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    /**
     * Task 5.3: Load user profile with error handling
     * Added try-catch for error scenarios
     */
    fun loadProfile(userId: String) {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isLoading = true, error = null) }

                authRepository.getCurrentUserFlow(userId).collect { user ->
                    if (user == null) {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                error = "User not found",
                                user = null
                            )
                        }
                    } else {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                user = user,
                                error = null
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = "Failed to load profile: ${e.message}"
                    )
                }
            }
        }
    }

    /**
     * Task 5.1: Update user profile (username and email)
     *
     * TODO - Person 4: Add this method to AuthRepository:
     * suspend fun updateUserProfile(userId: String, userName: String, email: String): Result<UserEntity>
     */
    fun updateProfile(userId: String, userName: String, email: String) {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isLoading = true, error = null, successMessage = null) }

                // TODO - Person 4: Uncomment when AuthRepository.updateUserProfile() is implemented
                /*
                val result = authRepository.updateUserProfile(userId, userName, email)
                result.fold(
                    onSuccess = { updatedUser ->
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                user = updatedUser,
                                successMessage = "Profile updated successfully"
                            )
                        }
                    },
                    onFailure = { error ->
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                error = "Failed to update profile: ${error.message}"
                            )
                        }
                    }
                )
                */

                // Temporary: Show error until Person 4 implements the repository method
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = "Update profile not yet implemented - Person 4 needs to add AuthRepository.updateUserProfile()"
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = "Failed to update profile: ${e.message}"
                    )
                }
            }
        }
    }

    fun updatePreferences(userId: String, preferences: Map<String, Any>) {
        viewModelScope.launch {
            try {
                authRepository.updateUserPreferences(userId, preferences)
                _uiState.update { it.copy(successMessage = "Preferences updated") }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "Failed to update preferences: ${e.message}") }
            }
        }
    }

    /**
     * Task 5.2: Change user password
     *
     * TODO - Person 4: Add this method to AuthRepository:
     * suspend fun changePassword(userId: String, oldPassword: String, newPassword: String): Result<Unit>
     */
    fun changePassword(userId: String, oldPassword: String, newPassword: String) {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isLoading = true, error = null, successMessage = null) }

                // Validation
                if (oldPassword.isBlank() || newPassword.isBlank()) {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = "Passwords cannot be empty"
                        )
                    }
                    return@launch
                }

                if (newPassword.length < 6) {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = "New password must be at least 6 characters"
                        )
                    }
                    return@launch
                }

                // TODO - Person 4: Uncomment when AuthRepository.changePassword() is implemented
                /*
                val result = authRepository.changePassword(userId, oldPassword, newPassword)
                result.fold(
                    onSuccess = {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                successMessage = "Password changed successfully"
                            )
                        }
                    },
                    onFailure = { error ->
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                error = "Failed to change password: ${error.message}"
                            )
                        }
                    }
                )
                */

                // Temporary: Show error until Person 4 implements the repository method
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = "Change password not yet implemented - Person 4 needs to add AuthRepository.changePassword()"
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = "Failed to change password: ${e.message}"
                    )
                }
            }
        }
    }

    /**
     * Logout current user
     * Calls AuthRepository.signOut() to clear authentication state
     *
     * TODO - Person 4: Ensure AuthRepository.signOut() clears DataStore session
     */
    fun logout() {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isLoading = true, error = null) }

                val result = authRepository.signOut()
                result.fold(
                    onSuccess = {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                user = null,
                                successMessage = "Logged out successfully"
                            )
                        }
                    },
                    onFailure = { error ->
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                error = "Failed to logout: ${error.message}"
                            )
                        }
                    }
                )
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = "Failed to logout: ${e.message}"
                    )
                }
            }
        }
    }

    /**
     * Clear success or error messages from UI state
     */
    fun clearMessages() {
        _uiState.update { it.copy(error = null, successMessage = null) }
    }
}

data class ProfileUiState(
    val isLoading: Boolean = false,
    val user: UserEntity? = null,
    val error: String? = null,
    val successMessage: String? = null
)