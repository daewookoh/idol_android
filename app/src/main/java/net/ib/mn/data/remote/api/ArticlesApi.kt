package net.ib.mn.data.remote.api

import net.ib.mn.data.remote.dto.ArticleLikeRequest
import net.ib.mn.data.remote.dto.ArticleLikeResponse
import net.ib.mn.data.remote.dto.ArticleVoteRequest
import net.ib.mn.data.remote.dto.ArticleVoteResponse
import net.ib.mn.data.remote.dto.CreateArticleRequest
import net.ib.mn.data.remote.dto.CreateArticleResponse
import net.ib.mn.data.remote.dto.InsertArticleRequest
import net.ib.mn.data.remote.dto.UpdateArticleRequest
import net.ib.mn.data.remote.dto.UpdateArticleResponse
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
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

    /**
     * 유저 피드 활동 조회 (사진/게시글)
     * Old 프로젝트: GET "articles/activity/"
     *
     * @param userId 유저 ID
     * @param type 타입 ("PHOTO" = 사진, "ALL" = 전체)
     * @param limit 페이지당 개수
     * @param offset 시작 위치
     * @param isSelf 본인 여부 (true면 비공개 포함)
     */
    @GET("articles/activity/")
    suspend fun getFeedActivity(
        @Query("user_id") userId: Int,
        @Query("type") type: String,
        @Query("limit") limit: Int,
        @Query("offset") offset: Int,
        @Query("self") isSelf: Boolean? = null
    ): Response<ResponseBody>

    /**
     * 게시글 삭제
     * Old 프로젝트: DELETE "articles/{id}/"
     */
    @DELETE("articles/{id}/")
    suspend fun deleteArticle(
        @Path("id") id: Long
    ): Response<ResponseBody>

    /**
     * 게시글 상세 조회
     * Old 프로젝트: GET "articles/{id}/"
     *
     * @param id 게시글 ID
     * @param translate 번역 언어 ("auto" 등), null이면 번역 안함
     */
    @GET("articles/{id}/")
    suspend fun getArticle(
        @Path("id") id: Long,
        @Query("translate") translate: String? = null
    ): Response<ResponseBody>

    /**
     * 공지사항 상세 조회
     * Old 프로젝트: GET "notices/{id}/"
     */
    @GET("notices/{id}/")
    suspend fun getNotice(
        @Path("id") id: Int,
        @Query("mode") mode: String? = null
    ): Response<ResponseBody>

    /**
     * 피드/자유게시판 게시글 작성
     * Old 프로젝트: POST "articles/create/"
     */
    @POST("articles/create/")
    suspend fun createArticle(
        @Body request: CreateArticleRequest
    ): CreateArticleResponse

    /**
     * 덕질게시판(팬톡) 게시글 작성
     * Old 프로젝트: POST "articles/insert/"
     */
    @POST("articles/insert/")
    suspend fun insertArticle(
        @Body request: InsertArticleRequest
    ): CreateArticleResponse

    /**
     * 게시글 이미지 처리 완료 확인
     * Old 프로젝트: GET "articles/check_ready/"
     *
     * 게시글 작성 후 이미지 처리가 완료되었는지 확인
     * - success=true: 처리 완료
     * - success=false, gcode=3902: 처리 실패
     */
    @GET("articles/check_ready/")
    suspend fun checkReady(
        @Query("article_id") articleId: Long
    ): Response<ResponseBody>

    /**
     * 게시글 수정
     * Old 프로젝트: POST "articles/update/"
     */
    @POST("articles/update/")
    suspend fun updateArticle(
        @Body request: UpdateArticleRequest
    ): UpdateArticleResponse
}
