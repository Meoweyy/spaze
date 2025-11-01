package com.sc2006.spaze.data.local

import android.content.Context
import android.content.SharedPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages app preferences using SharedPreferences
 * Used for persisting login session
 */
@Singleton
class PreferencesManager @Inject constructor(
    @ApplicationContext context: Context
) {
    private val prefs: SharedPreferences = context.getSharedPreferences(
        "spaze_prefs",
        Context.MODE_PRIVATE
    )

    companion object {
        private const val KEY_CURRENT_USER_ID = "current_user_id"
        private const val KEY_IS_FIRST_LAUNCH = "is_first_launch"
    }

    /**
     * Save current user ID (login session)
     */
    fun setCurrentUserId(userId: String?) {
        prefs.edit().putString(KEY_CURRENT_USER_ID, userId).apply()
    }

    /**
     * Get current user ID (restore session)
     */
    fun getCurrentUserId(): String? {
        return prefs.getString(KEY_CURRENT_USER_ID, null)
    }

    /**
     * Clear login session
     */
    fun clearSession() {
        prefs.edit().remove(KEY_CURRENT_USER_ID).apply()
    }

    /**
     * Check if this is first app launch
     */
    fun isFirstLaunch(): Boolean {
        return prefs.getBoolean(KEY_IS_FIRST_LAUNCH, true)
    }

    /**
     * Mark that app has been launched
     */
    fun setFirstLaunchComplete() {
        prefs.edit().putBoolean(KEY_IS_FIRST_LAUNCH, false).apply()
    }
}