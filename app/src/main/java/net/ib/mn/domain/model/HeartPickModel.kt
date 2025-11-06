package net.ib.mn.domain.model

import com.google.gson.annotations.SerializedName

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
    @SerializedName("subtitle") val subtitle: String = "",
    @SerializedName("title") val title: String = "",
    @SerializedName("vote") val vote: Int = 0,
    @SerializedName("num_comments") var numComments: Int = 0,
    @SerializedName("heartpick_idols") val heartPickIdols: ArrayList<HeartPickIdol>? = null,
    @SerializedName("prize") val prize: PrizeModel? = null,
    @SerializedName("type") val type: String? = "I"
)

data class HeartPickIdol(
    @SerializedName("id") val id: Int = 0,
    @SerializedName("idol_id") val idolId: Int = 0,
    @SerializedName("group_id") val groupId: Int = 0,
    @SerializedName("image_url") val imageUrl: String = "",
    @SerializedName("subtitle") val subtitle: String = "",
    @SerializedName("title") val title: String = "",
    @SerializedName("vote") val vote: Int = 0
)

data class PrizeModel(
    @SerializedName("id") val id: Int = 0,
    @SerializedName("image_url") val imageUrl: String? = "",
    @SerializedName("location") val location: String? = "",
    @SerializedName("name") val name: String? = ""
)
