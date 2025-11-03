package com.sc2006.spaze.data.repository

import com.sc2006.spaze.data.local.dao.BudgetDao
import com.sc2006.spaze.data.local.entity.BudgetEntity
import kotlinx.coroutines.flow.Flow
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BudgetRepository @Inject constructor(
    private val budgetDao: BudgetDao
) {

    suspend fun getCurrentMonthBudget(userId: String): BudgetEntity? {
        val currentMonth = getCurrentMonth()
        return budgetDao.getBudgetForMonth(userId, currentMonth)
    }

    fun getCurrentMonthBudgetFlow(userId: String): Flow<BudgetEntity?> {
        val currentMonth = getCurrentMonth()
        return budgetDao.getBudgetForMonthFlow(userId, currentMonth)
    }

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

    suspend fun addSpending(userId: String, amount: Double): Result<Unit> {
        return try {
            val budget = getCurrentMonthBudget(userId) ?: throw Exception("Budget not set for current month")
            budgetDao.addSpending(budget.budgetID, amount)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun removeSpending(userId: String, amount: Double): Result<Unit> {
        return try {
            val budget = getCurrentMonthBudget(userId) ?: throw Exception("Budget not set for current month")
            budgetDao.removeSpending(budget.budgetID, amount)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun checkBudgetWarning(userId: String): Boolean {
        val budget = getCurrentMonthBudget(userId) ?: return false
        return budget.shouldSendWarning()
    }

    suspend fun checkBudgetExceeded(userId: String): Boolean {
        val budget = getCurrentMonthBudget(userId) ?: return false
        return budget.shouldSendCritical()
    }

    suspend fun markBudgetWarningAsSent(userId: String) {
        val budget = getCurrentMonthBudget(userId) ?: return
        budgetDao.markWarningAsSent(budget.budgetID)
    }

    suspend fun markBudgetExceededAsSent(userId: String) {
        val budget = getCurrentMonthBudget(userId) ?: return
        budgetDao.markCriticalAsSent(budget.budgetID)
    }

    fun getAllBudgets(userId: String): Flow<List<BudgetEntity>> = budgetDao.getAllBudgets(userId)

    private fun getCurrentMonth(): String {
        // Use SimpleDateFormat for clarity
        val sdf = SimpleDateFormat("yyyy-MM", Locale.getDefault())
        return sdf.format(Date())
    }
}
