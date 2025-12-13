package net.ib.mn.data.remote.api

import net.ib.mn.data.remote.dto.BaseResponse
import net.ib.mn.data.remote.dto.ImagePickAlarmRequest
import net.ib.mn.data.remote.dto.ImagePickListResponse
import net.ib.mn.data.remote.dto.ImagePickResponse
import net.ib.mn.data.remote.dto.ImagePickResultResponse
import net.ib.mn.data.remote.dto.ImagePickVoteRequest
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * Imagepick API (이미지픽)
 *
 * 이미지픽 관련 API 엔드포인트
 * 서버 엔드포인트: onepick/
 */
interface ImagepickApi {

    /**
     * 이미지픽 목록 조회
     *
     * @param offset 페이지 offset
     * @param limit 페이지 limit
     * @return Response<ImagePickListResponse>
     */
    @GET("onepick/")
    suspend fun getImagePickList(
        @Query("offset") offset: Int,
        @Query("limit") limit: Int
    ): Response<ImagePickListResponse>

    /**
     * 특정 이미지픽 상세 조회
     *
     * @param id 이미지픽 ID
     * @return Response<ImagePickResponse>
     */
    @GET("onepick/{id}/")
    suspend fun getImagePick(
        @Path(value = "id") id: Int
    ): Response<ImagePickResponse>

    /**
     * 이미지픽 결과/순위 조회
     * 투표하기 전에 후보 목록을 가져오거나, 순위 결과를 조회할 때 사용
     *
     * @param id 이미지픽 ID
     * @param status 상태 필터 (optional)
     * @return Response<ImagePickResultResponse>
     */
    @GET("onepick/{id}/")
    suspend fun getImagePickResult(
        @Path(value = "id") id: Int,
        @Query("status") status: String? = null
    ): Response<ImagePickResultResponse>

    /**
     * 이미지픽 투표
     *
     * @param body 투표 요청 DTO (id, vote_ids, vote_type)
     * @return Response<BaseResponse<Unit>>
     */
    @POST("onepick/")
    suspend fun voteImagePick(
        @Body body: ImagePickVoteRequest
    ): Response<BaseResponse<Unit>>

    /**
     * 이미지픽 개설 알림 설정
     *
     * @param body 알림 설정 요청 DTO (id)
     * @return Response<BaseResponse<Unit>>
     */
    @POST("onepick/alarm/")
    suspend fun setImagePickAlarm(
        @Body body: ImagePickAlarmRequest
    ): Response<BaseResponse<Unit>>
}
