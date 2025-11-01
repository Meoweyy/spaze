package com.sc2006.spaze.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Carpark Entity - Represents a carpark with availability information
 * Maps to the Carpark class in the class diagram
 */
@Entity(tableName = "carparks")
data class CarparkEntity(
    @PrimaryKey
    val carparkID: String,
    val location: String,
    val address: String,
    val latitude: Double,
    val longitude: Double,
    val totalLots: Int,
    val availableLots: Int,

    // Additional fields for enhanced functionality
    val lotTypes: String = "", // JSON string: {"car": 100, "motorcycle": 20, "ev": 5, "accessible": 3}
    val pricingInfo: String = "", // JSON string with pricing structure
    val operatingHours: String = "24/7",
    val lastUpdated: Long = System.currentTimeMillis(),
    val dataSource: String = "LTA DataMall", // API source
    val priceTier: String = PriceTier.UNKNOWN.name,
    val baseHourlyRate: Double? = null,

    // Cached calculated values
    val distanceFromUser: Float? = null, // in meters
    val estimatedWalkTime: Int? = null, // in minutes
    val estimatedDriveTime: Int? = null, // in minutes

    // Metadata
    val isFavorite: Boolean = false,
    val lastViewed: Long? = null
) {
    /**
     * Get availability percentage
     */
    fun getAvailabilityPercentage(): Float {
        return if (totalLots > 0) {
            (availableLots.toFloat() / totalLots.toFloat()) * 100f
        } else {
            0f
        }
    }

    /**
     * Check if carpark has available lots
     */
    fun hasAvailability(minLots: Int = 1): Boolean {
        return availableLots >= minLots
    }

    /**
     * Get availability status
     */
    fun getAvailabilityStatus(): AvailabilityStatus {
        val percentage = getAvailabilityPercentage()
        return when {
            availableLots == 0 -> AvailabilityStatus.FULL
            percentage < 10 -> AvailabilityStatus.ALMOST_FULL
            percentage < 30 -> AvailabilityStatus.LIMITED
            else -> AvailabilityStatus.AVAILABLE
        }
    }

    /**
     * Check if data is stale (older than 5 minutes)
     */
    fun isDataStale(): Boolean {
        val fiveMinutesInMillis = 5 * 60 * 1000
        return System.currentTimeMillis() - lastUpdated > fiveMinutesInMillis
    }

    enum class AvailabilityStatus {
        AVAILABLE,
        LIMITED,
        ALMOST_FULL,
        FULL
    }

    enum class PriceTier {
        BUDGET,
        STANDARD,
        PREMIUM,
        UNKNOWN
    }

    companion object {
        /**
         * Create a carpark entity from API response
         */
        fun fromApiResponse(
            id: String,
            location: String,
            address: String,
            lat: Double,
            lng: Double,
            total: Int,
            available: Int
        ): CarparkEntity {
            return CarparkEntity(
                carparkID = id,
                location = location,
                address = address,
                latitude = lat,
                longitude = lng,
                totalLots = total,
                availableLots = available
            )
        }
    }
}
