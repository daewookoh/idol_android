package net.ib.mn.model

import com.google.gson.annotations.SerializedName

data class AccessModel(
    @SerializedName("permissions") val permissions: List<PermissionsModel>,
    @SerializedName("title") val title: String,
)

data class PermissionsModel(
    @SerializedName("name") val name: String = "",
    @SerializedName("required") val required: Boolean = false,
    @SerializedName("value") val value: String = "",
)