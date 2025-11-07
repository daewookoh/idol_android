package net.ib.mn.data.remote.dto

import com.google.gson.annotations.SerializedName
import net.ib.mn.domain.model.ThemePickModel

/**
 * 테마픽 목록 응답
 */
data class ThemePickListResponse(
    @SerializedName("objects") val objects: List<ThemePickModel>? = null,
    @SerializedName("meta") val meta: MetaData? = null
)

/**
 * 테마픽 상세 응답
 */
data class ThemePickResponse(
    @SerializedName("object") val `object`: ThemePickModel? = null
)
