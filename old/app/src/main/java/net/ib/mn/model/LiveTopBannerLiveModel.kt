package net.ib.mn.model

import com.google.gson.annotations.SerializedName
import java.io.Serializable
import java.util.*

data class LiveTopBannerLiveModel(
    @SerializedName("id") var id: Int?,
    @SerializedName("level_limit") var levelLimit: Int =0,
    @SerializedName("start_at") var startAt: Date?,
    @SerializedName("status") var status: Int?
): Serializable
