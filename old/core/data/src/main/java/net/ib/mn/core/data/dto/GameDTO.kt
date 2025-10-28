package net.ib.mn.core.data.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class GameFeeDTO(
    @SerialName("heart") val heart: Int,
    @SerialName("game_id") val gameId: String
)

@Serializable
data class GameRewardDTO(
    @SerialName("game_id") val gameId: String,
    @SerialName("score") val score: Int,
    @SerialName("log_heart_id") val logHeartId: Int
)
