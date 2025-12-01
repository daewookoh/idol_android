package net.ib.mn.data.repository

import android.util.Log
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import net.ib.mn.data.remote.api.UsersApi
import net.ib.mn.domain.model.ApiResult
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
                Log.d(TAG, "updateMost success: $jsonObject")
                Result.success(jsonObject)
            } else {
                val errorBody = response.errorBody()?.string() ?: "Unknown error"
                Log.e(TAG, "updateMost failed: ${response.code()} - $errorBody")
                Result.failure(Exception("API Error: ${response.code()} - $errorBody"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "updateMost exception: ${e.message}", e)
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
            Log.d(TAG, "========================================")
            Log.d(TAG, "🏆 Calling getRankedUser API (users/ranked_user/)")
            Log.d(TAG, "  - idolId: $idolId")
            Log.d(TAG, "========================================")

            val response = usersApi.getRankedUser(idolId)

            Log.d(TAG, "📦 Response received:")
            Log.d(TAG, "  - HTTP Code: ${response.code()}")
            Log.d(TAG, "  - isSuccessful: ${response.isSuccessful}")

            if (response.isSuccessful && response.body() != null) {
                val jsonString = response.body()!!.string()

                Log.d(TAG, "✅ getRankedUser SUCCESS")
                Log.d(TAG, "  - JSON length: ${jsonString.length}")
                Log.d(TAG, "  - JSON preview: ${jsonString.take(300)}")

                emit(ApiResult.Success(jsonString))
            } else {
                Log.e(TAG, "❌ Response not successful or body null")
                Log.e(TAG, "  - Error body: ${response.errorBody()?.string()}")
                emit(ApiResult.Error(
                    exception = HttpException(response),
                    code = response.code()
                ))
            }
        } catch (e: HttpException) {
            Log.e(TAG, "❌ HttpException: ${e.code()} - ${e.message()}", e)
            emit(ApiResult.Error(
                exception = e,
                code = e.code(),
                message = "HTTP ${e.code()}: ${e.message()}"
            ))
        } catch (e: IOException) {
            Log.e(TAG, "❌ IOException: ${e.message}", e)
            emit(ApiResult.Error(
                exception = e,
                message = "Network error: ${e.message}"
            ))
        } catch (e: Exception) {
            Log.e(TAG, "❌ Exception: ${e.message}", e)
            emit(ApiResult.Error(
                exception = e,
                message = "Unknown error: ${e.message}"
            ))
        }
    }
}
