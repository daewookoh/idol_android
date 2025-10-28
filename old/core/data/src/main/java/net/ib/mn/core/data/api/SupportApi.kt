/**
 * Copyright (C) 2024. ExodusEnt Corp. All rights reserved.
 * You must have prior written permission to read this file.
 * @author __jungSangMin__ jnugg0819@gmail.com
 * Description:
 *
 * */

package net.ib.mn.core.data.api

import net.ib.mn.core.data.dto.ChatRoomDTO
import net.ib.mn.core.data.dto.ReportChatRoomDTO
import net.ib.mn.core.data.dto.SupportGiveDiamondDTO
import net.ib.mn.core.data.dto.SupportLikeDTO
import net.ib.mn.core.data.model.ChatRoomCreateModel
import okhttp3.MultipartBody
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Path
import retrofit2.http.Query

interface SupportApi {

    /**
     * 서포트 목록
     * */
    @GET("idol_supports/")
    suspend fun getSupports(
        @Query("limit") limit: Int,
        @Query("offset") offset: Int,
        @Query("idol_id") idolId: String? = null, // 셀럽
        @Query("group_id") groupId: String? = null, // 애돌
        @Query("order_by") orderBy: String? = null,
        @Query("status") status: String? = null,
        @Query("yearmonth") yearMonth: String? = null,
    ): Response<ResponseBody>

    /**
     * 서포트 상세
     */
    @GET("idol_supports/{id}/")
    suspend fun getSupportDetail(
        @Path(value="id") id: Int
    ): Response<ResponseBody>

    /**
     * 서포트 개설
     */
    @Multipart
    @POST("idol_supports/")
    suspend fun createSupport(
        @Part idolId: MultipartBody.Part,
        @Part title: MultipartBody.Part,
        @Part adId: MultipartBody.Part,
        @Part utcDate: MultipartBody.Part,
        @Part image: MultipartBody.Part? = null,
    ): Response<ResponseBody>

    /**
     * 다이아 투표
     */
    @POST("idol_supports/give_diamond/")
    suspend fun giveDiamond(
        @Body body: SupportGiveDiamondDTO
    ): Response<ResponseBody>

    /**
     * 좋아요
     */
    @POST("idol_supports/support_like/")
    suspend fun like(
        @Body body: SupportLikeDTO
    ): Response<ResponseBody>

    @GET("idol_supports/top5/")
    suspend fun getTop5(
        @Query("support_id") supportId: Int,
    ): Response<ResponseBody>

    @GET("idol_supports/inapp_banner/")
    suspend fun getInAppBanner(
    ): Response<ResponseBody>
}