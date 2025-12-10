package net.ib.mn.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import net.ib.mn.data.remote.api.UsersApi
import net.ib.mn.data.remote.dto.BlockUserRequest
import net.ib.mn.data.remote.dto.ProvideHeartRequest
import net.ib.mn.data.remote.dto.ProvideHeartResponse
import net.ib.mn.domain.model.ApiError
import net.ib.mn.domain.model.ApiResult
import net.ib.mn.util.logD
import net.ib.mn.util.logE
import org.json.JSONObject
import retrofit2.HttpException
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * UsersRepository - 사용자 관련 API Repository
 *
 * old 프로젝트: core/data/src/main/java/net/ib/mn/core/data/repository/UsersRepositoryImpl.kt
 */
@Singleton
class UsersRepository @Inject constructor(
    private val usersApi: UsersApi
) {
    companion object {
        private const val TAG = "UsersRepository"
    }

    /**
     * 최애 아이돌 변경
     *
     * @param userResourceUri 사용자 resource URI (ex: "/api/v1/users/12345/")
     * @param idolResourceUri 아이돌 resource URI (ex: "/api/v1/idols/678/"), null이면 최애 해제
     * @return 성공 시 JSONObject, 실패 시 null
     */
    suspend fun updateMost(
        userResourceUri: String,
        idolResourceUri: String?
    ): Result<JSONObject> {
        return try {
            val response = if (idolResourceUri == null) {
                // 최애 해제
                usersApi.deleteMost()
            } else {
                // 최애 변경
                val body = mapOf("most" to idolResourceUri)
                usersApi.updateMost(userResourceUri, body)
            }

            if (response.isSuccessful) {
                val jsonString = response.body()?.string() ?: "{}"
                val jsonObject = JSONObject(jsonString)
                logD(TAG, "updateMost success: $jsonObject")
                Result.success(jsonObject)
            } else {
                val errorBody = response.errorBody()?.string() ?: "Unknown error"
                logE(TAG, "updateMost failed: ${response.code()} - $errorBody")
                Result.failure(Exception("API Error: ${response.code()} - $errorBody"))
            }
        } catch (e: Exception) {
            logE(TAG, "updateMost exception: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * 유저 상태 정보 조회
     *
     * old 프로젝트: FeedActivity에서 사용
     * 응답: { "success": true, "status_message": "...", "item_no": 0, "feed_is_viewable": "Y", ... }
     *
     * @param userId 유저 ID
     * @return Result<JSONObject>
     */
    suspend fun getStatus(userId: Int): Result<JSONObject> {
        return try {
            logD(TAG, "getStatus called for userId: $userId")
            val response = usersApi.getStatus(userId)

            if (response.isSuccessful) {
                val jsonString = response.body()?.string() ?: "{}"
                val jsonObject = JSONObject(jsonString)
                logD(TAG, "getStatus success: $jsonObject")
                Result.success(jsonObject)
            } else {
                val errorBody = response.errorBody()?.string() ?: "Unknown error"
                logE(TAG, "getStatus failed: ${response.code()} - $errorBody")
                Result.failure(Exception("API Error: ${response.code()} - $errorBody"))
            }
        } catch (e: Exception) {
            logE(TAG, "getStatus exception: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * 친구(유저) 정보 조회
     *
     * old 프로젝트: FeedActivity에서 사용
     * 응답: { "success": true, "objects": [{ "user": {..., "most": {...}}, ... }] }
     *
     * @param userId 유저 ID
     * @return Result<JSONObject>
     */
    suspend fun getFriendInfo(userId: Int): Result<JSONObject> {
        return try {
            logD(TAG, "getFriendInfo called for userId: $userId")
            val response = usersApi.getFriendInfo(userId)

            if (response.isSuccessful) {
                val jsonString = response.body()?.string() ?: "{}"
                val jsonObject = JSONObject(jsonString)
                logD(TAG, "getFriendInfo success: $jsonObject")
                Result.success(jsonObject)
            } else {
                val errorBody = response.errorBody()?.string() ?: "Unknown error"
                logE(TAG, "getFriendInfo failed: ${response.code()} - $errorBody")
                Result.failure(Exception("API Error: ${response.code()} - $errorBody"))
            }
        } catch (e: Exception) {
            logE(TAG, "getFriendInfo exception: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * 특정 아이돌에게 투표한 유저 랭킹 조회
     *
     * old 프로젝트: HeartVoteRankingActivity에서 사용
     * 응답: { "ranks": { "objects": [...] }, "my_rank": "123" }
     *
     * @param idolId 아이돌 ID
     * @return Flow<ApiResult<String>> JSON 형식의 응답
     */
    fun getRankedUser(idolId: Int): Flow<ApiResult<String>> = flow {
        emit(ApiResult.Loading)

        try {
            logD(TAG, "========================================")
            logD(TAG, "🏆 Calling getRankedUser API (users/ranked_user/)")
            logD(TAG, "  - idolId: $idolId")
            logD(TAG, "========================================")

            val response = usersApi.getRankedUser(idolId)

            logD(TAG, "📦 Response received:")
            logD(TAG, "  - HTTP Code: ${response.code()}")
            logD(TAG, "  - isSuccessful: ${response.isSuccessful}")

            if (response.isSuccessful && response.body() != null) {
                val jsonString = response.body()!!.string()

                logD(TAG, "✅ getRankedUser SUCCESS")
                logD(TAG, "  - JSON length: ${jsonString.length}")
                logD(TAG, "  - JSON preview: ${jsonString.take(300)}")

                emit(ApiResult.Success(jsonString))
            } else {
                logE(TAG, "❌ Response not successful or body null")
                emit(ApiResult.Error(ApiError.fromHttpCode(response.code(), response.message())))
            }
        } catch (e: HttpException) {
            logE(TAG, "❌ HttpException: ${e.code()}", e)
            emit(ApiResult.Error(ApiError.fromHttpCode(e.code(), e.message())))
        } catch (e: IOException) {
            logE(TAG, "❌ IOException: ${e.message}", e)
            emit(ApiResult.Error(ApiError.Network(exception = e)))
        } catch (e: Exception) {
            logE(TAG, "❌ Exception: ${e.message}", e)
            emit(ApiResult.Error(ApiError.Unknown(exception = e)))
        }
    }

    /**
     * 특정 사용자가 차단되어 있는지 확인
     *
     * @param targetId 확인할 사용자 ID
     * @return Boolean true면 차단됨
     */
    suspend fun isUserBlocked(targetId: Int): Boolean {
        return try {
            logD(TAG, "isUserBlocked called for targetId: $targetId")
            val response = usersApi.getBlocks("Y")

            if (response.isSuccessful) {
                val jsonString = response.body()?.string() ?: "{}"
                val jsonObject = JSONObject(jsonString)
                val blockIds = jsonObject.optJSONArray("block_ids")
                if (blockIds != null) {
                    for (i in 0 until blockIds.length()) {
                        if (blockIds.optInt(i) == targetId) {
                            logD(TAG, "User $targetId is blocked")
                            return true
                        }
                    }
                }
                logD(TAG, "User $targetId is NOT blocked")
                false
            } else {
                logE(TAG, "isUserBlocked failed: ${response.code()}")
                false
            }
        } catch (e: Exception) {
            logE(TAG, "isUserBlocked exception: ${e.message}", e)
            false
        }
    }

    /**
     * 사용자 차단
     *
     * old 프로젝트: FeedActivity에서 사용
     * 응답: { "success": true, "gcode": 0 }
     *
     * @param targetId 차단할 사용자 ID
     * @return BlockResult
     */
    suspend fun addBlock(targetId: Int): BlockResult {
        return try {
            logD(TAG, "addBlock called for targetId: $targetId")
            val request = BlockUserRequest(targetId = targetId, block = "Y")
            val response = usersApi.addBlock(request)

            if (response.isSuccessful) {
                val body = response.body()
                logD(TAG, "addBlock success: ${body?.success}, gcode: ${body?.gcode}")
                if (body?.success == true) {
                    BlockResult.Success
                } else {
                    BlockResult.Error(gcode = body?.gcode ?: -1)
                }
            } else {
                logE(TAG, "addBlock failed: ${response.code()}")
                BlockResult.Error(message = "API Error: ${response.code()}")
            }
        } catch (e: Exception) {
            logE(TAG, "addBlock exception: ${e.message}", e)
            BlockResult.Error(message = e.message)
        }
    }

    /**
     * 사용자 차단 해제
     *
     * old 프로젝트: FeedActivity에서 사용
     * 응답: { "success": true, "gcode": 0 }
     *
     * @param targetId 차단 해제할 사용자 ID
     * @return BlockResult
     */
    suspend fun removeBlock(targetId: Int): BlockResult {
        return try {
            logD(TAG, "removeBlock called for targetId: $targetId")
            val request = BlockUserRequest(targetId = targetId, block = "N")
            val response = usersApi.addBlock(request)

            if (response.isSuccessful) {
                val body = response.body()
                logD(TAG, "removeBlock success: ${body?.success}, gcode: ${body?.gcode}")
                if (body?.success == true) {
                    BlockResult.Success
                } else {
                    BlockResult.Error(gcode = body?.gcode ?: -1)
                }
            } else {
                logE(TAG, "removeBlock failed: ${response.code()}")
                BlockResult.Error(message = "API Error: ${response.code()}")
            }
        } catch (e: Exception) {
            logE(TAG, "removeBlock exception: ${e.message}", e)
            BlockResult.Error(message = e.message)
        }
    }

    /**
     * 하트박스 클릭 시 하트 제공 API
     *
     * old 프로젝트: BaseWidePhotoFragment에서 사용
     * 응답: { "success": true, "viewable": true, "heart": 10, "button": false }
     *
     * @param type 제공 타입 ("heartbox")
     * @return ProvideHeartResult
     */
    suspend fun provideHeart(type: String = "heartbox"): ProvideHeartResult {
        return try {
            logD(TAG, "provideHeart called with type: $type")
            val request = ProvideHeartRequest(type = type)
            val response = usersApi.provideHeart(request)

            if (response.isSuccessful) {
                val jsonString = response.body()?.string() ?: "{}"
                val jsonObject = JSONObject(jsonString)
                logD(TAG, "provideHeart response: $jsonObject")

                val success = jsonObject.optBoolean("success", false)
                if (!success) {
                    logD(TAG, "provideHeart: success=false")
                    return ProvideHeartResult.Error(message = "API returned success=false")
                }

                val viewable = jsonObject.optBoolean("viewable", true)
                val heart = jsonObject.optLong("heart", 0)
                val button = jsonObject.optBoolean("button", false)

                logD(TAG, "provideHeart success: viewable=$viewable, heart=$heart, button=$button")
                ProvideHeartResult.Success(
                    viewable = viewable,
                    heart = heart.toInt(),
                    button = button
                )
            } else {
                val errorBody = response.errorBody()?.string() ?: "Unknown error"
                logE(TAG, "provideHeart failed: ${response.code()} - $errorBody")
                ProvideHeartResult.Error(message = "API Error: ${response.code()}")
            }
        } catch (e: Exception) {
            logE(TAG, "provideHeart exception: ${e.message}", e)
            ProvideHeartResult.Error(message = e.message)
        }
    }

    /**
     * 웹 토큰 조회
     *
     * old 프로젝트: FriendsViewModel.getWebTokenSuspend()
     * FriendInvite 웹뷰에서 사용하는 토큰
     *
     * @return WebTokenResult
     */
    suspend fun getWebToken(): WebTokenResult {
        return try {
            logD(TAG, "getWebToken called")
            val response = usersApi.getWebToken()

            if (response.isSuccessful) {
                val jsonString = response.body()?.string() ?: "{}"
                val jsonObject = JSONObject(jsonString)
                logD(TAG, "getWebToken response: $jsonObject")

                val success = jsonObject.optBoolean("success", false)
                val token = jsonObject.optString("token", "")

                if (success && token.isNotBlank()) {
                    logD(TAG, "getWebToken success")
                    WebTokenResult.Success(token)
                } else {
                    val msg = jsonObject.optString("msg", "Failed to get token")
                    logE(TAG, "getWebToken failed: $msg")
                    WebTokenResult.ApiError(msg)
                }
            } else {
                val errorBody = response.errorBody()?.string() ?: "Unknown error"
                logE(TAG, "getWebToken failed: ${response.code()} - $errorBody")
                WebTokenResult.ApiError("API Error: ${response.code()}")
            }
        } catch (e: IOException) {
            logE(TAG, "getWebToken network error: ${e.message}", e)
            WebTokenResult.NetworkError(e.message)
        } catch (e: Exception) {
            logE(TAG, "getWebToken exception: ${e.message}", e)
            WebTokenResult.NetworkError(e.message)
        }
    }
}

/** 하트박스 결과 */
sealed interface ProvideHeartResult {
    data class Success(
        val viewable: Boolean,
        val heart: Int,
        val button: Boolean
    ) : ProvideHeartResult

    data class Error(val message: String? = null) : ProvideHeartResult
}

/** 차단 결과 */
sealed interface BlockResult {
    data object Success : BlockResult
    data class Error(val gcode: Int? = null, val message: String? = null) : BlockResult
}

/** 웹 토큰 결과 */
sealed interface WebTokenResult {
    data class Success(val token: String) : WebTokenResult
    data class ApiError(val message: String? = null) : WebTokenResult
    data class NetworkError(val message: String? = null) : WebTokenResult
}
