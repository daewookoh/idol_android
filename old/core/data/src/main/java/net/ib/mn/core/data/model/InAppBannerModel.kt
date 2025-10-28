package net.ib.mn.core.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class InAppBannerModel(
    val id: Int,
    @SerialName("image_url") val imageUrl: String,
    val link: String?,
    val section: String = "M"
)
