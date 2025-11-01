package com.sc2006.spaze.data.repository

import com.sc2006.spaze.data.local.dao.UserDao
import com.sc2006.spaze.data.local.entity.UserEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.random.Random

/**
 * Authentication Repository (offline / fake auth)
 * Removes Firebase dependencies but keeps same interface.
 */
@Singleton
class AuthRepository @Inject constructor(
    private val userDao: UserDao
) {

    // --- in-memory fake auth store ---
    private var currentUid: String? = null
    private val accounts = mutableMapOf<String, String>() // email -> password
    private val emailToUid = mutableMapOf<String, String>()

    /** Get current authenticated user (if any) */
    suspend fun getCurrentUser(): UserEntity? {
        val uid = currentUid ?: return null
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
        accounts[email] = password
        val uid = "uid_" + Random.nextLong().toString(16)
        emailToUid[email] = uid
        currentUid = uid

        val user = UserEntity.create(
            userID = uid,
            userName = userName,
            email = email,
            password = null,
            authProvider = UserEntity.AuthProvider.EMAIL
        )
        userDao.insertUser(user)
        user
    }

    /** Sign in with email and password */
    suspend fun signInWithEmail(email: String, password: String): Result<UserEntity> = runCatching {
        val stored = accounts[email] ?: error("Account not found")
        if (stored != password) error("Invalid credentials")
        val uid = emailToUid[email]!!
        currentUid = uid

        var user = userDao.getUserById(uid)
        if (user == null) {
            user = UserEntity.create(
                userID = uid,
                userName = email.substringBefore('@'),
                email = email,
                password = null,
                authProvider = UserEntity.AuthProvider.EMAIL
            )
            userDao.insertUser(user)
        }

        userDao.updateLastLogin(user.userID, System.currentTimeMillis())
        user
    }

    /** Fake Google sign-in */
    suspend fun signInWithGoogle(idToken: String): Result<UserEntity> = runCatching {
        val email = "$idToken@example.com"
        if (!emailToUid.containsKey(email)) {
            val uid = "g_${idToken.hashCode()}"
            emailToUid[email] = uid
            accounts[email] = "google"
        }
        val uid = emailToUid[email]!!
        currentUid = uid

        var user = userDao.getUserById(uid)
        if (user == null) {
            user = UserEntity.create(
                userID = uid,
                userName = "GoogleUser",
                email = email,
                password = null,
                authProvider = UserEntity.AuthProvider.GOOGLE
            )
            userDao.insertUser(user)
        }

        userDao.updateLastLogin(user.userID, System.currentTimeMillis())
        user
    }

    /** Reset password (no-op in fake mode) */
    suspend fun resetPassword(email: String): Result<Unit> = runCatching {
        if (!accounts.containsKey(email)) error("Account not found")
    }

    /** Sign out */
    suspend fun signOut(): Result<Unit> = runCatching {
        currentUid = null
    }

    /** Check if user is authenticated */
    fun isAuthenticated(): Boolean = currentUid != null

    /** Update user preferences */
    suspend fun updateUserPreferences(userId: String, preferences: Map<String, Any>): Result<Unit> =
        runCatching {
            val user = userDao.getUserById(userId) ?: error("User not found")
            val updatedUser = user.setPreferences(preferences)
            userDao.updateUser(updatedUser)
        }

    // ===================================================================================
    // TODO - Person 4: Implement the following methods for Person 3's ProfileViewModel
    // ===================================================================================

    /**
     * TODO - Person 4: Update user profile (username and email)
     * Required by ProfileViewModel.updateProfile() (Task 5.1)
     *
     * Implementation steps:
     * 1. Get user from database using userId
     * 2. Update userName and email fields
     * 3. Save updated user to database
     * 4. Return Result<UserEntity>
     */
    /*
    suspend fun updateUserProfile(userId: String, userName: String, email: String): Result<UserEntity> =
        runCatching {
            val user = userDao.getUserById(userId) ?: error("User not found")
            val updatedUser = user.copy(userName = userName, email = email)
            userDao.updateUser(updatedUser)
            updatedUser
        }
    */

    /**
     * TODO - Person 4: Change user password
     * Required by ProfileViewModel.changePassword() (Task 5.2)
     *
     * Implementation steps:
     * 1. Verify old password matches stored password
     * 2. Update password in accounts map
     * 3. Return Result<Unit>
     */
    /*
    suspend fun changePassword(userId: String, oldPassword: String, newPassword: String): Result<Unit> =
        runCatching {
            val user = userDao.getUserById(userId) ?: error("User not found")
            val storedPassword = accounts[user.email] ?: error("Password not found")
            if (storedPassword != oldPassword) error("Incorrect old password")
            accounts[user.email] = newPassword
        }
    */

    // ===================================================================================
    // TODO - Person 4: DataStore Integration for Session Persistence
    // ===================================================================================

    /**
     * TODO - Person 4: Integrate DataStore for persistent authentication
     *
     * Steps:
     * 1. Create AuthPreferencesManager in data/local/ package
     * 2. Inject AuthPreferencesManager into AuthRepository constructor
     * 3. In signUpWithEmail() and signInWithEmail(): Call authPreferences.saveAuthState(uid, email)
     * 4. In signOut(): Call authPreferences.clearAuthState()
     * 5. Create restoreSession() method to load auth state from DataStore on app start
     *
     * Example:
     * suspend fun restoreSession() {
     *     val authState = authPreferences.getAuthStateSync()
     *     if (authState.isAuthenticated) {
     *         currentUid = authState.uid
     *     }
     * }
     */
}

