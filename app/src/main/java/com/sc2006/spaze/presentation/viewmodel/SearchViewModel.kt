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
import kotlin.math.roundToInt

/**
 * Search ViewModel - Person 3 (ViewModel Layer)
 * Week 2 Tasks:
 *  - Task 3.1: Implement filterResults() ✅
 *  - Task 3.2: Implement sortResults() ✅
 *  - Task 3.3: Add error handling to searchCarparks() ✅
 */
@HiltViewModel
class SearchViewModel @Inject constructor(
    private val carparkRepository: CarparkRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    private val _searchResults = MutableStateFlow<List<CarparkEntity>>(emptyList())
    val searchResults: StateFlow<List<CarparkEntity>> = _searchResults.asStateFlow()

    // Store unfiltered results for re-filtering
    private var unfilteredResults: List<CarparkEntity> = emptyList()

    private val _recentSearches = MutableStateFlow<List<RecentSearchEntity>>(emptyList())
    val recentSearches: StateFlow<List<RecentSearchEntity>> = _recentSearches.asStateFlow()

    /**
     * Task 3.3: Search carparks with error handling
     * Wrapped in try-catch to handle repository errors gracefully
     */
    fun searchCarparks(query: String, userId: String) {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isLoading = true, error = null, searchQuery = query) }

                // Add to search history
                carparkRepository.addSearchToHistory(
                    userId,
                    query,
                    RecentSearchEntity.SearchType.PLACE_NAME
                )

                // Perform search
                carparkRepository.searchCarparks(query).collect { results ->
                    unfilteredResults = results

                    // Apply current filters and sorting
                    val filtered = applyFilters(results, _uiState.value.filters)
                    val sorted = applySorting(filtered, _uiState.value.filters.sortBy)

                    _searchResults.value = sorted
                    _uiState.update { it.copy(isLoading = false, error = null) }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = "Search failed: ${e.message}"
                    )
                }
                _searchResults.value = emptyList()
            }
        }
    }

    /**
     * Task 3.1: Filter search results by availability and distance
     *
     * Filtering logic:
     * - minLots: Minimum available parking lots required
     * - maxDistance: Maximum distance from user in meters (null = no distance filter)
     *
     * Edge cases handled:
     * - Null distanceFromUser values (when location not calculated)
     * - Empty results
     * - Invalid filter values
     */
    fun filterResults(minLots: Int, maxDistance: Float?) {
        viewModelScope.launch {
            try {
                // Validate inputs
                val validMinLots = minLots.coerceAtLeast(0)
                val validMaxDistance = maxDistance?.coerceAtLeast(0f)

                // Update filter state
                _uiState.update {
                    it.copy(
                        filters = it.filters.copy(
                            minLots = validMinLots,
                            maxDistance = validMaxDistance
                        )
                    )
                }

                // Apply filters to current results
                val filtered = applyFilters(unfilteredResults, _uiState.value.filters)
                val sorted = applySorting(filtered, _uiState.value.filters.sortBy)

                _searchResults.value = sorted
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(error = "Filter failed: ${e.message}")
                }
            }
        }
    }

    /**
     * Task 3.2: Sort search results by specified criteria
     *
     * Sorting options:
     * - DISTANCE: Sort by proximity to user (closest first)
     * - AVAILABILITY: Sort by number of available lots (most available first)
     * - PRICE: Sort by hourly rate (cheapest first)
     *
     * Edge cases handled:
     * - Null distanceFromUser for DISTANCE sorting
     * - Carparks without distance data appear last
     * - Stable sorting to maintain relative order
     */
    fun sortResults(sortBy: SortOption) {
        viewModelScope.launch {
            try {
                // Update sort option in state
                _uiState.update {
                    it.copy(filters = it.filters.copy(sortBy = sortBy))
                }

                // Apply sorting to current results
                val sorted = applySorting(_searchResults.value, sortBy)
                _searchResults.value = sorted
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(error = "Sort failed: ${e.message}")
                }
            }
        }
    }

    /**
     * Apply filters to carpark list
     * Private helper method for filtering logic
     */
    private fun applyFilters(
        carparks: List<CarparkEntity>,
        filters: SearchFilters
    ): List<CarparkEntity> {
        return carparks.filter { carpark ->
            // Filter by minimum available lots
            val hasEnoughLots = carpark.availableLots >= filters.minLots

            // Filter by maximum distance (if specified)
            val withinDistance = filters.maxDistance?.let { maxDist ->
                carpark.distanceFromUser?.let { distance ->
                    distance <= maxDist
                } ?: false // Exclude carparks without distance data when filter is active
            } ?: true // Include all if no distance filter

            hasEnoughLots && withinDistance
        }
    }

    /**
     * Apply sorting to carpark list
     * Private helper method for sorting logic
     */
    private fun applySorting(
        carparks: List<CarparkEntity>,
        sortBy: SortOption
    ): List<CarparkEntity> {
        return when (sortBy) {
            SortOption.DISTANCE -> {
                // Sort by distance (closest first)
                // Carparks without distance data appear last
                carparks.sortedWith(compareBy(
                    nullsLast<Float>() // Null distances go to end
                ) { it.distanceFromUser })
            }

            SortOption.AVAILABILITY -> {
                // Sort by available lots (most available first)
                carparks.sortedByDescending { it.availableLots }
            }

            SortOption.PRICE -> {
                // TODO - Person 4: Add parsed hourly rate field to CarparkEntity
                // Currently pricingInfo is a JSON string that needs parsing
                //
                // Suggested implementation:
                // 1. Add field to CarparkEntity: val hourlyRate: Float? = null
                // 2. Parse pricingInfo JSON when fetching from API
                // 3. Update this sorting logic to:
                //    carparks.sortedBy { it.hourlyRate ?: Float.MAX_VALUE }
                //
                // For now, sort by availability as fallback
                carparks.sortedByDescending { it.availableLots }
            }
        }
    }

    /**
     * Load user's recent searches from database
     * Includes error handling for database access failures
     */
    fun loadRecentSearches(userId: String) {
        viewModelScope.launch {
            try {
                carparkRepository.getRecentSearches(userId).collect { searches ->
                    _recentSearches.value = searches
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(error = "Failed to load search history: ${e.message}")
                }
            }
        }
    }

    /**
     * Clear user's search history
     * Includes error handling for database operations
     */
    fun clearSearchHistory(userId: String) {
        viewModelScope.launch {
            try {
                carparkRepository.clearRecentSearches(userId)
                _recentSearches.value = emptyList()
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(error = "Failed to clear search history: ${e.message}")
                }
            }
        }
    }

    /**
     * Clear error message from UI state
     */
    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    /**
     * Reset all filters to default values
     */
    fun resetFilters() {
        viewModelScope.launch {
            _uiState.update { it.copy(filters = SearchFilters()) }

            // Re-apply with default filters
            val filtered = applyFilters(unfilteredResults, SearchFilters())
            val sorted = applySorting(filtered, SearchFilters().sortBy)
            _searchResults.value = sorted
        }
    }
}

/**
 * UI State for Search Screen
 * Includes loading state, error messages, search query, and active filters
 */
data class SearchUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val searchQuery: String = "",
    val filters: SearchFilters = SearchFilters()
)

/**
 * Search Filters Configuration
 * Holds all active filter parameters
 */
data class SearchFilters(
    val minLots: Int = 0,
    val maxDistance: Float? = null, // in meters, null = no distance filter
    val sortBy: SortOption = SortOption.DISTANCE
)

/**
 * Sort Options for Search Results
 * - DISTANCE: Sort by proximity to user (requires location)
 * - AVAILABILITY: Sort by number of available lots
 * - PRICE: Sort by hourly rate (requires Person 4 to add parsed price field)
 */
enum class SortOption {
    DISTANCE,
    AVAILABILITY,
    PRICE
}