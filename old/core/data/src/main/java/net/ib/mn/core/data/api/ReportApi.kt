/**
 * Copyright (C) 2024. ExodusEnt Corp. All rights reserved.
 * You must have prior written permission to read this file.
 * @author __jungSangMin__ jnugg0819@gmail.com
 * Description:
 *
 * */

package net.ib.mn.core.data.api

import net.ib.mn.core.data.dto.ReportDTO
import net.ib.mn.core.data.dto.ReportFeedDTO
import net.ib.mn.core.data.dto.ReportHeartPickDTO
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

interface ReportApi {

    /**
     * 글,댓글 신고하기
     * */
    @POST("reports/")
    suspend fun doReport(
        @Body body: ReportDTO
    ): Response<ResponseBody>

    /**
     * 피드 신고하기
     */
    @POST("reports/")
    suspend fun doReportFeed(
        @Body body: ReportFeedDTO
    ): Response<ResponseBody>

    /**
     * 하트픽 신고하기
     */
    @POST("reports/")
    suspend fun doReportHeartPick(
        @Body body: ReportHeartPickDTO
    ): Response<ResponseBody>

    /**
     * 신고 가능 여부 확인
     */
    @GET("reports/possible/user/")
    suspend fun getReportPossible(
        @Query("recv_user_id") recvUserId: Long,
    ): Response<ResponseBody>
}