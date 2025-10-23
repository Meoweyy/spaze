package com.sc2006.spaze.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

/**
 * User Entity - Represents a user in the system
 * Maps to the User class in the class diagram
 */
@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey
    val userID: String,
    val userName: String,
    val email: String,
    val password: String?, // Nullable for Google/Social login
    val preferencesJson: String = "{}", // Stored as JSON string
    val createdAt: Long = System.currentTimeMillis(),
    val lastLogin: Long = System.currentTimeMillis(),
    val authProvider: AuthProvider = AuthProvider.EMAIL
) {
    enum class AuthProvider {
        EMAIL,
        GOOGLE,
        SOCIAL
    }

    /**
     * Get preferences as a Map
     */
    fun getPreferences(): Map<String, Any> {
        return try {
            val type = object : TypeToken<Map<String, Any>>() {}.type
            Gson().fromJson(preferencesJson, type) ?: emptyMap()
        } catch (e: Exception) {
            emptyMap()
        }
    }

    /**
     * Set preferences from a Map
     */
    fun setPreferences(prefs: Map<String, Any>): UserEntity {
        val json = Gson().toJson(prefs)
        return this.copy(preferencesJson = json)
    }

    companion object {
        /**
         * Create a new user entity
         */
        fun create(
            userID: String,
            userName: String,
            email: String,
            password: String?,
            authProvider: AuthProvider
        ): UserEntity {
            return UserEntity(
                userID = userID,
                userName = userName,
                email = email,
                password = password,
                authProvider = authProvider
            )
        }
    }
}
