package net.ib.mn.data.model

import kotlinx.serialization.Serializable
import net.ib.mn.data.DataMapper
import net.ib.mn.domain.model.InAppBanner

@Serializable
data class InAppBannerPrefsEntity(
    val id: Int,
    val imageUrl: String,
    val link: String?,
    val section: String
) : DataMapper<InAppBanner> {

    override fun toDomain(): InAppBanner =
        InAppBanner(
            id,
            imageUrl,
            link,
            section
        )
}

fun InAppBanner.toEntity(): InAppBannerPrefsEntity =
    InAppBannerPrefsEntity(
        id,
        imageUrl,
        link,
        section
    )