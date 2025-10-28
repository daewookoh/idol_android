package net.ib.mn.core.domain.usecase

import kotlinx.coroutines.flow.Flow
import net.ib.mn.core.data.model.RecommendRewardResponse
import net.ib.mn.core.data.repository.recommend.RecommendRepository
import javax.inject.Inject

class RecommendRewardUseCase @Inject constructor(
    private val repository: RecommendRepository
) {
    suspend operator fun invoke(): Flow<RecommendRewardResponse> = repository.postRecommendReward()
}