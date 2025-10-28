package com.example.idol_android.di

import android.content.Context
import androidx.room.Room
import com.example.idol_android.data.local.IdolDatabase
import com.example.idol_android.data.local.dao.IdolDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module for providing database-related dependencies.
 */
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideIdolDatabase(
        @ApplicationContext context: Context
    ): IdolDatabase {
        return Room.databaseBuilder(
            context,
            IdolDatabase::class.java,
            IdolDatabase.DATABASE_NAME
        )
            .fallbackToDestructiveMigration() // For development, recreate DB on schema changes
            .build()
    }

    @Provides
    @Singleton
    fun provideIdolDao(database: IdolDatabase): IdolDao {
        return database.idolDao()
    }
}
