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

    init {
        loadCarparks()
        observeCarparks()
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
