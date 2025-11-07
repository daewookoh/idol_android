package net.ib.mn.data.remote.api

import net.ib.mn.data.remote.dto.ImagePickListResponse
import net.ib.mn.data.remote.dto.ImagePickResponse
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * Onepick API (이미지픽)
 *
 * 이미지픽 관련 API 엔드포인트
 */
interface OnepickApi {

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
}
