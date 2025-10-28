/**
 * Copyright (C) 2024. ExodusEnt Corp. All rights reserved.
 * You must have prior written permission to read this file.
 * @author __jungSangMin__ jnugg0819@gmail.com
 * Description:
 *
 * */

package net.ib.mn.core.data.api

import net.ib.mn.core.data.dto.CreateArticleDTO
import net.ib.mn.core.data.dto.DownloadArticleDTO
import net.ib.mn.core.data.dto.GiveHeartToArticleDTO
import net.ib.mn.core.data.dto.InsertArticleDTO
import net.ib.mn.core.data.dto.LikeArticleDTO
import net.ib.mn.core.data.dto.UpdateArticleDTO
import net.ib.mn.core.data.dto.ViewCountDTO
import net.ib.mn.core.data.model.ArticleCheckReadyResponse
import net.ib.mn.core.data.model.ArticleLikeModel
import net.ib.mn.core.data.model.ObjectsBaseDataModel
import net.ib.mn.core.data.model.GiveHeartModel
import net.ib.mn.core.data.model.GenericArticleResponse
import okhttp3.ResponseBody
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Url


/**
 * @see
 * */

interface ArticlesApi {

    /**
     * 게시물 삭제
     */
    @DELETE("articles/{id}/")
    suspend fun deleteArticle(@Path(value="id") id: Long): ObjectsBaseDataModel<String>

    /**
     * 게시물 불러오기
     * */
    @GET("articles/")
    suspend fun getArticles(
        @Query("idol") idolId: Int,
        @Query("order_by") orderBy: String,
        @Query("is_most") isMost: String,
        @Query("keyword") keyword: String? = null,
        @Query("tags") tags: String? = null,
        @Query("primary_file_type") primaryFileType: String? = null,
        @Query("image_only") imageOnly: String? = null,
        @Query("is_popular") isPopular: String? = null, // Y or N,
        @Query("locale") locale: String? = null
    ): Response<ResponseBody>

    /**
     * 게시물 불러오기 (resource uri 기반)
     * */
    @GET
    suspend fun getArticles(
        @Url url: String,
        @Query("is_most") isMost: String,
        @Query("keyword") keyword: String? = null,
        @Query("tags") tags: String? = null,
        @Query("primary_file_type") primaryFileType: String? = null,
        @Query("image_only") imageOnly: String? = null,
        @Query("is_popular") isPopular: String? = null, // Y or N
    ): Response<ResponseBody>

    /**
     * 덕질게시판
     */
    @GET("articles/inventory/")
    suspend fun getSmallTalkInventory(
        @Query("idol") idolId: Int,
        @Query("is_most") isMost: String,
        @Query("type") type: String,
        @Query("order_by") orderBy: String,
        @Query("limit") limit: Int? = null,
        @Query("keyword") keyword: String? = null,
        @Query("locale") locale: String? = null,
    ): Response<ResponseBody>

    /**
     * 게시글 좋아요
     */
    @POST("articles/like/")
    suspend fun postLikeArticle(
        @Body body: LikeArticleDTO
    ): ArticleLikeModel

    /**
     * 게시글 투표
     */
    @POST("articles/give_heart/")
    suspend fun postArticleGiveHeart(
        @Body body: GiveHeartToArticleDTO
    ): GiveHeartModel

    /**
     * 게시글 작성
     */
    @POST("articles/create/")
    suspend fun createArticle(
        @Body body: CreateArticleDTO
    ): GenericArticleResponse

    /**
     * 덕질게시판 글 작성
     */
    @POST("articles/insert/")
    suspend fun insertArticle(
        @Body body: InsertArticleDTO
    ): GenericArticleResponse

    /**
     * 덕질게시판 글 수정
     */
    @POST("articles/update/")
    suspend fun updateArticle(
        @Body body: UpdateArticleDTO
    ): GenericArticleResponse

    @GET("articles/check_ready/")
    suspend fun checkReady(
        @Query("article_id") articleId: Long
    ): ArticleCheckReadyResponse

    @POST("articles/download/")
    suspend fun downloadArticle(
        @Body body: DownloadArticleDTO
    ): Response<ResponseBody>

    @POST("articles/viewcount/")
    suspend fun postViewCount(
        @Body body: ViewCountDTO
    ): Response<ResponseBody>

    @GET("articles/activity/")
    suspend fun getFeedActivity(
        @Query("user_id") userId: Long,
        @Query("type") type: String,
        @Query("limit") limit: Int,
        @Query("offset") offset: Int,
        @Query("self") isSelf: Boolean? = null // 내 게시글,댓글이 아니면 안보내야 함
    ): Response<ResponseBody>

    @GET("articles/{id}/")
    suspend fun getArticle(
        @Path("id") articleId: Long,
        @Query("translate") translate: String? = null,
    ): Response<ResponseBody>

    // 자게 인기글
    @GET("articles/popular/")
    suspend fun getFreeBoardHot(
        @Query("order_by") orderBy: String,
        @Query("keyword") keyword: String? = null,
        @Query("locale") locale: String? = null,
        @Query("limit") limit: Int = 50,
        @Query("offset") offset: Int = 0,
    ): Response<ResponseBody>

    // 자게 인기글 (resource uri 기반)
    @GET
    suspend fun getFreeBoardHot(
        @Url url: String,
    ): Response<ResponseBody>

    @GET
    suspend fun getArticle(
        @Url url: String,
    ): Response<ResponseBody>

    // 자게 모든 게시물
    @GET("articles/all_tag/")
    suspend fun getFreeBoardAll(
        @Query("order_by") orderBy: String,
        @Query("keyword") keyword: String? = null,
        @Query("locale") locale: String? = null,
        @Query("limit") limit: Int = 50,
        @Query("offset") offset: Int = 0,
    ): Response<ResponseBody>

    // 자게 모든 게시물 (resource uri 기반)
    @GET
    suspend fun getFreeBoardAll(
        @Url url: String,
    ): Response<ResponseBody>
}