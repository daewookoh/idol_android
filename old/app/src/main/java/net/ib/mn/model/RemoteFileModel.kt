package net.ib.mn.model

import com.google.gson.annotations.SerializedName
import java.io.Serializable

data class RemoteFileModel(
    @SerializedName("origin_url") val originUrl: String? = null,
    @SerializedName("thumbnail_url") val thumbnailUrl: String? = null,
    @SerializedName("umjjal_url") val umjjalUrl: String? = null,
    @SerializedName("seq") val seq: Int = 1
) : Serializable