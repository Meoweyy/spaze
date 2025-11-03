// app/src/main/java/com/sc2006/spaze/di/ApiKeyModule.kt
package com.sc2006.spaze.di

import com.sc2006.spaze.BuildConfig
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Named
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object ApiKeyModule {

    @Provides
    @Singleton
    @Named("ltaApiKey")
    fun provideLtaApiKey(): String = ""  // no key needed

    @Provides
    @Singleton
    @Named("googleMapsApiKey")
    fun provideGoogleMapsApiKey(): String {
        return try {
            BuildConfig.GOOGLE_MAPS_API_KEY
        } catch (e: Exception) {
            "YOUR_API_KEY_HERE" // Fallback
        }
    }
}
