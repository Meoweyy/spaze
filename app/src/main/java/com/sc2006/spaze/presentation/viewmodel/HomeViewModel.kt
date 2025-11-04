package com.sc2006.spaze.presentation.viewmodel

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.maps.model.LatLng
import com.sc2006.spaze.data.local.PreferencesManager
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.CancellationException
// no Mutex.withLock; we serialize via a Job to avoid NPEs on older runtimes
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
    private val locationService: LocationService,
    private val preferences: PreferencesManager
) : ViewModel() {

    companion object {
        private const val TAG = "HomeViewModel"
    }

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private val _carparks = MutableStateFlow<List<CarparkEntity>>(emptyList())
    val carparks: StateFlow<List<CarparkEntity>> = _carparks.asStateFlow()

    init {
        Log.d(TAG, "HomeViewModel initialized")
        initializeDatabase()
        loadCarparks()
        observeCarparks()
        getLastKnownLocation()
        observeLocationPreferences()
        ensureDefaultReference()
    }

    /**
     * Initialize database from CSV on first launch
     */
    private fun initializeDatabase() {
        viewModelScope.launch {
            try {
                Log.d(TAG, "=== Database Initialization ===")

                // Always check carpark count first
                val currentCount = carparkRepository.getCarparkCount()
                Log.d(TAG, "Current carpark count in database: $currentCount")

                if (currentCount == 0 || preferences.isFirstLaunch()) {
                    Log.d(TAG, "Database needs initialization - loading CSV data")
                    _uiState.update { it.copy(isLoading = true, error = "Loading carpark database...") }

                    val result = carparkRepository.initializeCarparksFromCsv()
                    result.fold(
                        onSuccess = { count ->
                            Log.d(TAG, "✅ Database initialized successfully with $count carparks")
                            preferences.setFirstLaunchComplete()
                            _uiState.update { it.copy(isLoading = false, error = null) }

                            // Verify carparks are actually in database
                            viewModelScope.launch {
                                val verifyCount = carparkRepository.getCarparkCount()
                                Log.d(TAG, "Verification: Database now contains $verifyCount carparks")
                            }
                        },
                        onFailure = { error -> if (error is CancellationException) return@fold
                            Log.e(TAG, "❌ Failed to initialize database", error)
                            _uiState.update { it.copy(
                                isLoading = false,
                                error = "Failed to load carpark data: ${error.message}\nPlease restart the app."
                            )}
                        }
                    )
                } else {
                    Log.d(TAG, "✅ Database already initialized with $currentCount carparks")
                }

                // Try refreshing availability in background
                Log.d(TAG, "Refreshing carpark availability from API...")
                runCatching {
                    carparkRepository.refreshCarparkAvailability()
                }.onSuccess {
                    Log.d(TAG, "✅ Availability refresh successful")
                }.onFailure { error ->
                    Log.w(TAG, "⚠️ Availability refresh failed (non-critical): ${error.message}")
                }
            } catch (e: Exception) {
                if (e is CancellationException) throw e  // Don't catch cancellations
                Log.e(TAG, "❌ Error in database initialization", e)
                _uiState.update { it.copy(error = "Database error: ${e.message}") }
            }
        }
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
                        if (e is CancellationException) return@catch
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
                if (e is CancellationException) throw e  // Don't catch cancellations
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
     * Uses the same reliable approach as ComposedCarparkFinder
     *
     * This implementation avoids nested flows and combines to prevent cancellations.
     * Instead, we maintain local state and manually trigger filtering.
     */
    @Volatile
    private var allCarparks: List<CarparkEntity>? = null
    @Volatile
    private var currentSearchRadius: Float = 1f  // Default 1km, matches PreferencesDataStore

    // Serialize filtering with a single running job to avoid overlap
    private var filterJob: Job? = null

    // Route request single-flight + cooldown controls
    private var routeJob: Job? = null
    private var lastRouteDestination: LatLng? = null
    private var lastRouteMode: String? = null
    private var routeCooldownUntilMillis: Long = 0L

    private fun observeCarparks() {
        // Observer 1: Watch all carparks from database
        viewModelScope.launch {
            try {
                carparkRepository.getAllCarparks()
                    .debounce(250)
                    .conflate()
                    .collectLatest { carparks ->
                    Log.d(TAG, "=== Carparks Updated from Database ===")
                    Log.d(TAG, "Total carparks: ${carparks?.size ?: 0}")

                    synchronized(this@HomeViewModel) {
                        allCarparks = carparks ?: emptyList()
                    }

                    filterAndUpdateCarparks()
                }
            } catch (e: Exception) { if (e is CancellationException) return@launch
                Log.e(TAG, "Error observing carparks from database", e)
                _uiState.update { it.copy(error = "Database error: ${e.message}") }
            }
        }

        // Observer 2: Watch search radius changes
        viewModelScope.launch {
            try {
                PreferencesDataStore.getSearchRadius(context).collect { radius ->
                    Log.d(TAG, "=== Search Radius Updated ===")
                    Log.d(TAG, "New radius: $radius km")

                    synchronized(this@HomeViewModel) {
                        currentSearchRadius = radius
                    }

                    filterAndUpdateCarparks()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error observing search radius", e)
            }
        }

        // Observer 3: Watch user location changes (only when coordinates change)
        viewModelScope.launch {
            try {
                _uiState
                    .map { it.userLatitude to it.userLongitude }
                    .distinctUntilChanged()
                    .collect { (lat, lng) ->
                        Log.d(TAG, "=== User Location Updated ===")
                        Log.d(TAG, "New location: ($lat, $lng)")
                        filterAndUpdateCarparks()
                    }
            } catch (e: Exception) {
                Log.e(TAG, "Error observing user location", e)
            }
        }
    }

    /**
     * Filter carparks based on current state and update the published list
     * Thread-safe with mutex to prevent concurrent filtering
     */
    private fun filterAndUpdateCarparks() {
        // Cancel any in-flight filter to process only the latest request
        filterJob?.cancel()
        filterJob = viewModelScope.launch {
            try {
                val state = _uiState.value

                // Create local copies for thread safety
                val carparksToFilter: List<CarparkEntity>
                val radiusKm: Float

                synchronized(this@HomeViewModel) {
                    // Handle null case - return empty list if carparks haven't loaded yet
                    carparksToFilter = allCarparks?.toList() ?: emptyList()
                    radiusKm = currentSearchRadius
                }

                Log.d(TAG, "=== Filtering Carparks ===")
                Log.d(TAG, "Total carparks: ${carparksToFilter.size}")
                Log.d(TAG, "Center: (${state.userLatitude}, ${state.userLongitude})")
                Log.d(TAG, "Search radius: $radiusKm km")

                if (carparksToFilter.isEmpty()) {
                    Log.w(TAG, "No carparks loaded from database yet")
                    _carparks.value = emptyList()
                    return@launch
                }

                    // Filter by distance and sort by proximity (off main thread)
                    val filtered = withContext(Dispatchers.Default) {
                        filterCarparksByDistance(
                            carparksToFilter,
                            state.userLatitude,
                            state.userLongitude,
                            radiusKm
                        )
                    }

                    Log.d(TAG, "Carparks within radius: ${filtered.size}")
                    if (filtered.isNotEmpty()) {
                        val nearest = filtered[0]
                        val distance = calculateDistance(
                            state.userLatitude,
                            state.userLongitude,
                            nearest.latitude,
                            nearest.longitude
                        )
                        Log.d(TAG, "Nearest carpark: ${nearest.address} (${String.format("%.2f", distance)} km)")
                    } else {
                        Log.w(TAG, "⚠️ No carparks found within $radiusKm km radius")
                        Log.w(TAG, "Try increasing the search radius or check your location")
                    }

                _carparks.value = filtered
            } catch (e: Exception) { if (e is CancellationException) return@launch
                if (e is CancellationException) return@launch
                Log.e(TAG, "Error in filterAndUpdateCarparks", e)
                _uiState.update { it.copy(error = "Error filtering carparks: ${e.message}") }
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
        // Prefer stored WGS84 lat/lng; fall back to conversion if missing
        val withDistances = buildList {
            for (cp in carparks) {
                val latLng = if (cp.latitude != 0.0 && cp.longitude != 0.0) {
                    cp.latitude to cp.longitude
                } else try {
                    com.sc2006.spaze.data.util.Svy21.convertToLatLon(cp.yCoord, cp.xCoord)
                } catch (_: Exception) {
                    0.0 to 0.0
                }
                val (lat, lon) = latLng
                if (lat == 0.0 && lon == 0.0) continue
                val dist = calculateDistance(userLat, userLng, lat, lon)
                if (dist <= maxDistanceKm) add(cp.copy(latitude = lat, longitude = lon) to dist)
            }
        }

        // Diagnostic: if empty, log nearest few to help tuning
        if (withDistances.isEmpty()) {
            var nearest: Pair<CarparkEntity, Float>? = null
            for (cp in carparks) {
                val latLng = if (cp.latitude != 0.0 && cp.longitude != 0.0) {
                    cp.latitude to cp.longitude
                } else try {
                    com.sc2006.spaze.data.util.Svy21.convertToLatLon(cp.yCoord, cp.xCoord)
                } catch (_: Exception) { 0.0 to 0.0 }
                val (lat, lon) = latLng
                if (lat == 0.0 && lon == 0.0) continue
                val d = calculateDistance(userLat, userLng, lat, lon)
                if (nearest == null || d < nearest!!.second) nearest = cp to d
            }
            nearest?.let { (cp, d) ->
                Log.d(TAG, "Nearest overall: ${cp.carparkNumber} ${String.format("%.2f", d)} km at (${cp.latitude}, ${cp.longitude})")
            }
        }

        return withDistances
            .sortedBy { it.second }
            .map { it.first }
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
                onFailure = { error -> if (error is CancellationException) return@fold
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
    fun loadRoute(destination: LatLng, mode: String = "driving", carpark: CarparkEntity? = null) {
        // Avoid spamming requests: dedupe same destination/mode while in flight
        if (routeJob?.isActive == true && lastRouteDestination == destination && lastRouteMode == mode) {
            return
        }
        // Respect cooldown after hard failures (e.g., REQUEST_DENIED)
        val now = System.currentTimeMillis()
        if (now < routeCooldownUntilMillis) {
            _uiState.update { it.copy(routeError = "Navigation temporarily disabled due to previous API error. Please try again later.") }
            return
        }

        routeJob?.cancel()
        lastRouteDestination = destination
        lastRouteMode = mode
        routeJob = viewModelScope.launch {
            try {
                Log.d(TAG, "=== Loading Navigation Route ===")
                _uiState.update { it.copy(isLoadingRoute = true, routeError = null) }

                val origin = LatLng(_uiState.value.userLatitude, _uiState.value.userLongitude)
                Log.d(TAG, "Origin: ($origin)")
                Log.d(TAG, "Destination: ($destination)")
                Log.d(TAG, "Mode: $mode")

                val result = if (mode == "driving") {
                    // Traffic model requires departure_time; use helper that sets it
                    navigationRepository.getDirectionsWithTraffic(
                        origin = origin,
                        destination = destination,
                        mode = mode
                    )
                } else {
                    navigationRepository.getDirections(
                        origin = origin,
                        destination = destination,
                        mode = mode,
                        alternatives = false,
                        trafficModel = null
                    )
                }

                result.fold(
                    onSuccess = { routes ->
                        Log.d(TAG, "Directions API returned ${routes.size} route(s)")
                        if (routes.isNotEmpty()) {
                            val route = routes[0]
                            Log.d(TAG, "Route summary: ${route.summary}")
                            Log.d(TAG, "Encoded polyline length: ${route.encodedPolyline.length} chars")
                            if (route.legs.isNotEmpty()) {
                                val leg = route.legs[0]
                                Log.d(TAG, "Distance: ${leg.distance.text} (${leg.distance.valueMeters}m)")
                                Log.d(TAG, "Duration: ${leg.duration.text} (${leg.duration.valueSeconds}s)")
                            }

                            _uiState.update {
                                it.copy(
                                    selectedRoute = route,
                                    isNavigating = true,
                                    destinationLatLng = destination,
                                    isLoadingRoute = false,
                                    selectedCarpark = carpark,  // Store carpark for display
                                    routeError = null
                                )
                            }
                            Log.d(TAG, "Route loaded successfully")
                        } else {
                            Log.w(TAG, "No routes found in response")
                            _uiState.update {
                                it.copy(
                                    isLoadingRoute = false,
                                    routeError = "No routes found between these locations"
                                )
                            }
                        }
                    },
                    onFailure = { error -> if (error is CancellationException) return@fold
                        Log.e(TAG, "Failed to load route", error)
                        // If it's a REQUEST_DENIED or missing key, apply a short cooldown to avoid hammering
                        val message = error.message.orEmpty()
                        if (message.contains("REQUEST_DENIED", ignoreCase = true) ||
                            message.contains("Missing Google Maps API key", ignoreCase = true)) {
                            // 5-minute cooldown
                            routeCooldownUntilMillis = System.currentTimeMillis() + 5 * 60 * 1000
                        }
                        _uiState.update {
                            it.copy(
                                isLoadingRoute = false,
                                routeError = when {
                                    message.contains("REQUEST_DENIED", ignoreCase = true) ->
                                        "Directions request denied. Ensure the API key is valid, billing is enabled, and the Directions API is allowed for this key."
                                    message.contains("Missing Google Maps API key", ignoreCase = true) ->
                                        "No Google Maps API key configured. Add GOOGLE_MAPS_API_KEY to local.properties and rebuild."
                                    else -> error.message ?: "Failed to load route. Check if Directions API is enabled."
                                }
                            )
                        }
                    }
                )
            } catch (e: Exception) { if (e is CancellationException) return@launch
                Log.e(TAG, "Exception in loadRoute", e)
                _uiState.update {
                    it.copy(
                        isLoadingRoute = false,
                        routeError = "Error: ${e.message}"
                    )
                }
            }
        }
    }

    /**
     * Load route to a carpark
     */
    fun navigateToCarpark(carpark: CarparkEntity, mode: String = "driving") {
        val destination = LatLng(carpark.latitude, carpark.longitude)
        loadRoute(destination, mode, carpark)
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
                routeError = null,
                selectedCarpark = null  // Clear carpark when clearing route
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
    val routeError: String? = null,
    val selectedCarpark: CarparkEntity? = null  // Store carpark info for route card
)

enum class MapViewType {
    STANDARD,
    SATELLITE
}
