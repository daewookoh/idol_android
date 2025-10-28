package net.ib.mn.core.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class NewPicksModel(
    @SerialName("heartpick") val heartpick: Boolean,
    @SerialName("onepick") val onepick: Boolean,
    @SerialName("themepick") val themepick: Boolean
)
