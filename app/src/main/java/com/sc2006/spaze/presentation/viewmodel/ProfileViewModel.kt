package com.sc2006.spaze.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sc2006.spaze.data.local.entity.UserEntity
import com.sc2006.spaze.data.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    fun loadProfile(userId: String) {
        viewModelScope.launch {
            authRepository.getCurrentUserFlow(userId).collect { user ->
                _uiState.update { it.copy(user = user) }
            }
        }
    }

    fun updateProfile(userId: String, userName: String, email: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            // TODO: Implement profile update
            _uiState.update { it.copy(isLoading = false) }
        }
    }

    fun updatePreferences(userId: String, preferences: Map<String, Any>) {
        viewModelScope.launch {
            authRepository.updateUserPreferences(userId, preferences)
        }
    }

    fun changePassword(oldPassword: String, newPassword: String) {
        viewModelScope.launch {
            // TODO: Implement password change
        }
    }
}

data class ProfileUiState(
    val isLoading: Boolean = false,
    val user: UserEntity? = null,
    val error: String? = null
)