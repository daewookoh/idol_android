/**
 * Copyright (C) 2024. ExodusEnt Corp. All rights reserved.
 * You must have prior written permission to read this file.
 * @author __jungSangMin__ jnugg0819@gmail.com
 * Description:
 *
 * */

package net.ib.mn.core.data.api

import net.ib.mn.core.data.dto.OnepickVoteDTO
import net.ib.mn.core.data.dto.OpenNotificationDTO
import okhttp3.ResponseBody
import org.json.JSONObject
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface OnepickApi {

    /**
     * 이미지픽 목록
     * */
    @GET("onepick/")
    suspend fun get(
        @Query("offset") offset: Int,
        @Query("limit") limit: Int,
    ): Response<ResponseBody>

    /**
     * 이미지픽 투표
     */
    @POST("onepick/")
    suspend fun vote(
        @Body body: OnepickVoteDTO,
    ): Response<ResponseBody>

    /**
     * 결과 조회
     */
    @GET("onepick/{id}/")
    suspend fun getResult(
        @Path(value="id") id: Int,
        @Query("status") status: String? = null,
    ): Response<ResponseBody>

    /*
    * 이미지픽 개설 알림 설정
    * */
    @POST("onepick/alarm/")
    suspend fun postOpenImagePickNotification(
        @Body vote: OpenNotificationDTO
    ): Response<ResponseBody>
}