/**
 * Copyright (C) 2024. ExodusEnt Corp. All rights reserved.
 * You must have prior written permission to read this file.
 * @author __jungSangMin__ jnugg0819@gmail.com
 * Description:
 *
 * */

package net.ib.mn.core.data.api

import net.ib.mn.core.data.dto.WriteCommentDTO
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


/**
 * @see
 * */

interface CommentsApi {

    /**
     * 댓글 작성
     */
    @POST("comments/")
    suspend fun writeComment(
        @Body body: WriteCommentDTO
    ): Response<ResponseBody>

    /**
     * 댓글 작성(multipart)
     */
    @Multipart
    @POST("comments/")
    suspend fun writeCommentMultipart(
        @Part articleId: MultipartBody.Part,
        @Part emoticon: MultipartBody.Part? = null,
        @Part content: MultipartBody.Part,
        @Part imageUrl: MultipartBody.Part? = null,
        @Part image: MultipartBody.Part? = null,
    ): Response<ResponseBody>

    @GET("comments/cursor/")
    suspend fun getCommentsCursor(
        @Query("article") articleId: Long,
        @Query("cursor") cursor: String? = null,
        @Query("limit") limit: Int,
    ): Response<ResponseBody>

    /**
     * 댓글 한개 가져오기
     */
    @GET("comments/{id}/")
    suspend fun getComment(
        @Path("id") articleId: Long,
        @Query("translate") translate: String? = null,
    ): Response<ResponseBody>

    @GET("comments/check_hash/")
    suspend fun checkHash(
        @Query("hash") hash: String?,
        @Query("idol_id") idolId: Int,
    ): Response<ResponseBody>

    @DELETE("{path}")
    suspend fun deleteComment(
        @Path(value = "path", encoded = true) path: String,
    ): Response<ResponseBody>
}