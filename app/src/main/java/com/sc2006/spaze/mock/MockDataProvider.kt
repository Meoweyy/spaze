package com.sc2006.spaze.mock

import com.sc2006.spaze.data.local.entity.CarparkEntity
import com.sc2006.spaze.data.local.entity.ParkingSessionEntity
import com.sc2006.spaze.data.local.entity.RecentSearchEntity
import com.sc2006.spaze.data.local.entity.UserEntity
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/**
 * MockDataProvider - Provides realistic sample data for UI testing
 *
 * Week 1 - Person 3: Mock data for Person 1 & 2 (UI team)
 *
 * Usage:
 * - Use these mock ViewModels during UI development
 * - No need for repository/database setup
 * - Simulates loading delays and realistic data
 * - Switch to real ViewModels when repository is ready
 */
object MockDataProvider {

    // Mock delay to simulate API calls (in milliseconds)
    const val MOCK_DELAY_SHORT = 500L
    const val MOCK_DELAY_MEDIUM = 1000L
    const val MOCK_DELAY_LONG = 1500L

    /**
     * Mock Carparks - Singapore locations
     */
    val mockCarparks = listOf(
        CarparkEntity(
            carparkID = "CP001",
            location = "Orchard Central",
            address = "181 Orchard Road, Singapore 238896",
            latitude = 1.301200,
            longitude = 103.838000,
            totalLots = 150,
            availableLots = 45,
            lotTypes = """{"car": 120, "motorcycle": 20, "ev": 8, "accessible": 2}""",
            pricingInfo = """{"weekday_rate": "2.50", "weekend_rate": "3.00", "peak_rate": "4.00"}""",
            operatingHours = "24/7",
            distanceFromUser = 250.5f,
            estimatedWalkTime = 3,
            estimatedDriveTime = 2,
            isFavorite = true
        ),
        CarparkEntity(
            carparkID = "CP002",
            location = "Marina Square",
            address = "6 Raffles Boulevard, Singapore 039594",
            latitude = 1.291400,
            longitude = 103.857100,
            totalLots = 300,
            availableLots = 180,
            lotTypes = """{"car": 250, "motorcycle": 40, "ev": 8, "accessible": 2}""",
            pricingInfo = """{"weekday_rate": "2.00", "weekend_rate": "2.50", "peak_rate": "3.50"}""",
            operatingHours = "24/7",
            distanceFromUser = 1200.0f,
            estimatedWalkTime = 15,
            estimatedDriveTime = 5,
            isFavorite = false
        ),
        CarparkEntity(
            carparkID = "CP003",
            location = "ION Orchard",
            address = "2 Orchard Turn, Singapore 238801",
            latitude = 1.304200,
            longitude = 103.831900,
            totalLots = 400,
            availableLots = 12,
            lotTypes = """{"car": 350, "motorcycle": 30, "ev": 15, "accessible": 5}""",
            pricingInfo = """{"weekday_rate": "3.00", "weekend_rate": "4.00", "peak_rate": "5.00"}""",
            operatingHours = "24/7",
            distanceFromUser = 450.0f,
            estimatedWalkTime = 6,
            estimatedDriveTime = 3,
            isFavorite = true
        ),
        CarparkEntity(
            carparkID = "CP004",
            location = "Vivocity",
            address = "1 Harbourfront Walk, Singapore 098585",
            latitude = 1.264200,
            longitude = 103.822200,
            totalLots = 500,
            availableLots = 320,
            lotTypes = """{"car": 450, "motorcycle": 40, "ev": 8, "accessible": 2}""",
            pricingInfo = """{"weekday_rate": "2.00", "weekend_rate": "2.50", "peak_rate": "3.00"}""",
            operatingHours = "24/7",
            distanceFromUser = 5500.0f,
            estimatedWalkTime = 70,
            estimatedDriveTime = 15,
            isFavorite = false
        ),
        CarparkEntity(
            carparkID = "CP005",
            location = "Bugis Junction",
            address = "200 Victoria Street, Singapore 188021",
            latitude = 1.299200,
            longitude = 103.855400,
            totalLots = 200,
            availableLots = 0,
            lotTypes = """{"car": 180, "motorcycle": 15, "ev": 4, "accessible": 1}""",
            pricingInfo = """{"weekday_rate": "2.50", "weekend_rate": "3.00", "peak_rate": "4.00"}""",
            operatingHours = "24/7",
            distanceFromUser = 800.0f,
            estimatedWalkTime = 10,
            estimatedDriveTime = 4,
            isFavorite = false
        ),
        CarparkEntity(
            carparkID = "CP006",
            location = "Suntec City",
            address = "3 Temasek Boulevard, Singapore 038983",
            latitude = 1.294700,
            longitude = 103.858500,
            totalLots = 600,
            availableLots = 400,
            lotTypes = """{"car": 550, "motorcycle": 40, "ev": 8, "accessible": 2}""",
            pricingInfo = """{"weekday_rate": "2.50", "weekend_rate": "3.00", "peak_rate": "4.00"}""",
            operatingHours = "24/7",
            distanceFromUser = 1500.0f,
            estimatedWalkTime = 18,
            estimatedDriveTime = 6,
            isFavorite = true
        ),
        CarparkEntity(
            carparkID = "CP007",
            location = "Changi Airport T3",
            address = "65 Airport Boulevard, Singapore 819663",
            latitude = 1.356500,
            longitude = 103.986900,
            totalLots = 1000,
            availableLots = 650,
            lotTypes = """{"car": 900, "motorcycle": 80, "ev": 15, "accessible": 5}""",
            pricingInfo = """{"weekday_rate": "1.50", "weekend_rate": "1.50", "peak_rate": "1.50"}""",
            operatingHours = "24/7",
            distanceFromUser = 18000.0f,
            estimatedWalkTime = 225,
            estimatedDriveTime = 25,
            isFavorite = false
        ),
        CarparkEntity(
            carparkID = "CP008",
            location = "Plaza Singapura",
            address = "68 Orchard Road, Singapore 238839",
            latitude = 1.300600,
            longitude = 103.845300,
            totalLots = 250,
            availableLots = 35,
            lotTypes = """{"car": 220, "motorcycle": 25, "ev": 4, "accessible": 1}""",
            pricingInfo = """{"weekday_rate": "2.50", "weekend_rate": "3.00", "peak_rate": "4.00"}""",
            operatingHours = "24/7",
            distanceFromUser = 350.0f,
            estimatedWalkTime = 4,
            estimatedDriveTime = 2,
            isFavorite = false
        )
    )

