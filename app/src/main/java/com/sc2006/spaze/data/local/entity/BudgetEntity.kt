package com.sc2006.spaze.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Budget Entity - Tracks user's monthly parking budget
 * Supports the Budgeting and Cost Controls functional requirements
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

    // Notification thresholds
    val warningThreshold: Double = 0.8, // 80%
    val criticalThreshold: Double = 1.0, // 100%

    // Tracking
    val budgetMonth: String, // Format: "YYYY-MM"
    val lastUpdated: Long = System.currentTimeMillis(),

    // Notification flags
    val hasWarningBeenSent: Boolean = false,
    val hasCriticalBeenSent: Boolean = false
) {
    /**
     * Get remaining budget
     */
    fun getRemainingBudget(): Double {
        return monthlyBudget - currentMonthSpending
    }

    /**
     * Get budget usage percentage
     */
    fun getBudgetUsagePercentage(): Double {
        return if (monthlyBudget > 0) {
            (currentMonthSpending / monthlyBudget) * 100
        } else {
            0.0
        }
    }

    /**
     * Check if budget is exceeded
     */
    fun isBudgetExceeded(): Boolean {
        return currentMonthSpending >= monthlyBudget
    }

    /**
     * Check if warning threshold reached
     */
    fun shouldSendWarning(): Boolean {
        if (hasWarningBeenSent) return false
        return currentMonthSpending >= (monthlyBudget * warningThreshold)
    }

    /**
     * Check if critical threshold reached
     */
    fun shouldSendCritical(): Boolean {
        if (hasCriticalBeenSent) return false
        return currentMonthSpending >= (monthlyBudget * criticalThreshold)
    }

    /**
     * Add spending to current month
     */
    fun addSpending(amount: Double): BudgetEntity {
        return this.copy(
            currentMonthSpending = currentMonthSpending + amount,
            lastUpdated = System.currentTimeMillis()
        )
    }

    /**
     * Remove spending from current month
     */
    fun removeSpending(amount: Double): BudgetEntity {
        return this.copy(
            currentMonthSpending = maxOf(0.0, currentMonthSpending - amount),
            lastUpdated = System.currentTimeMillis()
        )
    }

    /**
     * Update monthly budget
     */
    fun updateMonthlyBudget(newBudget: Double): BudgetEntity {
        return this.copy(
            monthlyBudget = newBudget,
            lastUpdated = System.currentTimeMillis()
        )
    }

    /**
     * Get status based on spending
     */
    fun getBudgetStatus(): BudgetStatus {
        val percentage = getBudgetUsagePercentage()
        return when {
            percentage >= 100 -> BudgetStatus.EXCEEDED
            percentage >= 80 -> BudgetStatus.WARNING
            percentage >= 50 -> BudgetStatus.MODERATE
            else -> BudgetStatus.HEALTHY
        }
    }

    /**
     * Get formatted budget summary
     */
    fun getFormattedSummary(): String {
        return String.format(
            "%s %.2f / %.2f (%.1f%%)",
            currency,
            currentMonthSpending,
            monthlyBudget,
            getBudgetUsagePercentage()
        )
    }

    enum class BudgetStatus {
        HEALTHY,
        MODERATE,
        WARNING,
        EXCEEDED
    }

    companion object {
        /**
         * Create a new budget for the current month
         */
        fun create(
            userID: String,
            monthlyBudget: Double
        ): BudgetEntity {
            val currentMonth = getCurrentMonth()
            return BudgetEntity(
                budgetID = "${userID}_$currentMonth",
                userID = userID,
                monthlyBudget = monthlyBudget,
                budgetMonth = currentMonth
            )
        }

        /**
         * Get current month in YYYY-MM format
         */
        private fun getCurrentMonth(): String {
            val calendar = java.util.Calendar.getInstance()
            val year = calendar.get(java.util.Calendar.YEAR)
            val month = calendar.get(java.util.Calendar.MONTH) + 1
            return String.format("%04d-%02d", year, month)
        }
    }
}
