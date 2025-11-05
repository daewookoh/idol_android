package net.ib.mn.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent
import dagger.hilt.android.scopes.ViewModelScoped
import net.ib.mn.domain.ranking.GlobalRankingDataSource
import net.ib.mn.domain.ranking.IdolIdsRankingDataSource
import net.ib.mn.domain.ranking.RankingDataSource
import net.ib.mn.domain.repository.RankingRepository
import javax.inject.Qualifier

/**
 * RankingDataSource DI Module
 *
 * Global, Group, Solo 세 가지 타입의 RankingDataSource를 제공합니다.
 */
@Module
@InstallIn(ViewModelComponent::class)
object RankingDataSourceModule {

    @Qualifier
    @Retention(AnnotationRetention.BINARY)
    annotation class GlobalRanking

    @Qualifier
    @Retention(AnnotationRetention.BINARY)
    annotation class GroupRanking

    @Qualifier
    @Retention(AnnotationRetention.BINARY)
    annotation class SoloRanking

    @Provides
    @ViewModelScoped
    @GlobalRanking
    fun provideGlobalRankingDataSource(
        rankingRepository: RankingRepository
    ): RankingDataSource {
        return GlobalRankingDataSource(rankingRepository)
    }

    @Provides
    @ViewModelScoped
    @GroupRanking
    fun provideGroupRankingDataSource(
        rankingRepository: RankingRepository
    ): RankingDataSource {
        return IdolIdsRankingDataSource.forGroup(rankingRepository)
    }

    @Provides
    @ViewModelScoped
    @SoloRanking
    fun provideSoloRankingDataSource(
        rankingRepository: RankingRepository
    ): RankingDataSource {
        return IdolIdsRankingDataSource.forSolo(rankingRepository)
    }
}
