package com.sc2006.spaze.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sc2006.spaze.data.local.entity.CarparkEntity
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

    fun searchCarparks(query: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            carparkRepository.searchCarparks(query).collect { results ->
                _searchResults.value = results
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    fun clearSearch() {
        _searchResults.value = emptyList()
    }
}

data class SearchUiState(
    val isLoading: Boolean = false,
    val error: String? = null
)