package net.ib.mn.model

import com.google.gson.annotations.SerializedName
import java.io.Serializable


data class OfficialLink (
        @SerializedName("image_url") val imageUrl: String?,
        @SerializedName("image_url_darkmode") val imageUrlDarkmode: String?,
        @SerializedName("link_url") val linkUrl: String?
) : Serializable