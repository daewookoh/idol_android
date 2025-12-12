package net.ib.mn.data.remote.api

import net.ib.mn.data.remote.dto.BaseResponse
import net.ib.mn.data.remote.dto.ThemePickListResponse
import net.ib.mn.data.remote.dto.ThemePickResponse
import net.ib.mn.data.remote.dto.ThemePickVoteRequest
import net.ib.mn.data.remote.dto.OpenNotificationRequest
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * Themepick API
 *
 * 테마픽 관련 API 엔드포인트
 */
interface ThemepickApi {

    /**
     * 테마픽 목록 조회
     *
     * @param offset 페이지 offset
     * @param limit 페이지 limit
     * @return Response<ThemePickListResponse>
     */
    @GET("themepick/")
    suspend fun getThemePickList(
        @Query("offset") offset: Int,
        @Query("limit") limit: Int
    ): Response<ThemePickListResponse>

    /**
     * 특정 테마픽 상세 조회 (후보 목록 포함)
     *
     * @param id 테마픽 ID
     * @return Response<ThemePickResponse>
     */
    @GET("themepick/{id}/")
    suspend fun getThemePick(
        @Path(value = "id") id: Int
    ): Response<ThemePickResponse>

    /**
     * 테마픽 투표
     *
     * @param voteRequest 투표 요청 데이터
     * @return Response<BaseResponse<Unit>>
     */
    @POST("themepick/vote/")
    suspend fun vote(
        @Body voteRequest: ThemePickVoteRequest
    ): Response<BaseResponse<Unit>>

    /**
     * 테마픽 개설 알림 설정
     *
     * @param notificationRequest 알림 설정 요청 데이터
     * @return Response<BaseResponse<Unit>>
     */
    @POST("themepick/open-notification/")
    suspend fun postOpenNotification(
        @Body notificationRequest: OpenNotificationRequest
    ): Response<BaseResponse<Unit>>
}
