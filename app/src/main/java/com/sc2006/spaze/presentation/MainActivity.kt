package com.sc2006.spaze.presentation

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.sc2006.spaze.data.preferences.PreferencesDataStore
import com.sc2006.spaze.presentation.navigation.SpazeNavigation
import com.sc2006.spaze.presentation.theme.SpazeTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val context = LocalContext.current
            val isDarkMode by PreferencesDataStore.getDarkModeEnabled(context)
                .collectAsState(initial = isSystemInDarkTheme())
            
            SpazeTheme(darkTheme = isDarkMode) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    SpazeNavigation()
                }
            }
        }
    }
}