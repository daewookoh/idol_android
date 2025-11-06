package net.ib.mn.data.remote.dto

import com.google.gson.annotations.SerializedName
import net.ib.mn.domain.model.HeartPickModel

/**
 * 하트픽 목록 응답
 */
data class HeartPickListResponse(
    @SerializedName("objects") val objects: List<HeartPickModel>? = null,
    @SerializedName("meta") val meta: MetaData? = null
)

/**
 * 하트픽 상세 응답
 */
data class HeartPickResponse(
    @SerializedName("object") val `object`: HeartPickModel? = null
)

data class MetaData(
    @SerializedName("next") val next: String? = null,
    @SerializedName("limit") val limit: Int = 0,
    @SerializedName("offset") val offset: Int = 0,
    @SerializedName("total_count") val totalCount: Int = 0
)
