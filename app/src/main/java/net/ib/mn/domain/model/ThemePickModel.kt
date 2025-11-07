package net.ib.mn.domain.model

import com.google.gson.annotations.SerializedName

/**
 * 테마픽 도메인 모델
 */
data class ThemePickModel(
    @SerializedName("id") val id: Int,
    @SerializedName("title") val title: String,
    @SerializedName("subtitle") val subtitle: String,
    @SerializedName("image_url") val imageUrl: String,
    @SerializedName("status") val status: Int,  // 0: 준비중, 1: 진행중, 2: 종료
    @SerializedName("vote") val vote: String,  // 투표 상태 코드 ("able", "see_videoad", etc.)
    @SerializedName("count") val count: Int,  // 실제 투표수
    @SerializedName("begin_at") val beginAt: String,
    @SerializedName("expired_at") val expiredAt: String,
    @SerializedName("image_ratio") val imageRatio: String,
    @SerializedName("alarm") val alarm: Boolean = false,
    val voteId: Int = 0
) {
    companion object {
        const val STATUS_PREPARING = 0
        const val STATUS_PROGRESS = 1
        const val STATUS_FINISHED = 2
    }
}
