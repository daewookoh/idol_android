package net.ib.mn.data.remote.api

import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

interface ReportApi {

    /**
     * 유저 신고하기
     */
    @POST("reports/")
    suspend fun reportUser(
        @Body body: ReportUserRequest
    ): Response<ResponseBody>

    /**
     * 신고 가능 여부 확인
     */
    @GET("reports/possible/user/")
    suspend fun getReportPossible(
        @Query("recv_user_id") recvUserId: Int,
    ): Response<ResponseBody>
}

data class ReportUserRequest(
    val user_id: Int,
    val reason: String
)
