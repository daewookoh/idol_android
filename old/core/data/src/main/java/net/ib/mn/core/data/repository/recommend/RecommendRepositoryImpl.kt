package net.ib.mn.core.data.repository.recommend

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import net.ib.mn.core.data.api.RecommendApi
import net.ib.mn.core.data.model.RecommendRewardResponse
import net.ib.mn.core.data.repository.BaseRepository
import javax.inject.Inject

class RecommendRepositoryImpl @Inject constructor(
    private val recommendApi: RecommendApi
) : RecommendRepository, BaseRepository() {
    override suspend fun postRecommendReward(): Flow<RecommendRewardResponse> = flow {
        try {
            val response = recommendApi.postRecommendRewardAll()
            emit(response)
        } catch (e: Exception) {
            e.printStackTrace()
            throw e
        }
    }.catch { e ->
        emit(RecommendRewardResponse(success = false, msg = e.message))
    }
}