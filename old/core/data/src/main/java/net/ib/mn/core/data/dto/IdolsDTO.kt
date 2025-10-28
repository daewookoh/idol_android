package net.ib.mn.core.data.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class GiveHeartToIdolDTO(
    @SerialName("idol_id") var idolId: String,
    @SerialName("number") var hearts: Long
)
