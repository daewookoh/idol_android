package net.ib.mn.data.remote.dto

import com.google.gson.annotations.SerializedName

/**
 * API 공통 응답 구조
 *
 * 모든 API 응답은 success, gcode 필드를 포함
 * old 프로젝트의 BaseModel, BaseDataModel 통합 버전
 *
 * @param success 비즈니스 로직 성공/실패
 * @param data 실제 응답 데이터
 * @param gcode 서버 상태 코드 (88888 = 점검 중)
 * @param msg 서버 메시지 (에러 시 상세 설명)
 * @param error 에러 데이터 (일부 API에서 사용)
 */
data class BaseResponse<T>(
    @SerializedName("success")
    val success: Boolean = false,

    @SerializedName("data")
    val data: T? = null,

    @SerializedName("gcode")
    val gcode: Int = 0,

    @SerializedName("msg")
    val msg: String? = null,

    @SerializedName("message")
    val message: String? = null,

    @SerializedName("description")
    val description: String? = null,

    @SerializedName("error")
    val error: ErrorData? = null
) {
    /**
     * 에러 메시지 반환 (msg > message > description > error.message 순서)
     */
    fun getErrorMessage(): String? {
        return msg ?: message ?: description ?: error?.message
    }
}

/**
 * object 필드를 사용하는 응답 (old: ObjectBaseDataModel)
 */
data class ObjectResponse<T>(
    @SerializedName("success")
    val success: Boolean = false,

    @SerializedName("object")
    val data: T? = null,

    @SerializedName("gcode")
    val gcode: Int = 0,

    @SerializedName("msg")
    val msg: String? = null,

    @SerializedName("description")
    val description: String? = null
)

/**
 * objects 필드를 사용하는 응답 (old: ObjectsBaseDataModel)
 */
data class ObjectsResponse<T>(
    @SerializedName("success")
    val success: Boolean = false,

    @SerializedName("objects")
    val objects: List<T> = emptyList(),

    @SerializedName("gcode")
    val gcode: Int = 0,

    @SerializedName("msg")
    val msg: String? = null,

    @SerializedName("description")
    val description: String? = null
)

data class ErrorData(
    @SerializedName("code")
    val code: Int?,

    @SerializedName("message")
    val message: String?
)
