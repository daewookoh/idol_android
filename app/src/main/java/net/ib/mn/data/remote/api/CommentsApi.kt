package net.ib.mn.data.remote.api

import okhttp3.MultipartBody
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * 댓글 관련 API
 * old 프로젝트의 CommentsApi.kt 참고
 */
interface CommentsApi {

    /**
     * 댓글 목록 조회 (cursor 기반 페이징)
     */
    @GET("comments/cursor/")
    suspend fun getComments(
        @Query("article") articleId: Long,
        @Query("cursor") cursor: String? = null,
        @Query("limit") limit: Int = 20
    ): Response<ResponseBody>

    /**
     * 댓글 작성 (텍스트만)
     */
    @Multipart
    @POST("comments/")
    suspend fun writeComment(
        @Part articleId: MultipartBody.Part,
        @Part content: MultipartBody.Part,
        @Part emoticon: MultipartBody.Part? = null
    ): Response<ResponseBody>

    /**
     * 댓글 작성 (이미지 포함)
     */
    @Multipart
    @POST("comments/")
    suspend fun writeCommentWithImage(
        @Part articleId: MultipartBody.Part,
        @Part content: MultipartBody.Part,
        @Part image: MultipartBody.Part?,
        @Part emoticon: MultipartBody.Part? = null
    ): Response<ResponseBody>

    /**
     * 댓글 삭제
     */
    @DELETE("comments/{id}/")
    suspend fun deleteComment(
        @Path("id") commentId: Int
    ): Response<ResponseBody>

    /**
     * 댓글 번역
     */
    @GET("comments/{id}/")
    suspend fun translateComment(
        @Path("id") commentId: Int,
        @Query("translate") translate: String? = "Y"
    ): Response<ResponseBody>
}
