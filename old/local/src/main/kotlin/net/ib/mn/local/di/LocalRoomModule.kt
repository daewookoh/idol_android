package net.ib.mn.local.di

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import net.ib.mn.local.room.AwardsDatabase
import net.ib.mn.local.room.AwardsRoomConstant
import net.ib.mn.local.room.IdolDatabase
import net.ib.mn.local.room.IdolRoomConstant
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal object LocalRoomModule {

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): IdolDatabase =
        Room.databaseBuilder(
            context,
            IdolDatabase::class.java,
            IdolRoomConstant.ROOM_DB_NAME
        )
            .fallbackToDestructiveMigration()
            .build()

    @Provides
    @Singleton
    fun provideIdolDao(database: IdolDatabase) = database.idolDao()

    @Provides
    @Singleton
    fun provideNotificationDao(database: IdolDatabase) = database.notificationDao()

    @Provides
    @Singleton
    fun provideAwardsDatabase(@ApplicationContext context: Context): AwardsDatabase =
        Room.databaseBuilder(
            context,
            AwardsDatabase::class.java,
            AwardsRoomConstant.ROOM_DB_NAME
        )
            .fallbackToDestructiveMigration()
            .build()

    @Provides
    @Singleton
    fun provideAwardsIdolDao(database: AwardsDatabase) = database.awardsIdolDao()
}