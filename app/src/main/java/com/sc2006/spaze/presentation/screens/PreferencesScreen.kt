package com.sc2006.spaze.presentation.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.sc2006.spaze.data.preferences.PreferencesDataStore
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PreferencesScreen(onNavigateBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    // Collect preferences from DataStore
    val notificationsEnabled by PreferencesDataStore.getNotificationsEnabled(context)
        .collectAsState(initial = false)
    val darkModeEnabled by PreferencesDataStore.getDarkModeEnabled(context)
        .collectAsState(initial = false)
    val searchRadius by PreferencesDataStore.getSearchRadius(context)
        .collectAsState(initial = 5f)
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Preferences") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
                .padding(vertical = 24.dp)
        ) {
            // Notifications Section
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Enable notifications",
                    style = MaterialTheme.typography.bodyLarge
                )
                Switch(
                    checked = notificationsEnabled,
                    onCheckedChange = { enabled ->
                        scope.launch {
                            PreferencesDataStore.setNotificationsEnabled(context, enabled)
                        }
                    }
                )
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            Divider()
            Spacer(modifier = Modifier.height(24.dp))
            
            // Dark Mode Section
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Dark mode",
                    style = MaterialTheme.typography.bodyLarge
                )
                Switch(
                    checked = darkModeEnabled,
                    onCheckedChange = { enabled ->
                        scope.launch {
                            PreferencesDataStore.setDarkModeEnabled(context, enabled)
                        }
                    }
                )
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            Divider()
            Spacer(modifier = Modifier.height(24.dp))
            
            // Search Radius Section
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Default search radius",
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Text(
                        text = "${searchRadius.toInt()} km",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Slider(
                    value = searchRadius,
                    onValueChange = { value ->
                        scope.launch {
                            PreferencesDataStore.setSearchRadius(context, value)
                        }
                    },
                    valueRange = 1f..10f,
                    steps = 8
                )
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "1 km",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        text = "10 km",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}

