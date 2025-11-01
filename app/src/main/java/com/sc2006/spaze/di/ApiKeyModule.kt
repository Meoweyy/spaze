// app/src/main/java/com/sc2006/spaze/di/ApiKeyModule.kt
package com.sc2006.spaze.di

import com.sc2006.spaze.BuildConfig
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object ApiKeyModule {

    @Provides
    @Singleton
    fun provideLtaApiKey(): String = BuildConfig.LTA_API_KEY
}
