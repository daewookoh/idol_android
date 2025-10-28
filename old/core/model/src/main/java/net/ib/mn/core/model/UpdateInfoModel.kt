package net.ib.mn.core.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class UpdateInfoModel(
    @SerialName("all_idol_update") val allIdolUpdate: String,
    @SerialName("daily_idol_update") val dailyIdolUpdate: String,
    @SerialName("gcode") val gcode: Int,
    @SerialName("sns_channel_update") val snsChannelUpdate: String? = null, // 셀럽은 없음
    @SerialName("success") val success: Boolean
)
