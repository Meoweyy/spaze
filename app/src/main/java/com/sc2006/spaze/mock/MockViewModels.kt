package com.sc2006.spaze.mock

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sc2006.spaze.data.local.entity.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/**
 * MOCK VIEWMODELS FOR UI TESTING (Person 1 & 2)
 *
 * Week 1 - Person 3 Deliverable
 *
 * These ViewModels provide mock data so UI team can develop screens
 * without waiting for repository/database implementation.
 *
 * Usage:
 * 1. Copy this file to your UI project
 * 2. Use these ViewModels in your Composables for testing
 * 3. Switch to real ViewModels when repository is ready
 */

// ============================================================================
// MOCK HOME VIEWMODEL
// ============================================================================

class MockHomeViewModel : ViewModel() {
    private val _carparks = MutableStateFlow(MockDataProvider.mockCarparks)
    val carparks: StateFlow<List<CarparkEntity>> = _carparks.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    init {
        loadNearbyCarparks()
    }

    fun loadNearbyCarparks() {
        viewModelScope.launch {
            _isLoading.value = true
            delay(1000) // Simulate API call
            _carparks.value = MockDataProvider.mockCarparks
            _isLoading.value = false
        }
    }

    fun refreshFromAPI() {
        viewModelScope.launch {
            _isLoading.value = true
            delay(1500)
            _carparks.value = MockDataProvider.mockCarparks
            _isLoading.value = false
        }
    }

    fun searchCarparks(query: String) {
        viewModelScope.launch {
            delay(500)
            _carparks.value = MockDataProvider.mockCarparks.filter {
                it.location.contains(query, ignoreCase = true) ||
                it.address.contains(query, ignoreCase = true)
            }
        }
    }

    fun filterByAvailability(minLots: Int) {
        _carparks.value = MockDataProvider.mockCarparks.filter {
            it.availableLots >= minLots
        }
    }
}

// ============================================================================
// MOCK SEARCH VIEWMODEL
// ============================================================================

class MockSearchViewModel : ViewModel() {
    private val _searchResults = MutableStateFlow<List<CarparkEntity>>(emptyList())
    val searchResults: StateFlow<List<CarparkEntity>> = _searchResults.asStateFlow()

    private val _recentSearches = MutableStateFlow(MockDataProvider.mockRecentSearches)
    val recentSearches: StateFlow<List<RecentSearchEntity>> = _recentSearches.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _filters = MutableStateFlow(SearchFilters())
    val filters: StateFlow<SearchFilters> = _filters.asStateFlow()

    fun searchCarparks(query: String) {
        viewModelScope.launch {
            _isLoading.value = true
            delay(500)
            _searchResults.value = MockDataProvider.mockCarparks.filter {
                it.location.contains(query, ignoreCase = true) ||
                it.address.contains(query, ignoreCase = true)
            }
            _isLoading.value = false
        }
    }

    fun filterResults(minLots: Int, maxDistance: Float?) {
        _filters.update { it.copy(minLots = minLots, maxDistance = maxDistance) }

        _searchResults.value = _searchResults.value.filter { carpark ->
            val hasEnoughLots = carpark.availableLots >= minLots
            val withinDistance = maxDistance?.let { max ->
                carpark.distanceFromUser?.let { dist -> dist <= max } ?: false
            } ?: true
            hasEnoughLots && withinDistance
        }
    }

    fun sortResults(sortBy: SortOption) {
        _filters.update { it.copy(sortBy = sortBy) }

        _searchResults.value = when (sortBy) {
            SortOption.DISTANCE -> _searchResults.value.sortedBy { it.distanceFromUser }
            SortOption.AVAILABILITY -> _searchResults.value.sortedByDescending { it.availableLots }
            SortOption.PRICE -> _searchResults.value.sortedByDescending { it.availableLots } // Mock price sort
        }
    }

    fun loadRecentSearches() {
        viewModelScope.launch {
            delay(300)
            _recentSearches.value = MockDataProvider.mockRecentSearches
        }
    }

    fun clearSearchHistory() {
        _recentSearches.value = emptyList()
    }
}

data class SearchFilters(
    val minLots: Int = 0,
    val maxDistance: Float? = null,
    val sortBy: SortOption = SortOption.DISTANCE
)

enum class SortOption {
    DISTANCE, AVAILABILITY, PRICE
}

// ============================================================================
// MOCK FAVOURITES VIEWMODEL
// ============================================================================

class MockFavouritesViewModel : ViewModel() {
    private val _favoriteCarparks = MutableStateFlow<List<CarparkEntity>>(emptyList())
    val favoriteCarparks: StateFlow<List<CarparkEntity>> = _favoriteCarparks.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _successMessage = MutableStateFlow<String?>(null)
    val successMessage: StateFlow<String?> = _successMessage.asStateFlow()

    private val favoriteIds = mutableSetOf<String>()

    init {
        loadFavorites()
    }

    fun loadFavorites() {
        viewModelScope.launch {
            _isLoading.value = true
            delay(500)
            // Load favorites from mock data
            favoriteIds.addAll(MockDataProvider.mockFavoriteIds)
            _favoriteCarparks.value = MockDataProvider.mockCarparks.filter {
                it.carparkID in favoriteIds
            }
            _isLoading.value = false
        }
    }

    fun addToFavorites(carparkId: String) {
        viewModelScope.launch {
            delay(300)
            favoriteIds.add(carparkId)
            _favoriteCarparks.value = MockDataProvider.mockCarparks.filter {
                it.carparkID in favoriteIds
            }
            _successMessage.value = "Added to favorites"
        }
    }

