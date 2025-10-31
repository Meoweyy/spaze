package com.sc2006.spaze.data.repository

import android.util.Log
import com.sc2006.spaze.data.local.dao.CarparkDao
import com.sc2006.spaze.data.local.dao.FavoriteDao
import com.sc2006.spaze.data.local.dao.RecentSearchDao
import com.sc2006.spaze.data.local.entity.CarparkEntity
import com.sc2006.spaze.data.local.entity.FavoriteEntity
import com.sc2006.spaze.data.local.entity.RecentSearchEntity
import com.sc2006.spaze.data.remote.api.CarparkApiService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Carpark Repository
 * Manages carpark data from API and local database
 * Implements: Carpark Discovery, Live Availability, Filters & Sorting
 */
@Singleton
class CarparkRepository @Inject constructor(
    private val carparkDao: CarparkDao,
    private val favoriteDao: FavoriteDao,
    private val recentSearchDao: RecentSearchDao,
    private val carparkApi: CarparkApiService,
    // Note: LTA API key should be injected via BuildConfig or secrets
    private val ltaApiKey: String = "YOUR_LTA_API_KEY"
) {

    companion object {
        private const val TAG = "CarparkRepository"
    }

    private val samplePlaces = listOf(
        PlaceSuggestion("Marina Bay Sands", 1.2834, 103.8607),
        PlaceSuggestion("Changi Airport", 1.3644, 103.9915),
        PlaceSuggestion("VivoCity", 1.2644, 103.8223),
        PlaceSuggestion("Jurong Point", 1.3391, 103.7061),
        PlaceSuggestion("Causeway Point", 1.4353, 103.7853),
        PlaceSuggestion("Bukit Panjang Plaza", 1.3787, 103.7639),
        PlaceSuggestion("ION Orchard", 1.3040, 103.8325),
        PlaceSuggestion("Bugis Junction", 1.3008, 103.8566),
        PlaceSuggestion("Waterway Point", 1.4065, 103.9022)
    )

    /**
     * Get all carparks
     */
    fun getAllCarparks(): Flow<List<CarparkEntity>> {
        return carparkDao.getAllCarparks()
    }

    /**
     * Get carpark by ID
     */
    suspend fun getCarparkById(carparkId: String): CarparkEntity? {
        return carparkDao.getCarparkById(carparkId)
    }

    /**
     * Fetch and refresh carpark availability from API
     * Implements: Live Availability & Data Refresh
     */
    suspend fun refreshCarparkAvailability(): Result<Unit> {
        return try {
            val response = carparkApi.getCarparkAvailability(ltaApiKey)

            if (response.isSuccessful) {
                val carparks = response.body()?.carparks ?: emptyList()

                // Convert API response to entities and save to database
                val carparkEntities = carparks.map { dto ->
                    CarparkEntity(
                        carparkID = dto.carparkID,
                        location = dto.location,
                        address = dto.development,
                        latitude = parseLatitude(dto.location),
                        longitude = parseLongitude(dto.location),
                        totalLots = 0, // Not provided by API, needs separate source
                        availableLots = dto.availableLots,
                        lotTypes = dto.lotType,
                        dataSource = dto.agency,
                        lastUpdated = System.currentTimeMillis()
                    )
                }

                carparkDao.insertCarparks(carparkEntities)
                Result.success(Unit)
            } else {
                Result.failure(Exception("API Error: ${response.code()}"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to refresh carpark availability", e)
            Result.failure(e)
        }
    }

    /**
     * Search carparks
     */
    fun searchCarparks(query: String): Flow<List<CarparkEntity>> {
        return carparkDao.searchCarparks(query)
    }

    fun searchSuggestions(query: String, limit: Int = 5): Flow<List<PlaceSuggestion>> {
        if (query.isBlank()) return flowOf(emptyList())

        return carparkDao.searchCarparks(query).map { carparkMatches ->
            val carparkResults = carparkMatches.take(limit).map { carpark ->
                PlaceSuggestion(
                    name = carpark.address,
                    latitude = carpark.latitude,
                    longitude = carpark.longitude,
                    carparkId = carpark.carparkID
                )
            }

            val remainingSlots = limit - carparkResults.size
            val sampleResults = if (remainingSlots > 0) {
                samplePlaces.filter { it.name.contains(query, ignoreCase = true) }
                    .take(remainingSlots)
            } else {
                emptyList()
            }

            (carparkResults + sampleResults).distinctBy { it.name }
        }
    }

    /**
     * Get carparks within map bounds
     */
    fun getCarparksInBounds(
        minLat: Double,
        maxLat: Double,
        minLng: Double,
        maxLng: Double
    ): Flow<List<CarparkEntity>> {
        return carparkDao.getCarparksInBounds(minLat, maxLat, minLng, maxLng)
    }

    /**
     * Filter carparks by availability
     * Implements: Filters & Sorting
     */
    fun getAvailableCarparks(minLots: Int): Flow<List<CarparkEntity>> {
        return carparkDao.getAvailableCarparks(minLots)
    }

    /**
     * Get favorite carparks for user
     */
    fun getFavoriteCarparks(userId: String): Flow<List<CarparkEntity>> {
        return favoriteDao.getUserFavorites(userId).map { favorites ->
            favorites.mapNotNull { favorite ->
                carparkDao.getCarparkById(favorite.carparkID)
            }
        }
    }

    /**
     * Add carpark to favorites
     */
    suspend fun addToFavorites(userId: String, carparkId: String): Result<Unit> {
        return try {
            val favorite = FavoriteEntity.create(userId, carparkId)
            favoriteDao.insertFavorite(favorite)
            carparkDao.updateFavoriteStatus(carparkId, true)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Remove carpark from favorites
     */
    suspend fun removeFromFavorites(userId: String, carparkId: String): Result<Unit> {
        return try {
            favoriteDao.removeFavorite(userId, carparkId)
            carparkDao.updateFavoriteStatus(carparkId, false)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Check if carpark is favorite
     */
    suspend fun isFavorite(userId: String, carparkId: String): Boolean {
        return favoriteDao.isFavorite(userId, carparkId)
    }

    /**
     * Get recently viewed carparks
     */
    fun getRecentlyViewedCarparks(limit: Int = 10): Flow<List<CarparkEntity>> {
        return carparkDao.getRecentlyViewedCarparks(limit)
    }

    /**
     * Mark carpark as viewed
     */
    suspend fun markCarparkAsViewed(userId: String, carparkId: String, carparkName: String) {
        carparkDao.updateLastViewed(carparkId, System.currentTimeMillis())
        val recentView = RecentSearchEntity.createCarparkView(userId, carparkId, carparkName)
        recentSearchDao.insertRecentSearch(recentView)
    }

    /**
     * Get recent searches
     */
    fun getRecentSearches(userId: String, limit: Int = 20): Flow<List<RecentSearchEntity>> {
        return recentSearchDao.getRecentSearches(userId, limit)
    }

    /**
     * Add search to history
     */
    suspend fun addSearchToHistory(
        userId: String,
        query: String,
        searchType: RecentSearchEntity.SearchType
    ) {
        val search = RecentSearchEntity.createSearch(userId, query, searchType)
        recentSearchDao.insertRecentSearch(search)
    }

    /**
     * Clear recent searches
     */
    suspend fun clearRecentSearches(userId: String) {
        recentSearchDao.clearRecentSearches(userId)
    }

    /**
     * Parse latitude from location string
     * Location format: "1.234 5.678" (lat lng)
     */
    private fun parseLatitude(location: String): Double {
        return try {
            location.trim().split(" ")[0].toDouble()
        } catch (e: Exception) {
            1.3521 // Default to Singapore
        }
    }

    /**
     * Parse longitude from location string
     */
    private fun parseLongitude(location: String): Double {
        return try {
            location.trim().split(" ")[1].toDouble()
        } catch (e: Exception) {
            103.8198 // Default to Singapore
        }
    }
}

data class PlaceSuggestion(
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val carparkId: String? = null
)
