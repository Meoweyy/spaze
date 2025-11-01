package com.sc2006.spaze.data.repository

import android.util.Log
import com.sc2006.spaze.data.local.entity.CarparkEntity
import com.sc2006.spaze.data.local.entity.CarparkEntity.PriceTier
import com.sc2006.spaze.data.local.seed.CarparkMetadataProvider
import com.sc2006.spaze.data.api.RetrofitClient
import com.sc2006.spaze.data.local.dao.CarparkDao
import com.sc2006.spaze.data.local.dao.FavoriteDao
import com.sc2006.spaze.data.local.dao.RecentSearchDao
import com.sc2006.spaze.data.local.entity.FavoriteEntity
import com.sc2006.spaze.data.local.entity.RecentSearchEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

data class CarparkLotAvailability(
    val lotType: String,
    val totalLots: Int,
    val availableLots: Int
)

data class CarparkDetail(
    val carparkNumber: String,
    val name: String,
    val address: String,
    val latitude: Double?,
    val longitude: Double?,
    val lotAvailability: List<CarparkLotAvailability>,
    val totalLots: Int,
    val availableLots: Int,
    val lastUpdated: Long,
    val lastUpdatedRaw: String?,
    val priceTier: PriceTier,
    val baseHourlyRate: Double?
)

/**
 * Carpark Repository
 * Manages carpark data from API and local database
 * Implements: Carpark Discovery, Live Availability, Filters & Sorting
 */
