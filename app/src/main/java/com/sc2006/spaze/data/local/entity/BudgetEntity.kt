package com.sc2006.spaze.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Budget Entity - Tracks user's monthly parking budget
 */
@Entity(tableName = "budgets")
data class BudgetEntity(
    @PrimaryKey
    val budgetID: String,
    val userID: String,

    // Budget settings
    val monthlyBudget: Double,
    val currentMonthSpending: Double = 0.0,
    val currency: String = "SGD",

    // Notification thresholds (fractions: 0.8 => 80%)
    val warningThreshold: Double = 0.8,
    val criticalThreshold: Double = 1.0,

    // Tracking
    val budgetMonth: String, // Format: "YYYY-MM"
    val lastUpdated: Long = System.currentTimeMillis(),

    // Notification flags
    val hasWarningBeenSent: Boolean = false,
    val hasCriticalBeenSent: Boolean = false
) {

    fun getRemainingBudget(): Double = monthlyBudget - currentMonthSpending

    fun getBudgetUsagePercentage(): Double =
        if (monthlyBudget > 0) (currentMonthSpending / monthlyBudget) * 100 else 0.0

    fun isBudgetExceeded(): Boolean = currentMonthSpending >= monthlyBudget

    fun shouldSendWarning(): Boolean {
        if (hasWarningBeenSent) return false
        return currentMonthSpending >= (monthlyBudget * warningThreshold)
    }

    fun shouldSendCritical(): Boolean {
        if (hasCriticalBeenSent) return false
        return currentMonthSpending >= (monthlyBudget * criticalThreshold)
    }

    fun addSpending(amount: Double): BudgetEntity =
        copy(currentMonthSpending = currentMonthSpending + amount, lastUpdated = System.currentTimeMillis())

    fun removeSpending(amount: Double): BudgetEntity =
        copy(currentMonthSpending = maxOf(0.0, currentMonthSpending - amount), lastUpdated = System.currentTimeMillis())

    fun updateMonthlyBudget(newBudget: Double): BudgetEntity =
        copy(monthlyBudget = newBudget, lastUpdated = System.currentTimeMillis())

    fun getFormattedSummary(): String =
        String.format("%s %.2f / %.2f (%.1f%%)", currency, currentMonthSpending, monthlyBudget, getBudgetUsagePercentage())

    enum class BudgetStatus { HEALTHY, MODERATE, WARNING, EXCEEDED }

    companion object {
        fun create(userID: String, monthlyBudget: Double): BudgetEntity {
            val currentMonth = getCurrentMonth()
            return BudgetEntity(
                budgetID = "${userID}_$currentMonth",
                userID = userID,
                monthlyBudget = monthlyBudget,
                budgetMonth = currentMonth
            )
        }

        private fun getCurrentMonth(): String {
            val calendar = java.util.Calendar.getInstance()
            val year = calendar.get(java.util.Calendar.YEAR)
            val month = calendar.get(java.util.Calendar.MONTH) + 1
            return String.format("%04d-%02d", year, month)
        }
    }
}
