package com.sc2006.spaze.presentation.screens

import android.Manifest
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*

import com.sc2006.spaze.data.local.entity.CarparkEntity
import com.sc2006.spaze.data.preferences.PreferencesDataStore
import com.sc2006.spaze.util.PolylineDecoder
import com.sc2006.spaze.presentation.viewmodel.HomeViewModel
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.flow.first
@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel = hiltViewModel(),
    onNavigateToSearch: () -> Unit,
    onNavigateToFavorites: () -> Unit,
    onNavigateToProfile: () -> Unit,
    onNavigateToBudget: () -> Unit,
    onNavigateToCarparkDetails: (String) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val carparks by viewModel.carparks.collectAsState()

    // Location permissions
    val locationPermissionsState = rememberMultiplePermissionsState(
        listOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
    )

    // Request location permissions and start tracking when granted
    LaunchedEffect(locationPermissionsState.allPermissionsGranted) {
        if (locationPermissionsState.allPermissionsGranted) {
            viewModel.startLocationTracking()
        }
    }

    // Request permissions on first launch
    LaunchedEffect(Unit) {
        if (!locationPermissionsState.allPermissionsGranted) {
            locationPermissionsState.launchMultiplePermissionRequest()
        }
    }

    // App context and search radius state
    val context = LocalContext.current
    val searchRadiusKm by remember {
        PreferencesDataStore.getSearchRadius(context)
    }.collectAsState(initial = 1.5f)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Spaze") },
                actions = {
                    IconButton(onClick = onNavigateToSearch) {
                        Icon(Icons.Default.Search, "Search")
                    }
                    IconButton(onClick = onNavigateToProfile) {
                        Icon(Icons.Default.Person, "Profile")
                    }
                }
            )
        },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Home, "Home") },
                    label = { Text("Home") },
                    selected = true,
                    onClick = { }
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Search, "Search") },
                    label = { Text("Search") },
                    selected = false,
                    onClick = onNavigateToSearch
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Favorite, "Favorites") },
                    label = { Text("Favorites") },
                    selected = false,
                    onClick = onNavigateToFavorites
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.AccountBalance, "Budget") },
                    label = { Text("Budget") },
                    selected = false,
                    onClick = onNavigateToBudget
                )
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)){
            // Google Maps View
            Box(
        modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                var selectedCarpark by remember { mutableStateOf<CarparkEntity?>(null) }
                val userLocation = LatLng(uiState.userLatitude, uiState.userLongitude)
                val cameraPositionState = rememberCameraPositionState {
                    position = CameraPosition.fromLatLngZoom(userLocation, 12f)
                }

                // Update camera when user location changes
                LaunchedEffect(uiState.userLatitude, uiState.userLongitude) {
                    if (uiState.isLocationTrackingEnabled) {
                        cameraPositionState.animate(
                            CameraUpdateFactory.newLatLngZoom(userLocation, 14f),
                            durationMs = 1000
                        )
                    }
                }

                // Use search radius for circle overlay

                GoogleMap(
                    modifier = Modifier.fillMaxSize(),
                    cameraPositionState = cameraPositionState,
                    properties = MapProperties(
                        mapType = if (uiState.mapViewType == com.sc2006.spaze.presentation.viewmodel.MapViewType.SATELLITE) {
                            MapType.SATELLITE
                        } else {
                            MapType.NORMAL
                        },
                        isMyLocationEnabled = locationPermissionsState.allPermissionsGranted
                    ),
                    uiSettings = MapUiSettings(
                        zoomControlsEnabled = true,
                        myLocationButtonEnabled = locationPermissionsState.allPermissionsGranted
                    ),
                    onMapClick = { selectedCarpark = null }
                ) {
                    // Draw search radius circle
                    Circle(
                        center = userLocation,
                        radius = (searchRadiusKm * 1000).toDouble(), // Convert km to meters
                        fillColor = Color.Blue.copy(alpha = 0.1f),
                        strokeColor = Color.Blue.copy(alpha = 0.5f),
                        strokeWidth = 2f
                    )
                    carparks.forEach { carpark ->
                        val carparkPosition = LatLng(carpark.latitude, carpark.longitude)
                        Marker(
                            state = MarkerState(position = carparkPosition),
                            title = carpark.address,
                            snippet = "Available: ${carpark.availableLotsC}/${carpark.totalLotsC}",
                            icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_VIOLET),
                            onClick = {
                                // Select carpark; dedicated control will start navigation
                                selectedCarpark = carpark
                                true
                            }
                        )
                    }

                    // Add marker for user location (only if location tracking is not enabled,
                    // as the map built-in location indicator will show it)
                    if (!uiState.isLocationTrackingEnabled) {
                        Marker(
                            state = MarkerState(position = userLocation),
                            title = "Your Location (Default)",
                            icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE)
                        )
                    }

                    // Render route polyline and destination marker when navigating
                    uiState.selectedRoute?.let { route ->
                        val polylinePoints = PolylineDecoder.decode(route.encodedPolyline)
                        Polyline(
                            points = polylinePoints,
                            color = Color.Blue,
                            width = 10f
                        )

                        uiState.destinationLatLng?.let { destination ->
                            Marker(
                                state = MarkerState(position = destination),
                                title = "Destination",
                                icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN)
                            )
                        }
                    }
                }
                // Map type toggle button (moved to left to avoid blocking Google Maps controls)
                FloatingActionButton(
                    onClick = { viewModel.toggleMapView() },
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(16.dp)) {
                    Icon(Icons.Default.Layers, "Toggle Map View")
                }

                // Dedicated Navigate control (appears when a carpark is selected, moved to left)
                selectedCarpark?.let { target ->
                    ExtendedFloatingActionButton(
                        onClick = { viewModel.navigateToCarpark(target) },
                        text = { Text("Navigate") },
                        icon = { Icon(Icons.Default.Directions, contentDescription = "Navigate") },
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(16.dp)
                    )
                }
            }

            // Error message display (filter out benign cancellation errors)
            uiState.error?.let { errorMsg ->
                if (errorMsg.isNotBlank() &&
                    !errorMsg.contains("cancelled", ignoreCase = true) &&
                    !errorMsg.contains("JobCancellationException")) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                    ) {
                        Text(
                            text = errorMsg,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
            }

            // Route info display when navigating
            uiState.routeError?.let { routeErrorMsg ->
                if (routeErrorMsg.isNotBlank()) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "Navigation Error",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                            Text(
                                text = routeErrorMsg,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
                }
            }

            uiState.selectedRoute?.let { route ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Route Information",
                                style = MaterialTheme.typography.titleMedium
                            )
                            IconButton(onClick = { viewModel.clearRoute() }) {
                                Icon(Icons.Default.Close, "Clear route")
                            }
                        }
                        if (route.legs.isNotEmpty()) {
                            val leg = route.legs[0]
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("Distance", style = MaterialTheme.typography.labelSmall)
                                    Text(leg.distance.text, style = MaterialTheme.typography.titleMedium)
                                }
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("Duration", style = MaterialTheme.typography.labelSmall)
                                    Text(leg.duration.text, style = MaterialTheme.typography.titleMedium)
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = route.summary,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            // Display carpark availability information
                            uiState.selectedCarpark?.let { carpark ->
                                Spacer(modifier = Modifier.height(12.dp))
                                Divider()
                                Spacer(modifier = Modifier.height(12.dp))

                                Text(
                                    text = "Carpark Details",
                                    style = MaterialTheme.typography.titleSmall,
                                    modifier = Modifier.padding(bottom = 4.dp)
                                )
                                Text(
                                    text = carpark.address,
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.padding(bottom = 8.dp)
                                )

                                // Show availability for each lot type
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceEvenly
                                ) {
                                    // Car lots (most common)
                                    if (carpark.totalLotsC > 0) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Text("Car Lots", style = MaterialTheme.typography.labelSmall)
                                            Text(
                                                text = "${carpark.availableLotsC}/${carpark.totalLotsC}",
                                                style = MaterialTheme.typography.titleMedium,
                                                color = if (carpark.availableLotsC > 0)
                                                    MaterialTheme.colorScheme.primary
                                                else
                                                    MaterialTheme.colorScheme.error
                                            )
                                        }
                                    }
                                    // Motorcycle lots
                                    if (carpark.totalLotsY > 0) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Text("Motorcycle", style = MaterialTheme.typography.labelSmall)
                                            Text(
                                                text = "${carpark.availableLotsY}/${carpark.totalLotsY}",
                                                style = MaterialTheme.typography.titleMedium,
                                                color = if (carpark.availableLotsY > 0)
                                                    MaterialTheme.colorScheme.primary
                                                else
                                                    MaterialTheme.colorScheme.error
                                            )
                                        }
                                    }
                                    // Heavy vehicle lots
                                    if (carpark.totalLotsH > 0) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Text("Heavy", style = MaterialTheme.typography.labelSmall)
                                            Text(
                                                text = "${carpark.availableLotsH}/${carpark.totalLotsH}",
                                                style = MaterialTheme.typography.titleMedium,
                                                color = if (carpark.availableLotsH > 0)
                                                    MaterialTheme.colorScheme.primary
                                                else
                                                    MaterialTheme.colorScheme.error
                                            )
                                        }
                                    }
                                }
                            }

                            // Get Directions button - opens Google Maps with navigation
                            uiState.destinationLatLng?.let { destination ->
                                Spacer(modifier = Modifier.height(12.dp))
                                Button(
                                    onClick = {
                                        // Open Google Maps with directions
                                        val intent = android.content.Intent(
                                            android.content.Intent.ACTION_VIEW,
                                            android.net.Uri.parse(
                                                "google.navigation:q=${destination.latitude},${destination.longitude}&mode=d"
                                            )
                                        ).apply {
                                            setPackage("com.google.android.apps.maps")
                                        }
                                        try {
                                            context.startActivity(intent)
                                        } catch (e: Exception) {
                                            // Fallback if Google Maps not installed - use generic geo intent
                                            val fallbackIntent = android.content.Intent(
                                                android.content.Intent.ACTION_VIEW,
                                                android.net.Uri.parse(
                                                    "geo:${destination.latitude},${destination.longitude}?q=${destination.latitude},${destination.longitude}"
                                                )
                                            )
                                            context.startActivity(fallbackIntent)
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(Icons.Default.NearMe, contentDescription = null)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Get Directions in Google Maps")
                                }
                            }
                        }
                    }
                }
            }

            // Carpark info section
            Text(
                text = "Nearby Carparks (within ${searchRadiusKm}km)",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(16.dp)
            )

            if (uiState.isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .padding(16.dp)
                )
            } else {
                Text(
                    text = "${carparks.size} carparks found",
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }
        }
    }
}











