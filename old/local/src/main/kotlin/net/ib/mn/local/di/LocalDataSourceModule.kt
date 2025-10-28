package net.ib.mn.local.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import net.ib.mn.data.local.AwardsLocalDataSource
import net.ib.mn.data.local.IdolLocalDataSource
import net.ib.mn.data.local.NotificationLocalDataSource
import net.ib.mn.data.local.datastore.AppPrefsDataSource
import net.ib.mn.data.local.datastore.FreeBoardPrefsDataSource
import net.ib.mn.data.local.datastore.IdolPrefsDataSource
import net.ib.mn.local.impl.AwardsLocalDataSourceImpl
import net.ib.mn.local.impl.IdolLocalDataSourceImpl
import net.ib.mn.local.impl.NotificationLocalDataSourceImpl
import net.ib.mn.local.impl.datastore.AppPrefsDataSourceImpl
import net.ib.mn.local.impl.datastore.FreeBoardPrefsDataSourceImpl
import net.ib.mn.local.impl.datastore.IdolPrefsDataSourceImpl
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal abstract class LocalDataSourceModule {

    @Binds
    @Singleton
    abstract fun bindIdolLocalDataSource(source: IdolLocalDataSourceImpl): IdolLocalDataSource

    @Binds
    @Singleton
    abstract fun bindAwardsLocalDataSource(source: AwardsLocalDataSourceImpl): AwardsLocalDataSource

    @Binds
    @Singleton
    abstract fun bindNotificationDatSource(source: NotificationLocalDataSourceImpl): NotificationLocalDataSource

    @Binds
    @Singleton
    abstract fun bindFreeBoardPrefsDataSource(source: FreeBoardPrefsDataSourceImpl): FreeBoardPrefsDataSource

    @Binds
    @Singleton
    abstract fun bindIdolPrefsDataSource(source: IdolPrefsDataSourceImpl): IdolPrefsDataSource

    @Binds
    @Singleton
    abstract fun bindAppPrefsDataSource(source: AppPrefsDataSourceImpl): AppPrefsDataSource
}