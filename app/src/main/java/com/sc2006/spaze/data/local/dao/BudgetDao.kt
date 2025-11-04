package com.sc2006.spaze.data.local.dao

import androidx.room.*
import com.sc2006.spaze.data.local.entity.BudgetEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for Budget operations
 */
@Dao
interface BudgetDao {

    @Query("SELECT * FROM budgets WHERE budgetID = :budgetId")
    suspend fun getBudgetById(budgetId: String): BudgetEntity?

    @Query("SELECT * FROM budgets WHERE budgetID = :budgetId")
    fun getBudgetByIdFlow(budgetId: String): Flow<BudgetEntity?>

    @Query("SELECT * FROM budgets WHERE userID = :userId AND budgetMonth = :month LIMIT 1")
    suspend fun getBudgetForMonth(userId: String, month: String): BudgetEntity?

    @Query("SELECT * FROM budgets WHERE userID = :userId AND budgetMonth = :month LIMIT 1")
    fun getBudgetForMonthFlow(userId: String, month: String): Flow<BudgetEntity?>

    @Query("SELECT * FROM budgets WHERE userID = :userId ORDER BY budgetMonth DESC")
    fun getAllBudgets(userId: String): Flow<List<BudgetEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBudget(budget: BudgetEntity)

    @Update
    suspend fun updateBudget(budget: BudgetEntity)

    @Delete
    suspend fun deleteBudget(budget: BudgetEntity)

    @Query("UPDATE budgets SET monthlyBudget = :amount, lastUpdated = :now WHERE budgetID = :budgetId")
    suspend fun updateMonthlyBudget(budgetId: String, amount: Double, now: Long = System.currentTimeMillis())

    @Query("UPDATE budgets SET currentMonthSpending = currentMonthSpending + :amount, lastUpdated = :now WHERE budgetID = :budgetId")
    suspend fun addSpending(budgetId: String, amount: Double, now: Long = System.currentTimeMillis())

    @Query("""
        UPDATE budgets 
        SET currentMonthSpending = CASE WHEN currentMonthSpending - :amount < 0 THEN 0 ELSE currentMonthSpending - :amount END,
            lastUpdated = :now
        WHERE budgetID = :budgetId
    """)
    suspend fun removeSpending(budgetId: String, amount: Double, now: Long = System.currentTimeMillis())

    @Query("UPDATE budgets SET hasWarningBeenSent = 1 WHERE budgetID = :budgetId")
    suspend fun markWarningAsSent(budgetId: String)

    @Query("UPDATE budgets SET hasCriticalBeenSent = 1 WHERE budgetID = :budgetId")
    suspend fun markCriticalAsSent(budgetId: String)

    @Query("DELETE FROM budgets WHERE userID = :userId")
    suspend fun deleteAllBudgets(userId: String)

    // NEW: reset current month spending + clear warning flags
    @Query("""
        UPDATE budgets 
        SET currentMonthSpending = 0.0, 
            hasWarningBeenSent = 0, 
            hasCriticalBeenSent = 0,
            lastUpdated = :now
        WHERE budgetID = :budgetId
    """)
    suspend fun resetSpending(budgetId: String, now: Long = System.currentTimeMillis())
}
