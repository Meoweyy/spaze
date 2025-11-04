package com.sc2006.spaze.data.repository

import android.util.Log
import com.google.android.gms.maps.model.LatLng
import com.sc2006.spaze.BuildConfig
import com.sc2006.spaze.data.mapper.toNavigationRoutes
import com.sc2006.spaze.data.model.NavigationRoute
import com.sc2006.spaze.data.remote.api.GoogleMapsApiService
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Navigation Repository
 * Manages navigation and routing data from Google Maps Directions API
 */
@Singleton
class NavigationRepository @Inject constructor(
    private val googleMapsApiService: GoogleMapsApiService
) {

    companion object {
        private const val TAG = "NavigationRepository"
    }

    /**
     * Get directions from origin to destination
     *
     * @param origin Starting location
     * @param destination Ending location
     * @param mode Travel mode (driving, walking, bicycling, transit)
     * @param alternatives Whether to return alternative routes
     * @param trafficModel Traffic model for duration calculation
     * @return Result containing list of navigation routes or error
     */
    suspend fun getDirections(
        origin: LatLng,
        destination: LatLng,
        mode: String = "driving",
        alternatives: Boolean = false,
        trafficModel: String? = "best_guess"
    ): Result<List<NavigationRoute>> {
        return try {
            // Basic guard: avoid firing requests without a real key
            if (BuildConfig.GOOGLE_MAPS_API_KEY.isNullOrBlank() ||
                BuildConfig.GOOGLE_MAPS_API_KEY == "YOUR_API_KEY_HERE") {
                val msg = "Missing Google Maps API key. Add GOOGLE_MAPS_API_KEY to local.properties and enable Directions API."
                Log.e(TAG, msg)
                return Result.failure(IllegalStateException(msg))
            }
            val originString = "${origin.latitude},${origin.longitude}"
            val destinationString = "${destination.latitude},${destination.longitude}"

            Log.d(TAG, "Fetching directions from $originString to $destinationString")

            val response = googleMapsApiService.getDirections(
                origin = originString,
                destination = destinationString,
                mode = mode,
                alternatives = alternatives,
                trafficModel = trafficModel,
                apiKey = BuildConfig.GOOGLE_MAPS_API_KEY
            )

            if (!response.isSuccessful) {
                val errorMsg = "API Error: ${response.code()} - ${response.message()}"
                Log.e(TAG, errorMsg)
                return Result.failure(Exception(errorMsg))
            }

            val directionsResponse = response.body()
                ?: return Result.failure(Exception("Empty response body"))

            // Check API status
            if (directionsResponse.status != "OK") {
                val errorMsg = buildString {
                    append("Directions API error: ${directionsResponse.status}")
                    directionsResponse.errorMessage?.let { append(" - ").append(it) }
                }
                Log.e(TAG, errorMsg)
                return Result.failure(Exception(errorMsg))
            }

            // Convert to domain models
            val navigationRoutes = directionsResponse.toNavigationRoutes()

            Log.d(TAG, "Successfully fetched ${navigationRoutes.size} route(s)")
            Result.success(navigationRoutes)

        } catch (e: Exception) {
            Log.e(TAG, "Failed to get directions", e)
            Result.failure(e)
        }
    }

    /**
     * Get directions with current time for traffic-aware routing
     */
    suspend fun getDirectionsWithTraffic(
        origin: LatLng,
        destination: LatLng,
        mode: String = "driving"
    ): Result<List<NavigationRoute>> {
        val departureTime = (System.currentTimeMillis() / 1000).toString() // Unix timestamp

        return try {
            // Basic guard: avoid firing requests without a real key
            if (BuildConfig.GOOGLE_MAPS_API_KEY.isNullOrBlank() ||
                BuildConfig.GOOGLE_MAPS_API_KEY == "YOUR_API_KEY_HERE") {
                val msg = "Missing Google Maps API key. Add GOOGLE_MAPS_API_KEY to local.properties and enable Directions API."
                Log.e(TAG, msg)
                return Result.failure(IllegalStateException(msg))
            }
            val originString = "${origin.latitude},${origin.longitude}"
            val destinationString = "${destination.latitude},${destination.longitude}"

            val response = googleMapsApiService.getDirections(
                origin = originString,
                destination = destinationString,
                mode = mode,
                alternatives = false,
                departureTime = departureTime,
                trafficModel = "best_guess",
                apiKey = BuildConfig.GOOGLE_MAPS_API_KEY
            )

            if (!response.isSuccessful || response.body() == null) {
                return Result.failure(Exception("API Error: ${response.code()}"))
            }

            val directionsResponse = response.body()!!
            if (directionsResponse.status != "OK") {
                val errorMsg = buildString {
                    append("Directions API error: ${directionsResponse.status}")
                    directionsResponse.errorMessage?.let { append(" - ").append(it) }
                }
                return Result.failure(Exception(errorMsg))
            }

            Result.success(directionsResponse.toNavigationRoutes())

        } catch (e: Exception) {
            Log.e(TAG, "Failed to get traffic-aware directions", e)
            Result.failure(e)
        }
    }
}
