/**
 * Copyright (C) 2024. ExodusEnt Corp. All rights reserved.
 * You must have prior written permission to read this file.
 * @author __jungSangMin__ jnugg0819@gmail.com
 * Description:
 *
 * */

package net.ib.mn.core.data.api

import net.ib.mn.core.data.dto.PlayLikeDTO
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query


/**
 * Quiz api
 * */

interface PlayApi {
    @GET("live/")
    suspend fun getList(
        @Query("offset") offset: Int,
        @Query("limit") limit: Int
    ): Response<ResponseBody>

    @GET("live/id/")
    suspend fun getInfo(
        @Path("id") id: Int
    ): Response<ResponseBody>

    @GET("live/token/")
    suspend fun getToken(
        @Query("live_id") id: Int
    ): Response<ResponseBody>

    @GET("live/top_banners/")
    suspend fun getTopBanners(): Response<ResponseBody>

    @POST("live/like/")
    suspend fun like(
        @Body body: PlayLikeDTO
    ): Response<ResponseBody>
}