package net.ib.mn.data.repository

import net.ib.mn.data.local.PreferencesManager
import net.ib.mn.data.remote.api.ConfigApi
import net.ib.mn.data.remote.dto.ConfigSelfResponse
import net.ib.mn.data.remote.dto.ConfigStartupResponse
import net.ib.mn.domain.model.ApiResult
import net.ib.mn.domain.repository.ConfigRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import retrofit2.HttpException
import java.io.IOException
import javax.inject.Inject

/**
 * Config Repository 구현체
 *
 * Retrofit API를 사용하여 실제 네트워크 요청 수행
 */
class ConfigRepositoryImpl @Inject constructor(
    private val configApi: ConfigApi,
    private val preferencesManager: PreferencesManager
) : ConfigRepository {

    override fun getConfigStartup(): Flow<ApiResult<ConfigStartupResponse>> = flow {
        emit(ApiResult.Loading)

        try {
            android.util.Log.d("ConfigRepo", "========================================")
            android.util.Log.d("ConfigRepo", "🔵 Calling ConfigStartup API")
            android.util.Log.d("ConfigRepo", "========================================")

            val response = configApi.getConfigStartup()

            android.util.Log.d("ConfigRepo", "📦 Response received:")
            android.util.Log.d("ConfigRepo", "  - HTTP Code: ${response.code()}")
            android.util.Log.d("ConfigRepo", "  - isSuccessful: ${response.isSuccessful}")
            android.util.Log.d("ConfigRepo", "  - Body null: ${response.body() == null}")

            if (response.isSuccessful && response.body() != null) {
                val body = response.body()!!

                // Raw JSON 응답 로그 (Gson 사용)
                try {
                    val gson = com.google.gson.Gson()
                    val jsonString = gson.toJson(body)
                    android.util.Log.d("ConfigRepo", "📄 Raw JSON Response:")
                    android.util.Log.d("ConfigRepo", jsonString)
                } catch (e: Exception) {
                    android.util.Log.e("ConfigRepo", "Failed to serialize: ${e.message}")
                }

                android.util.Log.d("ConfigRepo", "📋 Parsed body:")
                android.util.Log.d("ConfigRepo", "  - success: ${body.success}")
                android.util.Log.d("ConfigRepo", "  - data null: ${body.data == null}")

                if (body.data != null) {
                    android.util.Log.d("ConfigRepo", "  - data.badWords size: ${body.data.badWords?.size ?: 0}")
                    android.util.Log.d("ConfigRepo", "  - data.boardTags size: ${body.data.boardTags?.size ?: 0}")
                    android.util.Log.d("ConfigRepo", "  - data.noticeList length: ${body.data.noticeList?.length ?: 0}")
                }

                if (body.success) {
                    android.util.Log.d("ConfigRepo", "✅ ConfigStartup SUCCESS")
                    emit(ApiResult.Success(body))
                } else {
                    android.util.Log.e("ConfigRepo", "❌ API returned success=false")
                    android.util.Log.e("ConfigRepo", "This means server processed request but returned failure")
                    emit(ApiResult.Error(
                        exception = Exception("API returned success=false"),
                        code = response.code(),
                        message = "Server returned success=false"
                    ))
                }
            } else {
                android.util.Log.e("ConfigRepo", "❌ Response not successful or body null")
                android.util.Log.e("ConfigRepo", "  - Error body: ${response.errorBody()?.string()}")
                emit(ApiResult.Error(
                    exception = HttpException(response),
                    code = response.code()
                ))
            }
        } catch (e: HttpException) {
            android.util.Log.e("ConfigRepo", "❌ HttpException: ${e.code()} - ${e.message()}", e)
            emit(ApiResult.Error(
                exception = e,
                code = e.code(),
                message = "HTTP ${e.code()}: ${e.message()}"
            ))
        } catch (e: IOException) {
            android.util.Log.e("ConfigRepo", "❌ IOException: ${e.message}", e)
            emit(ApiResult.Error(
                exception = e,
                message = "Network error: ${e.message}"
            ))
        } catch (e: Exception) {
            android.util.Log.e("ConfigRepo", "❌ Exception: ${e.message}", e)
            emit(ApiResult.Error(
                exception = e,
                message = "Unknown error: ${e.message}"
            ))
        }
    }

    override fun getConfigSelf(): Flow<ApiResult<ConfigSelfResponse>> = flow {
        emit(ApiResult.Loading)

        try {
            // Get token from DataStore
            val accessToken = preferencesManager.accessToken.first()
            val token = "Bearer ${accessToken ?: ""}"

            val response = configApi.getConfigSelf(token)

            if (response.isSuccessful && response.body() != null) {
                val body = response.body()!!

                if (body.success) {
                    emit(ApiResult.Success(body))
                } else {
                    emit(ApiResult.Error(
                        exception = Exception("API returned success=false"),
                        code = response.code()
                    ))
                }
            } else {
                emit(ApiResult.Error(
                    exception = HttpException(response),
                    code = response.code()
                ))
            }
        } catch (e: HttpException) {
            emit(ApiResult.Error(
                exception = e,
                code = e.code(),
                message = "HTTP ${e.code()}: ${e.message()}"
            ))
        } catch (e: IOException) {
            emit(ApiResult.Error(
                exception = e,
                message = "Network error: ${e.message}"
            ))
        } catch (e: Exception) {
            emit(ApiResult.Error(
                exception = e,
                message = "Unknown error: ${e.message}"
            ))
        }
    }
}
