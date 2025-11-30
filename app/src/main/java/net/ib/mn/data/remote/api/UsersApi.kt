package net.ib.mn.data.remote.api

import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Url

/**
 * Users API - 사용자 관련 API
 *
 * old 프로젝트: core/data/src/main/java/net/ib/mn/core/data/api/UsersApi.kt
 */
interface UsersApi {

    /**
     * 최애 아이돌 변경
     * PATCH {userResourceUri}
     * Body: { "most": "{idolResourceUri}" }
     */
    @PATCH
    suspend fun updateMost(
        @Url resourceUri: String,
        @Body body: Map<String, String?>
    ): Response<ResponseBody>

    /**
     * 최애 아이돌 해제
     * POST users/delmost/
     */
    @POST("users/delmost/")
    suspend fun deleteMost(): Response<ResponseBody>
}
