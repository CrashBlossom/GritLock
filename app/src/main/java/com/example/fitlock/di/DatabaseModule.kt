package com.example.fitlock.di

import android.content.Context
import com.example.fitlock.data.GritLockDao
import com.example.fitlock.data.GritLockDatabase
import com.example.fitlock.data.GritLockRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * DatabaseModule provides instructions for Hilt on how to create
 * our database-related objects. 
 */
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): GritLockDatabase {
        return GritLockDatabase.getDatabase(context)
    }

    @Provides
    fun provideDao(database: GritLockDatabase): GritLockDao {
        return database.dao()
    }

    @Provides
    @Singleton
    fun provideRepository(dao: GritLockDao): GritLockRepository {
        return GritLockRepository(dao)
    }
}
