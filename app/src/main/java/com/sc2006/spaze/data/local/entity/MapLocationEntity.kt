package com.sc2006.spaze.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Map Location Entity - Represents map-related data for carparks
 * Maps to the Map class in the class diagram
 * Stores routing and location information
 */
@Entity(tableName = "map_locations")
data class MapLocationEntity(
    @PrimaryKey
    val carparkNumber: String,  // ← Changed from carparkID to carparkNumber

    // Location coordinates
    val latitude: Double,
    val longitude: Double,
    val address: String,

    // Routing information
    val route: String = "", // JSON string with route polyline
    val travelTime: Int = 0, // in minutes
    val distance: Float = 0f, // in meters
    val travelMode: TravelMode = TravelMode.WALKING,

    // Metadata
    val lastRouteUpdate: Long = System.currentTimeMillis(),
    val isRouteActive: Boolean = false
) {
    enum class TravelMode {
        WALKING,
        DRIVING,
        TRANSIT
    }

    /**
     * Get formatted distance
     */
    fun getFormattedDistance(): String {
        return when {
            distance < 1000 -> "${distance.toInt()} m"
            else -> String.format("%.1f km", distance / 1000)
        }
    }

    /**
     * Get formatted travel time
     */
    fun getFormattedTravelTime(): String {
        return when {
            travelTime < 60 -> "$travelTime min"
            else -> {
                val hours = travelTime / 60
                val mins = travelTime % 60
                "${hours}h ${mins}min"
            }
        }
    }

    /**
     * Check if route is stale (older than 10 minutes)
     */
    fun isRouteStale(): Boolean {
        val tenMinutesInMillis = 10 * 60 * 1000
        return System.currentTimeMillis() - lastRouteUpdate > tenMinutesInMillis
    }

    /**
     * Calculate estimated walk time based on distance
     * Assumes average walking speed of 5 km/h
     */
    fun calculateEstimatedWalkTime(): Int {
        val walkingSpeedKmPerHour = 5.0
        val distanceKm = distance / 1000.0
        val timeHours = distanceKm / walkingSpeedKmPerHour
        return (timeHours * 60).toInt() // Convert to minutes
    }

    /**
     * Calculate estimated drive time based on distance
     * Assumes average city driving speed of 30 km/h
     */
    fun calculateEstimatedDriveTime(): Int {
        val drivingSpeedKmPerHour = 30.0
        val distanceKm = distance / 1000.0
        val timeHours = distanceKm / drivingSpeedKmPerHour
        return (timeHours * 60).toInt() // Convert to minutes
    }

    companion object {
        /**
         * Create from carpark entity
         */
        fun fromCarpark(carpark: CarparkEntity): MapLocationEntity {
            return MapLocationEntity(
                carparkNumber = carpark.carparkNumber,  // ← Changed from carparkID
                latitude = carpark.latitude,
                longitude = carpark.longitude,
                address = carpark.address
            )
        }
    }
}