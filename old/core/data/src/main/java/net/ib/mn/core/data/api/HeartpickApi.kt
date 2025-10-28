/**
 * Copyright (C) 2024. ExodusEnt Corp. All rights reserved.
 * You must have prior written permission to read this file.
 * @author __jungSangMin__ jnugg0819@gmail.com
 * Description:
 *
 * */

package net.ib.mn.core.data.api

import net.ib.mn.core.data.dto.HeartpickVoteDTO
import net.ib.mn.core.data.dto.OpenNotificationDTO
import okhttp3.MultipartBody
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Path
import retrofit2.http.Query

interface HeartpickApi {

    @GET("heartpick/")
    suspend fun get(
        @Query("offset") offset: Int,
        @Query("limit") limit: Int
    ): Response<ResponseBody>

    @GET("heartpick/{id}/")
    suspend fun get(
        @Path(value="id") id: Int,
        @Query("offset") offset: Int,
        @Query("limit") limit: Int
    ): Response<ResponseBody>

    @POST("heartpick/vote/")
    suspend fun vote(
        @Body vote: HeartpickVoteDTO
    ): Response<ResponseBody>

    @GET("heartpick/replies/")
    suspend fun getReplies(
        @Query("id") heartPickId: Int,
        @Query("limit") limit: Int,
        @Query("cursor") cursor: String? = null,
    ): Response<ResponseBody>

    @GET("replies/{id}/")
    suspend fun getReply(
        @Path(value="id") id: Int,
        @Query(value="translate") translate: String? = null,
    ): Response<ResponseBody>

    @Multipart
    @POST("heartpick/replies/")
    suspend fun postReplyMultipart(
        @Part id: MultipartBody.Part,
        @Part emoticon: MultipartBody.Part? = null,
        @Part content: MultipartBody.Part,
        @Part image_url: MultipartBody.Part? = null,
        @Part image: MultipartBody.Part? = null,
    ): Response<ResponseBody>

    @POST("heartpick/alarm/")
    suspend fun postOpenHeartPickNotification(
        @Body vote: OpenNotificationDTO
    ): Response<ResponseBody>

    @GET("heartpick/alarm/")
    suspend fun getOpenHeartPickNotification(
        @Query("id") heartPickId: Int,
    ): Response<ResponseBody>
}
