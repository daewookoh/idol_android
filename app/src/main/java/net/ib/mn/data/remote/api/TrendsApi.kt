package net.ib.mn.data.remote.api

import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

/**
 * Trends API (이붙그램)
 */
interface TrendsApi {

    /**
     * 이붙그램 목록 (개인/그룹 누적순위)
     */
    @GET("trends/recent/")
    suspend fun getRecent(
        @Query("idol_id") idolId: Int,
        @Query("code") chartCode: String? = null,
        @Query("offset") offset: Int? = null
    ): Response<ResponseBody>

    /**
     * 일일 순위 히스토리 조회 (특정 날짜의 전체 랭킹)
     * Old: trends/daily_history/ 엔드포인트
     */
    @GET("trends/daily_history/")
    suspend fun getDailyHistory(
        @Query("type") type: String? = null,
        @Query("category") category: String? = null,
        @Query("history_param") historyParam: String? = null,
        @Query("code") chartCode: String? = null
    ): Response<ResponseBody>
}
