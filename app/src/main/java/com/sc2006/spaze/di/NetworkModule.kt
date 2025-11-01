// app/src/main/java/com/sc2006/spaze/di/NetworkModule.kt
package com.sc2006.spaze.di

import com.sc2006.spaze.data.api.CarparkApiService
import com.sc2006.spaze.data.api.RetrofitClient
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideCarparkApiService(): CarparkApiService = RetrofitClient.instance
}
