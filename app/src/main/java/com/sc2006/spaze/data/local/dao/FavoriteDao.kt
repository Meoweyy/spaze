package com.sc2006.spaze.data.local.dao

import androidx.room.*
import com.sc2006.spaze.data.local.entity.FavoriteEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for Favorite operations
 */
@Dao
interface FavoriteDao {

    @Query("SELECT * FROM favorites WHERE userID = :userId ORDER BY addedAt DESC")
    fun getUserFavorites(userId: String): Flow<List<FavoriteEntity>>

    @Query("SELECT * FROM favorites WHERE userID = :userId AND carparkID = :carparkId LIMIT 1")
    suspend fun getFavorite(userId: String, carparkId: String): FavoriteEntity?

    @Query("SELECT EXISTS(SELECT 1 FROM favorites WHERE userID = :userId AND carparkID = :carparkId)")
    suspend fun isFavorite(userId: String, carparkId: String): Boolean

    @Query("SELECT EXISTS(SELECT 1 FROM favorites WHERE userID = :userId AND carparkID = :carparkId)")
    fun isFavoriteFlow(userId: String, carparkId: String): Flow<Boolean>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFavorite(favorite: FavoriteEntity)

    @Delete
    suspend fun deleteFavorite(favorite: FavoriteEntity)

    @Query("DELETE FROM favorites WHERE userID = :userId AND carparkID = :carparkId")
    suspend fun removeFavorite(userId: String, carparkId: String)

    @Query("DELETE FROM favorites WHERE userID = :userId")
    suspend fun deleteAllFavorites(userId: String)
}
