package com.sc2006.spaze.presentation.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sc2006.spaze.data.local.entity.ParkingSessionEntity
import com.sc2006.spaze.data.preferences.PreferencesDataStore
import com.sc2006.spaze.data.repository.BudgetRepository
import com.sc2006.spaze.data.repository.ParkingSessionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.math.ceil

/**
 * Parking Session ViewModel
 * Implements: Session Tracking, Real-time Cost Calculation, Budget Warnings
 * Business Logic: Cost estimation, budget cap validation, timer updates
 */
@HiltViewModel
class ParkingSessionViewModel @Inject constructor(
    private val sessionRepository: ParkingSessionRepository,
    private val budgetRepository: BudgetRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(ParkingSessionUiState())
    val uiState: StateFlow<ParkingSessionUiState> = _uiState.asStateFlow()

    private val _activeSession = MutableStateFlow<ParkingSessionEntity?>(null)
    val activeSession: StateFlow<ParkingSessionEntity?> = _activeSession.asStateFlow()

    private val _sessionHistory = MutableStateFlow<List<ParkingSessionEntity>>(emptyList())
    val sessionHistory: StateFlow<List<ParkingSessionEntity>> = _sessionHistory.asStateFlow()

    private var timerJob: Job? = null
    private var costUpdateJob: Job? = null

    companion object {
        // Singapore carpark pricing (average rates)
        private const val DEFAULT_COST_PER_HOUR = 2.0  // SGD per hour
        private const val COST_PER_30_MIN = 1.0  // SGD per 30 minutes
        private const val MAX_DAILY_COST = 20.0  // SGD max per day

        // Thresholds
        private const val BUDGET_WARNING_PERCENTAGE = 0.8  // 80%
        private const val BUDGET_CRITICAL_PERCENTAGE = 0.95  // 95%

        // Update intervals
        private const val TIMER_UPDATE_INTERVAL_MS = 1000L  // 1 second
        private const val COST_UPDATE_INTERVAL_MS = 60000L  // 1 minute
    }

    // ═══════════════════════════════════════════════════
    // SESSION MANAGEMENT
    // ═══════════════════════════════════════════════════

    /**
     * Load active session and start timer
     */
    fun loadActiveSession(userId: String) {
        viewModelScope.launch {
            try {
                sessionRepository.getActiveSessionFlow(userId).collect { session ->
                    _activeSession.value = session
                    if (session != null) {
                        _uiState.update {
                            it.copy(
                                isSessionActive = true,
                                elapsedTime = session.getElapsedTimeFormatted(),
                                estimatedCost = session.estimatedCost,
                                carparkName = session.carparkName,
                                carparkAddress = session.carparkAddress,
                                budgetCap = session.perSessionBudgetCap
                            )
                        }
                        startTimer()
                        startCostUpdates(userId, session)
                    } else {
                        _uiState.update { it.copy(isSessionActive = false) }
                        stopTimer()
                        stopCostUpdates()
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(error = e.message ?: "Failed to load session")
                }
            }
        }
    }

    /**
     * Load session history
     */
    fun loadSessionHistory(userId: String, limit: Int = 20) {
        viewModelScope.launch {
            sessionRepository.getRecentSessions(userId, limit).collect { sessions ->
                _sessionHistory.value = sessions
            }
        }
    }

    /**
     * Start parking session with validation
     */
    fun startSession(
        userId: String,
        carparkId: String,
        carparkName: String,
        carparkAddress: String,
        budgetCap: Double? = null
    ) {
        // Validate budget cap
        budgetCap?.let { cap ->
            if (cap <= 0) {
                _uiState.update { it.copy(error = "Budget cap must be greater than zero") }
                return
            }
            if (cap > MAX_DAILY_COST) {
                _uiState.update {
                    it.copy(error = "Budget cap cannot exceed SGD $MAX_DAILY_COST")
                }
                return
            }
        }

        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isLoading = true, error = null) }

                val result = sessionRepository.startSession(
                    userId = userId,
                    carparkId = carparkId,
                    carparkName = carparkName,
                    carparkAddress = carparkAddress,
                    perSessionBudgetCap = budgetCap
                )

                result.fold(
                    onSuccess = { session ->
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                isSessionActive = true,
                                successMessage = "Parking session started at $carparkName"
                            )
                        }
                    },
                    onFailure = { error ->
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                error = error.message ?: "Failed to start session"
                            )
                        }
                    }
                )
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = e.message ?: "Failed to start session"
                    )
                }
            }
        }
    }

    /**
     * End parking session with final cost
     */
    fun endSession(userId: String, sessionId: String, finalCost: Double? = null) {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isLoading = true, error = null) }

                // Use estimated cost if final cost not provided
                val cost = finalCost ?: _activeSession.value?.estimatedCost ?: 0.0

                val result = sessionRepository.endSession(sessionId, cost)
                result.fold(
                    onSuccess = {
                        // Add spending to budget
                        budgetRepository.addSpending(userId, cost)

                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                isSessionActive = false,
                                successMessage = "Session ended. Total cost: SGD ${String.format("%.2f", cost)}"
                            )
                        }

                        stopTimer()
                        stopCostUpdates()
                    },
                    onFailure = { error ->
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                error = error.message ?: "Failed to end session"
                            )
                        }
                    }
                )
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = e.message ?: "Failed to end session"
                    )
                }
            }
        }
    }

    // ═══════════════════════════════════════════════════
    // BUSINESS LOGIC - Cost Calculation & Warnings
    // ═══════════════════════════════════════════════════

    /**
     * Calculate estimated cost based on elapsed time
     * Uses standard Singapore carpark rates
     */
    private fun calculateEstimatedCost(elapsedMinutes: Long): Double {
        // Round up to nearest 30 minutes (standard carpark billing)
        val billingBlocks = ceil(elapsedMinutes / 30.0).toLong()
        val cost = billingBlocks * COST_PER_30_MIN

        // Cap at max daily cost
        return minOf(cost, MAX_DAILY_COST)
    }

    /**
     * Start automatic cost updates
     */
    private fun startCostUpdates(userId: String, session: ParkingSessionEntity) {
        stopCostUpdates()

        costUpdateJob = viewModelScope.launch {
            while (isActive) {
                val elapsedMinutes = session.getElapsedTimeMinutes().toLong()
                val estimatedCost = calculateEstimatedCost(elapsedMinutes)

                // Update cost in repository
                sessionRepository.updateEstimatedCost(session.sessionID, estimatedCost)

                // Check budget warnings
                checkBudgetWarnings(session, estimatedCost)

                // Check monthly budget
                checkMonthlyBudget(userId, estimatedCost)

                delay(COST_UPDATE_INTERVAL_MS)
            }
        }
    }

    /**
     * Stop cost updates
     */
    private fun stopCostUpdates() {
        costUpdateJob?.cancel()
        costUpdateJob = null
    }

    /**
     * Check session budget cap warnings
     */
    private fun checkBudgetWarnings(session: ParkingSessionEntity, currentCost: Double) {
        // Check if notifications are enabled before showing warnings
        viewModelScope.launch {
            val notificationsEnabled = PreferencesDataStore.getNotificationsEnabled(context).first()
            if (!notificationsEnabled) return@launch // Skip if notifications disabled
            
            session.perSessionBudgetCap?.let { cap ->
                val percentage = currentCost / cap

                when {
                    percentage >= 1.0 && !session.hasExceededBeenSent -> {
                        _uiState.update {
                            it.copy(
                                showBudgetExceeded = true,
                                budgetExceededMessage = "Session cost exceeded your budget cap of SGD ${String.format("%.2f", cap)}!"
                            )
                        }
                        sessionRepository.markBudgetExceededAsSent(session.sessionID)
                    }
                    percentage >= BUDGET_CRITICAL_PERCENTAGE && !session.hasWarningBeenSent -> {
                        _uiState.update {
                            it.copy(
                                showBudgetWarning = true,
                                budgetWarningMessage = "You're at ${(percentage * 100).toInt()}% of your session budget cap"
                            )
                        }
                        sessionRepository.markBudgetWarningAsSent(session.sessionID)
                    }
                    else -> {
                        // No warning needed
                    }
                }
            }
        }
    }

    /**
     * Check monthly budget impact
     */
    private suspend fun checkMonthlyBudget(userId: String, sessionCost: Double) {
        // Check if notifications are enabled before showing warnings
        val notificationsEnabled = PreferencesDataStore.getNotificationsEnabled(context).first()
        if (!notificationsEnabled) return // Skip if notifications disabled
        
        val currentBudget = budgetRepository.getCurrentMonthBudget(userId) ?: return

        val projectedTotal = currentBudget.currentMonthSpending + sessionCost
        val percentage = projectedTotal / currentBudget.monthlyBudget

        if (percentage >= BUDGET_WARNING_PERCENTAGE) {
            _uiState.update {
                it.copy(
                    showMonthlyBudgetWarning = true,
                    monthlyBudgetWarningMessage = "This session will use ${(percentage * 100).toInt()}% of your monthly budget"
                )
            }
        }
    }

    // ═══════════════════════════════════════════════════
    // TIMER FUNCTIONALITY
    // ═══════════════════════════════════════════════════

    /**
     * Start timer for elapsed time updates
     */
    private fun startTimer() {
        stopTimer()

        timerJob = viewModelScope.launch {
            while (isActive) {
                _activeSession.value?.let { session ->
                    _uiState.update {
                        it.copy(
                            elapsedTime = session.getElapsedTimeFormatted(),
                            estimatedCost = session.estimatedCost
                        )
                    }
                }
                delay(TIMER_UPDATE_INTERVAL_MS)
            }
        }
    }

    /**
     * Stop timer
     */
    private fun stopTimer() {
        timerJob?.cancel()
        timerJob = null
    }

    // ═══════════════════════════════════════════════════
    // UI STATE MANAGEMENT
    // ═══════════════════════════════════════════════════

    /**
     * Dismiss budget warning
     */
    fun dismissBudgetWarning() {
        _uiState.update {
            it.copy(showBudgetWarning = false, budgetWarningMessage = null)
        }
    }

    /**
     * Dismiss budget exceeded alert
     */
    fun dismissBudgetExceeded() {
        _uiState.update {
            it.copy(showBudgetExceeded = false, budgetExceededMessage = null)
        }
    }

    /**
     * Dismiss monthly budget warning
     */
    fun dismissMonthlyBudgetWarning() {
        _uiState.update {
            it.copy(showMonthlyBudgetWarning = false, monthlyBudgetWarningMessage = null)
        }
    }

    /**
     * Clear success message
     */
    fun clearSuccessMessage() {
        _uiState.update { it.copy(successMessage = null) }
    }

    /**
     * Clear error
     */
    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    /**
     * Delete session history
     */
    fun deleteSessionHistory(userId: String) {
        viewModelScope.launch {
            try {
                val result = sessionRepository.deleteSessionHistory(userId)
                result.fold(
                    onSuccess = {
                        _sessionHistory.value = emptyList()
                        _uiState.update {
                            it.copy(successMessage = "Session history cleared")
                        }
                    },
                    onFailure = { error ->
                        _uiState.update {
                            it.copy(error = error.message ?: "Failed to delete history")
                        }
                    }
                )
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(error = e.message ?: "Failed to delete history")
                }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        stopTimer()
        stopCostUpdates()
    }
}

data class ParkingSessionUiState(
    val isLoading: Boolean = false,
    val isSessionActive: Boolean = false,
    val carparkName: String = "",
    val carparkAddress: String = "",
    val elapsedTime: String = "00:00:00",
    val estimatedCost: Double = 0.0,
    val budgetCap: Double? = null,

    // Warnings
    val showBudgetWarning: Boolean = false,
    val budgetWarningMessage: String? = null,
    val showBudgetExceeded: Boolean = false,
    val budgetExceededMessage: String? = null,
    val showMonthlyBudgetWarning: Boolean = false,
    val monthlyBudgetWarningMessage: String? = null,

    // Messages
    val successMessage: String? = null,
    val error: String? = null
)