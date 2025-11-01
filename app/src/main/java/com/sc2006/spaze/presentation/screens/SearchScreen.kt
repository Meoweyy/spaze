package com.sc2006.spaze.presentation.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.sc2006.spaze.presentation.components.PlaceAutocompleteTextField
import com.sc2006.spaze.presentation.components.PlaceResult

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    onNavigateBack: () -> Unit,
    onNavigateToCarparkDetails: (String) -> Unit
) {
    var searchQuery by remember { mutableStateOf(TextFieldValue()) }
    var selectedPlace by remember { mutableStateOf<PlaceResult?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Search Carparks") },
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
                .padding(16.dp)
        ) {
            PlaceAutocompleteTextField(
                query = searchQuery,
                onQueryChange = { searchQuery = it },
                onPlaceSelected = { result ->
                    searchQuery = TextFieldValue(result.name)
                    selectedPlace = result
                    result.latLng?.let { latLng ->
                        // If you later map carparks by lat/lng, you can navigate to details here
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                placeholder = "Search carparks or places"
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Recent Searches",
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text("3 recent searches")

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = buildString {
                    append("Selected: ")
                    selectedPlace?.let { append(it.name) } ?: append("None")
                },
                style = MaterialTheme.typography.titleMedium
            )
        }
    }
}