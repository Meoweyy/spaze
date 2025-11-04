package com.sc2006.spaze.data.model

import com.google.android.gms.maps.model.LatLng

/**
 * Domain model for a navigation route
 */
data class NavigationRoute(
    val legs: List<NavigationLeg>,
    val encodedPolyline: String,
    val summary: String,
    val warnings: List<String> = emptyList()
)

/**
 * Domain model for a leg of the route (origin to destination segment)
 */
data class NavigationLeg(
    val distance: NavigationDistance,
    val duration: NavigationDuration,
    val endAddress: String,
    val endLocation: LatLng,
    val startAddress: String,
    val startLocation: LatLng,
    val steps: List<NavigationStep>
)

/**
 * Domain model for a navigation step (turn-by-turn instruction)
 */
data class NavigationStep(
    val distance: NavigationDistance,
    val duration: NavigationDuration,
    val endLocation: LatLng,
    val instructions: String,
    val encodedPolyline: String,
    val startLocation: LatLng,
    val travelMode: String,
    val maneuver: String? = null
)

/**
 * Domain model for distance information
 */
data class NavigationDistance(
    val text: String,          // "1.5 km"
    val valueMeters: Int       // 1500
)

/**
 * Domain model for duration information
 */
data class NavigationDuration(
    val text: String,          // "5 mins"
    val valueSeconds: Int      // 300
)
