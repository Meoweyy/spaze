package com.sc2006.spaze.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.maps.model.LatLng
import com.sc2006.spaze.data.local.entity.CarparkEntity
import com.sc2006.spaze.data.local.entity.CarparkEntity.PriceTier
import com.sc2006.spaze.data.repository.CarparkRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Home ViewModel
 * Corresponds to HomepageController in class diagram
 * Implements: Map and Positioning, Carpark Discovery
 */
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val carparkRepository: CarparkRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private val _carparks = MutableStateFlow<List<CarparkUiModel>>(emptyList())
    val carparks: StateFlow<List<CarparkUiModel>> = _carparks.asStateFlow()

    private val _cameraEvents = MutableSharedFlow<LatLng>(extraBufferCapacity = 1)
    val cameraEvents = _cameraEvents.asSharedFlow()

    init {
        observeCarparks()
    }

    /**
     * Observe carparks from database
     */
    private fun observeCarparks() {
        viewModelScope.launch {
            carparkRepository.getAllCarparks().collect { carparks ->
                _carparks.value = carparks.toUiModels()
            }
        }
    }

    /**
     * Load carparks from API
     */
    fun fetchCarparkAvailability() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            val result = carparkRepository.refreshCarparkAvailability()
            result.fold(
                onSuccess = { count ->
                    android.util.Log.d("CarparkAPI", "Fetched $count carparks")
                    _uiState.update { it.copy(isLoading = false) }
                },
                onFailure = { error ->
                    carparkRepository.seedSampleCarparks()
                    _uiState.update {
                        it.copy(isLoading = false, error = error.message)
                    }
                    android.util.Log.e("CarparkAPI", "Error fetching carparks", error)
                }
            )
        }
    }

    /**
     * Search carparks
     */
    fun searchCarparks(query: String) {
        viewModelScope.launch {
            carparkRepository.searchCarparks(query).collect { results ->
                _carparks.value = results.toUiModels()
            }
        }
    }

    /**
     * Get carparks within bounds
     */
    fun getCarparksInBounds(
        minLat: Double,
        maxLat: Double,
        minLng: Double,
        maxLng: Double
    ) {
        viewModelScope.launch {
            carparkRepository.getCarparksInBounds(minLat, maxLat, minLng, maxLng)
                .collect { carparks ->
                    _carparks.value = carparks.toUiModels()
                }
        }
    }

    /**
     * Filter by availability
     */
    fun filterByAvailability(minLots: Int) {
        viewModelScope.launch {
            carparkRepository.getAvailableCarparks(minLots).collect { carparks ->
                _carparks.value = carparks.toUiModels()
            }
        }
    }

    /**
     * Toggle map view type
     */
    fun toggleMapView() {
        _uiState.update {
            it.copy(mapViewType = if (it.mapViewType == MapViewType.STANDARD) MapViewType.SATELLITE else MapViewType.STANDARD)
        }
    }

    /**
     * Update user location
     */
    fun updateUserLocation(lat: Double, lng: Double) {
        _uiState.update { it.copy(userLatitude = lat, userLongitude = lng) }
    }

    fun focusOnPlace(latLng: LatLng?) {
        viewModelScope.launch {
            if (latLng != null) {
                _cameraEvents.emit(latLng)
                updateUserLocation(latLng.latitude, latLng.longitude)
            }
        }
    }
}

data class HomeUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val userLatitude: Double = 1.3521, // Singapore default
    val userLongitude: Double = 103.8198,
    val mapViewType: MapViewType = MapViewType.STANDARD
)

enum class MapViewType {
    STANDARD,
    SATELLITE
}

data class CarparkUiModel(
    val id: String,
    val name: String,
    val address: String,
    val latitude: Double,
    val longitude: Double,
    val availableLots: Int,
    val totalLots: Int,
    val availabilityRatio: Float,
    val availabilityStatus: OccupancyStatus,
    val availabilityLabel: String,
    val statusLabel: String,
    val priceTier: PriceTier,
    val priceTierLabel: String,
    val hourlyRateLabel: String?,
    val lastUpdated: Long
)

enum class OccupancyStatus {
    HIGH,
    MODERATE,
    LOW,
    EMPTY
}

private fun List<CarparkEntity>.toUiModels(): List<CarparkUiModel> = map { it.toUiModel() }

private fun CarparkEntity.toUiModel(): CarparkUiModel {
    val total = if (totalLots > 0) totalLots else maxOf(availableLots, 1)
    val availabilityRatio = if (total > 0) availableLots.toFloat() / total.toFloat() else 0f
    val status = when {
        availableLots <= 0 -> OccupancyStatus.EMPTY
        availabilityRatio <= 0.25f -> OccupancyStatus.LOW
        availabilityRatio <= 0.55f -> OccupancyStatus.MODERATE
        else -> OccupancyStatus.HIGH
    }
    val tier = runCatching { PriceTier.valueOf(priceTier) }.getOrElse { PriceTier.UNKNOWN }
    val tierLabel = when (tier) {
        PriceTier.BUDGET -> "Budget"
        PriceTier.STANDARD -> "Standard"
        PriceTier.PREMIUM -> "Premium"
        PriceTier.UNKNOWN -> "Unknown"
    }
    val hourlyLabel = baseHourlyRate?.let { "S$${"%.2f".format(it)}/hr" }
    val statusLabel = when (status) {
        OccupancyStatus.HIGH -> "Plenty of lots"
        OccupancyStatus.MODERATE -> "Limited"
        OccupancyStatus.LOW -> "Almost full"
        OccupancyStatus.EMPTY -> "Full"
    }

    return CarparkUiModel(
        id = carparkID,
        name = if (location.isNotBlank()) location else carparkID,
        address = address,
        latitude = latitude,
        longitude = longitude,
        availableLots = availableLots,
        totalLots = total,
        availabilityRatio = availabilityRatio,
        availabilityStatus = status,
        availabilityLabel = "$availableLots / $total",
        statusLabel = statusLabel,
        priceTier = tier,
        priceTierLabel = tierLabel,
        hourlyRateLabel = hourlyLabel,
        lastUpdated = lastUpdated
    )
}