    fun removeFromFavorites(carparkId: String) {
        viewModelScope.launch {
            delay(300)
            favoriteIds.remove(carparkId)
            _favoriteCarparks.value = MockDataProvider.mockCarparks.filter {
                it.carparkID in favoriteIds
            }
            _successMessage.value = "Removed from favorites"
        }
    }

    suspend fun isFavorite(carparkId: String): Boolean {
        delay(100)
        return carparkId in favoriteIds
    }

    fun clearMessages() {
        _successMessage.value = null
    }
}

// ============================================================================
// MOCK PROFILE VIEWMODEL
// ============================================================================

class MockProfileViewModel : ViewModel() {
    private val _user = MutableStateFlow(MockDataProvider.mockUser)
    val user: StateFlow<UserEntity> = _user.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _successMessage = MutableStateFlow<String?>(null)
    val successMessage: StateFlow<String?> = _successMessage.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    init {
        loadProfile()
    }

    fun loadProfile() {
        viewModelScope.launch {
            _isLoading.value = true
            delay(500)
            _user.value = MockDataProvider.mockUser
            _isLoading.value = false
        }
    }

    fun updateProfile(userName: String, email: String) {
        viewModelScope.launch {
            _isLoading.value = true
            delay(800)
            _user.value = _user.value.copy(userName = userName, email = email)
            _successMessage.value = "Profile updated successfully"
            _isLoading.value = false
        }
    }

    fun changePassword(oldPassword: String, newPassword: String) {
        viewModelScope.launch {
            _isLoading.value = true
            delay(800)
            if (newPassword.length < 6) {
                _error.value = "Password must be at least 6 characters"
            } else {
                _successMessage.value = "Password changed successfully"
            }
            _isLoading.value = false
        }
    }

    fun logout() {
        viewModelScope.launch {
            delay(300)
            _successMessage.value = "Logged out successfully"
        }
    }

    fun clearMessages() {
        _successMessage.value = null
        _error.value = null
    }
}

// ============================================================================
// MOCK PARKING SESSION VIEWMODEL
// ============================================================================

class MockParkingSessionViewModel : ViewModel() {
    private val _activeSession = MutableStateFlow<ParkingSessionEntity?>(null)
    val activeSession: StateFlow<ParkingSessionEntity?> = _activeSession.asStateFlow()

    private val _estimatedCost = MutableStateFlow(0.0f)
    val estimatedCost: StateFlow<Float> = _estimatedCost.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init {
        loadActiveSession()
    }

    fun loadActiveSession() {
        viewModelScope.launch {
            _isLoading.value = true
            delay(500)
            _activeSession.value = MockDataProvider.mockActiveParkingSession
            updateEstimatedCost()
            _isLoading.value = false
        }
    }

    fun startSession(carparkId: String, carparkName: String, lotNumber: String) {
        viewModelScope.launch {
            delay(500)
            val session = ParkingSessionEntity(
                sessionID = "session_${System.currentTimeMillis()}",
                userID = "mock_user_123",
                carparkID = carparkId,
                carparkName = carparkName,
                startTime = System.currentTimeMillis(),
                endTime = null,
                estimatedCost = 2.50f,
                actualCost = null,
                lotNumber = lotNumber
            )
            _activeSession.value = session
            updateEstimatedCost()
        }
    }

    fun endSession() {
        viewModelScope.launch {
            delay(500)
            _activeSession.value = null
            _estimatedCost.value = 0.0f
        }
    }

    private fun updateEstimatedCost() {
        viewModelScope.launch {
            while (_activeSession.value != null) {
                val session = _activeSession.value ?: break
                val durationHours = (System.currentTimeMillis() - session.startTime) / (1000 * 60 * 60).toFloat()
                _estimatedCost.value = durationHours * 2.50f // $2.50 per hour
                delay(60000) // Update every minute
            }
        }
    }
}

// ============================================================================
// MOCK BUDGET VIEWMODEL
// ============================================================================

class MockBudgetViewModel : ViewModel() {
    private val _monthlyBudget = MutableStateFlow(100.0f)
    val monthlyBudget: StateFlow<Float> = _monthlyBudget.asStateFlow()

    private val _currentSpending = MutableStateFlow(45.50f)
    val currentSpending: StateFlow<Float> = _currentSpending.asStateFlow()

    private val _remainingBudget = MutableStateFlow(54.50f)
    val remainingBudget: StateFlow<Float> = _remainingBudget.asStateFlow()

    private val _isWarningTriggered = MutableStateFlow(false)
    val isWarningTriggered: StateFlow<Boolean> = _isWarningTriggered.asStateFlow()

    private val _isBudgetExceeded = MutableStateFlow(false)
    val isBudgetExceeded: StateFlow<Boolean> = _isBudgetExceeded.asStateFlow()

    fun setMonthlyBudget(amount: Float) {
        viewModelScope.launch {
            delay(300)
            _monthlyBudget.value = amount
            calculateBudgetStatus()
        }
    }

    fun addSpending(amount: Float) {
        viewModelScope.launch {
            delay(300)
            _currentSpending.value += amount
            calculateBudgetStatus()
        }
    }

    private fun calculateBudgetStatus() {
        val remaining = _monthlyBudget.value - _currentSpending.value
        _remainingBudget.value = remaining

        val spendingPercentage = (_currentSpending.value / _monthlyBudget.value) * 100
        _isWarningTriggered.value = spendingPercentage >= 80
        _isBudgetExceeded.value = spendingPercentage >= 100
    }

    fun checkBudgetThreshold(parkingCost: Float): Boolean {
        val potentialSpending = _currentSpending.value + parkingCost
        return potentialSpending <= _monthlyBudget.value
    }
}
