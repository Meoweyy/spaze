package com.sc2006.spaze.data.repository

import com.sc2006.spaze.data.local.PreferencesManager
import com.sc2006.spaze.data.local.dao.UserDao
import com.sc2006.spaze.data.local.entity.UserEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.random.Random
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Authentication Repository (offline / fake auth)
 * Removes Firebase dependencies but keeps same interface.
 * Uses PreferencesManager to persist login sessions locally.
 */
@Singleton
class AuthRepository @Inject constructor(
    private val userDao: UserDao,
    private val preferencesManager: PreferencesManager
) {

    // --- in-memory fake auth store ---
    // These will be lost on app restart, but that's okay - we only need them during runtime
    // The actual session persistence happens via PreferencesManager + Room
    private val accounts = mutableMapOf<String, String>() // email -> password
    private val emailToUid = mutableMapOf<String, String>()

    fun currentUserId(): String? = preferencesManager.getCurrentUserId()

    /** Update username/email for the user entity */
    suspend fun updateUserProfile(
        userId: String,
        userName: String,
        email: String
    ): Result<UserEntity> = runCatching {
        withContext(Dispatchers.IO) {
            val existing = userDao.getUserById(userId) ?: error("User not found")
            // If email changed, ensure uniqueness
            val duplicate = userDao.getUserByEmail(email)
            if (duplicate != null && duplicate.userID != userId) {
                error("Email is already used by another account")
            }

            val updated = existing.copy(
                userName = userName,
                email = email
            )
            userDao.updateUser(updated)
            updated
        }
    }

    /** Get current authenticated user (if any) - restored from PreferencesManager */
    suspend fun getCurrentUser(): UserEntity? {
        val uid = preferencesManager.getCurrentUserId() ?: return null
        return userDao.getUserById(uid)
    }

    /** Get current user as Flow */
    fun getCurrentUserFlow(userId: String): Flow<UserEntity?> =
        userDao.getUserByIdFlow(userId)

    /** Sign up with email and password (local fake auth) */
    suspend fun signUpWithEmail(
        userName: String,
        email: String,
        password: String
    ): Result<UserEntity> = runCatching {
        if (accounts.containsKey(email)) error("Account already exists")

        // Check if account already exists in database
        val existingUser = userDao.getUserByEmail(email)
        if (existingUser != null) error("Account already exists")

        accounts[email] = password
        val uid = "uid_" + Random.nextLong().toString(16)
        emailToUid[email] = uid

        // Save session to SharedPreferences
        preferencesManager.setCurrentUserId(uid)

        val user = UserEntity.create(
            userID = uid,
            userName = userName,
            email = email,
            password = password, // Store hashed password in real app
            authProvider = UserEntity.AuthProvider.EMAIL
        )
        userDao.insertUser(user)
        user
    }

    /** Sign in with email and password */
    suspend fun signInWithEmail(email: String, password: String): Result<UserEntity> = runCatching {
        // Try to get user from database first
        var user = userDao.getUserByEmail(email)

        if (user == null) {
            // User doesn't exist in database
            error("Account not found")
        }

        // Verify password (in a real app, you'd compare hashed passwords)
        if (user.password != null && user.password != password) {
            error("Invalid credentials")
        }

        // Add to in-memory store
        accounts[email] = password
        emailToUid[email] = user.userID

        // Save session to SharedPreferences
        preferencesManager.setCurrentUserId(user.userID)

        userDao.updateLastLogin(user.userID, System.currentTimeMillis())
        user
    }

    /** Fake Google sign-in */
    suspend fun signInWithGoogle(idToken: String): Result<UserEntity> = runCatching {
        val email = "$idToken@example.com"
        val uid: String

        // Check if user already exists in database
        var user = userDao.getUserByEmail(email)

        if (user == null) {
            // Create new user
            uid = "g_${idToken.hashCode()}"
            emailToUid[email] = uid
            accounts[email] = "google"

            user = UserEntity.create(
                userID = uid,
                userName = "GoogleUser",
                email = email,
                password = null,
                authProvider = UserEntity.AuthProvider.GOOGLE
            )
            userDao.insertUser(user)
        } else {
            uid = user.userID
            emailToUid[email] = uid
            accounts[email] = "google"
        }

        // Save session to SharedPreferences
        preferencesManager.setCurrentUserId(uid)

        userDao.updateLastLogin(uid, System.currentTimeMillis())
        user
    }

    /** Reset password (no-op in fake mode) */
    suspend fun resetPassword(email: String): Result<Unit> = runCatching {
        val user = userDao.getUserByEmail(email)
        if (user == null) error("Account not found")
    }

    /** Sign out */
    suspend fun signOut(): Result<Unit> = runCatching {
        preferencesManager.clearSession()
        accounts.clear()
        emailToUid.clear()
    }

    /** Check if user is authenticated */
    fun isAuthenticated(): Boolean = preferencesManager.getCurrentUserId() != null

    /** Update user preferences */
    suspend fun updateUserPreferences(userId: String, preferences: Map<String, Any>): Result<Unit> =
        runCatching {
            val user = userDao.getUserById(userId) ?: error("User not found")
            val updatedUser = user.setPreferences(preferences)
            userDao.updateUser(updatedUser)
        }
}