package net.ib.mn.di

import net.ib.mn.data.repository.AdRepositoryImpl
import net.ib.mn.data.repository.ConfigRepositoryImpl
import net.ib.mn.data.repository.HeartpickRepositoryImpl
import net.ib.mn.data.repository.IdolRepositoryImpl
import net.ib.mn.data.repository.MessageRepositoryImpl
import net.ib.mn.data.repository.UserRepositoryImpl
import net.ib.mn.data.repository.UtilityRepositoryImpl
import net.ib.mn.domain.repository.AdRepository
import net.ib.mn.domain.repository.ConfigRepository
import net.ib.mn.domain.repository.HeartpickRepository
import net.ib.mn.domain.repository.IdolRepository
import net.ib.mn.domain.repository.MessageRepository
import net.ib.mn.domain.repository.UserRepository
import net.ib.mn.domain.repository.UtilityRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Repository DI 모듈
 *
 * Repository 인터페이스와 구현체 바인딩
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindConfigRepository(
        impl: ConfigRepositoryImpl
    ): ConfigRepository

    @Binds
    @Singleton
    abstract fun bindUserRepository(
        impl: UserRepositoryImpl
    ): UserRepository

    @Binds
    @Singleton
    abstract fun bindIdolRepository(
        impl: IdolRepositoryImpl
    ): IdolRepository

    @Binds
    @Singleton
    abstract fun bindAdRepository(
        impl: AdRepositoryImpl
    ): AdRepository

    @Binds
    @Singleton
    abstract fun bindMessageRepository(
        impl: MessageRepositoryImpl
    ): MessageRepository

    @Binds
    @Singleton
    abstract fun bindUtilityRepository(
        impl: UtilityRepositoryImpl
    ): UtilityRepository

    @Binds
    @Singleton
    abstract fun bindHeartpickRepository(
        impl: HeartpickRepositoryImpl
    ): HeartpickRepository
}
