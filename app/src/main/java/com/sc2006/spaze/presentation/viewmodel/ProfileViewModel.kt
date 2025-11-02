package com.sc2006.spaze.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sc2006.spaze.data.local.entity.CarparkEntity
import com.sc2006.spaze.data.local.entity.ParkingSessionEntity
import com.sc2006.spaze.data.local.entity.RecentSearchEntity
import com.sc2006.spaze.data.local.entity.UserEntity
import com.sc2006.spaze.data.repository.AuthRepository
import com.sc2006.spaze.data.repository.CarparkRepository
import com.sc2006.spaze.data.repository.ParkingSessionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Profile ViewModel
 * Implements: User Profile Management, Settings, Recent Activity
 * Corresponds to ProfileController in class diagram
 */
@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val carparkRepository: CarparkRepository,
    private val parkingSessionRepository: ParkingSessionRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    private val _recentSearches = MutableStateFlow<List<RecentSearchEntity>>(emptyList())
    val recentSearches: StateFlow<List<RecentSearchEntity>> = _recentSearches.asStateFlow()

    private val _recentlyViewedCarparks = MutableStateFlow<List<CarparkEntity>>(emptyList())
    val recentlyViewedCarparks: StateFlow<List<CarparkEntity>> = _recentlyViewedCarparks.asStateFlow()

    private val _parkingHistory = MutableStateFlow<List<ParkingSessionEntity>>(emptyList())
    val parkingHistory: StateFlow<List<ParkingSessionEntity>> = _parkingHistory.asStateFlow()

    /**
     * Load complete profile data
     */
    fun loadProfile(userId: String) {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isLoading = true, error = null) }

                // Load user profile
                authRepository.getCurrentUserFlow(userId).collect { user ->
                    _uiState.update { it.copy(user = user, isLoading = false) }
                }

                // Load additional profile data
                loadRecentActivity(userId)
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isLoading = false, error = e.message ?: "Failed to load profile")
                }
            }
        }
    }

    /**
     * Load recent activity (searches, viewed carparks, parking sessions)
     */
    private fun loadRecentActivity(userId: String) {
        viewModelScope.launch {
            // Load recent searches
            carparkRepository.getRecentSearches(userId, limit = 10).collect { searches ->
                _recentSearches.value = searches
            }
        }

        viewModelScope.launch {
            // Load recently viewed carparks
            carparkRepository.getRecentlyViewedCarparks(limit = 10).collect { carparks ->
                _recentlyViewedCarparks.value = carparks
            }
        }

        viewModelScope.launch {
            // Load parking history
            parkingSessionRepository.getRecentSessions(userId, limit = 20).collect { sessions ->
                _parkingHistory.value = sessions
            }
        }
    }

    /**
     * Reload recent searches only
     */
    fun loadRecentSearches(userId: String) {
        viewModelScope.launch {
            carparkRepository.getRecentSearches(userId, limit = 10).collect { searches ->
                _recentSearches.value = searches
            }
        }
    }

    /**
     * Reload recently viewed carparks only
     */
    fun loadRecentlyViewedCarparks() {
        viewModelScope.launch {
            carparkRepository.getRecentlyViewedCarparks(limit = 10).collect { carparks ->
                _recentlyViewedCarparks.value = carparks
            }
        }
    }

    /**
     * Reload parking history only
     */
    fun loadParkingHistory(userId: String) {
        viewModelScope.launch {
            parkingSessionRepository.getRecentSessions(userId, limit = 20).collect { sessions ->
                _parkingHistory.value = sessions
            }
        }
    }

    /**
     * Update user profile information
     */
    fun updateProfile(userId: String, userName: String, email: String) {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isLoading = true, error = null) }

                // Get current user
                val currentUser = authRepository.getCurrentUser()
                    ?: throw Exception("User not found")

                // Update user profile (this needs to be implemented in AuthRepository)
                // For now, we just update preferences
                val preferences = mapOf(
                    "userName" to userName,
                    "email" to email
                )

                val result = authRepository.updateUserPreferences(userId, preferences)
                result.fold(
                    onSuccess = {
                        _uiState.update { it.copy(isLoading = false, updateSuccess = true) }
                    },
                    onFailure = { error ->
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                error = error.message ?: "Failed to update profile"
                            )
                        }
                    }
                )
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = e.message ?: "Failed to update profile"
                    )
                }
            }
        }
    }

    /**
     * Update user preferences (settings)
     */
    fun updatePreferences(userId: String, preferences: Map<String, Any>) {
        viewModelScope.launch {
            try {
                val result = authRepository.updateUserPreferences(userId, preferences)
                result.fold(
                    onSuccess = {
                        _uiState.update { it.copy(updateSuccess = true) }
                    },
                    onFailure = { error ->
                        _uiState.update {
                            it.copy(error = error.message ?: "Failed to update preferences")
                        }
                    }
                )
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(error = e.message ?: "Failed to update preferences")
                }
            }
        }
    }

    /**
     * Change password
     */
    fun changePassword(oldPassword: String, newPassword: String) {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isLoading = true, error = null) }

                // TODO: Implement password change in AuthRepository
                // For now, this is a placeholder

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = "Password change not yet implemented"
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = e.message ?: "Failed to change password"
                    )
                }
            }
        }
    }

    /**
     * Clear recent searches
     */
    fun clearRecentSearches(userId: String) {
        viewModelScope.launch {
            try {
                carparkRepository.clearRecentSearches(userId)
                _recentSearches.value = emptyList()
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(error = e.message ?: "Failed to clear recent searches")
                }
            }
        }
    }

    /**
     * Clear parking history
     */
    fun clearParkingHistory(userId: String) {
        viewModelScope.launch {
            try {
                val result = parkingSessionRepository.deleteSessionHistory(userId)
                result.fold(
                    onSuccess = {
                        _parkingHistory.value = emptyList()
                    },
                    onFailure = { error ->
                        _uiState.update {
                            it.copy(error = error.message ?: "Failed to clear parking history")
                        }
                    }
                )
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(error = e.message ?: "Failed to clear parking history")
                }
            }
        }
    }

    /**
     * Sign out
     */
    fun signOut() {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isLoading = true, error = null) }

                val result = authRepository.signOut()
                result.fold(
                    onSuccess = {
                        _uiState.update {
                            it.copy(isLoading = false, user = null, isSignedOut = true)
                        }
                    },
                    onFailure = { error ->
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                error = error.message ?: "Failed to sign out"
                            )
                        }
                    }
                )
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = e.message ?: "Failed to sign out"
                    )
                }
            }
        }
    }

    /**
     * Clear update success flag
     */
    fun clearUpdateSuccess() {
        _uiState.update { it.copy(updateSuccess = false) }
    }

    /**
     * Clear error message
     */
    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}

data class ProfileUiState(
    val isLoading: Boolean = false,
    val user: UserEntity? = null,
    val error: String? = null,
    val updateSuccess: Boolean = false,
    val isSignedOut: Boolean = false
)