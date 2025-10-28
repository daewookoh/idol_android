/**
 * Copyright (C) 2024. ExodusEnt Corp. All rights reserved.
 * You must have prior written permission to read this file.
 * @author __jungSangMin__ jnugg0819@gmail.com
 * Description:
 *
 * */

package net.ib.mn.core.data.api

import net.ib.mn.core.data.dto.ClaimMessageDTO
import net.ib.mn.core.data.dto.DeleteMessageByTypeDTO
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query


/**
 * Message/Coupon api
 * */

interface MessagesApi {
    @POST("messages/delete_by_type/")
    suspend fun deleteByType(
        @Body body: DeleteMessageByTypeDTO
    ): Response<ResponseBody>

    @GET("messages/")
    suspend fun get(
        @Query("type") type: String? = null,
        @Query("after") after: String? = null,
    ): Response<ResponseBody>

    @DELETE("messages/{id}/")
    suspend fun delete(
        @Path("id") id: Int
    ): Response<ResponseBody>

    @POST("messages/claim/")
    suspend fun claim(
        @Body body: ClaimMessageDTO
    ): Response<ResponseBody>
}