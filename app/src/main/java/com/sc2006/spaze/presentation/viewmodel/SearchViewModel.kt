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

    fun searchCarparks(query: String, userId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            carparkRepository.addSearchToHistory(
                userId,
                query,
                RecentSearchEntity.SearchType.PLACE_NAME
            )

            carparkRepository.searchCarparks(query).collect { results ->
                _searchResults.value = results
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    fun filterResults(minLots: Int, maxDistance: Float?) {
        // TODO: Implement filtering logic
    }

    fun sortResults(sortBy: SortOption) {
        // TODO: Implement sorting logic
    }

    fun loadRecentSearches(userId: String) {
        viewModelScope.launch {
            carparkRepository.getRecentSearches(userId).collect { searches ->
                _recentSearches.value = searches
            }
        }
    }

    fun clearSearchHistory(userId: String) {
        viewModelScope.launch {
            carparkRepository.clearRecentSearches(userId)
        }
    }
}

data class SearchUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val searchQuery: String = "",
    val filters: SearchFilters = SearchFilters()
)

data class SearchFilters(
    val minLots: Int = 0,
    val maxDistance: Float? = null,
    val sortBy: SortOption = SortOption.DISTANCE
)

enum class SortOption {
    DISTANCE,
    AVAILABILITY,
    PRICE
}