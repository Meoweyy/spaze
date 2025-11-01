package com.sc2006.spaze.presentation

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.google.android.libraries.places.api.Places
import com.sc2006.spaze.BuildConfig
import com.sc2006.spaze.presentation.navigation.SpazeNavigation
import com.sc2006.spaze.presentation.theme.SpazeTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val apiKey = BuildConfig.MAPS_API_KEY
        if (apiKey.isBlank()) {
            Log.e("MainActivity", "MAPS_API_KEY is blank at runtime")
            Toast.makeText(this, "Missing MAPS_API_KEY. Check local.properties.", Toast.LENGTH_LONG).show()
        } else {
            Log.d("MainActivity", "MAPS_API_KEY loaded: $apiKey")
        }

        if (!Places.isInitialized() && apiKey.isNotBlank()) {
            Places.initialize(applicationContext, apiKey)
        }
        setContent {
            SpazeTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    SpazeNavigation(startDestination = "home")
                }
            }
        }
    }
}