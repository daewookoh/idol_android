package net.ib.mn.data.remote.api

import net.ib.mn.data.remote.dto.HeartPickListResponse
import net.ib.mn.data.remote.dto.HeartPickResponse
import retrofit2.Response
import retrofit2.http.GET
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
}
