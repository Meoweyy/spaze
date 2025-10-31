package com.sc2006.spaze.presentation.screens

import android.Manifest
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Divider
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.draw.shadow
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.PermissionStatus
import com.google.accompanist.permissions.rememberPermissionState
import com.google.android.gms.maps.CameraUpdateFactory
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
import com.sc2006.spaze.data.local.entity.CarparkEntity
import com.sc2006.spaze.data.repository.PlaceSuggestion
import com.sc2006.spaze.presentation.viewmodel.HomeViewModel
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
    val suggestions by viewModel.suggestions.collectAsState()
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
            SearchBar(
                searchField = searchField,
                onSearchFieldChange = {
                    searchField = it
                    viewModel.onSearchQueryChange(it.text)
                    if (it.text.length < 2) {
                        viewModel.clearSuggestions()
                    }
                },
                suggestions = suggestions,
                onSuggestionClick = { suggestion ->
                    viewModel.focusOnSuggestion(suggestion)
                    searchField = TextFieldValue(suggestion.name)
                    viewModel.clearSuggestions()
                }
            )

            Spacer(modifier = Modifier.height(12.dp))

            MapSection(
                cameraPositionState = cameraPositionState,
                carparks = carparks,
                onMarkerClick = onNavigateToCarparkDetails,
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
        }
    }
}

@Composable
private fun SearchBar(
    searchField: TextFieldValue,
    onSearchFieldChange: (TextFieldValue) -> Unit,
    suggestions: List<PlaceSuggestion>,
    onSuggestionClick: (PlaceSuggestion) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        OutlinedTextField(
            value = searchField,
            onValueChange = onSearchFieldChange,
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            placeholder = { Text(text = stringResource(id = R.string.home_search_placeholder)) },
            modifier = Modifier
                .fillMaxWidth()
                .semantics { contentDescription = "home_search_field" },
            singleLine = true
        )

        if (suggestions.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(elevation = 4.dp, shape = RoundedCornerShape(12.dp)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column {
                    suggestions.forEachIndexed { index, suggestion ->
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSuggestionClick(suggestion) }
                                .padding(horizontal = 16.dp, vertical = 12.dp)
                        ) {
                            Text(
                                text = suggestion.name,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.SemiBold
                            )

                            Text(
                                text = if (suggestion.carparkId != null) {
                                    stringResource(id = R.string.home_search_result_carpark)
                                } else {
                                    stringResource(id = R.string.home_search_result_place)
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        if (index != suggestions.lastIndex) {
                            Divider()
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MapSection(
    cameraPositionState: CameraPositionState,
    carparks: List<CarparkEntity>,
    onMarkerClick: (String) -> Unit,
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
                Marker(
                    state = MarkerState(position = LatLng(carpark.latitude, carpark.longitude)),
                    title = carpark.address,
                    snippet = stringResource(id = R.string.home_marker_snippet, carpark.availableLots),
                    onClick = {
                        onMarkerClick(carpark.carparkID)
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