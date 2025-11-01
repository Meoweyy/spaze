package com.sc2006.spaze.data.local.dao

import androidx.room.*
import com.sc2006.spaze.data.local.entity.MapLocationEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for Map Location operations
 */
@Dao
interface MapLocationDao {

    @Query("SELECT * FROM map_locations WHERE carparkNumber = :carparkNumber")
    suspend fun getMapLocationById(carparkNumber: String): MapLocationEntity?

    @Query("SELECT * FROM map_locations WHERE carparkNumber = :carparkNumber")
    fun getMapLocationByIdFlow(carparkNumber: String): Flow<MapLocationEntity?>

    @Query("SELECT * FROM map_locations WHERE isRouteActive = 1 LIMIT 1")
    suspend fun getActiveRoute(): MapLocationEntity?

    @Query("SELECT * FROM map_locations WHERE isRouteActive = 1 LIMIT 1")
    fun getActiveRouteFlow(): Flow<MapLocationEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMapLocation(mapLocation: MapLocationEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMapLocations(mapLocations: List<MapLocationEntity>)

    @Update
    suspend fun updateMapLocation(mapLocation: MapLocationEntity)

    @Delete
    suspend fun deleteMapLocation(mapLocation: MapLocationEntity)

    @Query("UPDATE map_locations SET isRouteActive = 0")
    suspend fun deactivateAllRoutes()

    @Query("UPDATE map_locations SET isRouteActive = 1 WHERE carparkNumber = :carparkNumber")
    suspend fun setActiveRoute(carparkNumber: String)

    @Query("DELETE FROM map_locations")
    suspend fun deleteAllMapLocations()

    @Query("DELETE FROM map_locations WHERE lastRouteUpdate < :timestamp")
    suspend fun deleteStaleRoutes(timestamp: Long)
}