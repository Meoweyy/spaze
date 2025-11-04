package com.sc2006.spaze.data.mapper

import com.google.android.gms.maps.model.LatLng
import com.sc2006.spaze.data.model.*
import com.sc2006.spaze.data.remote.api.*

/**
 * Mapper functions to convert Google Maps API DTOs to domain models
 */

fun DirectionsResponse.toNavigationRoutes(): List<NavigationRoute> {
    return routes.map { it.toNavigationRoute() }
}

fun Route.toNavigationRoute(): NavigationRoute {
    return NavigationRoute(
        legs = legs.map { it.toNavigationLeg() },
        encodedPolyline = overviewPolyline.points,
        summary = summary,
        warnings = warnings ?: emptyList()
    )
}

fun Leg.toNavigationLeg(): NavigationLeg {
    return NavigationLeg(
        distance = distance.toNavigationDistance(),
        duration = duration.toNavigationDuration(),
        endAddress = endAddress,
        endLocation = LatLng(endLocation.lat, endLocation.lng),
        startAddress = startAddress,
        startLocation = LatLng(startLocation.lat, startLocation.lng),
        steps = steps.map { it.toNavigationStep() }
    )
}

fun Step.toNavigationStep(): NavigationStep {
    return NavigationStep(
        distance = distance.toNavigationDistance(),
        duration = duration.toNavigationDuration(),
        endLocation = LatLng(endLocation.lat, endLocation.lng),
        instructions = htmlInstructions,
        encodedPolyline = polyline.points,
        startLocation = LatLng(startLocation.lat, startLocation.lng),
        travelMode = travelMode,
        maneuver = maneuver
    )
}

fun Distance.toNavigationDistance(): NavigationDistance {
    return NavigationDistance(
        text = text,
        valueMeters = value
    )
}

fun Duration.toNavigationDuration(): NavigationDuration {
    return NavigationDuration(
        text = text,
        valueSeconds = value
    )
}