    /**
     * Mock User
     */
    val mockUser = UserEntity.create(
        userID = "mock_user_123",
        userName = "John Doe",
        email = "john.doe@example.com",
        password = null,
        authProvider = UserEntity.AuthProvider.EMAIL
    )

    /**
     * Mock Recent Searches
     */
    val mockRecentSearches = listOf(
        RecentSearchEntity(
            searchID = "search1",
            userID = "mock_user_123",
            searchQuery = "Orchard",
            searchType = RecentSearchEntity.SearchType.PLACE_NAME,
            timestamp = System.currentTimeMillis() - 3600000 // 1 hour ago
        ),
        RecentSearchEntity(
            searchID = "search2",
            userID = "mock_user_123",
            searchQuery = "Marina Bay",
            searchType = RecentSearchEntity.SearchType.PLACE_NAME,
            timestamp = System.currentTimeMillis() - 7200000 // 2 hours ago
        ),
        RecentSearchEntity(
            searchID = "search3",
            userID = "mock_user_123",
            searchQuery = "Bugis",
            searchType = RecentSearchEntity.SearchType.PLACE_NAME,
            timestamp = System.currentTimeMillis() - 86400000 // 1 day ago
        )
    )

    /**
     * Mock Favorite Carpark IDs
     */
    val mockFavoriteIds = setOf("CP001", "CP003", "CP006")

