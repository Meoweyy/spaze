package com.sc2006.spaze

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

/**
 * Spaze Application Class
 * Entry point for Hilt dependency injection
 */
@HiltAndroidApp
class SpazeApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        // Initialize any application-level components here
    }
}