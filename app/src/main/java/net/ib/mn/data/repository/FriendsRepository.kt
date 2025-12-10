package net.ib.mn.data.repository

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import net.ib.mn.data.remote.api.FriendRequestBody
import net.ib.mn.data.remote.api.FriendsApi
import net.ib.mn.data.remote.api.GiveHeartBody
import net.ib.mn.data.remote.api.RespondRequestBody
import net.ib.mn.domain.model.ApiResult
import net.ib.mn.domain.model.FriendModel
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

    private val gson = Gson()

    /**
     * 내 친구 목록 조회 (Flow)
     *
     * @return Flow<ApiResult<FriendsResponse>>
     */
    fun getFriendsSelfFlow(): Flow<ApiResult<FriendsResponse>> =
        safeApiCallWithJsonString(
            apiCall = { friendsApi.getFriendsSelf() },
            parser = { json -> parseFriendsResponse(json) }
        )

    /**
     * 내 친구 목록 조회 (suspend)
     *
     * @return FriendsResponse
     */
    suspend fun getFriendsSelf(): FriendsResponse {
        return when (val result = getFriendsSelfFlow().first { it !is ApiResult.Loading }) {
            is ApiResult.Success -> result.data
            is ApiResult.Error -> FriendsResponse(
                success = false,
                friends = emptyList(),
                requesters = emptyList(),
                waiters = emptyList(),
                errorMessage = result.error.message
            )
            is ApiResult.Loading -> FriendsResponse(
                success = false,
                friends = emptyList(),
                requesters = emptyList(),
                waiters = emptyList(),
                errorMessage = "로딩 중 오류"
            )
        }
    }

    /**
     * 친구에게 하트 보내기 (Flow)
     */
    fun giveHeartFlow(friendId: Int, number: Int = 1): Flow<ApiResult<HeartResponse>> =
        safeApiCallWithJsonString(
            apiCall = { friendsApi.giveHeart(GiveHeartBody(friendId.toLong(), number)) },
            parser = { json -> parseHeartResponse(json) }
        )

    /**
     * 친구에게 하트 보내기 (suspend)
     */
    suspend fun giveHeart(friendId: Int, number: Int = 1): HeartResponse {
        return when (val result = giveHeartFlow(friendId, number).first { it !is ApiResult.Loading }) {
            is ApiResult.Success -> result.data
            is ApiResult.Error -> HeartResponse(success = false, message = result.error.message)
            is ApiResult.Loading -> HeartResponse(success = false, message = "로딩 중 오류")
        }
    }

    /**
     * 모든 친구에게 하트 보내기 (Flow)
     */
    fun giveAllHeartFlow(number: Int = 1): Flow<ApiResult<HeartAllResponse>> =
        safeApiCallWithJsonString(
            apiCall = { friendsApi.giveAllHeart(GiveHeartBody(number = number)) },
            parser = { json -> parseHeartAllResponse(json) }
        )

    /**
     * 모든 친구에게 하트 보내기 (suspend)
     */
    suspend fun giveAllHeart(number: Int = 1): HeartAllResponse {
        return when (val result = giveAllHeartFlow(number).first { it !is ApiResult.Loading }) {
            is ApiResult.Success -> result.data
            is ApiResult.Error -> HeartAllResponse(success = false, count = 0, message = result.error.message)
            is ApiResult.Loading -> HeartAllResponse(success = false, count = 0, message = "로딩 중 오류")
        }
    }

    /**
     * 친구 하트 받기 (Flow)
     */
    fun receiveFriendHeartFlow(): Flow<ApiResult<ReceiveHeartResponse>> =
        safeApiCallWithJsonString(
            apiCall = { friendsApi.receiveFriendHeart() },
            parser = { json -> parseReceiveHeartResponse(json) }
        )

    /**
     * 친구 하트 받기 (suspend)
     */
    suspend fun receiveFriendHeart(): ReceiveHeartResponse {
        return when (val result = receiveFriendHeartFlow().first { it !is ApiResult.Loading }) {
            is ApiResult.Success -> result.data
            is ApiResult.Error -> ReceiveHeartResponse(success = false, heart = 0, message = result.error.message, gcode = 0)
            is ApiResult.Loading -> ReceiveHeartResponse(success = false, heart = 0, message = "로딩 중 오류", gcode = 0)
        }
    }

    /**
     * 친구 요청 응답 (수락/거절) (Flow)
     */
    fun respondFriendRequestFlow(partnerId: Int, accept: Boolean): Flow<ApiResult<FriendRequestActionResponse>> =
        safeApiCallWithJsonString(
            apiCall = { friendsApi.respondFriendRequest(RespondRequestBody(partnerId.toLong(), if (accept) "Y" else "N")) },
            parser = { json -> parseFriendRequestActionResponse(json) }
        )

    /**
     * 친구 요청 응답 (수락/거절) (suspend)
     */
    suspend fun respondFriendRequest(partnerId: Int, accept: Boolean): FriendRequestActionResponse {
        return when (val result = respondFriendRequestFlow(partnerId, accept).first { it !is ApiResult.Loading }) {
            is ApiResult.Success -> result.data
            is ApiResult.Error -> FriendRequestActionResponse(success = false, message = result.error.message)
            is ApiResult.Loading -> FriendRequestActionResponse(success = false, message = "로딩 중 오류")
        }
    }

    /**
     * 모든 친구 요청 수락 (Flow)
     */
    fun respondAllFriendRequestFlow(): Flow<ApiResult<FriendRequestActionResponse>> =
        safeApiCallWithJsonString(
            apiCall = { friendsApi.respondAllFriendRequest() },
            parser = { json -> parseFriendRequestActionResponse(json) }
        )

    /**
     * 모든 친구 요청 수락 (suspend)
     */
    suspend fun respondAllFriendRequest(): FriendRequestActionResponse {
        return when (val result = respondAllFriendRequestFlow().first { it !is ApiResult.Loading }) {
            is ApiResult.Success -> result.data
            is ApiResult.Error -> FriendRequestActionResponse(success = false, message = result.error.message)
            is ApiResult.Loading -> FriendRequestActionResponse(success = false, message = "로딩 중 오류")
        }
    }

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

    private fun parseFriendsResponse(jsonString: String): FriendsResponse {
        val json = JSONObject(jsonString)
        if (!json.optBoolean("success", false)) {
            return FriendsResponse(
                success = false,
                friends = emptyList(),
                requesters = emptyList(),
                waiters = emptyList(),
                errorMessage = json.optString("msg", "알 수 없는 오류")
            )
        }

        val array = json.optJSONArray("objects")
        if (array == null || array.length() == 0) {
            return FriendsResponse(
                success = true,
                friends = emptyList(),
                requesters = emptyList(),
                waiters = emptyList()
            )
        }

        val friends = mutableListOf<FriendModel>()
        val requesters = mutableListOf<FriendModel>()
        val waiters = mutableListOf<FriendModel>()

        val type = object : TypeToken<FriendModel>() {}.type

        for (i in 0 until array.length()) {
            try {
                val model: FriendModel = gson.fromJson(array.getJSONObject(i).toString(), type)
                when {
                    model.isFriendYes() -> friends.add(model)
                    model.userType == FriendModel.SEND_USER -> requesters.add(model)
                    model.userType == FriendModel.RECV_USER -> waiters.add(model)
                }
            } catch (e: Exception) {
                // 파싱 에러 무시
            }
        }

        // 가나다순 정렬
        friends.sortBy { it.user.nickname }

        return FriendsResponse(
            success = true,
            friends = friends,
            requesters = requesters,
            waiters = waiters
        )
    }

    private fun parseHeartResponse(jsonString: String): HeartResponse {
        val json = JSONObject(jsonString)
        return HeartResponse(
            success = json.optBoolean("success", false),
            message = json.optString("msg", "")
        )
    }

    private fun parseHeartAllResponse(jsonString: String): HeartAllResponse {
        val json = JSONObject(jsonString)
        return HeartAllResponse(
            success = json.optBoolean("success", false),
            count = json.optInt("count", 0),
            message = json.optString("msg", "")
        )
    }

    private fun parseReceiveHeartResponse(jsonString: String): ReceiveHeartResponse {
        val json = JSONObject(jsonString)
        return ReceiveHeartResponse(
            success = json.optBoolean("success", false),
            heart = json.optInt("heart", 0),
            message = json.optString("msg", ""),
            gcode = json.optInt("gcode", 0)
        )
    }

    private fun parseFriendRequestActionResponse(jsonString: String): FriendRequestActionResponse {
        val json = JSONObject(jsonString)
        return FriendRequestActionResponse(
            success = json.optBoolean("success", false),
            message = json.optString("msg", "")
        )
    }

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

/**
 * 친구 목록 응답
 */
data class FriendsResponse(
    val success: Boolean,
    val friends: List<FriendModel>,
    val requesters: List<FriendModel>,  // 나한테 친구 요청을 한 사람
    val waiters: List<FriendModel>,     // 내가 친구 요청을 한 사람
    val errorMessage: String? = null
)

/**
 * 하트 보내기 응답
 */
data class HeartResponse(
    val success: Boolean,
    val message: String? = null
)

/**
 * 모든 친구에게 하트 보내기 응답
 */
data class HeartAllResponse(
    val success: Boolean,
    val count: Int,
    val message: String? = null
)

/**
 * 하트 받기 응답
 */
data class ReceiveHeartResponse(
    val success: Boolean,
    val heart: Int,
    val message: String? = null,
    val gcode: Int = 0
)

/**
 * 친구 요청 액션 응답 (수락/거절)
 */
data class FriendRequestActionResponse(
    val success: Boolean,
    val message: String? = null
)
