package net.ib.mn.data.remote.dto

import com.google.gson.annotations.SerializedName

/**
 * API 공통 응답 구조
 *
 * 모든 API 응답은 success 필드를 포함
 */
data class BaseResponse<T>(
    @SerializedName("success")
    val success: Boolean,

    @SerializedName("data")
    val data: T?,

    @SerializedName("error")
    val error: ErrorData?
)

data class ErrorData(
    @SerializedName("code")
    val code: Int?,

    @SerializedName("message")
    val message: String?
)
