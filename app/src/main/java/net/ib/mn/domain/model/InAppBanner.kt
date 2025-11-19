package net.ib.mn.domain.model

data class InAppBanner(
    val id: Int,
    val imageUrl: String,
    val link: String?,
    val section: String = "M"
)
