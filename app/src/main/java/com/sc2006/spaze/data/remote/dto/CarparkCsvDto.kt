package com.sc2006.spaze.data.remote.dto

import com.sc2006.spaze.data.local.entity.CarparkEntity
import kotlin.math.cos

/**
 * DTO for parsing CSV file
 * Maps to columns in HDBCarparkInformation.csv
 */
data class CarparkCsvDto(
    val carParkNo: String,              // Column: car_park_no
    val address: String,                // Column: address
    val xCoord: String,                 // Column: x_coord (SVY21)
    val yCoord: String,                 // Column: y_coord (SVY21)
    val carParkType: String,            // Column: car_park_type
    val typeOfParkingSystem: String,    // Column: type_of_parking_system
    val shortTermParking: String,       // Column: short_term_parking
    val freeParking: String,            // Column: free_parking
    val nightParking: String,           // Column: night_parking
    val carParkDecks: String,           // Column: car_park_decks
    val gantryHeight: String,           // Column: gantry_height
    val carParkBasement: String         // Column: car_park_basement
) {
    /**
     * Convert to CarparkEntity with default live data values
     */
    fun toEntity(): CarparkEntity {
        // Convert SVY21 coordinates (northing=y, easting=x) to WGS84 (lat/lng)
        val x = xCoord.toDoubleOrNull() ?: 0.0
        val y = yCoord.toDoubleOrNull() ?: 0.0
        val (lat, lng) = try {
            com.sc2006.spaze.data.util.Svy21.convertToLatLon(y, x)
        } catch (e: Exception) {
            0.0 to 0.0
        }

        return CarparkEntity(
            carparkNumber = carParkNo.trim(),
            address = address.trim(),
            xCoord = xCoord.toDoubleOrNull() ?: 0.0,
            yCoord = yCoord.toDoubleOrNull() ?: 0.0,
            latitude = lat,
            longitude = lng,
            carParkType = carParkType.trim(),
            typeOfParkingSystem = typeOfParkingSystem.trim(),
            shortTermParking = shortTermParking.trim(),
            freeParking = freeParking.trim(),
            nightParking = nightParking.trim(),
            carParkDecks = carParkDecks.toIntOrNull() ?: 0,
            gantryHeight = gantryHeight.toDoubleOrNull() ?: 0.0,
            carParkBasement = carParkBasement.trim(),
            // Live data will be populated by API later
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
    }

    companion object {
        /**
         * Convert SVY21 (Singapore's coordinate system) to WGS84 (lat/lng)
         * This is a simplified conversion - for production, use proper library
         */
        private fun convertSVY21ToWGS84(x: Double, y: Double): Pair<Double, Double> {
            // SVY21 origin
            val originLat = 1.366666  // degrees
            val originLon = 103.833333 // degrees

            // Approximate conversion (1 degree ≈ 111km)
            // For accurate conversion, use svy21 library or similar
            val latitude = originLat + (y / 111000.0)
            val longitude = originLon + (x / (111000.0 * cos(Math.toRadians(originLat))))

            return Pair(latitude, longitude)
        }
    }
}
