package net.ib.mn.core.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * 투표 후 응답
 */
@Serializable
data class GiveHeartModel (
    @SerialName("bonus_heart") var bonusHeart: Int? = null,
    // 아래 두 값은 deprecated 됐으나 코드에 존재하여 일단 남겨둔다
    @SerialName("event_heart") var eventHeart: Boolean = false,
    @SerialName("event_heart_count") var eventHeartCount: Int = 0,
    var msg: String? = null,
    var success : Boolean = false,
)
