package net.ib.mn.model

import com.google.gson.annotations.SerializedName

data class VideoAdOrderModel(
    @SerializedName("priority") val order: Int?,
    @SerializedName("type") val type: String?,
    var available: Boolean = true
)
