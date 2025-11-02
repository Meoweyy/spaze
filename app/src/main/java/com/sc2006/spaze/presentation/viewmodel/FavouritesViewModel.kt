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
 * Favorites ViewModel
 * Implements: Favorite Carparks Management
 * Corresponds to FavoriteController in class diagram
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
     * Load user's favorite carparks
     */
    fun loadFavorites(userId: String) {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isLoading = true, error = null) }

                carparkRepository.getFavoriteCarparks(userId).collect { carparks ->
                    _favoriteCarparks.value = carparks
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isEmpty = carparks.isEmpty()
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = e.message ?: "Failed to load favorites"
                    )
                }
            }
        }
    }

    /**
     * Add carpark to favorites
     */
    fun addToFavorites(userId: String, carparkId: String) {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(error = null) }

                val result = carparkRepository.addToFavorites(userId, carparkId)
                result.fold(
                    onSuccess = {
                        _uiState.update {
                            it.copy(
                                operationSuccess = true,
                                successMessage = "Added to favorites"
                            )
                        }
                    },
                    onFailure = { error ->
                        _uiState.update {
                            it.copy(
                                error = error.message ?: "Failed to add to favorites"
                            )
                        }
                    }
                )
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        error = e.message ?: "Failed to add to favorites"
                    )
                }
            }
        }
    }

    /**
     * Remove carpark from favorites
     */
    fun removeFromFavorites(userId: String, carparkId: String) {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(error = null) }

                val result = carparkRepository.removeFromFavorites(userId, carparkId)
                result.fold(
                    onSuccess = {
                        _uiState.update {
                            it.copy(
                                operationSuccess = true,
                                successMessage = "Removed from favorites"
                            )
                        }
                    },
                    onFailure = { error ->
                        _uiState.update {
                            it.copy(
                                error = error.message ?: "Failed to remove from favorites"
                            )
                        }
                    }
                )
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        error = e.message ?: "Failed to remove from favorites"
                    )
                }
            }
        }
    }

    /**
     * Toggle favorite status for a carpark
     */
    fun toggleFavorite(userId: String, carparkId: String, isFavorite: Boolean) {
        if (isFavorite) {
            removeFromFavorites(userId, carparkId)
        } else {
            addToFavorites(userId, carparkId)
        }
    }

    /**
     * Check if a carpark is favorited
     */
    suspend fun isFavorite(userId: String, carparkId: String): Boolean {
        return try {
            carparkRepository.isFavorite(userId, carparkId)
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Get carpark by ID from favorites
     */
    fun getFavoriteCarparkById(carparkId: String): CarparkEntity? {
        return _favoriteCarparks.value.find { it.carparkNumber == carparkId }
    }

    /**
     * Sort favorites by name
     */
    fun sortByName() {
        _favoriteCarparks.value = _favoriteCarparks.value.sortedBy { it.address }
    }

    /**
     * Sort favorites by availability
     */
    fun sortByAvailability() {
        _favoriteCarparks.value = _favoriteCarparks.value.sortedByDescending {
            it.getTotalAvailableLots()
        }
    }

    /**
     * Sort favorites by distance (requires user location)
     */
    fun sortByDistance(userLat: Double, userLng: Double) {
        _favoriteCarparks.value = _favoriteCarparks.value.sortedBy {
            calculateDistance(userLat, userLng, it.latitude, it.longitude)
        }
    }

    /**
     * Calculate distance using Haversine formula
     */
    private fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val R = 6371.0 // Earth's radius in km

        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)

        val a = kotlin.math.sin(dLat / 2) * kotlin.math.sin(dLat / 2) +
                kotlin.math.cos(Math.toRadians(lat1)) * kotlin.math.cos(Math.toRadians(lat2)) *
                kotlin.math.sin(dLon / 2) * kotlin.math.sin(dLon / 2)

        val c = 2 * kotlin.math.atan2(kotlin.math.sqrt(a), kotlin.math.sqrt(1 - a))

        return R * c
    }

    /**
     * Clear success message
     */
    fun clearSuccessMessage() {
        _uiState.update { it.copy(operationSuccess = false, successMessage = null) }
    }

    /**
     * Clear error message
     */
    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}

data class FavoritesUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val isEmpty: Boolean = false,
    val operationSuccess: Boolean = false,
    val successMessage: String? = null
)