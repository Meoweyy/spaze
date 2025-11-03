
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
import com.sc2006.spaze.presentation.viewmodel.SearchViewModel
import com.sc2006.spaze.presentation.viewmodel.AuthViewModel
import com.sc2006.spaze.presentation.viewmodel.SortOption
import com.sc2006.spaze.presentation.viewmodel.LotType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    viewModel: SearchViewModel = hiltViewModel(),
    authViewModel: AuthViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit,
    onNavigateToCarparkDetails: (String) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val results by viewModel.searchResults.collectAsState()
    val auth by authViewModel.uiState.collectAsState()
    val userId = auth.currentUser?.userID ?: ""

    var query by remember { mutableStateOf(uiState.searchQuery) }

    LaunchedEffect(userId) {
        if (userId.isNotBlank()) viewModel.loadRecentSearches(userId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Search Carparks") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) { Icon(Icons.Default.ArrowBack, "Back") }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier.fillMaxSize().padding(paddingValues).padding(16.dp)
        ) {
            OutlinedTextField(
                value = query,
                onValueChange = {
                    query = it
                    if (userId.isNotBlank()) viewModel.searchCarparks(userId, it)
                },
                label = { Text("Search by address or code") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(12.dp))

            // Filters row (simple)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AssistChip(
                    onClick = { viewModel.setSortOption(SortOption.AVAILABILITY) },
                    label = { Text("Sort: Availability") }
                )
                AssistChip(
                    onClick = { viewModel.setSortOption(SortOption.NAME) },
                    label = { Text("Sort: Name") }
                )
                AssistChip(
                    onClick = {
                        viewModel.setLotTypeFilter(
                            if (uiState.selectedLotType == LotType.ALL) LotType.CAR else LotType.ALL
                        )
                    },
                    label = { Text(if (uiState.selectedLotType == LotType.ALL) "Filter: Car" else "Filter: All") }
                )
            }

            Spacer(Modifier.height(12.dp))

            if (uiState.isLoading) {
                LinearProgressIndicator(Modifier.fillMaxWidth())
                Spacer(Modifier.height(8.dp))
            }

            if (uiState.error != null) {
                Text(uiState.error!!, color = MaterialTheme.colorScheme.error)
                Spacer(Modifier.height(8.dp))
            }

            if (results.isEmpty() && query.isNotBlank() && !uiState.isLoading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No results.")
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(results) { cp ->
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
