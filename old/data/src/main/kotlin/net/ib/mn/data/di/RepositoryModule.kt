package net.ib.mn.data.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import net.ib.mn.data.impl.AppRepositoryImpl
import net.ib.mn.data.impl.AwardsRepositoryImpl
import net.ib.mn.data.impl.FreeBoardRepositoryImpl
import net.ib.mn.data.impl.IdolRepositoryImpl
import net.ib.mn.data.impl.NotificationRepositoryImpl
import net.ib.mn.domain.repository.AppRepository
import net.ib.mn.domain.repository.AwardsRepository
import net.ib.mn.domain.repository.FreeBoardRepository
import net.ib.mn.domain.repository.IdolRepository
import net.ib.mn.domain.repository.NotificationRepository

@Module
@InstallIn(SingletonComponent::class)
internal abstract class RepositoryModule {

    @Binds
    abstract fun bindIdolRepository(repo: IdolRepositoryImpl): IdolRepository

    @Binds
    abstract fun bindAwardsRepository(repository: AwardsRepositoryImpl): AwardsRepository

    @Binds
    abstract fun bindNotificationRepository(repository: NotificationRepositoryImpl): NotificationRepository

    @Binds
    abstract fun bindFreeBoardRepository(repository: FreeBoardRepositoryImpl): FreeBoardRepository

    @Binds
    abstract fun bindAppRepository(repository: AppRepositoryImpl): AppRepository
}