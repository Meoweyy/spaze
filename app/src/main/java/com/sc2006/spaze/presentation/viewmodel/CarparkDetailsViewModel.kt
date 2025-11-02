package com.sc2006.spaze.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sc2006.spaze.data.local.entity.CarparkEntity
import com.sc2006.spaze.data.repository.CarparkRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Carpark Details ViewModel
 * Implements: Detailed carpark information, navigation, favorites
 * Business Logic: Distance calculation, availability status, favorite management
 */
@HiltViewModel
class CarparkDetailsViewModel @Inject constructor(
    private val carparkRepository: CarparkRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(CarparkDetailsUiState())
    val uiState: StateFlow<CarparkDetailsUiState> = _uiState.asStateFlow()

    private val _carpark = MutableStateFlow<CarparkEntity?>(null)
    val carpark: StateFlow<CarparkEntity?> = _carpark.asStateFlow()

    // ═══════════════════════════════════════════════════
    // CARPARK DETAILS
    // ═══════════════════════════════════════════════════

    /**
     * Load carpark details by ID
     */
    fun loadCarparkDetails(userId: String, carparkId: String, userLat: Double? = null, userLng: Double? = null) {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isLoading = true, error = null) }

                val carparkEntity = carparkRepository.getCarparkById(carparkId)
                if (carparkEntity == null) {
                    _uiState.update {
                        it.copy(isLoading = false, error = "Carpark not found")
                    }
                    return@launch
                }

                _carpark.value = carparkEntity

                // Calculate distance if user location provided
                val distance = if (userLat != null && userLng != null) {
                    calculateDistance(userLat, userLng, carparkEntity.latitude, carparkEntity.longitude)
                } else null

                // Check if favorited
                val isFavorite = carparkRepository.isFavorite(userId, carparkId)

                // Mark as viewed (for recent searches)
                carparkRepository.markCarparkAsViewed(userId, carparkId, carparkEntity.address)

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        isFavorite = isFavorite,
                        distanceKm = distance,
                        availabilityStatus = getAvailabilityStatus(carparkEntity)
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = e.message ?: "Failed to load carpark details"
                    )
                }
            }
        }
    }

    /**
     * Refresh carpark data
     */
    fun refreshCarparkData(userId: String, carparkId: String, userLat: Double? = null, userLng: Double? = null) {
        loadCarparkDetails(userId, carparkId, userLat, userLng)
    }

    // ═══════════════════════════════════════════════════
    // FAVORITE MANAGEMENT
    // ═══════════════════════════════════════════════════

    /**
     * Toggle favorite status
     */
    fun toggleFavorite(userId: String, carparkId: String) {
        viewModelScope.launch {
            try {
                val currentStatus = _uiState.value.isFavorite

                if (currentStatus) {
                    val result = carparkRepository.removeFromFavorites(userId, carparkId)
                    result.fold(
                        onSuccess = {
                            _uiState.update {
                                it.copy(
                                    isFavorite = false,
                                    successMessage = "Removed from favorites"
                                )
                            }
                        },
                        onFailure = { error ->
                            _uiState.update {
                                it.copy(error = error.message ?: "Failed to remove favorite")
                            }
                        }
                    )
                } else {
                    val result = carparkRepository.addToFavorites(userId, carparkId)
                    result.fold(
                        onSuccess = {
                            _uiState.update {
                                it.copy(
                                    isFavorite = true,
                                    successMessage = "Added to favorites"
                                )
                            }
                        },
                        onFailure = { error ->
                            _uiState.update {
                                it.copy(error = error.message ?: "Failed to add favorite")
                            }
                        }
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(error = e.message ?: "Failed to update favorite")
                }
            }
        }
    }

    // ═══════════════════════════════════════════════════
    // BUSINESS LOGIC
    // ═══════════════════════════════════════════════════

    /**
     * Calculate distance using Haversine formula
     */
    private fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val R = 6371.0 // Earth's radius in km

        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)

        val a = sin(dLat / 2) * sin(dLat / 2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                sin(dLon / 2) * sin(dLon / 2)

        val c = 2 * atan2(sqrt(a), sqrt(1 - a))

        return R * c
    }

    /**
     * Get availability status with color coding
     */
    private fun getAvailabilityStatus(carpark: CarparkEntity): AvailabilityStatus {
        val totalLots = carpark.getTotalLots()
        val availableLots = carpark.getTotalAvailableLots()

        if (totalLots == 0) return AvailabilityStatus.UNKNOWN

        val percentage = (availableLots.toDouble() / totalLots.toDouble()) * 100

        return when {
            availableLots == 0 -> AvailabilityStatus.FULL
            percentage < 10 -> AvailabilityStatus.ALMOST_FULL
            percentage < 30 -> AvailabilityStatus.LIMITED
            percentage < 50 -> AvailabilityStatus.MODERATE
            else -> AvailabilityStatus.AVAILABLE
        }
    }

    /**
     * Get lot breakdown by type
     */
    fun getLotBreakdown(): LotBreakdown? {
        val carpark = _carpark.value ?: return null

        return LotBreakdown(
            carLots = LotInfo(carpark.totalLotsC, carpark.availableLotsC),
            motorcycleLots = LotInfo(carpark.totalLotsH, carpark.availableLotsH),
            heavyVehicleLots = LotInfo(carpark.totalLotsY, carpark.availableLotsY),
            seasonLots = LotInfo(carpark.totalLotsS, carpark.availableLotsS)
        )
    }

    /**
     * Get directions URL (Google Maps)
     */
    fun getDirectionsUrl(): String? {
        val carpark = _carpark.value ?: return null
        return "https://www.google.com/maps/dir/?api=1&destination=${carpark.latitude},${carpark.longitude}"
    }

    /**
     * Get shareable text
     */
    fun getShareableText(): String? {
        val carpark = _carpark.value ?: return null
        val distance = _uiState.value.distanceKm

        val distanceText = if (distance != null) {
            String.format("%.2f km away", distance)
        } else ""

        return """
            Check out this carpark on Spaze!

            ${carpark.address}
            $distanceText

            Available Lots: ${carpark.getTotalAvailableLots()} / ${carpark.getTotalLots()}

            View in Spaze App
        """.trimIndent()
    }

    // ═══════════════════════════════════════════════════
    // UI STATE MANAGEMENT
    // ═══════════════════════════════════════════════════

    /**
     * Clear success message
     */
    fun clearSuccessMessage() {
        _uiState.update { it.copy(successMessage = null) }
    }

    /**
     * Clear error
     */
    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}

