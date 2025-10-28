package net.ib.mn.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import net.ib.mn.domain.model.InAppBanner

@Serializable
data class InAppBannerModel(
    val id: Int,
    @SerialName("image_url") val imageUrl: String,
    val link: String?,
    val section: String = "M"
)

fun InAppBannerModel.toDomain(): InAppBanner =
    InAppBanner(
        id,
        imageUrl,
        link,
        section
    )

fun InAppBanner.toPresentation(): InAppBannerModel =
    InAppBannerModel(
        id,
        imageUrl,
        link,
        section
    )