package net.ib.mn.model

import com.google.gson.annotations.SerializedName
import java.io.Serializable
import java.util.*

data class StampModel(
    @SerializedName("created_at") val created_at: Date?,
    @SerializedName("days") val days: Int?,
    @SerializedName("deviceid") val deviceid: String?,
    @SerializedName("id") val id: Int?,
    @SerializedName("is_viewable") val is_viewable: String?,
    @SerializedName("resource_uri") val resource_uri: String?,
    @SerializedName("stamped_at") val stamped_at: String?,
    @SerializedName("updated_at") val updated_at: Date?
) : Serializable