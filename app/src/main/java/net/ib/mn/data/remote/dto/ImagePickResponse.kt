package net.ib.mn.data.remote.dto

import com.google.gson.annotations.SerializedName
import net.ib.mn.domain.model.ImagePickModel

/**
 * 이미지픽 목록 응답
 */
data class ImagePickListResponse(
    @SerializedName("objects") val objects: List<ImagePickModel>? = null,
    @SerializedName("meta") val meta: MetaData? = null
)

/**
 * 이미지픽 상세 응답
 */
data class ImagePickResponse(
    @SerializedName("object") val `object`: ImagePickModel? = null
)
