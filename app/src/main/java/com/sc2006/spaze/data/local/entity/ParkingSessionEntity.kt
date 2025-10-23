package com.sc2006.spaze.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Parking Session Entity - Tracks active and historical parking sessions
 * Supports the Parking Session Tracking functional requirements
 */
@Entity(tableName = "parking_sessions")
data class ParkingSessionEntity(
    @PrimaryKey
    val sessionID: String,
    val userID: String,
    val carparkID: String,
    val carparkName: String,
    val carparkAddress: String,

    // Session timing
    val startTime: Long,
    val endTime: Long? = null,
    val isActive: Boolean = true,

    // Cost tracking
    val estimatedCost: Double = 0.0,
    val actualCost: Double? = null,
    val perSessionBudgetCap: Double? = null,

    // Notifications
    val budgetWarningThreshold: Double = 0.8, // 80%
    val hasWarningBeenSent: Boolean = false,
    val hasExceededBeenSent: Boolean = false,

    // Auto-tracking
    val isAutoStarted: Boolean = false,

    // Metadata
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    /**
     * Get elapsed time in milliseconds
     */
    fun getElapsedTimeMillis(): Long {
        val end = endTime ?: System.currentTimeMillis()
        return end - startTime
    }

    /**
     * Get elapsed time in minutes
     */
    fun getElapsedTimeMinutes(): Int {
        return (getElapsedTimeMillis() / (60 * 1000)).toInt()
    }

    /**
     * Get elapsed time formatted as HH:MM:SS
     */
    fun getElapsedTimeFormatted(): String {
        val totalSeconds = getElapsedTimeMillis() / 1000
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60
        return String.format("%02d:%02d:%02d", hours, minutes, seconds)
    }

    /**
     * Check if budget warning threshold reached
     */
    fun shouldSendBudgetWarning(): Boolean {
        if (hasWarningBeenSent || perSessionBudgetCap == null) return false
        return estimatedCost >= (perSessionBudgetCap * budgetWarningThreshold)
    }

    /**
     * Check if budget exceeded
     */
    fun isBudgetExceeded(): Boolean {
        if (perSessionBudgetCap == null) return false
        return estimatedCost >= perSessionBudgetCap
    }

    /**
     * Get remaining budget
     */
    fun getRemainingBudget(): Double? {
        return perSessionBudgetCap?.let { it - estimatedCost }
    }

    /**
     * Get remaining time before budget exceeded (in minutes)
     * Returns null if no cap or cost per minute is 0
     */
    fun getRemainingTimeMinutes(costPerMinute: Double): Int? {
        if (perSessionBudgetCap == null || costPerMinute <= 0) return null
        val remaining = getRemainingBudget() ?: return null
        if (remaining <= 0) return 0
        return (remaining / costPerMinute).toInt()
    }

    /**
     * Update estimated cost
     */
    fun updateEstimatedCost(newCost: Double): ParkingSessionEntity {
        return this.copy(
            estimatedCost = newCost,
            updatedAt = System.currentTimeMillis()
        )
    }

    /**
     * End the session
     */
    fun endSession(finalCost: Double): ParkingSessionEntity {
        return this.copy(
            endTime = System.currentTimeMillis(),
            isActive = false,
            actualCost = finalCost,
            updatedAt = System.currentTimeMillis()
        )
    }

    companion object {
        /**
         * Create a new parking session
         */
        fun create(
            userID: String,
            carparkID: String,
            carparkName: String,
            carparkAddress: String,
            perSessionBudgetCap: Double?,
            isAutoStarted: Boolean = false
        ): ParkingSessionEntity {
            return ParkingSessionEntity(
                sessionID = "${userID}_${System.currentTimeMillis()}",
                userID = userID,
                carparkID = carparkID,
                carparkName = carparkName,
                carparkAddress = carparkAddress,
                startTime = System.currentTimeMillis(),
                perSessionBudgetCap = perSessionBudgetCap,
                isAutoStarted = isAutoStarted
            )
        }
    }
}
