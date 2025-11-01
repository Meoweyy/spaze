package com.sc2006.spaze

import android.app.Application
import androidx.lifecycle.lifecycleScope
import com.sc2006.spaze.data.repository.AuthRepository
import com.sc2006.spaze.data.repository.CarparkRepository
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class SpazeApplication : Application() {

    @Inject
    lateinit var authRepository: AuthRepository

    @Inject
    lateinit var carparkRepository: CarparkRepository

    override fun onCreate() {
        super.onCreate()

        // Initialize app
        initializeApp()
    }

    private fun initializeApp() {
        // Can't use lifecycleScope in Application class
        // Use GlobalScope or create coroutine scope
        kotlinx.coroutines.GlobalScope.launch {
            // Restore login session
            authRepository.restoreSession()

            // Initialize carpark database from CSV (first launch only)
            carparkRepository.initializeCarparksFromCsv()
        }
    }
}