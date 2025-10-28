/**
 * Copyright (C) 2025. ExodusEnt Corp. All rights reserved.
 * You must have prior written permission to read this file.
 * @author parkboo@gmail.com
 * Description:
 *
 * */

package net.ib.mn.core.data.api

import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query
import retrofit2.http.QueryMap


interface HofsApi {
    @GET("hofs/")
    suspend fun get(
        @QueryMap(
            encoded = true,
        ) params: Map<String, String?>,
    ): Response<ResponseBody>

    @GET("hofs/top1_count/")
    suspend fun getTop1Count(
    ): String
}