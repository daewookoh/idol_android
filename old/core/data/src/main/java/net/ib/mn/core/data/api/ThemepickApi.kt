/**
 * Copyright (C) 2024. ExodusEnt Corp. All rights reserved.
 * You must have prior written permission to read this file.
 * @author __jungSangMin__ jnugg0819@gmail.com
 * Description:
 *
 * */

package net.ib.mn.core.data.api

import net.ib.mn.core.data.dto.OpenNotificationDTO
import net.ib.mn.core.data.dto.ThemepickVoteDTO
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface ThemepickApi {

    /**
     * 테마픽 목록
     * */
    @GET("themepick/")
    suspend fun get(
        @Query("offset") offset: Int,
        @Query("limit") limit: Int,
    ): Response<ResponseBody>

    /**
     * 테마픽 투표
     */
    @POST("themepick/vote/")
    suspend fun vote(
        @Body body: ThemepickVoteDTO,
    ): Response<ResponseBody>

    /**
     * 테마픽 조회
     */
    @GET("themepick/{id}/")
    suspend fun getResult(
        @Path(value="id") id: Int,
    ): Response<ResponseBody>

    /*
    * 테마픽 개설 알림 설정
    * */
    @POST("themepick/alarm/")
    suspend fun postOpenThemePickNotification(
        @Body vote: OpenNotificationDTO
    ): Response<ResponseBody>
}