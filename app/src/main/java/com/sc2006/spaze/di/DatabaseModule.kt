// app/src/main/java/com/sc2006/spaze/di/DatabaseModule.kt
package com.sc2006.spaze.di

import android.content.Context
import androidx.room.Room
import com.sc2006.spaze.data.local.AppDatabase
import com.sc2006.spaze.data.local.dao.BudgetDao
import com.sc2006.spaze.data.local.dao.CarparkDao
import com.sc2006.spaze.data.local.dao.FavoriteDao
import com.sc2006.spaze.data.local.dao.MapLocationDao
import com.sc2006.spaze.data.local.dao.ParkingSessionDao
import com.sc2006.spaze.data.local.dao.RecentSearchDao
import com.sc2006.spaze.data.local.dao.UserDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "spaze.db"
        )
            .fallbackToDestructiveMigration()
            .build()

    @Provides fun provideUserDao(db: AppDatabase): UserDao = db.userDao()
    @Provides fun provideBudgetDao(db: AppDatabase): BudgetDao = db.budgetDao()
    @Provides fun provideCarparkDao(db: AppDatabase): CarparkDao = db.carparkDao()
    @Provides fun provideFavoriteDao(db: AppDatabase): FavoriteDao = db.favoriteDao()
    @Provides fun provideMapLocationDao(db: AppDatabase): MapLocationDao = db.mapLocationDao()
    @Provides fun provideParkingSessionDao(db: AppDatabase): ParkingSessionDao = db.parkingSessionDao()
    @Provides fun provideRecentSearchDao(db: AppDatabase): RecentSearchDao = db.recentSearchDao()
}
