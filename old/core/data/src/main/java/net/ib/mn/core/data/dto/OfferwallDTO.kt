package net.ib.mn.core.data.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class OfferWallRewardRequest(
    @SerialName("to")
    val to: String
)