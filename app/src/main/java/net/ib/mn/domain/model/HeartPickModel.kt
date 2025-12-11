package net.ib.mn.domain.model

import com.google.gson.annotations.SerializedName

/**
 * 하트픽 모델
 *
 * old 프로젝트: app/src/main/java/net/ib/mn/model/HeartPickModel.kt
 */
data class HeartPickModel(
    @SerializedName("id") var id: Int = 0,
    @SerializedName("title") val title: String = "",
    @SerializedName("subtitle") val subtitle: String = "",
    @SerializedName("banner_url") val bannerUrl: String? = null,
    @SerializedName("begin_at") val beginAt: String = "",
    @SerializedName("end_at") val endAt: String = "",
    @SerializedName("begin_at_utc") val beginAtUtc: String = "",
    @SerializedName("end_at_utc") val endAtUtc: String = "",
    @SerializedName("status") val status: Int = 0,
    @SerializedName("vote") val vote: Int = 0,
    @SerializedName("num_comments") var numComments: Int = 0,
    @SerializedName("is_viewable") val isViewable: String = "Y",
    @SerializedName("prize_id") val prizeId: Int = 0,
    @SerializedName("type") val type: String? = "I",
    @SerializedName("heartpick_idols") val heartPickIdols: ArrayList<HeartPickIdol>? = null,
    @SerializedName("prize") val prize: PrizeModel? = null
) {
    companion object {
        const val STATUS_PRELAUNCH = 0      // 투표 예정
        const val STATUS_VOTING = 1         // 투표 중
        const val STATUS_VOTE_FINISHED = 2  // 투표 종료
    }

    fun isVoteFinished(): Boolean = status == STATUS_VOTE_FINISHED
    fun isPrelaunch(): Boolean = status == STATUS_PRELAUNCH
    fun isVoting(): Boolean = status == STATUS_VOTING

    fun getFirstPlaceVote(): Int = heartPickIdols?.firstOrNull()?.vote ?: 0
    fun getLastPlaceVote(): Int = heartPickIdols?.lastOrNull()?.vote ?: 0

    fun getFirstPlacePercent(): Int {
        if (vote == 0) return 0
        val firstVote = getFirstPlaceVote().toFloat()
        return (100.0f * firstVote / vote.toFloat()).toInt()
    }
}

/**
 * 하트픽 아이돌
 */
data class HeartPickIdol(
    @SerializedName("id") val id: Int = 0,
    @SerializedName("idol_id") val idolId: Int = 0,
    @SerializedName("group_id") val groupId: Int = 0,
    @SerializedName("image_url") val imageUrl: String = "",
    @SerializedName("title") val title: String = "",
    @SerializedName("subtitle") val subtitle: String = "",
    @SerializedName("vote") val vote: Int = 0,
    // 계산 필드 (서버에서 오지 않음)
    var rank: Int = 0,
    var diffVote: Int = 0
)

/**
 * 하트픽 상품
 */
data class PrizeModel(
    @SerializedName("id") val id: Int = 0,
    @SerializedName("name") val name: String? = "",
    @SerializedName("image_url") val imageUrl: String? = "",
    @SerializedName("location") val location: String? = ""
)
