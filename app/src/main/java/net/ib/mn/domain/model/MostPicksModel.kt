package net.ib.mn.domain.model

import com.google.gson.annotations.SerializedName

/**
 * MostPicksModel
 * 최애의 픽 참여 현황을 나타내는 모델
 */
data class MostPicksModel(
    @SerializedName("heartpick") val heartpick: List<Int>? = null,
    @SerializedName("onepick") val onepick: List<Int>? = null,
    @SerializedName("themepick") val themepick: List<Int>? = null,
    @SerializedName("miracle") val miracle: Boolean? = false
)
