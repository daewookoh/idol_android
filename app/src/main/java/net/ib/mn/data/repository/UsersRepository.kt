package net.ib.mn.data.repository

import android.util.Log
import net.ib.mn.data.remote.api.UsersApi
import org.json.JSONObject
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
}
