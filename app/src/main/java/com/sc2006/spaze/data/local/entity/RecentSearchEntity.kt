package com.sc2006.spaze.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Recent Search Entity - Tracks user's recent searches and viewed carparks
 * Supports Favorites & Recents functional requirements
 */
@Entity(tableName = "recent_searches")
data class RecentSearchEntity(
    @PrimaryKey
    val searchID: String,
    val userID: String,
    val searchQuery: String,
    val searchType: SearchType,
    val carparkID: String? = null, // For viewed carparks
    val timestamp: Long = System.currentTimeMillis()
) {
    enum class SearchType {
        ADDRESS,
        POSTAL_CODE,
        PLACE_NAME,
        CARPARK_VIEW
    }

    companion object {
        fun createSearch(
            userID: String,
            query: String,
            type: SearchType
        ): RecentSearchEntity {
            return RecentSearchEntity(
                searchID = "${userID}_${System.currentTimeMillis()}",
                userID = userID,
                searchQuery = query,
                searchType = type
            )
        }

        fun createCarparkView(
            userID: String,
            carparkID: String,
            carparkName: String
        ): RecentSearchEntity {
            return RecentSearchEntity(
                searchID = "${userID}_${carparkID}_${System.currentTimeMillis()}",
                userID = userID,
                searchQuery = carparkName,
                searchType = SearchType.CARPARK_VIEW,
                carparkID = carparkID
            )
        }
    }
}
