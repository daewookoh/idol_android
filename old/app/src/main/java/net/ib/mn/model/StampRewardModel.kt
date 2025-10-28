package net.ib.mn.model

import com.google.gson.annotations.SerializedName
import java.io.Serializable

data class StampRewardModel(
    @SerializedName("heart") val heart: Int = 0,
    @SerializedName("diamond") val diamond: Int = 0
) : Serializable
