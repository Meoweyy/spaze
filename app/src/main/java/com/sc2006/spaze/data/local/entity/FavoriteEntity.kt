package com.sc2006.spaze.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Favorite Entity - Tracks user's favorite carparks
 * Supports Favorites & Recents functional requirements
 */
@Entity(tableName = "favorites")
data class FavoriteEntity(
    @PrimaryKey
    val favoriteID: String,
    val userID: String,
    val carparkID: String,
    val addedAt: Long = System.currentTimeMillis(),
    val nickname: String? = null // Optional custom name for the carpark
) {
    companion object {
        fun create(userID: String, carparkID: String, nickname: String? = null): FavoriteEntity {
            return FavoriteEntity(
                favoriteID = "${userID}_$carparkID",
                userID = userID,
                carparkID = carparkID,
                nickname = nickname
            )
        }
    }
}
