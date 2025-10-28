package net.ib.mn.model

import com.google.gson.annotations.SerializedName
import java.io.Serializable

data class HeartPickVoteRewardModel(
    @SerializedName("bonus_heart") val bonusHeart: Int = 0,
    @SerializedName("voted") val voted: Long = 0
) : Serializable
