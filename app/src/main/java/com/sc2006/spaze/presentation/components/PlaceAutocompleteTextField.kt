package com.sc2006.spaze.presentation.components

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.google.android.gms.maps.model.LatLng
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.AutocompletePrediction
import com.google.android.libraries.places.api.model.AutocompleteSessionToken
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.net.FetchPlaceRequest
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

private const val PLACES_TAG = "Places"

data class PlaceResult(
    val name: String,
    val latLng: LatLng?
)

/**
 * A reusable autocomplete text field backed by Google Places SDK.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaceAutocompleteTextField(
    query: TextFieldValue,
    onQueryChange: (TextFieldValue) -> Unit,
    onPlaceSelected: (PlaceResult) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "Search for places"
) {
    val context = LocalContext.current
    val placesClient = remember {
        Places.createClient(context)
    }
    val coroutineScope = rememberCoroutineScope()
    val predictions = remember { mutableStateListOf<AutocompletePrediction>() }
    var pendingJob by remember { mutableStateOf<Job?>(null) }
    val sessionToken = remember { AutocompleteSessionToken.newInstance() }
    var lastErrorMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(query.text) {
        pendingJob?.cancel()
        if (query.text.length < 2) {
            predictions.clear()
            return@LaunchedEffect
        }

        pendingJob = coroutineScope.launch(Dispatchers.IO) {
            delay(250)
            val request = FindAutocompletePredictionsRequest.builder()
                .setCountries(listOf("SG"))
                .setSessionToken(sessionToken)
                .setQuery(query.text)
                .build()

            runCatching {
                placesClient.findAutocompletePredictions(request).await().autocompletePredictions
            }.onSuccess { results ->
                predictions.clear()
                predictions.addAll(results)
                lastErrorMessage = null
            }.onFailure { error ->
                android.util.Log.e(PLACES_TAG, "Error fetching predictions: ${'$'}{error.message}")
                predictions.clear()
                lastErrorMessage = error.message
                coroutineScope.launch(Dispatchers.Main) {
                    Toast.makeText(
                        context,
                        "Unable to fetch places. Check API key or network.",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    Column(modifier = modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            placeholder = { Text(placeholder) },
            modifier = Modifier.fillMaxWidth()
        )

        if (predictions.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            Surface(
                tonalElevation = 4.dp,
                shadowElevation = 4.dp,
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.fillMaxWidth()
            ) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 240.dp)
                        .background(MaterialTheme.colorScheme.surface)
                ) {
                    items(predictions, key = { it.placeId }) { prediction ->
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    coroutineScope.launch(Dispatchers.IO) {
                                        val request = FetchPlaceRequest.builder(
                                            prediction.placeId,
                                            listOf(Place.Field.NAME, Place.Field.LAT_LNG)
                                        ).setSessionToken(sessionToken).build()

                                        val place = runCatching {
                                            placesClient.fetchPlace(request).await().place
                                        }.onFailure { error ->
                                            android.util.Log.e(PLACES_TAG, "Error fetching place: ${'$'}{error.message}")
                                            coroutineScope.launch(Dispatchers.Main) {
                                                Toast.makeText(
                                                    context,
                                                    "Unable to fetch place details.",
                                                    Toast.LENGTH_LONG
                                                ).show()
                                            }
                                        }.getOrNull()

                                        val result = if (place != null) {
                                            PlaceResult(
                                                name = place.name ?: prediction.getPrimaryText(null).toString(),
                                                latLng = place.latLng
                                            )
                                        } else {
                                            PlaceResult(
                                                name = prediction.getPrimaryText(null).toString(),
                                                latLng = null
                                            )
                                        }
                                        withContext(Dispatchers.Main) {
                                            onPlaceSelected(result)
                                        }
                                    }
                                    predictions.clear()
                                }
                                .padding(horizontal = 16.dp, vertical = 12.dp)
                        ) {
                            Text(
                                text = prediction.getPrimaryText(null).toString(),
                                style = MaterialTheme.typography.bodyLarge
                            )
                            val secondary = prediction.getSecondaryText(null)
                            if (!secondary.isNullOrBlank()) {
                                Text(
                                    text = secondary.toString(),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        if (prediction != predictions.last()) {
                            Divider()
                        }
                    }
                }
            }
        } else if (lastErrorMessage != null) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "No suggestions: ${'$'}lastErrorMessage",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(horizontal = 8.dp)
            )
        }
    }
}

