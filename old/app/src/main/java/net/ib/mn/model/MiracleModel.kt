package net.ib.mn.model

import com.google.gson.annotations.SerializedName
import java.io.Serializable

data class MiracleModel(
    @SerializedName("banner_url") var bannerUrl: String? = null,
    @SerializedName("first_day") val firstDay : String? = null,
    @SerializedName("last_day") val lastDay : String? = null
): Serializable