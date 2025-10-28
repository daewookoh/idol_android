package net.ib.mn.core.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class TagModel(
    @SerialName("id") val id: Int = 0,
    @SerialName("name") val name: String? = "",
    @SerialName("admin_only") val adminOnly: String? = "",
    var selected: Boolean = false
)
