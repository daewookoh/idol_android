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
     * @param error 구조화된 에러 정보
     */
    data class Error(val error: ApiError) : ApiResult<Nothing>() {
        // 하위 호환성을 위한 프로퍼티들
        val exception: Throwable get() = error.exception
        val code: Int? get() = error.httpCode
        val gcode: Int? get() = error.gcode
        val message: String? get() = error.message

        companion object {
            /**
             * 하위 호환성을 위한 팩토리 메서드
             * 기존 코드에서 ApiResult.Error(exception = e, code = 401, message = "...") 형태로 사용하던 것을
             * ApiResult.Error.create(exception = e, code = 401, message = "...") 로 변경
             */
            fun create(
                exception: Throwable,
                code: Int? = null,
                message: String? = exception.message,
                data: Any? = null
            ): Error {
                val apiError = when {
                    code == 401 -> ApiError.Unauthorized(message, exception)
                    code != null -> ApiError.Http(code, message, exception)
                    exception is java.io.IOException -> ApiError.Network(message, exception)
                    else -> ApiError.Unknown(message, exception)
                }
                return Error(apiError)
            }
        }
    }

    /**
     * API 호출 중 (로딩)
     */
    data object Loading : ApiResult<Nothing>()
}

/**
 * 구조화된 API 에러
 *
 * old 프로젝트의 gcode 기반 에러 처리를 타입 세이프하게 구현
 */
sealed class ApiError(
    open val exception: Throwable,
    open val httpCode: Int? = null,
    open val gcode: Int? = null,
    open val message: String? = null
) {
    /**
     * 서버 점검 중 (gcode == 88888)
     */
    data class Maintenance(
        override val message: String? = "서버 점검 중입니다",
        override val exception: Throwable = Exception("Server Maintenance")
    ) : ApiError(exception, null, GCode.MAINTENANCE, message)

    /**
     * HTTP 에러 (4xx, 5xx)
     */
    data class Http(
        override val httpCode: Int,
        override val message: String? = null,
        override val exception: Throwable = Exception("HTTP $httpCode")
    ) : ApiError(exception, httpCode, null, message)

    /**
     * 인증 에러 (401 Unauthorized)
     */
    data class Unauthorized(
        override val message: String? = "인증이 필요합니다",
        override val exception: Throwable = Exception("Unauthorized")
    ) : ApiError(exception, 401, null, message)

    /**
     * 네트워크 에러 (IOException)
     */
    data class Network(
        override val message: String? = "네트워크 연결을 확인해주세요",
        override val exception: Throwable
    ) : ApiError(exception, null, null, message)

    /**
     * 비즈니스 로직 에러 (gcode != 0)
     */
    data class Business(
        override val gcode: Int,
        override val message: String? = null,
        override val exception: Throwable = Exception("Business Error: gcode=$gcode")
    ) : ApiError(exception, null, gcode, message)

    /**
     * 알 수 없는 에러
     */
    data class Unknown(
        override val message: String? = "알 수 없는 오류가 발생했습니다",
        override val exception: Throwable
    ) : ApiError(exception, null, null, message)

    companion object {
        /**
         * gcode로부터 적절한 ApiError 생성
         */
        fun fromGcode(gcode: Int, message: String? = null): ApiError {
            return when (gcode) {
                GCode.MAINTENANCE -> Maintenance(message)
                GCode.SUCCESS -> Unknown(message, Exception("Unexpected success gcode in error"))
                else -> Business(gcode, message)
            }
        }

        /**
         * HTTP 상태 코드로부터 적절한 ApiError 생성
         */
        fun fromHttpCode(code: Int, message: String? = null): ApiError {
            return when (code) {
                401 -> Unauthorized(message)
                else -> Http(code, message)
            }
        }
    }
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
    is ApiResult.Error -> ApiResult.Error(error)
    is ApiResult.Loading -> ApiResult.Loading
}
