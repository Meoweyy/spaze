package com.sc2006.spaze.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sc2006.spaze.data.local.entity.BudgetEntity
import com.sc2006.spaze.data.repository.BudgetRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class BudgetViewModel @Inject constructor(
    private val budgetRepository: BudgetRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(BudgetUiState())
    val uiState: StateFlow<BudgetUiState> = _uiState.asStateFlow()

    private val _currentBudget = MutableStateFlow<BudgetEntity?>(null)
    val currentBudget: StateFlow<BudgetEntity?> = _currentBudget.asStateFlow()

    fun loadBudget(userId: String) {
        viewModelScope.launch {
            budgetRepository.getCurrentMonthBudgetFlow(userId).collect { budget ->
                _currentBudget.value = budget
                _uiState.update { it.copy(
                    remainingBudget = budget?.getRemainingBudget() ?: 0.0,
                    usagePercentage = budget?.getBudgetUsagePercentage() ?: 0.0
                )}
            }
        }
    }

    fun setMonthlyBudget(userId: String, amount: Double) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            budgetRepository.setMonthlyBudget(userId, amount)
            _uiState.update { it.copy(isLoading = false) }
        }
    }

    fun addSpending(userId: String, amount: Double) {
        viewModelScope.launch {
            budgetRepository.addSpending(userId, amount)
            checkBudgetThresholds(userId)
        }
    }

    private suspend fun checkBudgetThresholds(userId: String) {
        if (budgetRepository.checkBudgetWarning(userId)) {
            _uiState.update { it.copy(showWarning = true) }
        }
        if (budgetRepository.checkBudgetExceeded(userId)) {
            _uiState.update { it.copy(showExceeded = true) }
        }
    }
}

data class BudgetUiState(
    val isLoading: Boolean = false,
    val remainingBudget: Double = 0.0,
    val usagePercentage: Double = 0.0,
    val showWarning: Boolean = false,
    val showExceeded: Boolean = false,
    val error: String? = null
)