
package com.sc2006.spaze.presentation.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.sc2006.spaze.presentation.viewmodel.AuthViewModel
import com.sc2006.spaze.presentation.viewmodel.FavoritesViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FavoritesScreen(
    viewModel: FavoritesViewModel = hiltViewModel(),
    authViewModel: AuthViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit,
    onNavigateToCarparkDetails: (String) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val favorites by viewModel.favoriteCarparks.collectAsState()
    val auth by authViewModel.uiState.collectAsState()
    val userId = auth.currentUser?.userID ?: ""

    LaunchedEffect(userId) {
        if (userId.isNotBlank()) viewModel.loadFavorites(userId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Favorites") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) { Icon(Icons.Default.ArrowBack, "Back") }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier.fillMaxSize().padding(paddingValues).padding(16.dp)
        ) {
            when {
                uiState.isLoading -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                favorites.isEmpty() -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No favorite carparks yet")
                    }
                }
                else -> {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(favorites) { cp ->
                            Card(
                                modifier = Modifier.fillMaxWidth().clickable {
                                    onNavigateToCarparkDetails(cp.carparkNumber)
                                }
                            ) {
                                Column(Modifier.padding(16.dp)) {
                                    Text(cp.address, style = MaterialTheme.typography.titleMedium)
                                    Spacer(Modifier.height(4.dp))
                                    Text(
                                        "Available: ${cp.getTotalAvailableLots()} / ${cp.getTotalLots()}",
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
