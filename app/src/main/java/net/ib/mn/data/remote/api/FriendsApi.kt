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
     * 친구 목록 불러오기
     * GET friends/self/
     *
     * 응답: { "success": true, "objects": [{ "user": {...}, "is_friend": "Y/N", "user_type": "recv_user/send_user", ... }] }
     */
    @GET("friends/self/")
    suspend fun getFriendsSelf(): Response<ResponseBody>

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
     * 친구에게 하트 보내기
     * POST friends/give_heart/
     *
     * @param body GiveHeartBody (friend_id, number)
     */
    @POST("friends/give_heart/")
    suspend fun giveHeart(
        @Body body: GiveHeartBody
    ): Response<ResponseBody>

    /**
     * 모든 친구에게 하트 보내기
     * POST friends/give_all_heart/
     *
     * @param body GiveHeartBody (number)
     */
    @POST("friends/give_all_heart/")
    suspend fun giveAllHeart(
        @Body body: GiveHeartBody
    ): Response<ResponseBody>

    /**
     * 친구 하트 받기
     * POST friends/receive_friendheart/
     */
    @POST("friends/receive_friendheart/")
    suspend fun receiveFriendHeart(): Response<ResponseBody>

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

    /**
     * 친구 요청 응답 (수락/거절)
     * POST friends/resp/
     *
     * @param body RespondRequestBody (partner_id, ans)
     */
    @POST("friends/resp/")
    suspend fun respondFriendRequest(
        @Body body: RespondRequestBody
    ): Response<ResponseBody>

    /**
     * 모든 친구 요청 수락
     * POST friends/resp_all/
     */
    @POST("friends/resp_all/")
    suspend fun respondAllFriendRequest(): Response<ResponseBody>

    /**
     * 친구 요청 취소
     * POST friends/del/
     *
     * @param body CancelFriendRequestBody (id)
     */
    @POST("friends/del/")
    suspend fun cancelFriendRequest(
        @Body body: CancelFriendRequestBody
    ): Response<ResponseBody>

    /**
     * 친구 삭제
     * POST friends/delete_list/
     *
     * @param body DeleteFriendsBody (ids)
     */
    @POST("friends/delete_list/")
    suspend fun deleteFriends(
        @Body body: DeleteFriendsBody
    ): Response<ResponseBody>
}

/**
 * 친구 요청 Body
 */
data class FriendRequestBody(
    @SerializedName("partner_id") val partnerId: Long
)

/**
 * 하트 보내기 Body
 */
data class GiveHeartBody(
    @SerializedName("friend_id") val friendId: Long? = null,
    @SerializedName("number") val number: Int = 1
)

/**
 * 친구 요청 응답 Body
 */
data class RespondRequestBody(
    @SerializedName("partner_id") val partnerId: Long,
    @SerializedName("ans") val ans: String // "Y" or "N"
)

/**
 * 친구 요청 취소 Body
 */
data class CancelFriendRequestBody(
    @SerializedName("id") val id: Int
)

/**
 * 친구 삭제 Body
 */
data class DeleteFriendsBody(
    @SerializedName("ids") val ids: List<Int>
)
