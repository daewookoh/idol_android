package net.ib.mn.data.remote.dto

import com.google.gson.annotations.SerializedName

/**
 * In-app 배너 데이터
 */
data class InAppBannerDto(
    @SerializedName("id")
    val id: Int,

    @SerializedName("image_url")
    val imageUrl: String,

    @SerializedName("link")
    val link: String?,

    @SerializedName("section")
    val section: String = "M"
)
