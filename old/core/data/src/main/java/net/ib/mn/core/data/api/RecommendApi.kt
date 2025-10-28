package net.ib.mn.core.data.api

import net.ib.mn.core.data.model.RecommendRewardResponse
import retrofit2.http.POST

interface RecommendApi {
    @POST("recommend/reward/all/")
    suspend fun postRecommendRewardAll(): RecommendRewardResponse
}