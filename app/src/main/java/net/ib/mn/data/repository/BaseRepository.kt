package net.ib.mn.data.repository

import kotlinx.coroutines.flow.Flow
import net.ib.mn.util.logE
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import net.ib.mn.domain.model.ApiError
import net.ib.mn.domain.model.ApiResult
import retrofit2.HttpException
import retrofit2.Response
import java.io.IOException

/**
 * BaseRepository
 *
 * 모든 Repository에서 공통으로 사용하는 API 호출 로직 제공
 * old 프로젝트의 에러 처리 패턴을 통합하여 일관된 응답 처리
 */
abstract class BaseRepository {

    companion object {
        private const val TAG = "BaseRepository"
    }

    // ============================================================
    // Public API Methods
    // ============================================================

    /**
     * 기본 API 호출 (Response<T> → ApiResult<T>)
     */
    protected fun <T> safeApiCall(
        apiCall: suspend () -> Response<T>
    ): Flow<ApiResult<T>> = executeApiCall { handleResponse(apiCall()) }

    /**
     * ResponseBody를 JSON String으로 받아서 파싱하는 API 호출
     *
     * 사용 예:
     * ```kotlin
     * safeApiCallWithJsonString(
     *     apiCall = { api.getSchedules(idolId) },
     *     parser = { json -> parseSchedulesResponse(json) }
     * )
     * ```
     */
    protected fun <T> safeApiCallWithJsonString(
        apiCall: suspend () -> Response<okhttp3.ResponseBody>,
        parser: (String) -> T
    ): Flow<ApiResult<T>> = executeApiCall {
        val response = apiCall()
        if (response.isSuccessful) {
            response.body()?.string()?.let { jsonString ->
                ApiResult.Success(parser(jsonString))
            } ?: ApiResult.Error(ApiError.Unknown(
                message = "Response body is null",
                exception = Exception("Empty response body")
            ))
        } else {
            ApiResult.Error(ApiError.fromHttpCode(response.code(), response.message()))
        }
    }

    // ============================================================
    // Flow Extensions for Result Transformation
    // ============================================================

    /**
     * ApiResult<T>에서 object 필드 추출
     *
     * 사용 예:
     * ```kotlin
     * safeApiCall { api.getItem(id) }.extractObject({ it.object }, "Item not found")
     * ```
     */
    protected fun <T, R> Flow<ApiResult<T>>.extractObject(
        extractor: (T) -> R?,
        errorMessage: String
    ): Flow<ApiResult<R>> = map { result ->
        when (result) {
            is ApiResult.Success -> {
                val extracted = extractor(result.data)
                if (extracted != null) {
                    ApiResult.Success(extracted)
                } else {
                    ApiResult.Error(ApiError.Business(gcode = 0, message = errorMessage))
                }
            }
            is ApiResult.Error -> result
            is ApiResult.Loading -> ApiResult.Loading
        }
    }

    /**
     * ApiResult<T>에서 리스트 필드 추출
     */
    protected fun <T, R> Flow<ApiResult<T>>.extractList(
        extractor: (T) -> List<R>?
    ): Flow<ApiResult<List<R>>> = map { result ->
        when (result) {
            is ApiResult.Success -> ApiResult.Success(extractor(result.data) ?: emptyList())
            is ApiResult.Error -> result
            is ApiResult.Loading -> ApiResult.Loading
        }
    }

    /**
     * success 필드 검증
     */
    protected fun <T : HasSuccess> Flow<ApiResult<T>>.validateSuccess(): Flow<ApiResult<T>> =
        map { result ->
            when (result) {
                is ApiResult.Success -> {
                    if (result.data.success) {
                        result
                    } else {
                        ApiResult.Error(ApiError.Business(gcode = 0, message = "API returned success=false"))
                    }
                }
                else -> result
            }
        }

    // ============================================================
    // Private Implementation
    // ============================================================

    /**
     * 공통 try-catch 로직 (Flow)
     */
    private fun <T> executeApiCall(
        block: suspend () -> ApiResult<T>
    ): Flow<ApiResult<T>> = flow {
        emit(ApiResult.Loading)
        emit(executeWithErrorHandling(block))
    }

    /**
     * 에러 핸들링 공통 로직
     */
    private suspend fun <T> executeWithErrorHandling(
        block: suspend () -> ApiResult<T>
    ): ApiResult<T> {
        return try {
            block()
        } catch (e: HttpException) {
            logE(TAG, "HttpException: ${e.code()} - ${e.message()}", e)
            ApiResult.Error(ApiError.fromHttpCode(e.code(), e.message()))
        } catch (e: IOException) {
            logE(TAG, "IOException: ${e.message}", e)
            ApiResult.Error(ApiError.Network(exception = e))
        } catch (e: Exception) {
            logE(TAG, "Unknown Exception: ${e.message}", e)
            ApiResult.Error(ApiError.Unknown(exception = e))
        }
    }

    /**
     * 일반 Response<T> 처리
     */
    private fun <T> handleResponse(response: Response<T>): ApiResult<T> {
        return if (response.isSuccessful) {
            response.body()?.let { ApiResult.Success(it) }
                ?: ApiResult.Error(ApiError.Unknown(
                    message = "Response body is null",
                    exception = Exception("Empty response body")
                ))
        } else {
            ApiResult.Error(ApiError.fromHttpCode(response.code(), response.message()))
        }
    }
}

/**
 * success 필드를 가진 응답의 공통 인터페이스
 */
interface HasSuccess {
    val success: Boolean
}
