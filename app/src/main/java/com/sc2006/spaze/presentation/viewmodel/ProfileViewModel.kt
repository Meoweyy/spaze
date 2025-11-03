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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

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

    init {
        // Optionally auto-load the current user at startup
        loadCurrentUser()
    }

    /** Loads the currently authenticated user's profile and recent activity */
    fun loadCurrentUser() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            val current = authRepository.getCurrentUser()
            if (current == null) {
                _uiState.update { it.copy(isLoading = false, error = "No active session") }
                return@launch
            }

            // Start a collection for live updates to the user entity
            viewModelScope.launch {
                authRepository.getCurrentUserFlow(current.userID).collect { user ->
                    _uiState.update { it.copy(user = user) }
                }
            }

            // Load additional profile data concurrently
            loadRecentActivity(current.userID)

            _uiState.update { it.copy(isLoading = false) }
        }
    }

    /** (Kept for compatibility) explicit-load by userId if needed elsewhere */
    fun loadProfile(userId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            // Collect user stream without blocking other work
            viewModelScope.launch {
                authRepository.getCurrentUserFlow(userId).collect { user ->
                    _uiState.update { it.copy(user = user) }
                }
            }

            loadRecentActivity(userId)
            _uiState.update { it.copy(isLoading = false) }
        }
    }

    private fun loadRecentActivity(userId: String) {
        viewModelScope.launch {
            carparkRepository.getRecentSearches(userId, limit = 10).collect { searches ->
                _recentSearches.value = searches
            }
        }
        viewModelScope.launch {
            carparkRepository.getRecentlyViewedCarparks(limit = 10).collect { carparks ->
                _recentlyViewedCarparks.value = carparks
            }
        }
        viewModelScope.launch {
            parkingSessionRepository.getRecentSessions(userId, limit = 20).collect { sessions ->
                _parkingHistory.value = sessions
            }
        }
    }

    /** Update user profile information (username/email) */
    fun updateProfile(userName: String, email: String) {
        val userId = authRepository.currentUserId() ?: run {
            _uiState.update { it.copy(error = "No active session") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            val result = authRepository.updateUserProfile(userId, userName, email)
            result.fold(
                onSuccess = {
                    _uiState.update { it.copy(isLoading = false, updateSuccess = true) }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(isLoading = false, error = error.message ?: "Failed to update profile")
                    }
                }
            )
        }
    }

    fun updatePreferences(preferences: Map<String, Any>) {
        val userId = authRepository.currentUserId() ?: run {
            _uiState.update { it.copy(error = "No active session") }
            return
        }
        viewModelScope.launch {
            val result = authRepository.updateUserPreferences(userId, preferences)
            result.fold(
                onSuccess = { _uiState.update { it.copy(updateSuccess = true) } },
                onFailure = { e -> _uiState.update { it.copy(error = e.message ?: "Failed to update preferences") } }
            )
        }
    }

    fun changePassword(currentPassword: String, newPassword: String) {
        val userId = authRepository.currentUserId() ?: run {
            _uiState.update { it.copy(error = "No active session") }
            return
        }
        
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            
            val result = authRepository.changePassword(userId, currentPassword, newPassword)
            result.fold(
                onSuccess = {
                    _uiState.update { 
                        it.copy(
                            isLoading = false, 
                            updateSuccess = true
                        ) 
                    }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false, 
                            error = error.message ?: "Failed to change password"
                        )
                    }
                }
            )
        }
    }

    fun clearRecentSearches() {
        val userId = authRepository.currentUserId() ?: return
        viewModelScope.launch {
            try {
                carparkRepository.clearRecentSearches(userId)
                _recentSearches.value = emptyList()
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message ?: "Failed to clear recent searches") }
            }
        }
    }

    fun clearParkingHistory() {
        val userId = authRepository.currentUserId() ?: return
        viewModelScope.launch {
            val result = parkingSessionRepository.deleteSessionHistory(userId)
            result.fold(
                onSuccess = { _parkingHistory.value = emptyList() },
                onFailure = { e -> _uiState.update { it.copy(error = e.message ?: "Failed to clear parking history") } }
            )
        }
    }

    fun signOut() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            val result = authRepository.signOut()
            result.fold(
                onSuccess = {
                    _uiState.update { it.copy(isLoading = false, user = null, isSignedOut = true) }
                },
                onFailure = { e ->
                    _uiState.update { it.copy(isLoading = false, error = e.message ?: "Failed to sign out") }
                }
            )
        }
    }

    fun clearUpdateSuccess() { _uiState.update { it.copy(updateSuccess = false) } }
    fun clearError() { _uiState.update { it.copy(error = null) } }
}

data class ProfileUiState(
    val isLoading: Boolean = false,
    val user: UserEntity? = null,
    val error: String? = null,
    val updateSuccess: Boolean = false,
    val isSignedOut: Boolean = false
)
