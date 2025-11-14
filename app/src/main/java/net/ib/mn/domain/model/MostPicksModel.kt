package net.ib.mn.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * MostPicksModel
 * 최애의 픽 참여 현황을 나타내는 모델
 */
@Serializable
data class MostPicksModel(
    @SerialName("heartpick") val heartpick: List<Int>? = null,
    @SerialName("onepick") val onepick: List<Int>? = null,
    @SerialName("themepick") val themepick: List<Int>? = null,
    @SerialName("miracle") val miracle: Boolean? = false
)
