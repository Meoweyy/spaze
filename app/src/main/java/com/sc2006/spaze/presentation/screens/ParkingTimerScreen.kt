
package com.sc2006.spaze.presentation.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.sc2006.spaze.presentation.viewmodel.AuthViewModel
import com.sc2006.spaze.presentation.viewmodel.ParkingSessionViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ParkingTimerScreen(
    onNavigateBack: () -> Unit,
    viewModel: ParkingSessionViewModel = hiltViewModel(),
    authViewModel: AuthViewModel = hiltViewModel()
) {
    val ui by viewModel.uiState.collectAsState()
    val active by viewModel.activeSession.collectAsState()
    val auth by authViewModel.uiState.collectAsState()
    val userId = auth.currentUser?.userID ?: ""

    LaunchedEffect(userId) {
        if (userId.isNotBlank()) viewModel.loadActiveSession(userId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Parking Timer") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) { Icon(Icons.Default.ArrowBack, "Back") }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier.fillMaxSize().padding(paddingValues).padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = ui.elapsedTime, style = MaterialTheme.typography.displayLarge)
            Spacer(Modifier.height(24.dp))
            Text(
                text = "Estimated Cost: SGD ${String.format("%.2f", ui.estimatedCost)}",
                style = MaterialTheme.typography.titleLarge
            )

            Spacer(Modifier.height(32.dp))

            Button(
                onClick = {
                    val sessionId = active?.sessionID ?: return@Button
                    viewModel.endSession(userId, sessionId, null)
                    onNavigateBack()
                },
                modifier = Modifier.fillMaxWidth()
            ) { Text("End Session") }

            if (ui.error != null) {
                Spacer(Modifier.height(12.dp))
                Text(ui.error!!, color = MaterialTheme.colorScheme.error)
            }
            if (ui.successMessage != null) {
                Spacer(Modifier.height(12.dp))
                Text(ui.successMessage!!)
            }
        }
    }
}
