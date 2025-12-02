package net.ib.mn.data.remote.api

import com.google.gson.annotations.SerializedName
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

/**
 * Friends API - 친구 관련 API
 *
 * old 프로젝트: core/data/src/main/java/net/ib/mn/core/data/api/FriendsApi.kt
 */
interface FriendsApi {

    /**
     * 친구 정보 조회
     * GET friends/friend_info/
     *
     * 응답: { "success": true, "objects": [{ "user": {...}, "is_friend": "Y/N", "user_type": "recv_user/send_user", ... }] }
     *
     * @param userId 유저 ID
     */
    @GET("friends/friend_info/")
    suspend fun getFriendInfo(
        @Query("friend_id") userId: Int
    ): Response<ResponseBody>

    /**
     * 친구 요청 보내기
     * POST friends/req/
     *
     * @param body FriendRequestBody (partner_id)
     */
    @POST("friends/req/")
    suspend fun sendFriendRequest(
        @Body body: FriendRequestBody
    ): Response<ResponseBody>
}

/**
 * 친구 요청 Body
 */
data class FriendRequestBody(
    @SerializedName("partner_id") val partnerId: Long
)
