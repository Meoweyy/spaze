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
class FavoritesViewModel @Inject constructor(
    private val carparkRepository: CarparkRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(FavoritesUiState())
    val uiState: StateFlow<FavoritesUiState> = _uiState.asStateFlow()

    private val _favoriteCarparks = MutableStateFlow<List<CarparkEntity>>(emptyList())
    val favoriteCarparks: StateFlow<List<CarparkEntity>> = _favoriteCarparks.asStateFlow()

    fun loadFavorites(userId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            carparkRepository.getFavoriteCarparks(userId).collect { carparks ->
                _favoriteCarparks.value = carparks
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    fun addToFavorites(userId: String, carparkId: String) {
        viewModelScope.launch {
            carparkRepository.addToFavorites(userId, carparkId)
        }
    }

    fun removeFromFavorites(userId: String, carparkId: String) {
        viewModelScope.launch {
            carparkRepository.removeFromFavorites(userId, carparkId)
        }
    }

    suspend fun isFavorite(userId: String, carparkId: String): Boolean {
        return carparkRepository.isFavorite(userId, carparkId)
    }
}

data class FavoritesUiState(
    val isLoading: Boolean = false,
    val error: String? = null
)