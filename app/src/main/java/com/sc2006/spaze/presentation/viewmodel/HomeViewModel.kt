package com.sc2006.spaze.presentation.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sc2006.spaze.data.local.entity.CarparkEntity
import com.sc2006.spaze.data.location.LocationService
import com.sc2006.spaze.data.preferences.PreferencesDataStore
import com.sc2006.spaze.data.repository.CarparkRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Home ViewModel
 * Corresponds to HomepageController in class diagram
 * Implements: Map and Positioning, Carpark Discovery
 */
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val carparkRepository: CarparkRepository,
    @ApplicationContext private val context: Context,
    private val locationService: LocationService
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private val _carparks = MutableStateFlow<List<CarparkEntity>>(emptyList())
    val carparks: StateFlow<List<CarparkEntity>> = _carparks.asStateFlow()

    init {
        loadCarparks()
        observeCarparks()
        getLastKnownLocation()
    }

    /**
     * Get last known location on startup
     */
    private fun getLastKnownLocation() {
        viewModelScope.launch {
            locationService.getLastLocation()?.let { location ->
                updateUserLocation(location.latitude, location.longitude)
            }
        }
    }

    /**
     * Start tracking user location
     */
    fun startLocationTracking() {
        viewModelScope.launch {
            locationService.getLocationUpdates(intervalMillis = 10000)
                .catch { e ->
                    _uiState.update { it.copy(error = "Location error: ${e.message}") }
                }
                .collect { location ->
                    updateUserLocation(location.latitude, location.longitude)
                    _uiState.update { it.copy(isLocationTrackingEnabled = true) }
                }
        }
    }

    /**
     * Stop location tracking
     */
    fun stopLocationTracking() {
        _uiState.update { it.copy(isLocationTrackingEnabled = false) }
    }

    /**
     * Observe carparks from database and filter by search radius
     */
    private fun observeCarparks() {
        viewModelScope.launch {
            combine(
                carparkRepository.getAllCarparks(),
                PreferencesDataStore.getSearchRadius(context),
                _uiState
            ) { carparks, searchRadiusKm, state ->
                filterCarparksByDistance(carparks, state.userLatitude, state.userLongitude, searchRadiusKm)
            }.collect { filtered ->
                _carparks.value = filtered
            }
        }
    }
    
    /**
     * Filter carparks by distance from user location
     */
    private fun filterCarparksByDistance(
        carparks: List<CarparkEntity>,
        userLat: Double,
        userLng: Double,
        maxDistanceKm: Float
    ): List<CarparkEntity> {
        return carparks.filter { carpark ->
            val distance = calculateDistance(
                userLat,
                userLng,
                carpark.latitude,
                carpark.longitude
            )
            distance <= maxDistanceKm
        }.sortedBy { carpark ->
            calculateDistance(
                userLat,
                userLng,
                carpark.latitude,
                carpark.longitude
            )
        }
    }
    
    /**
     * Calculate distance between two lat/lng points using Haversine formula
     * Returns distance in kilometers
     */
    private fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Float {
        val R = 6371.0 // Earth's radius in kilometers
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2) * sin(dLat / 2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                sin(dLon / 2) * sin(dLon / 2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return (R * c).toFloat()
    }

    /**
     * Load carparks from API
     */
    fun loadCarparks() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            val result = carparkRepository.refreshCarparkAvailability()
            result.fold(
                onSuccess = {
                    _uiState.update { it.copy(isLoading = false) }
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
     * Search carparks
     */
    fun searchCarparks(query: String) {
        viewModelScope.launch {
            carparkRepository.searchCarparks(query).collect { results ->
                _carparks.value = results
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
                    _carparks.value = carparks
                }
        }
    }

    /**
     * Filter by availability
     */
    fun filterByAvailability(minLots: Int) {
        viewModelScope.launch {
            carparkRepository.getAvailableCarparks(minLots).collect { carparks ->
                _carparks.value = carparks
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
}

data class HomeUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val userLatitude: Double = 1.3521, // Singapore default (will be overridden by real location)
    val userLongitude: Double = 103.8198,
    val mapViewType: MapViewType = MapViewType.STANDARD,
    val isLocationTrackingEnabled: Boolean = false
)

enum class MapViewType {
    STANDARD,
    SATELLITE
}
