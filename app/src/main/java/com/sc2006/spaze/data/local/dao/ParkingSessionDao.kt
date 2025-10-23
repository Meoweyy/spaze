package com.sc2006.spaze.data.local.dao

import androidx.room.*
import com.sc2006.spaze.data.local.entity.ParkingSessionEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for Parking Session operations
 */
@Dao
interface ParkingSessionDao {

    @Query("SELECT * FROM parking_sessions WHERE sessionID = :sessionId")
    suspend fun getSessionById(sessionId: String): ParkingSessionEntity?

    @Query("SELECT * FROM parking_sessions WHERE sessionID = :sessionId")
    fun getSessionByIdFlow(sessionId: String): Flow<ParkingSessionEntity?>

    @Query("SELECT * FROM parking_sessions WHERE userID = :userId AND isActive = 1 LIMIT 1")
    suspend fun getActiveSession(userId: String): ParkingSessionEntity?

    @Query("SELECT * FROM parking_sessions WHERE userID = :userId AND isActive = 1 LIMIT 1")
    fun getActiveSessionFlow(userId: String): Flow<ParkingSessionEntity?>

    @Query("SELECT * FROM parking_sessions WHERE userID = :userId AND isActive = 0 ORDER BY startTime DESC")
    fun getSessionHistory(userId: String): Flow<List<ParkingSessionEntity>>

    @Query("SELECT * FROM parking_sessions WHERE userID = :userId AND isActive = 0 ORDER BY startTime DESC LIMIT :limit")
    fun getRecentSessions(userId: String, limit: Int = 10): Flow<List<ParkingSessionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: ParkingSessionEntity)

    @Update
    suspend fun updateSession(session: ParkingSessionEntity)

    @Delete
    suspend fun deleteSession(session: ParkingSessionEntity)

    @Query("UPDATE parking_sessions SET isActive = 0, endTime = :endTime, actualCost = :actualCost WHERE sessionID = :sessionId")
    suspend fun endSession(sessionId: String, endTime: Long, actualCost: Double)

    @Query("UPDATE parking_sessions SET estimatedCost = :cost, updatedAt = :timestamp WHERE sessionID = :sessionId")
    suspend fun updateEstimatedCost(sessionId: String, cost: Double, timestamp: Long)

    @Query("UPDATE parking_sessions SET hasWarningBeenSent = 1 WHERE sessionID = :sessionId")
    suspend fun markWarningAsSent(sessionId: String)

    @Query("UPDATE parking_sessions SET hasExceededBeenSent = 1 WHERE sessionID = :sessionId")
    suspend fun markExceededAsSent(sessionId: String)

    @Query("DELETE FROM parking_sessions WHERE userID = :userId AND isActive = 0")
    suspend fun deleteSessionHistory(userId: String)

    @Query("DELETE FROM parking_sessions")
    suspend fun deleteAllSessions()
}
