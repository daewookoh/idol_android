package net.ib.mn.core.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class FamilyAppModel(
    @SerialName("app_id") val appId: String?= null,
    @SerialName("need_update") val needUpdate: String?= null,
    @SerialName("update_url") val updateUrl: String? = null,
    @SerialName("version") val version: String? = null,
)
