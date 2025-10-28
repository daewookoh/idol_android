package net.ib.mn.core.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class EndPopupModel(
    @SerialName("image_url") val imageUrl: String? = "",
    @SerialName("link_url") val linkUrl: String? = "",
)
