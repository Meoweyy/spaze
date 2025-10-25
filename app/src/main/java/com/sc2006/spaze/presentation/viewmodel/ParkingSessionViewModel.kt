package com.sc2006.spaze.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sc2006.spaze.data.local.entity.ParkingSessionEntity
import com.sc2006.spaze.data.repository.ParkingSessionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ParkingSessionViewModel @Inject constructor(
    private val sessionRepository: ParkingSessionRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ParkingSessionUiState())
    val uiState: StateFlow<ParkingSessionUiState> = _uiState.asStateFlow()

    private val _activeSession = MutableStateFlow<ParkingSessionEntity?>(null)
    val activeSession: StateFlow<ParkingSessionEntity?> = _activeSession.asStateFlow()

    fun loadActiveSession(userId: String) {
        viewModelScope.launch {
            sessionRepository.getActiveSessionFlow(userId).collect { session ->
                _activeSession.value = session
                session?.let {
                    _uiState.update { state -> state.copy(
                        isSessionActive = true,
                        elapsedTime = it.getElapsedTimeFormatted(),
                        estimatedCost = it.estimatedCost
                    )}
                }
            }
        }
    }

    fun startSession(
        userId: String,
        carparkId: String,
        carparkName: String,
        carparkAddress: String,
        budgetCap: Double?
    ) {
        viewModelScope.launch {
            sessionRepository.startSession(
                userId, carparkId, carparkName, carparkAddress, budgetCap
            )
        }
    }

    fun endSession(sessionId: String, finalCost: Double) {
        viewModelScope.launch {
            sessionRepository.endSession(sessionId, finalCost)
            _uiState.update { it.copy(isSessionActive = false) }
        }
    }

    fun updateEstimatedCost(sessionId: String, cost: Double) {
        viewModelScope.launch {
            sessionRepository.updateEstimatedCost(sessionId, cost)
        }
    }
}

data class ParkingSessionUiState(
    val isLoading: Boolean = false,
    val isSessionActive: Boolean = false,
    val elapsedTime: String = "00:00:00",
    val estimatedCost: Double = 0.0,
    val error: String? = null
)