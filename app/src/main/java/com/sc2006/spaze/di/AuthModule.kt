package com.sc2006.spaze.di

import com.sc2006.spaze.data.local.PreferencesManager
import com.sc2006.spaze.data.local.dao.UserDao
import com.sc2006.spaze.data.repository.AuthRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AuthModule {

    @Provides
    @Singleton
    fun provideAuthRepository(
        userDao: UserDao,
        preferencesManager: PreferencesManager
    ): AuthRepository = AuthRepository(userDao, preferencesManager)
}
