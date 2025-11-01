package com.sc2006.spaze.presentation.screens

import android.Manifest
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.PermissionStatus
import com.google.accompanist.permissions.rememberPermissionState
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.location.LocationServices
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.CameraPositionState
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState
import com.sc2006.spaze.R
import com.sc2006.spaze.presentation.components.PlaceAutocompleteTextField
import com.sc2006.spaze.presentation.viewmodel.CarparkUiModel
import com.sc2006.spaze.presentation.viewmodel.HomeViewModel
import com.sc2006.spaze.presentation.viewmodel.OccupancyStatus
import com.sc2006.spaze.data.local.entity.CarparkEntity.PriceTier
import kotlinx.coroutines.tasks.await

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
    var searchField by remember { mutableStateOf(TextFieldValue()) }
    val cameraEvents = viewModel.cameraEvents
    val context = LocalContext.current
    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }
    val locationPermissionState = rememberPermissionState(permission = Manifest.permission.ACCESS_FINE_LOCATION)
    val hasLocationPermission = locationPermissionState.status is PermissionStatus.Granted
    val cameraPositionState = rememberCameraPositionState {
        position = com.google.android.gms.maps.model.CameraPosition.fromLatLngZoom(
            LatLng(uiState.userLatitude, uiState.userLongitude),
            15f
        )
    }

    LaunchedEffect(locationPermissionState.status) {
        if (locationPermissionState.status is PermissionStatus.Denied) {
            locationPermissionState.launchPermissionRequest()
        }
    }

    LaunchedEffect(Unit) {
        viewModel.fetchCarparkAvailability()
    }

    LaunchedEffect(hasLocationPermission) {
        if (hasLocationPermission) {
            val location = runCatching { fusedLocationClient.lastLocation.await() }.getOrNull()
            location?.let {
                viewModel.updateUserLocation(it.latitude, it.longitude)
            }
        }
    }

    LaunchedEffect(uiState.userLatitude, uiState.userLongitude) {
        cameraPositionState.animate(
            update = CameraUpdateFactory.newLatLngZoom(
                LatLng(uiState.userLatitude, uiState.userLongitude),
                15f
            )
        )
    }

    LaunchedEffect(Unit) {
        cameraEvents.collect { latLng ->
            cameraPositionState.animate(
                update = CameraUpdateFactory.newLatLngZoom(latLng, 16f)
            )
        }
    }

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
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            PlaceAutocompleteTextField(
                query = searchField,
                onQueryChange = { searchField = it },
                onPlaceSelected = { result ->
                    searchField = TextFieldValue(result.name)
                    viewModel.focusOnPlace(result.latLng)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            MapSection(
                cameraPositionState = cameraPositionState,
                carparks = carparks,
                onMarkerClick = { carpark ->
                    onNavigateToCarparkDetails(carpark.id)
                },
                showMyLocation = hasLocationPermission
            )

            if (!hasLocationPermission) {
                PermissionRationale(onRequestPermission = { locationPermissionState.launchPermissionRequest() })
            }

            Text(
                text = stringResource(id = R.string.home_nearby_title),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp)
            )

            if (uiState.isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
            } else {
                Text(
                    text = stringResource(id = R.string.home_nearby_count, carparks.size),
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }

            AvailabilityLegend(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(top = 12.dp),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(carparks, key = { it.id }) { carpark ->
                    CarparkListItem(
                        carpark = carpark,
                        onFocus = {
                            viewModel.focusOnPlace(LatLng(it.latitude, it.longitude))
                        },
                        onNavigateDetails = onNavigateToCarparkDetails
                    )
                }
            }
        }
    }
}

@Composable
private fun MapSection(
    cameraPositionState: CameraPositionState,
    carparks: List<CarparkUiModel>,
    onMarkerClick: (CarparkUiModel) -> Unit,
    showMyLocation: Boolean
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1.2f)
    ) {
        val mapProperties = MapProperties(isMyLocationEnabled = showMyLocation)
        val mapUiSettings = MapUiSettings(zoomControlsEnabled = false, myLocationButtonEnabled = showMyLocation)

        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState,
            properties = mapProperties,
            uiSettings = mapUiSettings
        ) {
            carparks.forEach { carpark ->
                val hue = when (carpark.availabilityStatus) {
                    OccupancyStatus.HIGH -> BitmapDescriptorFactory.HUE_GREEN
                    OccupancyStatus.MODERATE -> BitmapDescriptorFactory.HUE_ORANGE
                    OccupancyStatus.LOW -> BitmapDescriptorFactory.HUE_RED
                    OccupancyStatus.EMPTY -> BitmapDescriptorFactory.HUE_ROSE
                }
                val snippet = buildString {
                    append("Lots: ${carpark.availabilityLabel}")
                    carpark.hourlyRateLabel?.let { append(" • $it") }
                }
                Marker(
                    state = MarkerState(position = LatLng(carpark.latitude, carpark.longitude)),
                    title = carpark.name,
                    snippet = snippet,
                    icon = BitmapDescriptorFactory.defaultMarker(hue),
                    onClick = {
                        onMarkerClick(carpark)
                        true
                    }
                )
            }
        }
    }
}

