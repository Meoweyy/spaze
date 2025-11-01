package com.sc2006.spaze.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Carpark Entity - Combines static CSV data + live API data
 */
@Entity(tableName = "carparks")
data class CarparkEntity(
    @PrimaryKey
    val carparkNumber: String,  // From CSV - "HG50", "ACB", etc.

    // ═══════════════════════════════════════════════════
    // STATIC DATA (from CSV - never changes)
    // ═══════════════════════════════════════════════════
    val address: String,                    // "BLK 270/271 BUKIT BATOK EAST AVE 4"
    val xCoord: Double,                     // 21414.6614 (SVY21 format)
    val yCoord: Double,                     // 36974.9264 (SVY21 format)
    val latitude: Double,                   // 1.349319 (WGS84 - converted from SVY21)
    val longitude: Double,                  // 103.753213 (WGS84 - converted from SVY21)
    val carParkType: String,                // "SURFACE CAR PARK", "MULTI-STOREY CAR PARK", etc.
    val typeOfParkingSystem: String,        // "ELECTRONIC PARKING", "COUPON PARKING"
    val shortTermParking: String,           // "WHOLE DAY", "NO", "7AM-7PM"
    val freeParking: String,                // "SUN & PH FR 7AM-10.30PM", "NO"
    val nightParking: String,               // "YES", "NO"
    val carParkDecks: Int,                  // 0 for surface, >0 for multi-storey
    val gantryHeight: Double,               // 2.15, 0.0 for surface
    val carParkBasement: String,            // "Y", "N"

    // ═══════════════════════════════════════════════════
    // LIVE DATA (from API - updates frequently)
    // ═══════════════════════════════════════════════════
    val totalLotsC: Int = 0,                // Total car lots (Type C)
    val availableLotsC: Int = 0,            // Available car lots
    val totalLotsH: Int = 0,                // Total heavy vehicle lots (Type H)
    val availableLotsH: Int = 0,            // Available heavy vehicle lots
    val totalLotsY: Int = 0,                // Total motorcycle lots (Type Y)
    val availableLotsY: Int = 0,            // Available motorcycle lots
    val totalLotsS: Int = 0,                // Total motorcycle with sidecar lots (Type S)
    val availableLotsS: Int = 0,            // Available motorcycle with sidecar lots

    val lastUpdated: Long = 0,              // Timestamp of last API update

    // ═══════════════════════════════════════════════════
    // COMPUTED/CACHED DATA
    // ═══════════════════════════════════════════════════
    val distanceFromUser: Float? = null,    // Distance in meters (calculated)
    val isFavorite: Boolean = false,        // User favorited this carpark
    val lastViewed: Long? = null            // When user last viewed details
) {

    /**
     * Get total available lots across all types
     */
    fun getTotalAvailableLots(): Int {
        return availableLotsC + availableLotsH + availableLotsY + availableLotsS
    }

    /**
     * Get total lots across all types
     */
    fun getTotalLots(): Int {
        return totalLotsC + totalLotsH + totalLotsY + totalLotsS
    }

    /**
     * Get availability percentage for cars (Type C)
     */
    fun getCarAvailabilityPercentage(): Float {
        return if (totalLotsC > 0) {
            (availableLotsC.toFloat() / totalLotsC.toFloat()) * 100f
        } else {
            0f
        }
    }

    /**
     * Check if carpark has available car lots
     */
    fun hasCarLotsAvailable(minLots: Int = 1): Boolean {
        return availableLotsC >= minLots
    }

    /**
     * Get availability status for cars
     */
    fun getAvailabilityStatus(): AvailabilityStatus {
        val percentage = getCarAvailabilityPercentage()
        return when {
            availableLotsC == 0 -> AvailabilityStatus.FULL
            percentage < 10 -> AvailabilityStatus.ALMOST_FULL
            percentage < 30 -> AvailabilityStatus.LIMITED
            else -> AvailabilityStatus.AVAILABLE
        }
    }

    /**
     * Check if data is stale (older than 5 minutes)
     */
    fun isDataStale(): Boolean {
        if (lastUpdated == 0L) return true
        val fiveMinutesInMillis = 5 * 60 * 1000
        return System.currentTimeMillis() - lastUpdated > fiveMinutesInMillis
    }

    /**
     * Has electronic parking system
     */
    fun hasElectronicParking(): Boolean {
        return typeOfParkingSystem.contains("ELECTRONIC", ignoreCase = true)
    }

    /**
     * Has free parking periods
     */
    fun hasFreeParkingPeriods(): Boolean {
        return freeParking.uppercase() != "NO"
    }

    enum class AvailabilityStatus {
        AVAILABLE,
        LIMITED,
        ALMOST_FULL,
        FULL
    }
}