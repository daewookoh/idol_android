package net.ib.mn.model

import com.google.gson.annotations.SerializedName
import java.io.Serializable
import java.util.*

data class ThemepickModel(
    @SerializedName("count") var count: Int,
    @SerializedName("begin_at") var beginAt: Date,
    @SerializedName("expired_at") var expiredAt: Date,
    @SerializedName("image_url")val imageUrl: String,
    @SerializedName("id")val id: Int,
    @SerializedName("status") var status: Int,
    @SerializedName("subtitle") var subtitle: String,
    @SerializedName("title") var title: String,
    @SerializedName("vote") var vote: String,
    @SerializedName("dummy") var dummy: String,
    @SerializedName("type") var type: String,
    @SerializedName("image_ratio") var image_ratio: String,
    @SerializedName("prize") var prize: PrizeModel? = null,
    @SerializedName("alarm") var alarm: Boolean = false,
    var voteId: Int = 0
) : Serializable {
    companion object {
        const val STATUS_PREPARING = 0
        const val STATUS_PROGRESS = 1
        const val STATUS_FINISHED = 2
    }
}