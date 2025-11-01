package com.sc2006.spaze.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sc2006.spaze.data.local.entity.CarparkEntity
import com.sc2006.spaze.data.repository.CarparkRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Favourites ViewModel - Person 3 (ViewModel Layer)
 * Week 2: Added comprehensive error handling to all methods
 *
 * Methods enhanced with error handling:
 * - loadFavorites() ✅
 * - addToFavorites() ✅
 * - removeFromFavorites() ✅
 * - isFavorite() ✅
 */
@HiltViewModel
class FavoritesViewModel @Inject constructor(
    private val carparkRepository: CarparkRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(FavoritesUiState())
    val uiState: StateFlow<FavoritesUiState> = _uiState.asStateFlow()

    private val _favoriteCarparks = MutableStateFlow<List<CarparkEntity>>(emptyList())
    val favoriteCarparks: StateFlow<List<CarparkEntity>> = _favoriteCarparks.asStateFlow()

    /**
     * Load user's favorite carparks from database
     * Enhanced with error handling and null safety
     */
    fun loadFavorites(userId: String) {
        viewModelScope.launch {
            try {
                // Validate input
                if (userId.isBlank()) {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = "Invalid user ID"
                        )
                    }
                    return@launch
                }

                _uiState.update { it.copy(isLoading = true, error = null, successMessage = null) }

                carparkRepository.getFavoriteCarparks(userId).collect { carparks ->
                    _favoriteCarparks.value = carparks
                    _uiState.update { it.copy(isLoading = false, error = null) }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = "Failed to load favorites: ${e.message}"
                    )
                }
                _favoriteCarparks.value = emptyList()
            }
        }
    }

    /**
     * Add carpark to user's favorites
     * Enhanced with error handling, validation, and success feedback
     */
    fun addToFavorites(userId: String, carparkId: String) {
        viewModelScope.launch {
            try {
                // Validate inputs
                if (userId.isBlank() || carparkId.isBlank()) {
                    _uiState.update {
                        it.copy(error = "Invalid user ID or carpark ID")
                    }
                    return@launch
                }

                _uiState.update { it.copy(isLoading = true, error = null, successMessage = null) }

                carparkRepository.addToFavorites(userId, carparkId)

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        successMessage = "Added to favorites"
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = "Failed to add to favorites: ${e.message}"
                    )
                }
            }
        }
    }

    /**
     * Remove carpark from user's favorites
     * Enhanced with error handling, validation, and success feedback
     */
    fun removeFromFavorites(userId: String, carparkId: String) {
        viewModelScope.launch {
            try {
                // Validate inputs
                if (userId.isBlank() || carparkId.isBlank()) {
                    _uiState.update {
                        it.copy(error = "Invalid user ID or carpark ID")
                    }
                    return@launch
                }

                _uiState.update { it.copy(isLoading = true, error = null, successMessage = null) }

                carparkRepository.removeFromFavorites(userId, carparkId)

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        successMessage = "Removed from favorites"
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = "Failed to remove from favorites: ${e.message}"
                    )
                }
            }
        }
    }

    /**
     * Check if carpark is in user's favorites
     * Enhanced with error handling and validation
     *
     * @return true if favorite, false if not or on error
     */
    suspend fun isFavorite(userId: String, carparkId: String): Boolean {
        return try {
            // Validate inputs
            if (userId.isBlank() || carparkId.isBlank()) {
                return false
            }

            carparkRepository.isFavorite(userId, carparkId)
        } catch (e: Exception) {
            // Log error but don't show to user (UI just shows unfavorited state)
            // Could add logging here if needed
            false
        }
    }

    /**
     * Clear error and success messages from UI state
     */
    fun clearMessages() {
        _uiState.update { it.copy(error = null, successMessage = null) }
    }
}

/**
 * UI State for Favorites Screen
 * Enhanced with successMessage field for user feedback
 */
data class FavoritesUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val successMessage: String? = null
)