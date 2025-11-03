package com.sc2006.spaze.data.remote.api

import com.google.gson.annotations.SerializedName
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

/**
 * Google Maps API Service for routing and directions
 */
interface GoogleMapsApiService {

    /**
     * Get directions between two points
     */
    @GET("maps/api/directions/json")
    suspend fun getDirections(
        @Query("origin") origin: String,
        @Query("destination") destination: String,
        @Query("mode") mode: String = "driving", // driving, walking, transit, bicycling
        @Query("alternatives") alternatives: Boolean = false,
        @Query("departure_time") departureTime: String? = null,
        @Query("traffic_model") trafficModel: String? = "best_guess", // best_guess, pessimistic, optimistic
        @Query("key") apiKey: String
    ): Response<DirectionsResponse>

    /**
     * Get distance matrix for multiple origins/destinations
     */
    @GET("maps/api/distancematrix/json")
    suspend fun getDistanceMatrix(
        @Query("origins") origins: String,
        @Query("destinations") destinations: String,
        @Query("mode") mode: String = "driving",
        @Query("key") apiKey: String
    ): Response<DistanceMatrixResponse>

    /**
     * Geocode an address to coordinates
     */
    @GET("maps/api/geocode/json")
    suspend fun geocodeAddress(
        @Query("address") address: String,
        @Query("key") apiKey: String
    ): Response<GeocodeResponse>

    /**
     * Reverse geocode coordinates to address
     */
    @GET("maps/api/geocode/json")
    suspend fun reverseGeocode(
        @Query("latlng") latLng: String,
        @Query("key") apiKey: String
    ): Response<GeocodeResponse>

    /**
     * Search nearby places
     */
    @GET("maps/api/place/nearbysearch/json")
    suspend fun searchNearbyPlaces(
        @Query("location") location: String,
        @Query("radius") radius: Int,
        @Query("type") type: String,
        @Query("key") apiKey: String
    ): Response<PlacesResponse>

    companion object {
        const val BASE_URL = "https://maps.googleapis.com/"
    }
}

// Response DTOs
data class DirectionsResponse(
    @SerializedName("routes")
    val routes: List<Route>,
    @SerializedName("status")
    val status: String
)

data class Route(
    @SerializedName("legs")
    val legs: List<Leg>,
    @SerializedName("overview_polyline")
    val overviewPolyline: OverviewPolyline,
    @SerializedName("summary")
    val summary: String,
    @SerializedName("warnings")
    val warnings: List<String>? = null
)

data class Leg(
    @SerializedName("distance")
    val distance: Distance,
    @SerializedName("duration")
    val duration: Duration,
    @SerializedName("start_address")
    val startAddress: String,
    @SerializedName("end_address")
    val endAddress: String,
    @SerializedName("start_location")
    val startLocation: Location,
    @SerializedName("end_location")
    val endLocation: Location,
    @SerializedName("steps")
    val steps: List<Step>
)

data class Distance(
    @SerializedName("text")
    val text: String,
    @SerializedName("value")
    val value: Int // meters
)

data class Duration(
    @SerializedName("text")
    val text: String,
    @SerializedName("value")
    val value: Int // seconds
)

data class OverviewPolyline(
    @SerializedName("points")
    val points: String
)

data class Step(
    @SerializedName("distance")
    val distance: Distance,
    @SerializedName("duration")
    val duration: Duration,
    @SerializedName("start_location")
    val startLocation: Location,
    @SerializedName("end_location")
    val endLocation: Location,
    @SerializedName("html_instructions")
    val htmlInstructions: String,
    @SerializedName("polyline")
    val polyline: OverviewPolyline,
    @SerializedName("travel_mode")
    val travelMode: String,
    @SerializedName("maneuver")
    val maneuver: String? = null
)

data class DistanceMatrixResponse(
    @SerializedName("rows")
    val rows: List<DistanceMatrixRow>,
    @SerializedName("status")
    val status: String
)

data class DistanceMatrixRow(
    @SerializedName("elements")
    val elements: List<DistanceMatrixElement>
)

data class DistanceMatrixElement(
    @SerializedName("distance")
    val distance: Distance,
    @SerializedName("duration")
    val duration: Duration,
    @SerializedName("status")
    val status: String
)

data class GeocodeResponse(
    @SerializedName("results")
    val results: List<GeocodeResult>,
    @SerializedName("status")
    val status: String
)

data class GeocodeResult(
    @SerializedName("formatted_address")
    val formattedAddress: String,
    @SerializedName("geometry")
    val geometry: Geometry,
    @SerializedName("place_id")
    val placeId: String
)

data class Geometry(
    @SerializedName("location")
    val location: Location
)

data class Location(
    @SerializedName("lat")
    val lat: Double,
    @SerializedName("lng")
    val lng: Double
)

data class PlacesResponse(
    @SerializedName("results")
    val results: List<Place>,
    @SerializedName("status")
    val status: String
)

data class Place(
    @SerializedName("name")
    val name: String,
    @SerializedName("geometry")
    val geometry: Geometry,
    @SerializedName("vicinity")
    val vicinity: String
)
