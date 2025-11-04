package com.sc2006.spaze.data.repository

import android.content.Context
import android.util.Log
import com.sc2006.spaze.data.local.dao.CarparkDao
import com.sc2006.spaze.data.local.dao.FavoriteDao
import com.sc2006.spaze.data.local.dao.RecentSearchDao
import com.sc2006.spaze.data.local.entity.CarparkEntity
import com.sc2006.spaze.data.local.entity.FavoriteEntity
import com.sc2006.spaze.data.local.entity.RecentSearchEntity
import com.sc2006.spaze.data.remote.api.CarparkApiService
import com.sc2006.spaze.data.util.CsvParser
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Carpark Repository
 * Manages carpark data from CSV (static) and API (live availability)
 */
@Singleton
class CarparkRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val carparkDao: CarparkDao,
    private val favoriteDao: FavoriteDao,
    private val recentSearchDao: RecentSearchDao,
    private val carparkApi: CarparkApiService
) {

    companion object {
        private const val TAG = "CarparkRepository"
    }

    // ═══════════════════════════════════════════════════
    // INITIALIZATION - Load Static Data from CSV
    // ═══════════════════════════════════════════════════

    /**
     * Initialize database with carpark data from CSV
     * Call this once on first app launch
     */
    suspend fun initializeCarparksFromCsv(): Result<Int> {
        return try {
            // Check if already initialized
            val existingCount = carparkDao.getCarparkCount()
            if (existingCount > 0) {
                Log.d(TAG, "Database already initialized with $existingCount carparks")
                return Result.success(existingCount)
            }

            // Parse CSV file
            val csvDtos = CsvParser.parseCarparkCsv(context)

            // Convert to entities
            val entities = csvDtos.map { it.toEntity() }

            // Insert into database
            carparkDao.insertCarparks(entities)

            Log.d(TAG, "Initialized ${entities.size} carparks from CSV")
            Result.success(entities.size)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize carparks from CSV", e)
            Result.failure(e)
        }
    }

    // ═══════════════════════════════════════════════════
    // API REFRESH - Update Live Availability
    // ═══════════════════════════════════════════════════

    /**
     * Fetch latest availability from Data.gov.sg API
     * Updates only the lot availability fields, keeps static data intact
     */
    suspend fun refreshCarparkAvailability(): Result<Unit> {
        return try {
            // Step 1: Call API
            val response = carparkApi.getCarparkAvailability()

            if (!response.isSuccessful) {
                return Result.failure(Exception("API Error: ${response.code()}"))
            }

            // Step 2: Extract data
            val dataGovResponse = response.body()
                ?: return Result.failure(Exception("Empty response body"))

            if (dataGovResponse.items.isEmpty()) {
                return Result.failure(Exception("No availability data in response"))
            }

            // Step 3: Get latest timestamp data
            val latestItem = dataGovResponse.items.first()
            val timestamp = parseTimestamp(latestItem.timestamp)

            // Step 4: Process each carpark's availability
            var updatedCount = 0
            latestItem.carparkData.forEach { carparkData ->
                try {
                    // Get carpark number
                    val carparkNumber = carparkData.carparkNumber.trim()

                    // Check if carpark exists in database
                    val existing = carparkDao.getCarparkById(carparkNumber)
                    if (existing == null) {
                        Log.w(TAG, "Carpark $carparkNumber from API not found in database; inserting placeholder")
                        // Insert a minimal placeholder so DB contains all carparks referenced by API.
                        // Coordinates/metadata unknown here; will remain non-filterable until CSV provides them.
                        val placeholder = com.sc2006.spaze.data.local.entity.CarparkEntity(
                            carparkNumber = carparkNumber,
                            address = "",
                            xCoord = 0.0,
                            yCoord = 0.0,
                            latitude = 0.0,
                            longitude = 0.0,
                            carParkType = "",
                            typeOfParkingSystem = "",
                            shortTermParking = "",
                            freeParking = "",
                            nightParking = "",
                            carParkDecks = 0,
                            gantryHeight = 0.0,
                            carParkBasement = "N",
                            totalLotsC = 0,
                            availableLotsC = 0,
                            totalLotsH = 0,
                            availableLotsH = 0,
                            totalLotsY = 0,
                            availableLotsY = 0,
                            totalLotsS = 0,
                            availableLotsS = 0,
                            lastUpdated = 0
                        )
                        carparkDao.insertCarpark(placeholder)
                    }

                    // Parse lot availability by type
                    var totalC = 0
                    var availableC = 0
                    var totalH = 0
                    var availableH = 0
                    var totalY = 0
                    var availableY = 0
                    var totalS = 0
                    var availableS = 0

                    carparkData.carparkInfo.forEach { lotInfo ->
                        val total = lotInfo.totalLots.toIntOrNull() ?: 0
                        val available = lotInfo.lotsAvailable.toIntOrNull() ?: 0

                        when (lotInfo.lotType) {
                            "C" -> {
                                totalC = total
                                availableC = available
                            }
                            "H" -> {
                                totalH = total
                                availableH = available
                            }
                            "Y" -> {
                                totalY = total
                                availableY = available
                            }
                            "S" -> {
                                totalS = total
                                availableS = available
                            }
                        }
                    }

                    // Update database with new availability
                    carparkDao.updateAvailability(
                        carparkNumber = carparkNumber,
                        totalLotsC = totalC,
                        availableLotsC = availableC,
                        totalLotsH = totalH,
                        availableLotsH = availableH,
                        totalLotsY = totalY,
                        availableLotsY = availableY,
                        totalLotsS = totalS,
                        availableLotsS = availableS,
                        lastUpdated = timestamp
                    )

                    updatedCount++
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to update carpark ${carparkData.carparkNumber}", e)
                }
            }

            Log.d(TAG, "Updated availability for $updatedCount carparks")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to refresh carpark availability", e)
            Result.failure(e)
        }
    }

    /**
     * Parse ISO 8601 timestamp to milliseconds
     */
    private fun parseTimestamp(timestamp: String): Long {
        return try {
            // "2025-08-07T09:01:00+08:00"
            java.time.ZonedDateTime.parse(timestamp).toInstant().toEpochMilli()
        } catch (e: Exception) {
            System.currentTimeMillis()
        }
    }

    // ═══════════════════════════════════════════════════
    // READ OPERATIONS
    // ═══════════════════════════════════════════════════

    fun getAllCarparks(): Flow<List<CarparkEntity>> {
        return carparkDao.getAllCarparks()
    }

    suspend fun getCarparkById(carparkNumber: String): CarparkEntity? {
        return carparkDao.getCarparkById(carparkNumber)
    }

    fun searchCarparks(query: String): Flow<List<CarparkEntity>> {
        return carparkDao.searchCarparks(query)
    }

    fun getCarparksInBounds(
        minLat: Double,
        maxLat: Double,
        minLng: Double,
        maxLng: Double
    ): Flow<List<CarparkEntity>> {
        return carparkDao.getCarparksInBounds(minLat, maxLat, minLng, maxLng)
    }

    fun getAvailableCarparks(minLots: Int): Flow<List<CarparkEntity>> {
        return carparkDao.getAvailableCarparks(minLots)
    }

    /**
     * Get total count of carparks in database
     */
    suspend fun getCarparkCount(): Int {
        return carparkDao.getCarparkCount()
    }

    // ═══════════════════════════════════════════════════
    // FAVORITES
    // ═══════════════════════════════════════════════════

    fun getFavoriteCarparks(userId: String): Flow<List<CarparkEntity>> {
        return favoriteDao.getUserFavorites(userId).map { favorites ->
            favorites.mapNotNull { favorite ->
                carparkDao.getCarparkById(favorite.carparkID)
            }
        }
    }

    suspend fun addToFavorites(userId: String, carparkNumber: String): Result<Unit> {
        return try {
            val favorite = FavoriteEntity.create(userId, carparkNumber)
            favoriteDao.insertFavorite(favorite)
            carparkDao.updateFavoriteStatus(carparkNumber, true)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun removeFromFavorites(userId: String, carparkNumber: String): Result<Unit> {
        return try {
            favoriteDao.removeFavorite(userId, carparkNumber)
            carparkDao.updateFavoriteStatus(carparkNumber, false)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun isFavorite(userId: String, carparkNumber: String): Boolean {
        return favoriteDao.isFavorite(userId, carparkNumber)
    }

    // ═══════════════════════════════════════════════════
    // RECENT SEARCHES
    // ═══════════════════════════════════════════════════

    fun getRecentlyViewedCarparks(limit: Int = 10): Flow<List<CarparkEntity>> {
        return carparkDao.getRecentlyViewedCarparks(limit)
    }

    suspend fun markCarparkAsViewed(
        userId: String,
        carparkNumber: String,
        carparkName: String
    ) {
        carparkDao.updateLastViewed(carparkNumber, System.currentTimeMillis())
        val recentView = RecentSearchEntity.createCarparkView(userId, carparkNumber, carparkName)
        recentSearchDao.insertRecentSearch(recentView)
    }

    fun getRecentSearches(userId: String, limit: Int = 20): Flow<List<RecentSearchEntity>> {
        return recentSearchDao.getRecentSearches(userId, limit)
    }

    suspend fun addSearchToHistory(
        userId: String,
        query: String,
        searchType: RecentSearchEntity.SearchType
    ) {
        val search = RecentSearchEntity.createSearch(userId, query, searchType)
        recentSearchDao.insertRecentSearch(search)
    }

    suspend fun clearRecentSearches(userId: String) {
        recentSearchDao.clearRecentSearches(userId)
    }
}
