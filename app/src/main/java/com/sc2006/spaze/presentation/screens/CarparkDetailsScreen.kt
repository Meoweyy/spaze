
package com.sc2006.spaze.presentation.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.sc2006.spaze.presentation.viewmodel.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CarparkDetailsScreen(
    carparkId: String,
    onNavigateBack: () -> Unit,
    onStartParkingSession: () -> Unit,
    detailsViewModel: CarparkDetailsViewModel = hiltViewModel(),
    authViewModel: AuthViewModel = hiltViewModel(),
    sessionViewModel: ParkingSessionViewModel = hiltViewModel()
) {
    val uiState by detailsViewModel.uiState.collectAsState()
    val carpark by detailsViewModel.carpark.collectAsState()
    val auth by authViewModel.uiState.collectAsState()
    val userId = auth.currentUser?.userID ?: ""

    LaunchedEffect(userId, carparkId) {
        if (userId.isNotBlank() && carparkId.isNotBlank()) {
            detailsViewModel.loadCarparkDetails(userId, carparkId)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Carpark Details") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) { Icon(Icons.Default.ArrowBack, "Back") }
                },
                actions = {
                    IconButton(
                        onClick = {
                            if (userId.isNotBlank() && carparkId.isNotBlank())
                                detailsViewModel.toggleFavorite(userId, carparkId)
                        }
                    ) {
                        Icon(
                            if (uiState.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            "Favorite"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier.fillMaxSize().padding(paddingValues).padding(16.dp)
        ) {
            if (uiState.error != null) {
                Text(uiState.error!!, color = MaterialTheme.colorScheme.error)
                Spacer(Modifier.height(8.dp))
            }
            if (uiState.isLoading) {
                LinearProgressIndicator(Modifier.fillMaxWidth())
                Spacer(Modifier.height(12.dp))
            }

            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Text("Carpark ID: $carparkId", style = MaterialTheme.typography.titleLarge)
                    Spacer(Modifier.height(8.dp))
                    Text(carpark?.address ?: "Loading...", style = MaterialTheme.typography.bodyLarge)
                    Spacer(Modifier.height(12.dp))
                    Text(
                        "Available: ${carpark?.getTotalAvailableLots() ?: 0} / ${carpark?.getTotalLots() ?: 0}",
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }

            Spacer(Modifier.height(24.dp))

            Button(
                onClick = {
                    val cp = carpark ?: return@Button
                    if (userId.isBlank()) return@Button
                    // Start session here, then navigate
                    sessionViewModel.startSession(
                        userId = userId,
                        carparkId = cp.carparkNumber,
                        carparkName = cp.address,
                        carparkAddress = cp.address,
                        budgetCap = null
                    )
                    onStartParkingSession()
                },
                modifier = Modifier.fillMaxWidth()
            ) { Text("Start Parking Session") }

            Spacer(Modifier.height(8.dp))

            OutlinedButton(
                onClick = {
                    detailsViewModel.getDirectionsUrl()?.let { url ->
                        // Hook: open CustomTabs or intent in your app shell
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) { Text("Get Directions") }
        }
    }
}
