package com.sc2006.spaze.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sc2006.spaze.data.local.entity.BudgetEntity
import com.sc2006.spaze.data.repository.BudgetRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Budget ViewModel
 * Implements: Budgeting and Cost Controls
 * Business Logic: Budget validation, spending tracking, threshold warnings
 */
@HiltViewModel
class BudgetViewModel @Inject constructor(
    private val budgetRepository: BudgetRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(BudgetUiState())
    val uiState: StateFlow<BudgetUiState> = _uiState.asStateFlow()

    private val _currentBudget = MutableStateFlow<BudgetEntity?>(null)
    val currentBudget: StateFlow<BudgetEntity?> = _currentBudget.asStateFlow()

    private val _allBudgets = MutableStateFlow<List<BudgetEntity>>(emptyList())
    val allBudgets: StateFlow<List<BudgetEntity>> = _allBudgets.asStateFlow()

    companion object {
        private const val MIN_BUDGET = 1.0  // SGD 1
        private const val MAX_BUDGET = 10000.0  // SGD 10,000
        private const val WARNING_THRESHOLD = 0.8  // 80%
        private const val CRITICAL_THRESHOLD = 1.0  // 100%
    }

    // ═══════════════════════════════════════════════════
    // VALIDATION METHODS
    // ═══════════════════════════════════════════════════

    /**
     * Validate budget amount
     */
    private fun validateBudgetAmount(amount: Double): String? {
        if (amount <= 0) return "Budget must be greater than zero"
        if (amount < MIN_BUDGET) return "Budget must be at least SGD $MIN_BUDGET"
        if (amount > MAX_BUDGET) return "Budget cannot exceed SGD $MAX_BUDGET"
        return null
    }

    /**
     * Validate spending amount
     */
    private fun validateSpendingAmount(amount: Double): String? {
        if (amount <= 0) return "Spending amount must be greater than zero"
        if (amount > MAX_BUDGET) return "Spending amount is unreasonably high"
        return null
    }

    // ═══════════════════════════════════════════════════
    // BUDGET MANAGEMENT
    // ═══════════════════════════════════════════════════

    /**
     * Load current month's budget
     */
    fun loadBudget(userId: String) {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isLoading = true, error = null) }

                budgetRepository.getCurrentMonthBudgetFlow(userId).collect { budget ->
                    _currentBudget.value = budget
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            hasBudget = budget != null,
                            remainingBudget = budget?.getRemainingBudget() ?: 0.0,
                            usagePercentage = budget?.getBudgetUsagePercentage() ?: 0.0,
                            totalBudget = budget?.monthlyBudget ?: 0.0,
                            totalSpending = budget?.currentMonthSpending ?: 0.0
                        )
                    }

                    // Check thresholds
                    budget?.let {
                        checkBudgetThresholds(userId)
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = e.message ?: "Failed to load budget"
                    )
                }
            }
        }
    }

    /**
     * Load all budgets (history)
     */
    fun loadAllBudgets(userId: String) {
        viewModelScope.launch {
            budgetRepository.getAllBudgets(userId).collect { budgets ->
                _allBudgets.value = budgets
            }
        }
    }

    /**
     * Set monthly budget with validation
     */
    fun setMonthlyBudget(userId: String, amount: Double) {
        // Validate amount
        validateBudgetAmount(amount)?.let { error ->
            _uiState.update { it.copy(error = error) }
            return
        }

        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isLoading = true, error = null) }

                val result = budgetRepository.setMonthlyBudget(userId, amount)
                result.fold(
                    onSuccess = { budget ->
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                hasBudget = true,
                                successMessage = "Budget set to SGD ${String.format("%.2f", amount)}"
                            )
                        }
                    },
                    onFailure = { error ->
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                error = error.message ?: "Failed to set budget"
                            )
                        }
                    }
                )
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = e.message ?: "Failed to set budget"
                    )
                }
            }
        }
    }

    /**
     * Add spending with validation and threshold checking
     */
    fun addSpending(userId: String, amount: Double) {
        // Validate amount
        validateSpendingAmount(amount)?.let { error ->
            _uiState.update { it.copy(error = error) }
            return
        }

        viewModelScope.launch {
            try {
                val result = budgetRepository.addSpending(userId, amount)
                result.fold(
                    onSuccess = {
                        checkBudgetThresholds(userId)
                    },
                    onFailure = { error ->
                        _uiState.update {
                            it.copy(error = error.message ?: "Failed to add spending")
                        }
                    }
                )
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(error = e.message ?: "Failed to add spending")
                }
            }
        }
    }

    /**
     * Remove spending (e.g., refund)
     */
    fun removeSpending(userId: String, amount: Double) {
        // Validate amount
        validateSpendingAmount(amount)?.let { error ->
            _uiState.update { it.copy(error = error) }
            return
        }

        viewModelScope.launch {
            try {
                val result = budgetRepository.removeSpending(userId, amount)
                result.fold(
                    onSuccess = {
                        _uiState.update {
                            it.copy(successMessage = "Refund of SGD ${String.format("%.2f", amount)} applied")
                        }
                    },
                    onFailure = { error ->
                        _uiState.update {
                            it.copy(error = error.message ?: "Failed to remove spending")
                        }
                    }
                )
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(error = e.message ?: "Failed to remove spending")
                }
            }
        }
    }

    // ═══════════════════════════════════════════════════
    // BUSINESS LOGIC - Budget Thresholds & Warnings
    // ═══════════════════════════════════════════════════

    /**
     * Check budget thresholds and trigger warnings
     */
    private suspend fun checkBudgetThresholds(userId: String) {
        try {
            // Check for warning (80% threshold)
            if (budgetRepository.checkBudgetWarning(userId)) {
                _uiState.update {
                    it.copy(
                        showWarning = true,
                        warningMessage = "You've used 80% of your monthly parking budget"
                    )
                }
                // Mark as sent to avoid duplicate notifications
                budgetRepository.markBudgetWarningAsSent(userId)
            }

            // Check for exceeded (100% threshold)
            if (budgetRepository.checkBudgetExceeded(userId)) {
                _uiState.update {
                    it.copy(
                        showExceeded = true,
                        exceededMessage = "You've exceeded your monthly parking budget!"
                    )
                }
                // Mark as sent to avoid duplicate notifications
                budgetRepository.markBudgetExceededAsSent(userId)
            }
        } catch (e: Exception) {
            // Silent fail for threshold checks
        }
    }

    /**
     * Calculate if adding a cost would exceed budget
     */
    fun willExceedBudget(amount: Double): Boolean {
        val budget = _currentBudget.value ?: return false
        val newTotal = budget.currentMonthSpending + amount
        return newTotal > budget.monthlyBudget
    }

    /**
     * Get remaining budget after potential spending
     */
    fun getRemainingAfterSpending(amount: Double): Double {
        val budget = _currentBudget.value ?: return 0.0
        return budget.getRemainingBudget() - amount
    }

    /**
     * Get budget status
     */
    fun getBudgetStatus(): BudgetStatus {
        val percentage = _uiState.value.usagePercentage
        return when {
            percentage >= CRITICAL_THRESHOLD -> BudgetStatus.EXCEEDED
            percentage >= WARNING_THRESHOLD -> BudgetStatus.WARNING
            percentage >= 0.5 -> BudgetStatus.MODERATE
            else -> BudgetStatus.HEALTHY
        }
    }

    // ═══════════════════════════════════════════════════
    // UI STATE MANAGEMENT
    // ═══════════════════════════════════════════════════

    /**
     * Dismiss warning
     */
    fun dismissWarning() {
        _uiState.update { it.copy(showWarning = false, warningMessage = null) }
    }

    /**
     * Dismiss exceeded alert
     */
    fun dismissExceeded() {
        _uiState.update { it.copy(showExceeded = false, exceededMessage = null) }
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
}

/**
 * Budget status enum
 */
enum class BudgetStatus {
    HEALTHY,    // < 50%
    MODERATE,   // 50-79%
    WARNING,    // 80-99%
    EXCEEDED    // >= 100%
}

data class BudgetUiState(
    val isLoading: Boolean = false,
    val hasBudget: Boolean = false,
    val totalBudget: Double = 0.0,
    val totalSpending: Double = 0.0,
    val remainingBudget: Double = 0.0,
    val usagePercentage: Double = 0.0,
    val showWarning: Boolean = false,
    val warningMessage: String? = null,
    val showExceeded: Boolean = false,
    val exceededMessage: String? = null,
    val successMessage: String? = null,
    val error: String? = null
)