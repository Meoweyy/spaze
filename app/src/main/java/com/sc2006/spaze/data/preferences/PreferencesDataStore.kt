package com.sc2006.spaze.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_preferences")

object PreferencesDataStore {
    private val NOTIFICATIONS_ENABLED = booleanPreferencesKey("notifications_enabled")
    private val DARK_MODE_ENABLED = booleanPreferencesKey("dark_mode_enabled")
    private val SEARCH_RADIUS_KM = floatPreferencesKey("search_radius_km")
    // Live location toggle
    private val LIVE_LOCATION_ENABLED = booleanPreferencesKey("live_location_enabled")
    // Reference position (used when live location is disabled)
    private val REFERENCE_LAT = doublePreferencesKey("reference_lat")
    private val REFERENCE_LNG = doublePreferencesKey("reference_lng")
    private val REFERENCE_NAME = stringPreferencesKey("reference_name")

    fun getNotificationsEnabled(context: Context): Flow<Boolean> {
        return context.dataStore.data.map { preferences ->
            preferences[NOTIFICATIONS_ENABLED] ?: false
        }
    }

    suspend fun setNotificationsEnabled(context: Context, enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[NOTIFICATIONS_ENABLED] = enabled
        }
    }

    fun getDarkModeEnabled(context: Context): Flow<Boolean> {
        return context.dataStore.data.map { preferences ->
            preferences[DARK_MODE_ENABLED] ?: false
        }
    }

    suspend fun setDarkModeEnabled(context: Context, enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[DARK_MODE_ENABLED] = enabled
        }
    }

    fun getSearchRadius(context: Context): Flow<Float> {
        return context.dataStore.data.map { preferences ->
            preferences[SEARCH_RADIUS_KM] ?: 5f
        }
    }

    suspend fun setSearchRadius(context: Context, radius: Float) {
        context.dataStore.edit { preferences ->
            preferences[SEARCH_RADIUS_KM] = radius
        }
    }

    // Live location toggle
    fun getLiveLocationEnabled(context: Context): Flow<Boolean> {
        return context.dataStore.data.map { preferences ->
            preferences[LIVE_LOCATION_ENABLED] ?: false
        }
    }

    suspend fun setLiveLocationEnabled(context: Context, enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[LIVE_LOCATION_ENABLED] = enabled
        }
    }

    // Reference position
    fun getReferenceLat(context: Context): Flow<Double?> {
        return context.dataStore.data.map { preferences ->
            preferences[REFERENCE_LAT]
        }
    }

    fun getReferenceLng(context: Context): Flow<Double?> {
        return context.dataStore.data.map { preferences ->
            preferences[REFERENCE_LNG]
        }
    }

    fun getReferenceName(context: Context): Flow<String?> {
        return context.dataStore.data.map { preferences ->
            preferences[REFERENCE_NAME]
        }
    }

    suspend fun setReferenceLatLng(
        context: Context,
        latitude: Double,
        longitude: Double,
        name: String? = null
    ) {
        context.dataStore.edit { preferences ->
            preferences[REFERENCE_LAT] = latitude
            preferences[REFERENCE_LNG] = longitude
            name?.let { preferences[REFERENCE_NAME] = it }
        }
    }
}