    /**
     * Mock Active Parking Session
     */
    val mockActiveParkingSession = ParkingSessionEntity(
        sessionID = "session_001",
        userID = "mock_user_123",
        carparkID = "CP001",
        carparkName = "Orchard Central",
        carparkAddress = "181 Orchard Road, Singapore 238896",
        startTime = System.currentTimeMillis() - 3600000, // Started 1 hour ago
        endTime = null, // Still active
        estimatedCost = 2.50,
        actualCost = null
    )

    /**
     * Mock Past Parking Sessions
     */
    val mockPastSessions = listOf(
        ParkingSessionEntity(
            sessionID = "session_002",
            userID = "mock_user_123",
            carparkID = "CP002",
            carparkName = "Marina Square",
            carparkAddress = "6 Raffles Boulevard, Singapore 039594",
            startTime = System.currentTimeMillis() - 172800000, // 2 days ago
            endTime = System.currentTimeMillis() - 169200000, // 2 days ago + 1 hour
            estimatedCost = 2.00,
            actualCost = 2.50
        ),
        ParkingSessionEntity(
            sessionID = "session_003",
            userID = "mock_user_123",
            carparkID = "CP003",
            carparkName = "ION Orchard",
            carparkAddress = "2 Orchard Turn, Singapore 238801",
            startTime = System.currentTimeMillis() - 259200000, // 3 days ago
            endTime = System.currentTimeMillis() - 255600000, // 3 days ago + 1 hour
            estimatedCost = 3.00,
            actualCost = 3.50
        )
    )

    /**
     * Mock Budget Data
     */
    data class MockBudget(
        val monthlyBudget: Float = 100.0f,
        val currentSpending: Float = 45.50f,
        val remainingBudget: Float = 54.50f,
        val warningThreshold: Float = 80.0f, // 80% of budget
        val isWarningTriggered: Boolean = false,
        val isBudgetExceeded: Boolean = false
    )

    val mockBudget = MockBudget()

    // ============================================================================
    // Helper Functions to Simulate Repository Operations
    // ============================================================================

    /**
     * Simulate getting all carparks with delay
     */
    fun getAllCarparksFlow(): Flow<List<CarparkEntity>> = flow {
        delay(MOCK_DELAY_MEDIUM)
        emit(mockCarparks)
    }

    /**
     * Simulate searching carparks
     */
    fun searchCarparksFlow(query: String): Flow<List<CarparkEntity>> = flow {
        delay(MOCK_DELAY_SHORT)
        val filtered = mockCarparks.filter {
            it.location.contains(query, ignoreCase = true) ||
                    it.address.contains(query, ignoreCase = true)
        }
        emit(filtered)
    }

    /**
     * Simulate getting favorite carparks
     */
    fun getFavoriteCarparksFlow(): Flow<List<CarparkEntity>> = flow {
        delay(MOCK_DELAY_SHORT)
        val favorites = mockCarparks.filter { it.carparkID in mockFavoriteIds }
        emit(favorites)
    }

    /**
     * Simulate adding to favorites
     */
    suspend fun addToFavorites(carparkId: String) {
        delay(MOCK_DELAY_SHORT)
        (mockFavoriteIds as MutableSet).add(carparkId)
    }

    /**
     * Simulate removing from favorites
     */
    suspend fun removeFromFavorites(carparkId: String) {
        delay(MOCK_DELAY_SHORT)
        (mockFavoriteIds as MutableSet).remove(carparkId)
    }

    /**
     * Simulate checking if carpark is favorite
     */
    suspend fun isFavorite(carparkId: String): Boolean {
        delay(100)
        return carparkId in mockFavoriteIds
    }

    /**
     * Simulate getting user
     */
    suspend fun getUser(): UserEntity {
        delay(MOCK_DELAY_SHORT)
        return mockUser
    }

    /**
     * Simulate getting recent searches
     */
    fun getRecentSearchesFlow(): Flow<List<RecentSearchEntity>> = flow {
        delay(MOCK_DELAY_SHORT)
        emit(mockRecentSearches)
    }

    /**
     * Simulate getting active parking session
     */
    fun getActiveParkingSessionFlow(): Flow<ParkingSessionEntity?> = flow {
        delay(MOCK_DELAY_SHORT)
        emit(mockActiveParkingSession)
    }
}
