package com.example.idol_android.di

import com.example.idol_android.data.repository.AdRepositoryImpl
import com.example.idol_android.data.repository.ConfigRepositoryImpl
import com.example.idol_android.data.repository.IdolRepositoryImpl
import com.example.idol_android.data.repository.MessageRepositoryImpl
import com.example.idol_android.data.repository.UserRepositoryImpl
import com.example.idol_android.data.repository.UtilityRepositoryImpl
import com.example.idol_android.domain.repository.AdRepository
import com.example.idol_android.domain.repository.ConfigRepository
import com.example.idol_android.domain.repository.IdolRepository
import com.example.idol_android.domain.repository.MessageRepository
import com.example.idol_android.domain.repository.UserRepository
import com.example.idol_android.domain.repository.UtilityRepository
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
}
