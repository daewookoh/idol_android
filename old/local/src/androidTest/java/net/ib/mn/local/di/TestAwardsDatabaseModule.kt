package net.ib.mn.local.di

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dagger.hilt.testing.TestInstallIn
import net.ib.mn.local.room.AwardsDatabase
import net.ib.mn.local.room.IdolDatabase
import javax.inject.Singleton

@Module
@TestInstallIn(
    components = [SingletonComponent::class],
    replaces = [LocalRoomModule::class]
)
object TestLocalRoomModule {

    @Provides
    @Singleton
    fun provideInMemoryIdolDatabase(@ApplicationContext context: Context): IdolDatabase =
        Room.inMemoryDatabaseBuilder(context, IdolDatabase::class.java)
            .allowMainThreadQueries()
            .build()

    @Provides
    @Singleton
    fun provideIdolDao(database: IdolDatabase) = database.idolDao()

    @Provides
    @Singleton
    fun provideNotificationDao(database: IdolDatabase) = database.notificationDao()

    @Provides
    @Singleton
    fun provideInMemoryAwardsDatabase(@ApplicationContext context: Context): AwardsDatabase =
        Room.inMemoryDatabaseBuilder(context, AwardsDatabase::class.java)
            .allowMainThreadQueries()
            .build()

    @Provides
    @Singleton
    fun provideAwardsIdolDao(database: AwardsDatabase) = database.awardsIdolDao()
}