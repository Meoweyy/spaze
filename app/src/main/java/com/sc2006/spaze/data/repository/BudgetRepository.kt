package com.sc2006.spaze.data.repository

import com.sc2006.spaze.data.local.dao.BudgetDao
import com.sc2006.spaze.data.local.entity.BudgetEntity
import kotlinx.coroutines.flow.Flow
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Budget Repository
 * Manages user's parking budget and spending tracking
 * Implements: Budgeting and Cost Controls
 */
@Singleton
class BudgetRepository @Inject constructor(
    private val budgetDao: BudgetDao
) {

    /**
     * Get current month's budget
     */
    suspend fun getCurrentMonthBudget(userId: String): BudgetEntity? {
        val currentMonth = getCurrentMonth()
        return budgetDao.getBudgetForMonth(userId, currentMonth)
    }

    /**
     * Get current month's budget as Flow
     */
    fun getCurrentMonthBudgetFlow(userId: String): Flow<BudgetEntity?> {
        val currentMonth = getCurrentMonth()
        return budgetDao.getBudgetForMonthFlow(userId, currentMonth)
    }

    /**
     * Create or update monthly budget
     */
    suspend fun setMonthlyBudget(userId: String, amount: Double): Result<BudgetEntity> {
        return try {
            val currentMonth = getCurrentMonth()
            var budget = budgetDao.getBudgetForMonth(userId, currentMonth)

            if (budget == null) {
                budget = BudgetEntity.create(userId, amount)
                budgetDao.insertBudget(budget)
            } else {
                budgetDao.updateMonthlyBudget(budget.budgetID, amount)
                budget = budgetDao.getBudgetForMonth(userId, currentMonth)!!
            }

            Result.success(budget)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Add spending to current month
     */
    suspend fun addSpending(userId: String, amount: Double): Result<Unit> {
        return try {
            val budget = getCurrentMonthBudget(userId)
                ?: throw Exception("Budget not set for current month")

            budgetDao.addSpending(budget.budgetID, amount)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Directly update current month's spending value
     */
    suspend fun updateCurrentSpending(userId: String, amount: Double): Result<Unit> {
        return try {
            val budget = getCurrentMonthBudget(userId)
                ?: throw Exception("Budget not set for current month")

            val updatedBudget = budget.copy(
                currentMonthSpending = amount,
                lastUpdated = System.currentTimeMillis()
            )

            budgetDao.updateBudget(updatedBudget)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Remove spending from current month
     */
    suspend fun removeSpending(userId: String, amount: Double): Result<Unit> {
        return try {
            val budget = getCurrentMonthBudget(userId)
                ?: throw Exception("Budget not set for current month")

            budgetDao.removeSpending(budget.budgetID, amount)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Check if budget warning should be sent
     */
    suspend fun checkBudgetWarning(userId: String): Boolean {
        val budget = getCurrentMonthBudget(userId) ?: return false
        return budget.shouldSendWarning()
    }

    /**
     * Check if budget exceeded notification should be sent
     */
    suspend fun checkBudgetExceeded(userId: String): Boolean {
        val budget = getCurrentMonthBudget(userId) ?: return false
        return budget.shouldSendCritical()
    }

    /**
     * Mark budget warning as sent
     */
    suspend fun markBudgetWarningAsSent(userId: String) {
        val budget = getCurrentMonthBudget(userId) ?: return
        budgetDao.markWarningAsSent(budget.budgetID)
    }

    /**
     * Mark budget exceeded notification as sent
     */
    suspend fun markBudgetExceededAsSent(userId: String) {
        val budget = getCurrentMonthBudget(userId) ?: return
        budgetDao.markCriticalAsSent(budget.budgetID)
    }

    /**
     * Get all budgets for user
     */
    fun getAllBudgets(userId: String): Flow<List<BudgetEntity>> {
        return budgetDao.getAllBudgets(userId)
    }

    /**
     * Get current month in YYYY-MM format
     */
    private fun getCurrentMonth(): String {
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH) + 1
        return String.format("%04d-%02d", year, month)
    }
}
