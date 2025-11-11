package net.ib.mn.data.remote.api

import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

/**
 * Favorites API
 *
 * 최애 관련 API 엔드포인트
 */
interface FavoritesApi {

    /**
     * 내 최애 목록 조회
     *
     * GET /api/v1/favorites/self/
     *
     * @return Response<ResponseBody> JSON 형식의 응답
     */
    @GET("favorites/self/")
    suspend fun getFavoritesSelf(): Response<ResponseBody>

    /**
     * 최애 추가
     *
     * POST /api/v1/favorites/
     *
     * @param body AddFavoriteRequest
     * @return Response<ResponseBody>
     */
    @POST("favorites/")
    suspend fun addFavorite(
        @Body body: AddFavoriteRequest
    ): Response<ResponseBody>

    /**
     * 최애 삭제
     *
     * DELETE /api/v1/favorites/{id}/
     *
     * @param id Favorite ID
     * @return Response<ResponseBody>
     */
    @DELETE("favorites/{id}/")
    suspend fun removeFavorite(
        @Path("id") id: Int
    ): Response<ResponseBody>

    /**
     * 최애 캐시 삭제
     *
     * DELETE /api/v1/favorites/cache/
     *
     * @return Response<ResponseBody>
     */
    @DELETE("favorites/cache/")
    suspend fun deleteCache(): Response<ResponseBody>
}

/**
 * 최애 추가 요청 DTO
 */
data class AddFavoriteRequest(
    val idol_id: Int
)