@Singleton
class CarparkRepository @Inject constructor(
    private val carparkDao: CarparkDao,
    private val favoriteDao: FavoriteDao,
    private val recentSearchDao: RecentSearchDao
) {

    companion object {
        private const val TAG = "CarparkRepository"
        private val UPDATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME
    }

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
    suspend fun refreshCarparkAvailability(): Result<Int> {
        return withContext(Dispatchers.IO) {
            try {
                val response = RetrofitClient.instance.getCarparkAvailability().execute()

                if (response.isSuccessful) {
                    val carparks = response.body()?.items?.firstOrNull()?.carpark_data ?: emptyList()
                    val carparkEntities = mutableListOf<CarparkEntity>()

                    for (info in carparks) {
                        val totalLots = info.carpark_info.sumOf { it.total_lots.toIntOrNull() ?: 0 }
                        val availableLots = info.carpark_info.sumOf { it.lots_available.toIntOrNull() ?: 0 }
                        val metadata = CarparkMetadataProvider.getMetadata(info.carpark_number)

                        val existing = carparkDao.getCarparkById(info.carpark_number)
                        val entity = if (existing != null) {
                            existing.copy(
                                location = metadata?.name ?: existing.location,
                                totalLots = metadata?.totalLots ?: if (totalLots > 0) totalLots else existing.totalLots,
                                availableLots = availableLots,
                                latitude = metadata?.latitude ?: existing.latitude,
                                longitude = metadata?.longitude ?: existing.longitude,
                                address = metadata?.address ?: existing.address,
                                dataSource = "data.gov.sg",
                                priceTier = metadata?.priceTier?.name ?: existing.priceTier,
                                baseHourlyRate = metadata?.baseHourlyRate ?: existing.baseHourlyRate,
                                lastUpdated = System.currentTimeMillis()
                            )
                        } else {
                            CarparkEntity(
                                carparkID = info.carpark_number,
                                location = metadata?.name ?: info.carpark_number,
                                address = metadata?.address ?: "Unknown address",
                                latitude = metadata?.latitude
                                    ?: CarparkMetadataProvider.allMetadata()
                                        .firstOrNull { it.carparkNumber == info.carpark_number }?.latitude
                                    ?: 1.3521,
                                longitude = metadata?.longitude
                                    ?: CarparkMetadataProvider.allMetadata()
                                        .firstOrNull { it.carparkNumber == info.carpark_number }?.longitude
                                    ?: 103.8198,
                                totalLots = metadata?.totalLots ?: totalLots,
                                availableLots = availableLots,
                                dataSource = "data.gov.sg",
                                priceTier = metadata?.priceTier?.name ?: PriceTier.UNKNOWN.name,
                                baseHourlyRate = metadata?.baseHourlyRate,
                                lastUpdated = System.currentTimeMillis()
                            )
                        }
                        carparkEntities.add(entity)
                    }

                    carparkDao.insertCarparks(carparkEntities)
                    Log.d(TAG, "Fetched ${carparks.size} carparks from LTA API")
                    Result.success(carparks.size)
                } else {
                    Log.e(TAG, "API Error: ${response.code()} ${response.message()}")
                    Result.failure(Exception("API Error: ${response.code()}"))
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to refresh carpark availability", e)
                Result.failure(e)
            }
        }
    }

    suspend fun seedSampleCarparks() {
        val samples = CarparkMetadataProvider.allMetadata().map { metadata ->
            CarparkEntity(
                carparkID = metadata.carparkNumber,
                location = metadata.name,
                address = metadata.address,
                latitude = metadata.latitude,
                longitude = metadata.longitude,
                totalLots = metadata.totalLots,
                availableLots = (metadata.totalLots * 0.6).toInt(),
                dataSource = "metadata",
                priceTier = metadata.priceTier.name,
                baseHourlyRate = metadata.baseHourlyRate
            )
        }
        carparkDao.insertCarparks(samples)
    }

    suspend fun getCarparkDetail(carparkNumber: String): Result<CarparkDetail> {
        return withContext(Dispatchers.IO) {
            try {
                val response = RetrofitClient.instance.getCarparkAvailability().execute()
                val metadata = CarparkMetadataProvider.getMetadata(carparkNumber)
                val cachedEntity = carparkDao.getCarparkById(carparkNumber)

                if (response.isSuccessful) {
                    val carpark = response.body()?.items
                        ?.firstOrNull()
                        ?.carpark_data
                        ?.firstOrNull { it.carpark_number.equals(carparkNumber, ignoreCase = true) }

                    if (carpark != null) {
                        val lotAvailability = carpark.carpark_info.map { info ->
                            CarparkLotAvailability(
                                lotType = info.lot_type,
                                totalLots = parseInt(info.total_lots),
                                availableLots = parseInt(info.lots_available)
                            )
                        }
                        val totalLots = lotAvailability.sumOf { it.totalLots }
                            .takeIf { it > 0 }
                            ?: metadata?.totalLots
                            ?: cachedEntity?.totalLots
                            ?: 0
                        val availableLots = lotAvailability.sumOf { it.availableLots }
                            .takeIf { it > 0 }
                            ?: cachedEntity?.availableLots
                            ?: 0

                        val detail = CarparkDetail(
                            carparkNumber = carpark.carpark_number,
                            name = metadata?.name ?: cachedEntity?.location ?: carpark.carpark_number,
                            address = metadata?.address ?: cachedEntity?.address ?: "Unknown address",
                            latitude = metadata?.latitude ?: cachedEntity?.latitude,
                            longitude = metadata?.longitude ?: cachedEntity?.longitude,
                            lotAvailability = lotAvailability,
                            totalLots = totalLots,
                            availableLots = availableLots,
                            lastUpdated = parseUpdateMillis(carpark.update_datetime),
                            lastUpdatedRaw = carpark.update_datetime,
                            priceTier = metadata?.priceTier ?: cachedEntity?.priceTier?.let { PriceTier.valueOf(it) } ?: PriceTier.UNKNOWN,
                            baseHourlyRate = metadata?.baseHourlyRate ?: cachedEntity?.baseHourlyRate
                        )
                        return@withContext Result.success(detail)
                    }
                }

                // Fallback to cached entity if API call failed or carpark missing
                if (cachedEntity != null) {
                    val detail = CarparkDetail(
                        carparkNumber = carparkNumber,
                        name = cachedEntity.location,
                        address = cachedEntity.address,
                        latitude = cachedEntity.latitude,
                        longitude = cachedEntity.longitude,
                        lotAvailability = emptyList(),
                        totalLots = cachedEntity.totalLots,
                        availableLots = cachedEntity.availableLots,
                        lastUpdated = cachedEntity.lastUpdated,
                        lastUpdatedRaw = null,
                        priceTier = runCatching { PriceTier.valueOf(cachedEntity.priceTier) }.getOrElse { PriceTier.UNKNOWN },
                        baseHourlyRate = cachedEntity.baseHourlyRate
                    )
                    Result.success(detail)
                } else {
                    Result.failure(Exception("Carpark $carparkNumber not found"))
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to fetch carpark detail", e)
                val cachedEntity = carparkDao.getCarparkById(carparkNumber)
                if (cachedEntity != null) {
                    Result.success(
                        CarparkDetail(
                            carparkNumber = carparkNumber,
                            name = cachedEntity.location,
                            address = cachedEntity.address,
                            latitude = cachedEntity.latitude,
                            longitude = cachedEntity.longitude,
                            lotAvailability = emptyList(),
                            totalLots = cachedEntity.totalLots,
                            availableLots = cachedEntity.availableLots,
                            lastUpdated = cachedEntity.lastUpdated,
                            lastUpdatedRaw = null,
                            priceTier = runCatching { PriceTier.valueOf(cachedEntity.priceTier) }.getOrElse { PriceTier.UNKNOWN },
                            baseHourlyRate = cachedEntity.baseHourlyRate
                        )
                    )
                } else {
                    Result.failure(e)
                }
            }
        }
    }

    private fun parseInt(value: String): Int = value.toIntOrNull() ?: 0

    private fun parseUpdateMillis(raw: String?): Long {
        if (raw.isNullOrBlank()) return System.currentTimeMillis()
        return runCatching {
            val localDateTime = LocalDateTime.parse(raw, UPDATE_FORMATTER)
            localDateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        }.getOrElse { System.currentTimeMillis() }
    }

    /**
     * Search carparks
     */
    fun searchCarparks(query: String): Flow<List<CarparkEntity>> {
        return carparkDao.searchCarparks(query)
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
