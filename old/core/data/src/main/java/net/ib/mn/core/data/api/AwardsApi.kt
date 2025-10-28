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
 * Awards api
 * */

interface AwardsApi {
    @GET("awards/current/")
    suspend fun current(
    ): Response<ResponseBody>

    @GET("awards/history/")
    suspend fun history(
    ): Response<ResponseBody>
}