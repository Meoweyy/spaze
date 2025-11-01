package com.sc2006.spaze

import android.app.Application
import android.util.Log
import com.sc2006.spaze.data.repository.CarparkRepository
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Spaze Application Class
 * Entry point for Hilt dependency injection
 */
@HiltAndroidApp
class SpazeApplication : Application() {

    @Inject lateinit var carparkRepository: CarparkRepository
    private val appScope = CoroutineScope(Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        appScope.launch {
            val result = carparkRepository.refreshCarparkAvailability()
            result.onSuccess {
                Log.d("SpazeApp", "Carpark data refreshed")
            }.onFailure { error ->
                Log.e("SpazeApp", "Failed to refresh carpark data", error)
                carparkRepository.seedSampleCarparks()
            }
        }
    }
}
