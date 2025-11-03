
package com.sc2006.spaze.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sc2006.spaze.data.local.entity.CarparkEntity
import com.sc2006.spaze.data.local.entity.RecentSearchEntity
import com.sc2006.spaze.data.repository.CarparkRepository
import com.sc2006.spaze.data.local.PreferencesManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val carparkRepository: CarparkRepository,
    private val preferences: PreferencesManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    private val _searchResults = MutableStateFlow<List<CarparkEntity>>(emptyList())
    val searchResults: StateFlow<List<CarparkEntity>> = _searchResults.asStateFlow()

    private val _recentSearches = MutableStateFlow<List<RecentSearchEntity>>(emptyList())
    val recentSearches: StateFlow<List<RecentSearchEntity>> = _recentSearches.asStateFlow()

    private val _allCarparks = MutableStateFlow<List<CarparkEntity>>(emptyList())

    init {
        viewModelScope.launch {
            // One-time bootstrap
            if (preferences.isFirstLaunch()) {
                carparkRepository.initializeCarparksFromCsv()
                preferences.setFirstLaunchComplete()
            }
            // Try refreshing availability in background (ok to fail silently)
            runCatching { carparkRepository.refreshCarparkAvailability() }

            // Keep local cache for filtering/sorting
            carparkRepository.getAllCarparks().collect { carparks ->
                _allCarparks.value = carparks
            }
        }
    }

    fun loadRecentSearches(userId: String) {
        viewModelScope.launch {
            carparkRepository.getRecentSearches(userId, limit = 10).collect { searches ->
                _recentSearches.value = searches
            }
        }
    }

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

    private fun applyFiltersAndSort(carparks: List<CarparkEntity>): List<CarparkEntity> {
        var filtered = carparks
        val s = _uiState.value

        if (s.filterByAvailability) {
            filtered = filtered.filter { it.getTotalAvailableLots() >= s.minAvailableLots }
        }

        if (s.selectedLotType != LotType.ALL) {
            filtered = when (s.selectedLotType) {
                LotType.CAR -> filtered.filter { it.totalLotsC > 0 }
                LotType.MOTORCYCLE -> filtered.filter { it.totalLotsY > 0 } // Y = motorcycle
                LotType.HEAVY_VEHICLE -> filtered.filter { it.totalLotsH > 0 } // H = heavy
                LotType.SEASON -> filtered.filter { it.totalLotsS > 0 }
                else -> filtered
            }
        }

        if (s.maxPrice != null) {
            // Placeholder for future pricing
        }

        filtered = when (s.sortBy) {
            SortOption.DISTANCE ->
                if (s.userLatitude != null && s.userLongitude != null) {
                    filtered.sortedBy {
                        calculateDistance(s.userLatitude, s.userLongitude, it.latitude, it.longitude)
                    }
                } else filtered
            SortOption.AVAILABILITY -> filtered.sortedByDescending { it.getTotalAvailableLots() }
            SortOption.NAME -> filtered.sortedBy { it.address }
            SortOption.NONE -> filtered
        }

        return filtered
    }

    private fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val R = 6371.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2) * sin(dLat / 2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                sin(dLon / 2) * sin(dLon / 2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return R * c
    }

    fun setMinAvailableLots(minLots: Int) {
        _uiState.update { it.copy(filterByAvailability = minLots > 0, minAvailableLots = minLots) }
        refreshResults()
    }

    fun setLotTypeFilter(lotType: LotType) {
        _uiState.update { it.copy(selectedLotType = lotType) }
        refreshResults()
    }

    fun setMaxPrice(price: Double?) {
        _uiState.update { it.copy(maxPrice = price) }
        refreshResults()
    }

    fun setSortOption(sortBy: SortOption) {
        _uiState.update { it.copy(sortBy = sortBy) }
        refreshResults()
    }

    fun updateUserLocation(latitude: Double, longitude: Double) {
        _uiState.update { it.copy(userLatitude = latitude, userLongitude = longitude) }
        if (_uiState.value.sortBy == SortOption.DISTANCE) refreshResults()
    }

    fun clearFilters() {
        _uiState.update {
            SearchUiState(searchQuery = it.searchQuery, userLatitude = it.userLatitude, userLongitude = it.userLongitude)
        }
        refreshResults()
    }

    fun clearSearch() {
        _searchResults.value = emptyList()
        _uiState.update { SearchUiState() }
    }

    private fun refreshResults() {
        if (_searchResults.value.isNotEmpty() || _uiState.value.searchQuery.isNotBlank()) {
            _searchResults.value = applyFiltersAndSort(_searchResults.value)
        }
    }

    fun clearRecentSearches(userId: String) {
        viewModelScope.launch { carparkRepository.clearRecentSearches(userId) }
    }

    fun searchFromRecent(userId: String, recentSearch: RecentSearchEntity) {
        when (recentSearch.searchType) {
            RecentSearchEntity.SearchType.ADDRESS,
            RecentSearchEntity.SearchType.POSTAL_CODE,
            RecentSearchEntity.SearchType.PLACE_NAME -> searchCarparks(userId, recentSearch.searchQuery)
            RecentSearchEntity.SearchType.CARPARK_VIEW -> { /* navigate to details externally */ }
        }
    }
}

data class SearchUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val searchQuery: String = "",
    val filterByAvailability: Boolean = false,
    val minAvailableLots: Int = 0,
    val selectedLotType: LotType = LotType.ALL,
    val maxPrice: Double? = null,
    val sortBy: SortOption = SortOption.NONE,
    val userLatitude: Double? = null,
    val userLongitude: Double? = null
)

enum class LotType { ALL, CAR, MOTORCYCLE, HEAVY_VEHICLE, SEASON }
enum class SortOption { NONE, DISTANCE, AVAILABILITY, NAME }
