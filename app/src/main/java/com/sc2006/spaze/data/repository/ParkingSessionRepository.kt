package com.sc2006.spaze.data.repository

import com.sc2006.spaze.data.local.dao.ParkingSessionDao
import com.sc2006.spaze.data.local.entity.ParkingSessionEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Parking Session Repository
 * Manages active parking sessions and session history
 * Implements: Parking Session Tracking
 */
@Singleton
class ParkingSessionRepository @Inject constructor(
    private val sessionDao: ParkingSessionDao
) {

    /**
     * Get active parking session for user
     */
    suspend fun getActiveSession(userId: String): ParkingSessionEntity? {
        return sessionDao.getActiveSession(userId)
    }

    /**
     * Get active session as Flow
     */
    fun getActiveSessionFlow(userId: String): Flow<ParkingSessionEntity?> {
        return sessionDao.getActiveSessionFlow(userId)
    }

    /**
     * Start a new parking session
     */
    suspend fun startSession(
        userId: String,
        carparkId: String,
        carparkName: String,
        carparkAddress: String,
        perSessionBudgetCap: Double?,
        isAutoStarted: Boolean = false
    ): Result<ParkingSessionEntity> {
        return try {
            // Check if there's already an active session
            val activeSession = getActiveSession(userId)
            if (activeSession != null) {
                return Result.failure(Exception("Active session already exists"))
            }

            val session = ParkingSessionEntity.create(
                userID = userId,
                carparkID = carparkId,
                carparkName = carparkName,
                carparkAddress = carparkAddress,
                perSessionBudgetCap = perSessionBudgetCap,
                isAutoStarted = isAutoStarted
            )

            sessionDao.insertSession(session)
            Result.success(session)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Update estimated cost for active session
     */
    suspend fun updateEstimatedCost(sessionId: String, cost: Double): Result<Unit> {
        return try {
            sessionDao.updateEstimatedCost(sessionId, cost, System.currentTimeMillis())
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * End parking session
     */
    suspend fun endSession(sessionId: String, finalCost: Double): Result<Unit> {
        return try {
            sessionDao.endSession(sessionId, System.currentTimeMillis(), finalCost)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Get session history
     */
    fun getSessionHistory(userId: String): Flow<List<ParkingSessionEntity>> {
        return sessionDao.getSessionHistory(userId)
    }

    /**
     * Get recent sessions
     */
    fun getRecentSessions(userId: String, limit: Int = 10): Flow<List<ParkingSessionEntity>> {
        return sessionDao.getRecentSessions(userId, limit)
    }

    /**
     * Mark budget warning as sent
     */
    suspend fun markBudgetWarningAsSent(sessionId: String) {
        sessionDao.markWarningAsSent(sessionId)
    }

    /**
     * Mark budget exceeded notification as sent
     */
    suspend fun markBudgetExceededAsSent(sessionId: String) {
        sessionDao.markExceededAsSent(sessionId)
    }

    /**
     * Delete session history
     */
    suspend fun deleteSessionHistory(userId: String): Result<Unit> {
        return try {
            sessionDao.deleteSessionHistory(userId)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Calculate estimated cost based on elapsed time and pricing
     * Simple implementation - should be enhanced with actual pricing rules
     */
    fun calculateEstimatedCost(
        session: ParkingSessionEntity,
        costPerHour: Double = 2.0 // Default SGD per hour
    ): Double {
        val elapsedHours = session.getElapsedTimeMinutes() / 60.0
        return elapsedHours * costPerHour
    }
}
