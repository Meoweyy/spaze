package com.sc2006.spaze.data.repository

import android.util.Log
import com.sc2006.spaze.data.local.dao.UserDao
import com.sc2006.spaze.data.local.entity.UserEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flowOf
import javax.inject.Inject
import javax.inject.Singleton
import java.security.MessageDigest
import com.sc2006.spaze.data.local.PreferencesManager

/**
 * Authentication Repository - Local Room-based authentication
 * No Firebase, just local database
 */
@Singleton
class AuthRepository @Inject constructor(
    private val userDao: UserDao,
    private val preferencesManager: PreferencesManager
) {

    companion object {
        private const val TAG = "AuthRepository"
    }

    // Current logged-in user ID (persisted across app restarts)
    private val _currentUserId = MutableStateFlow<String?>(null)
    val currentUserId: StateFlow<String?> = _currentUserId.asStateFlow()

    /**
     * Get current authenticated user
     */
    suspend fun getCurrentUser(): UserEntity? {
        val userId = _currentUserId.value ?: return null
        return userDao.getUserById(userId)
    }

    /**
     * Get current user as Flow (reactive)
     */
    fun getCurrentUserFlow(userId1: String): Flow<UserEntity?> {
        val userId = _currentUserId.value ?: return flowOf(null)
        return userDao.getUserByIdFlow(userId)
    }

    /**
     * Sign up with email and password
     */
    suspend fun signUpWithEmail(
        userName: String,
        email: String,
        password: String
    ): Result<UserEntity> {
        return try {
            // Validate input
            require(userName.isNotBlank()) { "Username cannot be empty" }
            require(email.isNotBlank()) { "Email cannot be empty" }
            require(email.contains("@")) { "Invalid email format" }
            require(password.length >= 6) { "Password must be at least 6 characters" }

            // Check if user already exists
            val existingUser = userDao.getUserByEmail(email)
            if (existingUser != null) {
                return Result.failure(Exception("An account with this email already exists"))
            }

            // Hash password
            val hashedPassword = hashPassword(password)

            // Create user
            val userId = generateUserId(email)
            val user = UserEntity.create(
                userID = userId,
                userName = userName,
                email = email,
                password = hashedPassword,
                authProvider = UserEntity.AuthProvider.EMAIL
            )

            // Save to database
            userDao.insertUser(user)

            // Save session
            _currentUserId.value = userId
            preferencesManager.setCurrentUserId(userId) // ← persist

            Log.d(TAG, "User signed up successfully: $email")
            Result.success(user)
        } catch (e: Exception) {
            Log.e(TAG, "Sign up failed", e)
            Result.failure(e)
        }
    }

    /**
     * Sign in with email and password
     */
    suspend fun signInWithEmail(
        email: String,
        password: String
    ): Result<UserEntity> {
        return try {
            // Validate input
            require(email.isNotBlank()) { "Email cannot be empty" }
            require(password.isNotBlank()) { "Password cannot be empty" }

            // Get user from database
            val user = userDao.getUserByEmail(email)
                ?: return Result.failure(Exception("No account found with this email"))

            // Check if password is correct
            val hashedPassword = hashPassword(password)
            if (user.password != hashedPassword) {
                return Result.failure(Exception("Incorrect password"))
            }

            // Update last login timestamp
            userDao.updateLastLogin(user.userID, System.currentTimeMillis())

            // Save session
            _currentUserId.value = user.userID
            preferencesManager.setCurrentUserId(user.userID) // ← persist

            Log.d(TAG, "User signed in successfully: $email")
            Result.success(user)
        } catch (e: Exception) {
            Log.e(TAG, "Sign in failed", e)
            Result.failure(e)
        }
    }

    /**
     * Sign out
     */
    suspend fun signOut(): Result<Unit> {
        return try {
            _currentUserId.value = null
            preferencesManager.clearSession() // ← clear persisted session
            Log.d(TAG, "User signed out")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Sign out failed", e)
            Result.failure(e)
        }
    }

    /**
     * Check if user is authenticated
     */
    fun isAuthenticated(): Boolean {
        return _currentUserId.value != null
    }

    /**
     * Reset password (for logged-in user)
     */
    suspend fun changePassword(
        userId: String,
        oldPassword: String,
        newPassword: String
    ): Result<Unit> {
        return try {
            require(newPassword.length >= 6) { "Password must be at least 6 characters" }

            val user = userDao.getUserById(userId)
                ?: return Result.failure(Exception("User not found"))

            // Verify old password
            val hashedOldPassword = hashPassword(oldPassword)
            if (user.password != hashedOldPassword) {
                return Result.failure(Exception("Current password is incorrect"))
            }

            // Update password
            val hashedNewPassword = hashPassword(newPassword)
            val updatedUser = user.copy(password = hashedNewPassword)
            userDao.updateUser(updatedUser)

            Log.d(TAG, "Password changed successfully")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Password change failed", e)
            Result.failure(e)
        }
    }

    /**
     * Update user profile
     */
    suspend fun updateProfile(
        userId: String,
        userName: String? = null,
        email: String? = null
    ): Result<Unit> {
        return try {
            val user = userDao.getUserById(userId)
                ?: return Result.failure(Exception("User not found"))

            val updatedUser = user.copy(
                userName = userName ?: user.userName,
                email = email ?: user.email
            )

            userDao.updateUser(updatedUser)
            Log.d(TAG, "Profile updated successfully")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Profile update failed", e)
            Result.failure(e)
        }
    }

    /**
     * Update user preferences
     */
    suspend fun updateUserPreferences(
        userId: String,
        preferences: Map<String, Any>
    ): Result<Unit> {
        return try {
            val user = userDao.getUserById(userId)
                ?: return Result.failure(Exception("User not found"))

            val updatedUser = user.setPreferences(preferences)
            userDao.updateUser(updatedUser)

            Log.d(TAG, "Preferences updated successfully")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Preferences update failed", e)
            Result.failure(e)
        }
    }

    /**
     * Delete account
     */
    suspend fun deleteAccount(userId: String, password: String): Result<Unit> {
        return try {
            val user = userDao.getUserById(userId)
                ?: return Result.failure(Exception("User not found"))

            // Verify password
            val hashedPassword = hashPassword(password)
            if (user.password != hashedPassword) {
                return Result.failure(Exception("Incorrect password"))
            }

            // Delete user
            userDao.deleteUser(user)

            // Sign out & clear persisted session
            _currentUserId.value = null
            preferencesManager.clearSession()

            Log.d(TAG, "Account deleted successfully")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Account deletion failed", e)
            Result.failure(e)
        }
    }

    /**
     * Set current user (for programmatic overrides)
     */
    fun setCurrentUserId(userId: String?) {
        _currentUserId.value = userId
        if (userId == null) {
            preferencesManager.clearSession()
        } else {
            preferencesManager.setCurrentUserId(userId)
        }
    }

    /**
     * Restore session on app startup
     */
    suspend fun restoreSession(): UserEntity? {
        return try {
            val userId = preferencesManager.getCurrentUserId()
            if (userId != null) {
                val user = userDao.getUserById(userId)
                if (user != null) {
                    _currentUserId.value = userId
                    Log.d(TAG, "Session restored for user: ${user.email}")
                    return user
                } else {
                    // User ID exists but user not in database - clear session
                    preferencesManager.clearSession()
                }
            }
            null
        } catch (e: Exception) {
            Log.e(TAG, "Failed to restore session", e)
            null
        }
    }

    // ═══════════════════════════════════════════════════
    // HELPER FUNCTIONS
    // ═══════════════════════════════════════════════════

    /**
     * Generate user ID from email
     */
    private fun generateUserId(email: String): String {
        val timestamp = System.currentTimeMillis()
        return "user_${email.hashCode().toString().replace("-", "")}_$timestamp"
    }

    /**
     * Hash password using SHA-256
     * NOTE: In production, use bcrypt, scrypt, or Argon2
     */
    private fun hashPassword(password: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(password.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }
}
