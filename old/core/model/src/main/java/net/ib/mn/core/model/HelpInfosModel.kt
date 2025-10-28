package net.ib.mn.core.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable


@Serializable
data class HelpInfosModel(
    @SerialName("heartpick") val heartPick: String? = null,
    @SerialName("onepick") val onePick: String? = null,
    @SerialName("themepick") val themePick: String? = null,
    @SerialName("free_board_placeholder") val freeBoardPlaceHolder: String? = null
)
