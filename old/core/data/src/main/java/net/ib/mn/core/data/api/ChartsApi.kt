/**
 * Copyright (C) 2024. ExodusEnt Corp. All rights reserved.
 * You must have prior written permission to read this file.
 * @author __jungSangMin__ jnugg0819@gmail.com
 * Description:
 *
 * */

package net.ib.mn.core.data.api

import net.ib.mn.core.data.model.AggregateRankModel
import net.ib.mn.core.data.model.CurrentChartResponse
import net.ib.mn.core.data.model.ObjectBaseDataModel
import net.ib.mn.core.data.model.ObjectsBaseDataModel
import retrofit2.http.GET
import retrofit2.http.Query


/**
 * @see
 * */

interface ChartsApi {

    /**
     * 현재 진행중인 차트 정보 가져오기
     * */
    @GET("charts/current/")
    suspend fun getChartsCurrent(): CurrentChartResponse

    /**
     * 해당 차트에 속한 아이돌 리스트 가져오기. 차트 하나에 대해 가져오는 것이므로 getChart...로 단수 처리
     */
    @GET("charts/idol_ids/")
    suspend fun getChartIdolIds(@Query("code") code: String): ObjectsBaseDataModel<List<Int>>

    /**
     * 해당 차트의 누적 집계결과 가져오기
     */
    @GET("charts/ranks/")
    suspend fun getChartRanks(@Query("code") code: String): ObjectsBaseDataModel<List<AggregateRankModel>>

    /*
    * 아이돌이 가지고 있는 차트 코드들 가져오기 (현재는 최애 화면 보여주기 용)
    * */
    @GET("charts/list_per_idol/")
    suspend fun getIdolChartCodes(): ObjectBaseDataModel<Map<String, ArrayList<String>>>
}