package com.sc2006.spaze.data.local.dao

import androidx.room.*
import com.sc2006.spaze.data.local.entity.CarparkEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for Carpark operations
 */
@Dao
interface CarparkDao {

    @Query("SELECT * FROM carparks")
    fun getAllCarparks(): Flow<List<CarparkEntity>>

    @Query("SELECT * FROM carparks WHERE carparkID = :carparkId")
    suspend fun getCarparkById(carparkId: String): CarparkEntity?

    @Query("SELECT * FROM carparks WHERE carparkID = :carparkId")
    fun getCarparkByIdFlow(carparkId: String): Flow<CarparkEntity?>

    @Query("SELECT * FROM carparks WHERE isFavorite = 1")
    fun getFavoriteCarparks(): Flow<List<CarparkEntity>>

    @Query("SELECT * FROM carparks WHERE availableLots >= :minLots")
    fun getAvailableCarparks(minLots: Int): Flow<List<CarparkEntity>>

    @Query("SELECT * FROM carparks WHERE lastViewed IS NOT NULL ORDER BY lastViewed DESC LIMIT :limit")
    fun getRecentlyViewedCarparks(limit: Int = 10): Flow<List<CarparkEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCarpark(carpark: CarparkEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCarparks(carparks: List<CarparkEntity>)

    @Update
    suspend fun updateCarpark(carpark: CarparkEntity)

    @Delete
    suspend fun deleteCarpark(carpark: CarparkEntity)

    @Query("UPDATE carparks SET isFavorite = :isFavorite WHERE carparkID = :carparkId")
    suspend fun updateFavoriteStatus(carparkId: String, isFavorite: Boolean)

    @Query("UPDATE carparks SET lastViewed = :timestamp WHERE carparkID = :carparkId")
    suspend fun updateLastViewed(carparkId: String, timestamp: Long)

    @Query("UPDATE carparks SET availableLots = :availableLots, lastUpdated = :timestamp WHERE carparkID = :carparkId")
    suspend fun updateAvailability(carparkId: String, availableLots: Int, timestamp: Long)

    @Query("DELETE FROM carparks")
    suspend fun deleteAllCarparks()

    @Query("DELETE FROM carparks WHERE lastUpdated < :timestamp")
    suspend fun deleteStaleCarparks(timestamp: Long)

    /**
     * Search carparks by location or address
     */
    @Query("SELECT * FROM carparks WHERE location LIKE '%' || :query || '%' OR address LIKE '%' || :query || '%'")
    fun searchCarparks(query: String): Flow<List<CarparkEntity>>

    /**
     * Get carparks within a bounding box
     */
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
}
