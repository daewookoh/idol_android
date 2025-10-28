/**
 * Copyright (C) 2024. ExodusEnt Corp. All rights reserved.
 * You must have prior written permission to read this file.
 * @author __jungSangMin__ jnugg0819@gmail.com
 * Description:
 *
 * */

package net.ib.mn.core.data.api

import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query


/**
 * Trends api
 * */

interface TrendsApi {
    // 특정 날짜의 당일 순위
    @GET("trends/daily_history/")
    suspend fun dailyHistory(
        @Query("type") type: String? = null,
        @Query("category") category: String? = null,
        @Query("history_param") date: String? = null,
        @Query("code") chartCode: String? = null
    ): Response<ResponseBody>

    // 누적순위 가져오기 (셀럽에서만 사용중/명전-누적순위)
    @GET("trends/rank/")
    suspend fun rank(
        @Query("type") type: String? = null,
        @Query("category") category: String? = null,
        @Query("league") league: String? = null,
        @Query("target_ym") targetYM: String? = null,
    ): Response<ResponseBody>

    // 개인/그룹 누적순위
    // 이붙
    @GET("trends/recent/")
    suspend fun recent(
        @Query("idol_id") idolId: Int,
        @Query("code") chartCode: String? = null,
        @Query("offset") offset: Int? = null,
    ): Response<ResponseBody>

    // 어워즈 누적순위 (기록실 및 진행중인 어워즈)
    @GET("trends/award_rank/")
    suspend fun awardRank(
        @Query("chart_code") chartCode: String? = null,
        @Query("event") event: String? = null,
    ): Response<ResponseBody>

    // 기록실-어워즈-누적순위변화
    @GET("trends/award_recent/")
    suspend fun awardRecent(
        @Query("type") type: String? = null,
        @Query("category") category: String? = null,
        @Query("event") event: String? = null,
        @Query("idol_id") idolId: Int? = null,
        @Query("chart_code") chartCode: String? = null,
        @Query("source_app") sourceApp: String? = null,
    ): Response<ResponseBody>

    // 투표 탑 100
    @GET("trends/most_votes_top100/")
    suspend fun mostVotesTop100(
        @Query("celeb") celeb: Boolean? = null,
    ): Response<ResponseBody>
}