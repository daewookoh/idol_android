package net.ib.mn.core.data.model

import kotlinx.serialization.Serializable

@Serializable
data class RewardModel(
    val heart: Int = 0,
    val diamond: Int = 0,
)
