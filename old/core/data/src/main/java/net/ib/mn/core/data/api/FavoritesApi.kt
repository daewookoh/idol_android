/**
 * Copyright (C) 2024. ExodusEnt Corp. All rights reserved.
 * You must have prior written permission to read this file.
 * @author __jungSangMin__ jnugg0819@gmail.com
 * Description:
 *
 * */

package net.ib.mn.core.data.api

import net.ib.mn.core.data.dto.AddFavoriteDTO
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path


/**
 * @see
 * */

interface FavoritesApi {

    @GET("favorites/self/")
    suspend fun getFavoritesSelf(
    ): Response<ResponseBody>

    @POST("favorites/")
    suspend fun addFavorite(
        @Body body: AddFavoriteDTO
    ): Response<ResponseBody>

    @DELETE("favorites/{id}/")
    suspend fun removeFavorite(
        @Path("id") id: Int
    ): Response<ResponseBody>

    @DELETE("favorites/cache/")
    suspend fun deleteCache(
    ): Response<ResponseBody>
}