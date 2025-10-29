package net.ib.mn.domain.model

/**
 * API 응답을 위한 Sealed Class
 *
 * Success, Error, Loading 상태를 타입 세이프하게 표현
 * old 프로젝트의 Result 클래스를 현대적으로 개선
 */
sealed class ApiResult<out T> {
    /**
     * API 호출 성공
     * @param data 응답 데이터
     */
    data class Success<T>(val data: T) : ApiResult<T>()

    /**
     * API 호출 실패
     * @param exception 에러 정보
     * @param code HTTP 상태 코드 (optional)
     */
    data class Error(
        val exception: Throwable,
        val code: Int? = null,
        val message: String? = exception.message
    ) : ApiResult<Nothing>()

    /**
     * API 호출 중 (로딩)
     */
    data object Loading : ApiResult<Nothing>()
}

/**
 * ApiResult 확장 함수들
 */

/**
 * Success인 경우 데이터를 반환, 아닌 경우 null
 */
fun <T> ApiResult<T>.getOrNull(): T? = when (this) {
    is ApiResult.Success -> data
    else -> null
}

/**
 * Success인 경우 데이터를 반환, 아닌 경우 기본값
 */
fun <T> ApiResult<T>.getOrDefault(default: T): T = when (this) {
    is ApiResult.Success -> data
    else -> default
}

/**
 * Success인 경우 true
 */
fun <T> ApiResult<T>.isSuccess(): Boolean = this is ApiResult.Success

/**
 * Error인 경우 true
 */
fun <T> ApiResult<T>.isError(): Boolean = this is ApiResult.Error

/**
 * Loading인 경우 true
 */
fun <T> ApiResult<T>.isLoading(): Boolean = this is ApiResult.Loading

/**
 * Success인 경우 블록 실행
 */
inline fun <T> ApiResult<T>.onSuccess(block: (T) -> Unit): ApiResult<T> {
    if (this is ApiResult.Success) {
        block(data)
    }
    return this
}

/**
 * Error인 경우 블록 실행
 */
inline fun <T> ApiResult<T>.onError(block: (Throwable) -> Unit): ApiResult<T> {
    if (this is ApiResult.Error) {
        block(exception)
    }
    return this
}

/**
 * Loading인 경우 블록 실행
 */
inline fun <T> ApiResult<T>.onLoading(block: () -> Unit): ApiResult<T> {
    if (this is ApiResult.Loading) {
        block()
    }
    return this
}

/**
 * Success 데이터를 변환
 */
fun <T, R> ApiResult<T>.map(transform: (T) -> R): ApiResult<R> = when (this) {
    is ApiResult.Success -> ApiResult.Success(transform(data))
    is ApiResult.Error -> ApiResult.Error(exception, code, message)
    is ApiResult.Loading -> ApiResult.Loading
}
