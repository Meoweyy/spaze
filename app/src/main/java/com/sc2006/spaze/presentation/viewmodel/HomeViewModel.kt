package com.sc2006.spaze.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.maps.model.LatLng
import com.sc2006.spaze.data.local.entity.CarparkEntity
import com.sc2006.spaze.data.repository.CarparkRepository
import com.sc2006.spaze.data.repository.PlaceSuggestion
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
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

    private val _carparks = MutableStateFlow<List<CarparkEntity>>(emptyList())
    val carparks: StateFlow<List<CarparkEntity>> = _carparks.asStateFlow()

    private val searchQuery = MutableStateFlow("")
    private val _suggestions = MutableStateFlow<List<PlaceSuggestion>>(emptyList())
    val suggestions: StateFlow<List<PlaceSuggestion>> = _suggestions.asStateFlow()

    private val _cameraEvents = MutableSharedFlow<LatLng>(extraBufferCapacity = 1)
    val cameraEvents = _cameraEvents.asSharedFlow()

    private var suggestionsJob: Job? = null

    init {
        loadCarparks()
        observeCarparks()
        observeSuggestions()
    }

    /**
     * Observe carparks from database
     */
    private fun observeCarparks() {
        viewModelScope.launch {
            carparkRepository.getAllCarparks().collect { carparks ->
                _carparks.value = carparks
            }
        }
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

    fun onSearchQueryChange(query: String) {
        searchQuery.value = query
    }

    private fun observeSuggestions() {
        suggestionsJob?.cancel()
        suggestionsJob = searchQuery
            .debounce(250)
            .filter { it.length >= 2 }
            .flatMapLatest { query ->
                carparkRepository.searchSuggestions(query)
            }
            .onEach { results ->
                _suggestions.value = results
            }
            .launchIn(viewModelScope)
    }

    fun clearSuggestions() {
        _suggestions.value = emptyList()
    }

    fun focusOnSuggestion(suggestion: PlaceSuggestion) {
        viewModelScope.launch {
            searchQuery.value = suggestion.name
            _cameraEvents.emit(LatLng(suggestion.latitude, suggestion.longitude))
            suggestion.carparkId?.let { carparkId ->
                carparkRepository.markCarparkAsViewed(
                    userId = "user123",
                    carparkId = carparkId,
                    carparkName = suggestion.name
                )
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
    val userLatitude: Double = 1.3521, // Singapore default
    val userLongitude: Double = 103.8198,
    val mapViewType: MapViewType = MapViewType.STANDARD
)

enum class MapViewType {
    STANDARD,
    SATELLITE
}
