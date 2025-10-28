package net.ib.mn.core.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class IdolLiteResponse(
    @SerialName("group_id") val groupId: Long = 0L,
    @SerialName("id") val id: Long = 0L,
    @SerialName("image_url") val imageUrl: String? = null,
    @SerialName("name") val name: String = "",
    @SerialName("name_en") val nameEN: String = "",
    @SerialName("name_jp") val nameJP: String = "",
    @SerialName("name_zh") val nameZH: String = "",
    @SerialName("name_zh_tw") val nameZHTW: String = "",
)
