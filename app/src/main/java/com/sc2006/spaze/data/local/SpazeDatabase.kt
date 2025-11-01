// app/src/main/java/com/sc2006/spaze/data/local/AppDatabase.kt
package com.sc2006.spaze.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.sc2006.spaze.data.local.dao.BudgetDao
import com.sc2006.spaze.data.local.dao.CarparkDao
import com.sc2006.spaze.data.local.dao.FavoriteDao
import com.sc2006.spaze.data.local.dao.MapLocationDao
import com.sc2006.spaze.data.local.dao.ParkingSessionDao
import com.sc2006.spaze.data.local.dao.RecentSearchDao
import com.sc2006.spaze.data.local.dao.UserDao
import com.sc2006.spaze.data.local.entity.BudgetEntity
import com.sc2006.spaze.data.local.entity.CarparkEntity
import com.sc2006.spaze.data.local.entity.FavoriteEntity
import com.sc2006.spaze.data.local.entity.MapLocationEntity
import com.sc2006.spaze.data.local.entity.ParkingSessionEntity
import com.sc2006.spaze.data.local.entity.RecentSearchEntity
import com.sc2006.spaze.data.local.entity.UserEntity

@Database(
    entities = [
        UserEntity::class,
        BudgetEntity::class,
        CarparkEntity::class,
        FavoriteEntity::class,
        MapLocationEntity::class,
        ParkingSessionEntity::class,
        RecentSearchEntity::class
    ],
    version = 6,               // bump if you just changed entities/DAOs
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun budgetDao(): BudgetDao
    abstract fun carparkDao(): CarparkDao
    abstract fun favoriteDao(): FavoriteDao
    abstract fun mapLocationDao(): MapLocationDao
    abstract fun parkingSessionDao(): ParkingSessionDao
    abstract fun recentSearchDao(): RecentSearchDao
}

