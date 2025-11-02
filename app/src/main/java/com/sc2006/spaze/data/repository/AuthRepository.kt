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

