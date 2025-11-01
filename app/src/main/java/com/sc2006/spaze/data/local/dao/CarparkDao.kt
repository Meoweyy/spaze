package com.sc2006.spaze.data.local.dao

import androidx.room.*
import com.sc2006.spaze.data.local.entity.CarparkEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CarparkDao {

    // ═══════════════════════════════════════════════════
    // READ OPERATIONS
    // ═══════════════════════════════════════════════════

    @Query("SELECT * FROM carparks")
    fun getAllCarparks(): Flow<List<CarparkEntity>>

    @Query("SELECT * FROM carparks WHERE carparkNumber = :carparkNumber")
    suspend fun getCarparkById(carparkNumber: String): CarparkEntity?

    @Query("SELECT * FROM carparks WHERE carparkNumber = :carparkNumber")
    fun getCarparkByIdFlow(carparkNumber: String): Flow<CarparkEntity?>

    @Query("SELECT * FROM carparks WHERE isFavorite = 1")
    fun getFavoriteCarparks(): Flow<List<CarparkEntity>>

    @Query("SELECT * FROM carparks WHERE availableLotsC >= :minLots")
    fun getAvailableCarparks(minLots: Int): Flow<List<CarparkEntity>>

    @Query("SELECT * FROM carparks WHERE lastViewed IS NOT NULL ORDER BY lastViewed DESC LIMIT :limit")
    fun getRecentlyViewedCarparks(limit: Int = 10): Flow<List<CarparkEntity>>

    @Query("SELECT COUNT(*) FROM carparks")
    suspend fun getCarparkCount(): Int

    // ═══════════════════════════════════════════════════
    // SEARCH
    // ═══════════════════════════════════════════════════

    @Query("""
        SELECT * FROM carparks 
        WHERE address LIKE '%' || :query || '%' 
        OR carparkNumber LIKE '%' || :query || '%'
    """)
    fun searchCarparks(query: String): Flow<List<CarparkEntity>>

    @Query("""
        SELECT * FROM carparks
        WHERE latitude BETWEEN :minLat AND :maxLat
        AND longitude BETWEEN :minLng AND :maxLng
    """)
    fun getCarparksInBounds(
        minLat: Double,
        maxLat: Double,
        minLng: Double,
        maxLng: Double
    ): Flow<List<CarparkEntity>>

    // ═══════════════════════════════════════════════════
    // WRITE OPERATIONS
    // ═══════════════════════════════════════════════════

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCarpark(carpark: CarparkEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCarparks(carparks: List<CarparkEntity>)

    @Update
    suspend fun updateCarpark(carpark: CarparkEntity)

    @Delete
    suspend fun deleteCarpark(carpark: CarparkEntity)

    // ═══════════════════════════════════════════════════
    // PARTIAL UPDATES
    // ═══════════════════════════════════════════════════

    @Query("UPDATE carparks SET isFavorite = :isFavorite WHERE carparkNumber = :carparkNumber")
    suspend fun updateFavoriteStatus(carparkNumber: String, isFavorite: Boolean)

    @Query("UPDATE carparks SET lastViewed = :timestamp WHERE carparkNumber = :carparkNumber")
    suspend fun updateLastViewed(carparkNumber: String, timestamp: Long)

    /**
     * Update live availability data (from API)
     */
    @Query("""
        UPDATE carparks 
        SET totalLotsC = :totalLotsC,
            availableLotsC = :availableLotsC,
            totalLotsH = :totalLotsH,
            availableLotsH = :availableLotsH,
            totalLotsY = :totalLotsY,
            availableLotsY = :availableLotsY,
            totalLotsS = :totalLotsS,
            availableLotsS = :availableLotsS,
            lastUpdated = :lastUpdated
        WHERE carparkNumber = :carparkNumber
    """)
    suspend fun updateAvailability(
        carparkNumber: String,
        totalLotsC: Int,
        availableLotsC: Int,
        totalLotsH: Int,
        availableLotsH: Int,
        totalLotsY: Int,
        availableLotsY: Int,
        totalLotsS: Int,
        availableLotsS: Int,
        lastUpdated: Long
    )

    @Query("DELETE FROM carparks")
    suspend fun deleteAllCarparks()

    @Query("DELETE FROM carparks WHERE lastUpdated < :timestamp")
    suspend fun deleteStaleCarparks(timestamp: Long)
}