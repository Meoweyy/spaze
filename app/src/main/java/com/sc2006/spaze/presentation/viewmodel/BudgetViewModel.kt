package com.sc2006.spaze.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sc2006.spaze.data.repository.BudgetRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class BudgetViewModel @Inject constructor(
    private val budgetRepository: BudgetRepository
) : ViewModel() {

    private val userId = "user123"
    private var budgetJob: Job? = null

    private val _uiState = MutableStateFlow(BudgetUiState(isLoading = true, showSetupDialog = true))
    val uiState: StateFlow<BudgetUiState> = _uiState.asStateFlow()

    init {
        observeBudget()
    }

    private fun observeBudget() {
        budgetJob?.cancel()
        budgetJob = viewModelScope.launch {
            budgetRepository.getCurrentMonthBudgetFlow(userId).collectLatest { budget ->
                _uiState.update { currentState ->
                    if (budget == null) {
                        currentState.copy(
                            isLoading = false,
                            budgetLimit = null,
                            currentAmount = 0.0,
                            showSetupDialog = true,
                            showChangeLimitDialog = false,
                            error = null
                        )
                    } else {
                        currentState.copy(
                            isLoading = false,
                            budgetLimit = budget.monthlyBudget,
                            currentAmount = budget.currentMonthSpending,
                            showSetupDialog = false,
                            showChangeLimitDialog = false,
                            error = null
                        )
                    }
                }
            }
        }
    }

    fun submitInitialLimit(limit: Double) {
        if (limit <= 0) {
            _uiState.update { it.copy(error = "Budget limit must be greater than 0") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            val result = budgetRepository.setMonthlyBudget(userId, limit)
            result.onFailure { error ->
                _uiState.update { it.copy(isLoading = false, error = error.message) }
            }
        }
    }

    fun onChangeLimitClicked() {
        _uiState.update { it.copy(showChangeLimitDialog = true, error = null) }
    }

    fun dismissChangeLimitDialog() {
        _uiState.update { it.copy(showChangeLimitDialog = false, error = null) }
    }

    fun updateLimit(limit: Double) {
        if (limit <= 0) {
            _uiState.update { it.copy(error = "Budget limit must be greater than 0") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            val result = budgetRepository.setMonthlyBudget(userId, limit)
            result.onFailure { error ->
                _uiState.update { it.copy(isLoading = false, error = error.message) }
            }
            _uiState.update { it.copy(showChangeLimitDialog = false) }
        }
    }

    fun onAmountChange(amount: Float) {
        val limit = uiState.value.budgetLimit ?: return
        _uiState.update { it.copy(currentAmount = amount.coerceIn(0f, limit.toFloat()).toDouble()) }
    }

    fun persistAmountChange() {
        val amount = uiState.value.currentAmount
        viewModelScope.launch {
            budgetRepository.updateCurrentSpending(userId, amount).onFailure { error ->
                _uiState.update { it.copy(error = error.message) }
            }
        }
    }
}

data class BudgetUiState(
    val isLoading: Boolean = false,
    val budgetLimit: Double? = null,
    val currentAmount: Double = 0.0,
    val showSetupDialog: Boolean = false,
    val showChangeLimitDialog: Boolean = false,
    val error: String? = null
)
