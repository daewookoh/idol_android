package net.ib.mn.core.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import net.ib.mn.core.data.dto.ClaimMissionRewardDTO

@Serializable
data class RecommendRewardResponse(
    var gcode: Int = 0,
    var success: Boolean = false,
    var msg: String? = null,
    @SerialName("mission_reward") val missionReward: RewardModel? = null,
    @SerialName("all_clear_reward") val allClearReward: RewardModel? = null,
)
