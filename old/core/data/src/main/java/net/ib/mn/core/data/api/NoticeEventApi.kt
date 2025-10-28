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
import retrofit2.http.Path
import retrofit2.http.Query


/**
 * Notice/Event 가져오기 api
 * */

interface NoticeEventApi {
    /**
    * 개별 공지/이벤트 가져오기
    * */
    @GET("{type}/{id}/")
    suspend fun get(
        @Path(value="type") type: String,
        @Path(value="id") id: Int,
        @Query("mode") mode: String? = null, // "dark" or null
    ): Response<ResponseBody>
}