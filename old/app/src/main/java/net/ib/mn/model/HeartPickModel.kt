package net.ib.mn.model

import com.google.gson.annotations.SerializedName
import java.io.Serializable

data class HeartPickModel(
    @SerializedName("banner_url") val bannerUrl: String? = null,
    @SerializedName("begin_at") val beginAt: String = "",
    @SerializedName("end_at") val endAt: String = "",
    @SerializedName("begin_at_utc") val beginAtUtc: String = "",
    @SerializedName("end_at_utc") val endAtUtc: String = "",
    @SerializedName("id") var id: Int = 0,
    @SerializedName("is_viewable") val isViewable: String = "Y",
    @SerializedName("prize_id") val prizeId: Int = 0,
    @SerializedName("status") val status: Int = 0,
    @SerializedName("subtitle") val subtitle: String = "Y",
    @SerializedName("title") val title: String = "Y",
    @SerializedName("vote") val vote: Int = 1,
    @SerializedName("num_comments") var numComments: Int = 0,
    @SerializedName("heartpick_idols") val heartPickIdols: ArrayList<HeartPickIdol>? = null,
    @SerializedName("prize") val prize: PrizeModel? = null,
    @SerializedName("type") val type: String? = "I",
    var heartPick1stPercent: Int = 0,
    var isShare: Boolean = false,
    var hasGoneToolTip: Boolean = false,
    var firstPlaceVote : Int = 1,
    var lastPlaceVote : Int = 0,
    var minPercent : Float = 0f,
    var isLoading: Boolean = false,
) : Serializable {

    fun setFirstPlaceVote() {
        firstPlaceVote = heartPickIdols?.find { it.rank == 1 }?.vote ?: 1
    }

    fun setLastPlaceVote() {
        lastPlaceVote = heartPickIdols?.last()?.vote ?: 1
    }
}

data class HeartPickIdol(
    @SerializedName("id") val id: Int = 0,
    @SerializedName("idol_id") val idol_id: Int = 0,
    @SerializedName("group_id") val groupId: Int = 0,
    @SerializedName("image_url") val image_url: String = "",
    @SerializedName("subtitle") val subtitle: String = "",
    @SerializedName("title") val title: String = "",
    @SerializedName("vote") val vote: Int = 0,
    var diffVote: Int = 0,
    var rank: Int = 0
): Serializable

data class PrizeModel(
    @SerializedName("id") val id: Int = 0,
    @SerializedName("image_url") val image_url: String? = "",
    @SerializedName("location") val location: String? = "",
    @SerializedName("name") val name: String? = ""
) : Serializable