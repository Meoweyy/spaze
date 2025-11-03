package com.sc2006.spaze.presentation.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.maps.model.LatLng
import com.sc2006.spaze.data.local.entity.CarparkEntity
import com.sc2006.spaze.data.location.LocationService
import com.sc2006.spaze.data.model.NavigationRoute
import com.sc2006.spaze.data.preferences.PreferencesDataStore
import com.sc2006.spaze.data.repository.CarparkRepository
import com.sc2006.spaze.data.repository.NavigationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Job
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
    private val navigationRepository: NavigationRepository,
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
        observeLocationPreferences()
        ensureDefaultReference()
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
    private var locationJob: Job? = null

    fun startLocationTracking() {
        // Cancel existing tracking if any
        locationJob?.cancel()
        locationJob = viewModelScope.launch {
            try {
                locationService.getLocationUpdates(intervalMillis = 10000)
                    .catch { e ->
                        _uiState.update { it.copy(error = "Location error: ${e.message}") }
                    }
                    .collect { location ->
                        updateUserLocation(location.latitude, location.longitude)
                        _uiState.update { it.copy(isLocationTrackingEnabled = true) }
                    }
            } catch (se: SecurityException) {
                // Permissions not granted
                _uiState.update { it.copy(error = "Location permission not granted", isLocationTrackingEnabled = false) }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message, isLocationTrackingEnabled = false) }
            }
        }
    }

    /**
     * Stop location tracking
     */
    fun stopLocationTracking() {
        locationJob?.cancel()
        locationJob = null
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
     * Observe live location toggle and stored reference position.
     * If live location is disabled and a reference is set, use it as the current location.
     */
    private fun observeLocationPreferences() {
        viewModelScope.launch {
            combine(
                PreferencesDataStore.getLiveLocationEnabled(context),
                PreferencesDataStore.getReferenceLat(context),
                PreferencesDataStore.getReferenceLng(context)
            ) { live, refLat, refLng -> Triple(live, refLat, refLng) }
                .collect { (liveEnabled, refLat, refLng) ->
                    if (!liveEnabled) {
                        // Stop live updates and switch to reference if present
                        stopLocationTracking()
                        if (refLat != null && refLng != null) {
                            updateUserLocation(refLat, refLng)
                        }
                    }
                }
        }
    }

    /**
     * If no reference is set yet, preload it to the provided NTU address.
     */
    private fun ensureDefaultReference() {
        viewModelScope.launch {
            val currentLat = PreferencesDataStore.getReferenceLat(context).first()
            val currentLng = PreferencesDataStore.getReferenceLng(context).first()
            if (currentLat == null || currentLng == null) {
                PreferencesDataStore.setReferenceLatLng(
                    context = context,
                    latitude = 1.347512,
                    longitude = 103.680908,
                    name = "50 Nanyang Avenue, Blk NS3-03-01, 639798"
                )
            }
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

    /**
     * Load navigation route from current location to destination
     */
    fun loadRoute(destination: LatLng, mode: String = "driving") {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingRoute = true, routeError = null) }

            val origin = LatLng(_uiState.value.userLatitude, _uiState.value.userLongitude)

            val result = navigationRepository.getDirections(
                origin = origin,
                destination = destination,
                mode = mode,
                alternatives = false,
                trafficModel = "best_guess"
            )

            result.fold(
                onSuccess = { routes ->
                    if (routes.isNotEmpty()) {
                        _uiState.update {
                            it.copy(
                                selectedRoute = routes[0],  // Use the first route
                                isNavigating = true,
                                destinationLatLng = destination,
                                isLoadingRoute = false,
                                routeError = null
                            )
                        }
                    } else {
                        _uiState.update {
                            it.copy(
                                isLoadingRoute = false,
                                routeError = "No routes found"
                            )
                        }
                    }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isLoadingRoute = false,
                            routeError = error.message ?: "Failed to load route"
                        )
                    }
                }
            )
        }
    }

    /**
     * Load route to a carpark
     */
    fun navigateToCarpark(carpark: CarparkEntity, mode: String = "driving") {
        val destination = LatLng(carpark.latitude, carpark.longitude)
        loadRoute(destination, mode)
    }

    /**
     * Clear the current navigation route
     */
    fun clearRoute() {
        _uiState.update {
            it.copy(
                selectedRoute = null,
                isNavigating = false,
                destinationLatLng = null,
                routeError = null
            )
        }
    }
}

data class HomeUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val userLatitude: Double = 1.347512, // Preloaded NTU reference - overridden by live location or saved reference
    val userLongitude: Double = 103.680908,
    val mapViewType: MapViewType = MapViewType.STANDARD,
    val isLocationTrackingEnabled: Boolean = false,
    // Navigation state
    val selectedRoute: NavigationRoute? = null,
    val isNavigating: Boolean = false,
    val destinationLatLng: LatLng? = null,
    val isLoadingRoute: Boolean = false,
    val routeError: String? = null
)

enum class MapViewType {
    STANDARD,
    SATELLITE
}
