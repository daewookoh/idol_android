package net.ib.mn.domain.model

import com.google.gson.annotations.SerializedName

/**
 * 아이돌 모델
 * 이미지픽 후보 등에서 사용
 */
data class IdolModel(
    @SerializedName("id") val id: Int = 0,
    @SerializedName("name") val name: String = "",
    @SerializedName("name_en") val nameEn: String = "",
    @SerializedName("name_jp") val nameJp: String = "",
    @SerializedName("name_zh") val nameZh: String = "",
    @SerializedName("name_zh_tw") val nameZhTw: String = "",
    @SerializedName("group_id") val groupId: Int = 0,
    @SerializedName("image_url") val imageUrl: String? = null,
    @SerializedName("image_url2") val imageUrl2: String? = null,
    @SerializedName("heart") val heart: Long = 0,
    @SerializedName("type") val type: String = "",
    @SerializedName("category") val category: String = "",
    @SerializedName("resource_uri") val resourceUri: String = ""
)
