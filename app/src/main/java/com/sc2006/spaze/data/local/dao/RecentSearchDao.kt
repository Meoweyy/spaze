package com.sc2006.spaze.data.local.dao

import androidx.room.*
import com.sc2006.spaze.data.local.entity.RecentSearchEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for Recent Search operations
 */
@Dao
interface RecentSearchDao {

    @Query("SELECT * FROM recent_searches WHERE userID = :userId ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentSearches(userId: String, limit: Int = 20): Flow<List<RecentSearchEntity>>

    @Query("SELECT * FROM recent_searches WHERE userID = :userId AND searchType = 'CARPARK_VIEW' ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentlyViewedCarparks(userId: String, limit: Int = 10): Flow<List<RecentSearchEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecentSearch(search: RecentSearchEntity)

    @Delete
    suspend fun deleteRecentSearch(search: RecentSearchEntity)

    @Query("DELETE FROM recent_searches WHERE userID = :userId")
    suspend fun clearRecentSearches(userId: String)

    @Query("DELETE FROM recent_searches WHERE userID = :userId AND searchType = 'CARPARK_VIEW'")
    suspend fun clearRecentlyViewedCarparks(userId: String)

    @Query("DELETE FROM recent_searches WHERE timestamp < :timestamp")
    suspend fun deleteOldSearches(timestamp: Long)
}
