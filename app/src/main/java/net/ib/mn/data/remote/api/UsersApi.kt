package net.ib.mn.data.remote.api

import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Query
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

    /**
     * 특정 아이돌에게 투표한 유저 랭킹 조회
     * GET users/ranked_user/
     *
     * old 프로젝트: HeartVoteRankingActivity에서 사용
     * 응답: { "ranks": { "objects": [...] }, "my_rank": "123" }
     *
     * @param idolId 아이돌 ID
     * @param league 리그 (optional)
     */
    @GET("users/ranked_user/")
    suspend fun getRankedUser(
        @Query("idol_id") idolId: Int,
        @Query("league") league: String? = null
    ): Response<ResponseBody>

    /**
     * 유저 상태 정보 조회
     * GET users/status/
     *
     * old 프로젝트: FeedActivity에서 사용
     * 응답: { "success": true, "status_message": "...", "item_no": 0, "feed_is_viewable": "Y", ... }
     *
     * @param userId 유저 ID
     */
    @GET("users/status/")
    suspend fun getStatus(
        @Query("user_id") userId: Int
    ): Response<ResponseBody>

    /**
     * 친구(유저) 정보 조회
     * GET friends/friend_info/
     *
     * old 프로젝트: FeedActivity에서 사용
     * 응답: { "success": true, "objects": [{ "user": {..., "most": {...}}, ... }] }
     *
     * @param userId 유저 ID
     */
    @GET("friends/friend_info/")
    suspend fun getFriendInfo(
        @Query("friend_id") userId: Int
    ): Response<ResponseBody>
}
