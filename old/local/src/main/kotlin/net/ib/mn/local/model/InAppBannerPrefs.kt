package net.ib.mn.local.model

import net.ib.mn.data.model.InAppBannerPrefsEntity
import net.ib.mn.local.LocalMapper

data class InAppBannerPrefs(
    val id: Int,
    val imageUrl: String,
    val link: String?,
    val section: String
): LocalMapper<InAppBannerPrefsEntity> {

    override fun toData(): InAppBannerPrefsEntity =
        InAppBannerPrefsEntity(
            id,
            imageUrl,
            link,
            section
        )
}

fun InAppBannerPrefsEntity.toLocal(): InAppBannerPrefs =
    InAppBannerPrefs(
        id,
        imageUrl,
        link,
        section
    )