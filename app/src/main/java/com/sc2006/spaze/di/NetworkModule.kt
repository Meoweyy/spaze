package com.sc2006.spaze.di

import com.sc2006.spaze.data.remote.api.CarparkApiService
import com.sc2006.spaze.data.remote.api.GoogleMapsApiService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Qualifier
import javax.inject.Singleton

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class CarparkRetrofit

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class GoogleMapsRetrofit

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        return OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .addInterceptor(logging)
            .build()
    }

    @Provides
    @Singleton
    @CarparkRetrofit
    fun provideCarparkRetrofit(client: OkHttpClient): Retrofit =
        Retrofit.Builder()
            .baseUrl(CarparkApiService.BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

    @Provides
    @Singleton
    @GoogleMapsRetrofit
    fun provideGoogleMapsRetrofit(client: OkHttpClient): Retrofit =
        Retrofit.Builder()
            .baseUrl(GoogleMapsApiService.BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

    @Provides
    @Singleton
    fun provideCarparkApiService(@CarparkRetrofit retrofit: Retrofit): CarparkApiService =
        retrofit.create(CarparkApiService::class.java)

    @Provides
    @Singleton
    fun provideGoogleMapsApiService(@GoogleMapsRetrofit retrofit: Retrofit): GoogleMapsApiService =
        retrofit.create(GoogleMapsApiService::class.java)
}