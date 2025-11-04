package net.ib.mn.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import net.ib.mn.data.repository.RankingRepositoryImpl
import net.ib.mn.domain.repository.RankingRepository
import javax.inject.Singleton

/**
 * Ranking Module
 *
 * Best Practice:
 * 1. Repository 인터페이스와 구현체를 Hilt로 바인딩
 * 2. @Binds를 사용하여 효율적인 의존성 주입
 * 3. Singleton으로 Repository 인스턴스 관리
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class RankingModule {

    @Binds
    @Singleton
    abstract fun bindRankingRepository(
        rankingRepositoryImpl: RankingRepositoryImpl
    ): RankingRepository
}
