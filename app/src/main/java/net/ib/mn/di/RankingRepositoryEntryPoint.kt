package net.ib.mn.di

import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import net.ib.mn.domain.repository.RankingRepository

/**
 * RankingRepository EntryPoint
 *
 * Composable에서 RankingRepository를 직접 주입받기 위한 EntryPoint
 */
@EntryPoint
@InstallIn(SingletonComponent::class)
interface RankingRepositoryEntryPoint {
    fun rankingRepository(): RankingRepository
}
