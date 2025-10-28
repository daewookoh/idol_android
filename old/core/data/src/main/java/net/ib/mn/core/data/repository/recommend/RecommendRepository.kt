package net.ib.mn.core.data.repository.recommend

import kotlinx.coroutines.flow.Flow
import net.ib.mn.core.data.model.RecommendRewardResponse

interface RecommendRepository {
    suspend fun postRecommendReward(): Flow<RecommendRewardResponse>
}