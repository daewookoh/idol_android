package net.ib.mn.data.remote.api

import net.ib.mn.data.remote.dto.ThemePickListResponse
import net.ib.mn.data.remote.dto.ThemePickResponse
import retrofit2.Response
import retrofit2.http.GET
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
     * 특정 테마픽 상세 조회
     *
     * @param id 테마픽 ID
     * @return Response<ThemePickResponse>
     */
    @GET("themepick/{id}/")
    suspend fun getThemePick(
        @Path(value = "id") id: Int
    ): Response<ThemePickResponse>
}
