package net.ib.mn.data.remote.api

import net.ib.mn.data.remote.dto.HeartPickListResponse
import net.ib.mn.data.remote.dto.HeartPickResponse
import okhttp3.MultipartBody
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * Heartpick API
 *
 * 하트픽 관련 API 엔드포인트
 */
interface HeartpickApi {

    /**
     * 하트픽 목록 조회
     *
     * @param offset 페이지 offset
     * @param limit 페이지 limit
     * @return Response<HeartPickListResponse>
     */
    @GET("heartpick/")
    suspend fun getHeartPickList(
        @Query("offset") offset: Int,
        @Query("limit") limit: Int
    ): Response<HeartPickListResponse>

    /**
     * 특정 하트픽 상세 조회
     *
     * @param id 하트픽 ID
     * @param offset 페이지 offset (아이돌 목록)
     * @param limit 페이지 limit (아이돌 목록)
     * @return Response<HeartPickResponse>
     */
    @GET("heartpick/{id}/")
    suspend fun getHeartPick(
        @Path(value = "id") id: Int,
        @Query("offset") offset: Int = 0,
        @Query("limit") limit: Int = 100
    ): Response<HeartPickResponse>

    /**
     * 하트픽 댓글 목록 조회
     *
     * @param heartPickId 하트픽 ID
     * @param limit 페이지당 개수
     * @param cursor 페이징 커서
     * @return Response<ResponseBody>
     */
    @GET("heartpick/replies/")
    suspend fun getReplies(
        @Query("id") heartPickId: Int,
        @Query("limit") limit: Int,
        @Query("cursor") cursor: String? = null
    ): Response<ResponseBody>

    /**
     * 하트픽 댓글 작성
     *
     * @param id 하트픽 ID
     * @param emoticon 이모티콘 ID
     * @param content 댓글 내용
     * @param imageUrl CDN 이미지 URL (있는 경우)
     * @param image 이미지 바이너리
     * @return Response<ResponseBody>
     */
    @Multipart
    @POST("heartpick/replies/")
    suspend fun postReply(
        @Part id: MultipartBody.Part,
        @Part emoticon: MultipartBody.Part? = null,
        @Part content: MultipartBody.Part,
        @Part image_url: MultipartBody.Part? = null,
        @Part image: MultipartBody.Part? = null
    ): Response<ResponseBody>
}
