package net.ib.mn.core.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SnsModel(
    @SerialName("image_url") val imageUrl: String? = null,
    @SerialName("image_url_darkmode") val imageUrlDarkmode: String? = null,
    @SerialName("link_url") val linkUrl: String? = null
)
