package net.ib.mn.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import net.ib.mn.data.remote.api.FriendRequestBody
import net.ib.mn.data.remote.api.FriendsApi
import net.ib.mn.domain.model.ApiResult
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

/**
 * FriendsRepository - 친구 관련 Repository
 *
 * old 프로젝트: core/data/src/main/java/net/ib/mn/core/data/repository/friends/FriendsRepositoryImpl.kt
 */
@Singleton
class FriendsRepository @Inject constructor(
    private val friendsApi: FriendsApi
) : BaseRepository() {

    /**
     * 친구 정보 조회 (Flow)
     *
     * @param userId 유저 ID
     * @return Flow<ApiResult<FriendInfo>>
     */
    fun getFriendInfoFlow(userId: Int): Flow<ApiResult<FriendInfo>> =
        safeApiCallWithJsonString(
            apiCall = { friendsApi.getFriendInfo(userId) },
            parser = { json -> parseFriendInfo(json) }
        )

    /**
     * 친구 정보 조회 (suspend)
     *
     * @param userId 유저 ID
     * @return FriendInfoResult
     */
    suspend fun getFriendInfo(userId: Int): FriendInfoResult {
        return when (val result = getFriendInfoFlow(userId).first { it !is ApiResult.Loading }) {
            is ApiResult.Success -> {
                val info = result.data
                if (info.found) {
                    FriendInfoResult.Success(
                        isFriend = info.isFriend,
                        userType = info.userType
                    )
                } else {
                    FriendInfoResult.NotFound
                }
            }
            is ApiResult.Error -> {
                FriendInfoResult.Error(result.error.message ?: "알 수 없는 오류가 발생했습니다.")
            }
            is ApiResult.Loading -> {
                FriendInfoResult.Error("로딩 중 오류")
            }
        }
    }

    /**
     * 친구 요청 보내기 (Flow)
     *
     * @param userId 유저 ID
     * @return Flow<ApiResult<FriendRequestResponse>>
     */
    fun sendFriendRequestFlow(userId: Int): Flow<ApiResult<FriendRequestResponse>> =
        safeApiCallWithJsonString(
            apiCall = { friendsApi.sendFriendRequest(FriendRequestBody(userId.toLong())) },
            parser = { json -> parseFriendRequestResponse(json) }
        )

    /**
     * 친구 요청 보내기 (suspend)
     *
     * @param userId 유저 ID
     * @return FriendRequestResult
     */
    suspend fun sendFriendRequest(userId: Int): FriendRequestResult {
        return when (val result = sendFriendRequestFlow(userId).first { it !is ApiResult.Loading }) {
            is ApiResult.Success -> {
                val response = result.data
                if (response.success) {
                    FriendRequestResult.Success
                } else {
                    FriendRequestResult.Error(gcode = response.gcode)
                }
            }
            is ApiResult.Error -> {
                FriendRequestResult.Error(message = result.error.message)
            }
            is ApiResult.Loading -> {
                FriendRequestResult.Error(message = "로딩 중 오류")
            }
        }
    }

    // ========== Parser Functions ==========

    private fun parseFriendInfo(jsonString: String): FriendInfo {
        val json = JSONObject(jsonString)
        if (!json.optBoolean("success", false)) {
            return FriendInfo(found = false, isFriend = false, userType = "")
        }

        val array = json.optJSONArray("objects")
        if (array == null || array.length() == 0) {
            return FriendInfo(found = false, isFriend = false, userType = "")
        }

        val obj = array.optJSONObject(0)
        val isFriend = obj?.optString("is_friend", "N")?.equals("Y", ignoreCase = true) ?: false
        val userType = obj?.optString("user_type", "") ?: ""

        return FriendInfo(found = true, isFriend = isFriend, userType = userType)
    }

    private fun parseFriendRequestResponse(jsonString: String): FriendRequestResponse {
        val json = JSONObject(jsonString)
        val success = json.optBoolean("success", false)
        val gcode = json.optInt("gcode", 0)
        return FriendRequestResponse(success = success, gcode = gcode)
    }
}

/**
 * 친구 정보 (파싱 결과)
 */
data class FriendInfo(
    val found: Boolean,
    val isFriend: Boolean,
    val userType: String
)

/**
 * 친구 요청 응답 (파싱 결과)
 */
data class FriendRequestResponse(
    val success: Boolean,
    val gcode: Int = 0
)

/**
 * 친구 정보 조회 결과
 */
sealed class FriendInfoResult {
    /**
     * @param isFriend 친구 여부
     * @param userType "recv_user" = 내가 친구 요청을 한 사람, "send_user" = 나한테 친구 요청을 한 사람
     */
    data class Success(val isFriend: Boolean, val userType: String) : FriendInfoResult()
    data object NotFound : FriendInfoResult()
    data class Error(val message: String) : FriendInfoResult()
}

/**
 * 친구 요청 결과
 */
sealed class FriendRequestResult {
    data object Success : FriendRequestResult()
    data class Error(val gcode: Int? = null, val message: String? = null) : FriendRequestResult()
}

/**
 * FriendModel user_type 상수
 */
object FriendUserType {
    const val RECV_USER = "recv_user"  // 내가 친구 요청을 한 사람
    const val SEND_USER = "send_user"  // 나한테 친구 요청을 한 사람
}
