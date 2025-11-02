package com.sc2006.spaze.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sc2006.spaze.data.local.entity.CarparkEntity
import com.sc2006.spaze.data.local.entity.RecentSearchEntity
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
 * Search ViewModel
 * Implements: Search, Filter, and Sort functionality
 * Corresponds to SearchController in class diagram
 */
@HiltViewModel
class SearchViewModel @Inject constructor(
    private val carparkRepository: CarparkRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    private val _searchResults = MutableStateFlow<List<CarparkEntity>>(emptyList())
    val searchResults: StateFlow<List<CarparkEntity>> = _searchResults.asStateFlow()

    private val _recentSearches = MutableStateFlow<List<RecentSearchEntity>>(emptyList())
    val recentSearches: StateFlow<List<RecentSearchEntity>> = _recentSearches.asStateFlow()

    private val _allCarparks = MutableStateFlow<List<CarparkEntity>>(emptyList())

    init {
        loadAllCarparks()
    }

    /**
     * Load all carparks for filtering
     */
    private fun loadAllCarparks() {
        viewModelScope.launch {
            carparkRepository.getAllCarparks().collect { carparks ->
                _allCarparks.value = carparks
            }
        }
    }

    /**
     * Load recent searches for user
     */
    fun loadRecentSearches(userId: String) {
        viewModelScope.launch {
            carparkRepository.getRecentSearches(userId, limit = 10).collect { searches ->
                _recentSearches.value = searches
            }
        }
    }

    /**
     * Search carparks by query
     */
    fun searchCarparks(userId: String, query: String) {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isLoading = true, error = null, searchQuery = query) }

                // Save search to history
                if (query.isNotBlank()) {
                    carparkRepository.addSearchToHistory(
                        userId = userId,
                        query = query,
                        searchType = RecentSearchEntity.SearchType.PLACE_NAME
                    )
                }

                carparkRepository.searchCarparks(query).collect { results ->
                    _searchResults.value = applyFiltersAndSort(results)
                    _uiState.update { it.copy(isLoading = false) }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isLoading = false, error = e.message ?: "Search failed")
                }
            }
        }
    }

    /**
     * Apply filters to search results
     */
    private fun applyFiltersAndSort(carparks: List<CarparkEntity>): List<CarparkEntity> {
        var filtered = carparks

        val currentState = _uiState.value

        // Filter by availability
        if (currentState.filterByAvailability) {
            filtered = filtered.filter {
                it.getTotalAvailableLots() >= currentState.minAvailableLots
            }
        }

        // Filter by lot type
        if (currentState.selectedLotType != LotType.ALL) {
            filtered = when (currentState.selectedLotType) {
                LotType.CAR -> filtered.filter { it.totalLotsC > 0 }
                LotType.MOTORCYCLE -> filtered.filter { it.totalLotsH > 0 }
                LotType.HEAVY_VEHICLE -> filtered.filter { it.totalLotsY > 0 }
                LotType.SEASON -> filtered.filter { it.totalLotsS > 0 }
                else -> filtered
            }
        }

        // Filter by price (if we had pricing data)
        // This is a placeholder for future implementation
        if (currentState.maxPrice != null) {
            // filtered = filtered.filter { it.price <= currentState.maxPrice }
        }

        // Apply sorting
        filtered = when (currentState.sortBy) {
            SortOption.DISTANCE -> {
                if (currentState.userLatitude != null && currentState.userLongitude != null) {
                    filtered.sortedBy {
                        calculateDistance(
                            currentState.userLatitude,
                            currentState.userLongitude,
                            it.latitude,
                            it.longitude
                        )
                    }
                } else {
                    filtered
                }
            }
            SortOption.AVAILABILITY -> filtered.sortedByDescending { it.getTotalAvailableLots() }
            SortOption.NAME -> filtered.sortedBy { it.address }
            SortOption.NONE -> filtered
        }

        return filtered
    }

    /**
     * Calculate distance between two points using Haversine formula
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
     * Update filter: minimum available lots
     */
    fun setMinAvailableLots(minLots: Int) {
        _uiState.update {
            it.copy(
                filterByAvailability = minLots > 0,
                minAvailableLots = minLots
            )
        }
        refreshResults()
    }

    /**
     * Update filter: lot type
     */
    fun setLotTypeFilter(lotType: LotType) {
        _uiState.update { it.copy(selectedLotType = lotType) }
        refreshResults()
    }

    /**
     * Update filter: max price
     */
    fun setMaxPrice(price: Double?) {
        _uiState.update { it.copy(maxPrice = price) }
        refreshResults()
    }

    /**
     * Update sort option
     */
    fun setSortOption(sortBy: SortOption) {
        _uiState.update { it.copy(sortBy = sortBy) }
        refreshResults()
    }

    /**
     * Update user location for distance calculations
     */
    fun updateUserLocation(latitude: Double, longitude: Double) {
        _uiState.update {
            it.copy(userLatitude = latitude, userLongitude = longitude)
        }
        if (_uiState.value.sortBy == SortOption.DISTANCE) {
            refreshResults()
        }
    }

    /**
     * Clear all filters
     */
    fun clearFilters() {
        _uiState.update {
            SearchUiState(
                searchQuery = it.searchQuery,
                userLatitude = it.userLatitude,
                userLongitude = it.userLongitude
            )
        }
        refreshResults()
    }

    /**
     * Clear search
     */
    fun clearSearch() {
        _searchResults.value = emptyList()
        _uiState.update { SearchUiState() }
    }

    /**
     * Refresh results with current filters
     */
    private fun refreshResults() {
        if (_searchResults.value.isNotEmpty() || _uiState.value.searchQuery.isNotBlank()) {
            _searchResults.value = applyFiltersAndSort(_searchResults.value)
        }
    }

    /**
     * Clear recent searches
     */
    fun clearRecentSearches(userId: String) {
        viewModelScope.launch {
            carparkRepository.clearRecentSearches(userId)
        }
    }

    /**
     * Perform search from recent search item
     */
    fun searchFromRecent(userId: String, recentSearch: RecentSearchEntity) {
        when (recentSearch.searchType) {
            RecentSearchEntity.SearchType.ADDRESS,
            RecentSearchEntity.SearchType.POSTAL_CODE,
            RecentSearchEntity.SearchType.PLACE_NAME -> {
                searchCarparks(userId, recentSearch.searchQuery)
            }
            RecentSearchEntity.SearchType.CARPARK_VIEW -> {
                // This is handled differently - navigate to carpark details
            }
        }
    }
}

data class SearchUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val searchQuery: String = "",

    // Filters
    val filterByAvailability: Boolean = false,
    val minAvailableLots: Int = 0,
    val selectedLotType: LotType = LotType.ALL,
    val maxPrice: Double? = null,

    // Sort
    val sortBy: SortOption = SortOption.NONE,

    // User location for distance calculation
    val userLatitude: Double? = null,
    val userLongitude: Double? = null
)

enum class LotType {
    ALL,
    CAR,           // C
    MOTORCYCLE,    // H
    HEAVY_VEHICLE, // Y
    SEASON         // S
}

enum class SortOption {
    NONE,
    DISTANCE,
    AVAILABILITY,
    NAME
}