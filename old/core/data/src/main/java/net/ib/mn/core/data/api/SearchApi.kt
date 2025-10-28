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
 * Quiz api
 * */

interface SearchApi {
    @GET("search/")
    suspend fun search(
        @Query("q") keyword: String?,
        @Query("category") category: String?,
        @Query("offset") offset: Int,
        @Query("limit") limit: Int
    ): Response<ResponseBody>

    @GET("search/trend/")
    suspend fun trend(
    ): Response<ResponseBody>

    @GET("search/suggest/")
    suspend fun suggest(
        @Query("q") q: String,
    ): Response<ResponseBody>
}