/**
 * Availability status enum with color coding
 */
enum class AvailabilityStatus(val displayText: String, val colorHex: String) {
    AVAILABLE("Available", "#4CAF50"),           // Green
    MODERATE("Moderate", "#8BC34A"),             // Light Green
    LIMITED("Limited", "#FFC107"),               // Orange
    ALMOST_FULL("Almost Full", "#FF9800"),       // Dark Orange
    FULL("Full", "#F44336"),                     // Red
    UNKNOWN("Unknown", "#9E9E9E")                // Gray
}

/**
 * Lot information data class
 */
data class LotInfo(
    val total: Int,
    val available: Int
) {
    val percentage: Double
        get() = if (total > 0) (available.toDouble() / total.toDouble()) * 100 else 0.0
}

/**
 * Lot breakdown by type
 */
data class LotBreakdown(
    val carLots: LotInfo,
    val motorcycleLots: LotInfo,
    val heavyVehicleLots: LotInfo,
    val seasonLots: LotInfo
)

data class CarparkDetailsUiState(
    val isLoading: Boolean = false,
    val isFavorite: Boolean = false,
    val distanceKm: Double? = null,
    val availabilityStatus: AvailabilityStatus = AvailabilityStatus.UNKNOWN,
    val successMessage: String? = null,
    val error: String? = null
)