@Composable
private fun PermissionRationale(onRequestPermission: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Text(
            text = stringResource(id = R.string.location_permission_required),
            style = MaterialTheme.typography.bodyMedium
        )
        Button(
            onClick = onRequestPermission,
            modifier = Modifier.padding(top = 8.dp)
        ) {
            Text(text = stringResource(id = R.string.grant_permission))
        }
    }
}

@Composable
private fun CarparkListItem(
    carpark: CarparkUiModel,
    onFocus: (CarparkUiModel) -> Unit,
    onNavigateDetails: (String) -> Unit
) {
    val statusColor = when (carpark.availabilityStatus) {
        OccupancyStatus.HIGH -> MaterialTheme.colorScheme.primary
        OccupancyStatus.MODERATE -> MaterialTheme.colorScheme.tertiary
        OccupancyStatus.LOW -> MaterialTheme.colorScheme.error
        OccupancyStatus.EMPTY -> MaterialTheme.colorScheme.error
    }
    val priceColor = when (carpark.priceTier) {
        PriceTier.BUDGET -> MaterialTheme.colorScheme.primaryContainer
        PriceTier.STANDARD -> MaterialTheme.colorScheme.secondaryContainer
        PriceTier.PREMIUM -> MaterialTheme.colorScheme.tertiaryContainer
        PriceTier.UNKNOWN -> MaterialTheme.colorScheme.surfaceVariant
    }
    ElevatedCard(
        onClick = { onFocus(carpark) },
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = carpark.name, style = MaterialTheme.typography.titleMedium)
                    Text(text = carpark.address, style = MaterialTheme.typography.bodySmall)
                }
                AssistChip(
                    onClick = { onFocus(carpark) },
                    label = { Text(carpark.priceTierLabel) },
                    colors = AssistChipDefaults.assistChipColors(containerColor = priceColor)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(text = "Availability", style = MaterialTheme.typography.labelMedium)
            LinearProgressIndicator(
                progress = carpark.availabilityRatio.coerceIn(0f, 1f),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                color = statusColor
            )
            Text(text = carpark.availabilityLabel, style = MaterialTheme.typography.bodyLarge)

            Spacer(modifier = Modifier.height(8.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AssistChip(
                    onClick = { /* no-op */ },
                    label = { Text(carpark.statusLabel) },
                    colors = AssistChipDefaults.assistChipColors(containerColor = statusColor.copy(alpha = 0.2f))
                )
                carpark.hourlyRateLabel?.let { rateLabel ->
                    AssistChip(
                        onClick = { /* no-op */ },
                        label = { Text(rateLabel) },
                        colors = AssistChipDefaults.assistChipColors(containerColor = priceColor.copy(alpha = 0.3f))
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            TextButton(onClick = { onNavigateDetails(carpark.id) }, modifier = Modifier.align(Alignment.End)) {
                Text(text = stringResource(id = R.string.view_details))
            }
        }
    }
}

@Composable
private fun AvailabilityLegend(modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            LegendChip(color = MaterialTheme.colorScheme.primary, label = "> 55% lots")
            LegendChip(color = MaterialTheme.colorScheme.tertiary, label = "25% - 55%")
            LegendChip(color = MaterialTheme.colorScheme.error, label = "< 25%")
        }
        Spacer(modifier = Modifier.height(4.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            LegendChip(color = MaterialTheme.colorScheme.primaryContainer, label = "Budget pricing")
            LegendChip(color = MaterialTheme.colorScheme.secondaryContainer, label = "Standard pricing")
            LegendChip(color = MaterialTheme.colorScheme.tertiaryContainer, label = "Premium pricing")
        }
    }
}

@Composable
private fun LegendChip(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .background(color, shape = CircleShape)
                .size(12.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(text = label, style = MaterialTheme.typography.labelSmall)
    }
}