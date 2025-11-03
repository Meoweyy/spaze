package com.sc2006.spaze.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
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
}

