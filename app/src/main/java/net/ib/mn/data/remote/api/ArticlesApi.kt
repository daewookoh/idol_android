package net.ib.mn.data.remote.api

import net.ib.mn.data.remote.dto.ArticleLikeRequest
import net.ib.mn.data.remote.dto.ArticleLikeResponse
import net.ib.mn.data.remote.dto.ArticleVoteRequest
import net.ib.mn.data.remote.dto.ArticleVoteResponse
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query
import retrofit2.http.Url

/**
 * 게시글 관련 API
 * old 프로젝트의 ArticlesApi.kt 참고
 */
interface ArticlesApi {

    /**
     * 자게 인기글 (HOT)
     */
    @GET("articles/popular/")
    suspend fun getFreeBoardHot(
        @Query("order_by") orderBy: String,
        @Query("keyword") keyword: String? = null,
        @Query("locale") locale: String? = null,
        @Query("limit") limit: Int = 50,
        @Query("offset") offset: Int = 0,
    ): Response<ResponseBody>

    /**
     * 자게 인기글 (resource uri 기반, 페이징)
     */
    @GET
    suspend fun getFreeBoardHotNext(
        @Url url: String,
    ): Response<ResponseBody>

    /**
     * 자게 모든 게시물 (ALL)
     */
    @GET("articles/all_tag/")
    suspend fun getFreeBoardAll(
        @Query("order_by") orderBy: String,
        @Query("keyword") keyword: String? = null,
        @Query("locale") locale: String? = null,
        @Query("limit") limit: Int = 50,
        @Query("offset") offset: Int = 0,
    ): Response<ResponseBody>

    /**
     * 자게 모든 게시물 (resource uri 기반, 페이징)
     */
    @GET
    suspend fun getFreeBoardAllNext(
        @Url url: String,
    ): Response<ResponseBody>

    /**
     * 게시물 불러오기 (태그별)
     */
    @GET("articles/")
    suspend fun getArticles(
        @Query("idol") idolId: Int,
        @Query("order_by") orderBy: String,
        @Query("is_most") isMost: String = "N",
        @Query("keyword") keyword: String? = null,
        @Query("tags") tags: String? = null,
        @Query("primary_file_type") primaryFileType: String? = null,
        @Query("image_only") imageOnly: String? = null,
        @Query("is_popular") isPopular: String? = null,
        @Query("locale") locale: String? = null
    ): Response<ResponseBody>

    /**
     * 게시물 불러오기 (resource uri 기반, 페이징)
     */
    @GET
    suspend fun getArticlesNext(
        @Url url: String,
        @Query("is_most") isMost: String = "N",
        @Query("keyword") keyword: String? = null,
        @Query("tags") tags: String? = null,
        @Query("primary_file_type") primaryFileType: String? = null,
        @Query("image_only") imageOnly: String? = null,
        @Query("is_popular") isPopular: String? = null,
    ): Response<ResponseBody>

    /**
     * 덕질게시판 (최애 탭)
     * 최애 아이돌 관련 게시글 조회
     */
    @GET("articles/inventory/")
    suspend fun getSmallTalkInventory(
        @Query("idol") idolId: Int,
        @Query("is_most") isMost: String = "Y",
        @Query("type") type: String = "M",
        @Query("order_by") orderBy: String,
        @Query("limit") limit: Int = 50,
        @Query("keyword") keyword: String? = null,
        @Query("locale") locale: String? = null,
    ): Response<ResponseBody>

    /**
     * 게시글 투표 (하트 투표)
     * Old 프로젝트: POST "articles/give_heart/"
     */
    @POST("articles/give_heart/")
    suspend fun voteArticle(
        @Body request: ArticleVoteRequest
    ): ArticleVoteResponse

    /**
     * 게시글 좋아요
     * Old 프로젝트: POST "articles/like/"
     */
    @POST("articles/like/")
    suspend fun likeArticle(
        @Body request: ArticleLikeRequest
    ): ArticleLikeResponse
}
