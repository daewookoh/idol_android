package net.ib.mn.data.remote.api

import net.ib.mn.data.remote.dto.AwardIdolsResponse
import net.ib.mn.data.remote.dto.AwardRankResponse
import net.ib.mn.data.remote.dto.AwardsCurrentResponse
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

/**
 * Awards API
 * 어워즈/이벤트 관련 API
 */
interface AwardsApi {

    /**
     * 현재 진행중인 어워즈 정보 조회
     * 플로팅 버튼 이미지 URL 포함
     */
    @GET("awards/current/")
    suspend fun getCurrent(): Response<AwardsCurrentResponse>

    /**
     * 어워즈 누적순위 조회
     * old: TrendsApi.awardRank()
     *
     * @param chartCode 차트 코드 (예: MSM, FSF, MGM, FGF)
     * @param event 이벤트명 (예: 어워즈 name)
     */
    @GET("trends/award_rank/")
    suspend fun getAwardRank(
        @Query("chart_code") chartCode: String?,
        @Query("event") event: String?
    ): Response<AwardRankResponse>

    /**
     * 어워즈 실시간 순위 조회 (Daily)
     * old: IdolsApi.getAwardIdols()
     *
     * @param chartCode 차트 코드 (예: MSM, FSF, MGM, FGF)
     * @param fields 필드 (예: "heart,top3")
     */
    @GET("idols/")
    suspend fun getAwardIdols(
        @Query("chart_code") chartCode: String?,
        @Query(value = "fields", encoded = true) fields: String? = "heart,top3"
    ): Response<AwardIdolsResponse>
}
