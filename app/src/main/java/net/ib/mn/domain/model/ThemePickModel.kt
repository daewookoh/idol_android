package net.ib.mn.domain.model

import com.google.gson.annotations.SerializedName

/**
 * 테마픽 도메인 모델
 *
 * old 프로젝트: ThemepickModel, ThemepickRankModel 통합
 *
 * 상태(status):
 * - 0: 준비중 (예정)
 * - 1: 진행중
 * - 2: 종료
 *
 * 투표 상태(vote): 서버에서 문자열로 반환
 * - "N": 투표 가능 (첫 투표)
 * - "V": 광고 시청 후 추가 투표 가능
 * - "Y": 오늘 투표 완료 (더 이상 투표 불가)
 */
data class ThemePickModel(
    @SerializedName("id") val id: Int,
    @SerializedName("title") val title: String,
    @SerializedName("subtitle") val subtitle: String,
    @SerializedName("image_url") val imageUrl: String,
    @SerializedName("status") val status: Int,  // 0: 준비중, 1: 진행중, 2: 종료
    @SerializedName("vote") val vote: String,  // 투표 상태 코드 ("N", "V", "Y")
    @SerializedName("count") val count: Int,  // 전체 투표수
    @SerializedName("begin_at") val beginAt: String,
    @SerializedName("expired_at") val expiredAt: String,
    @SerializedName("image_ratio") val imageRatio: String,
    @SerializedName("alarm") val alarm: Boolean = false,
    @SerializedName("vote_id") val voteId: Int = 0,
    @SerializedName("type") val type: String = "I",  // I: 아이돌, 그 외: 기타
    @SerializedName("dummy") val dummy: String? = null,  // 캐싱 방지용
    @SerializedName("objects") val candidates: ArrayList<ThemePickIdol>? = null,  // 후보 목록
    @SerializedName("prize") val prize: PrizeModel? = null
) {
    companion object {
        const val STATUS_PREPARING = 0
        const val STATUS_PROGRESS = 1
        const val STATUS_FINISHED = 2

        // 투표 상태 코드 (old 프로젝트 OnePickVoteStatus와 동일)
        const val VOTE_ABLE = "N"         // 첫 투표 가능
        const val VOTE_SEE_VIDEOAD = "V"  // 광고 시청 후 추가 투표 가능
        const val VOTE_IMPOSSIBLE = "Y"   // 오늘 투표 완료
    }

    fun isPreparing(): Boolean = status == STATUS_PREPARING
    fun isProgress(): Boolean = status == STATUS_PROGRESS
    fun isFinished(): Boolean = status == STATUS_FINISHED

    fun canVote(): Boolean = vote == VOTE_ABLE || vote == VOTE_SEE_VIDEOAD
    fun needsVideoAd(): Boolean = vote == VOTE_SEE_VIDEOAD
    fun hasVotedToday(): Boolean = vote == VOTE_IMPOSSIBLE

    fun getFirstPlaceVote(): Long = candidates?.firstOrNull()?.vote ?: 0L
    fun getLastPlaceVote(): Long = candidates?.lastOrNull()?.vote ?: 0L
}

/**
 * 테마픽 후보 (아이돌/아이템)
 *
 * old 프로젝트: ThemepickRankModel
 */
data class ThemePickIdol(
    @SerializedName("id") val id: Int = 0,
    @SerializedName("idol_id") val idolId: Int? = null,
    @SerializedName("title") val title: String = "",
    @SerializedName("subtitle") val subtitle: String = "",
    @SerializedName("image_url") val imageUrl: String = "",
    @SerializedName("vote") val vote: Long = 0L,
    // 계산 필드 (서버에서 오지 않음)
    var rank: Int = 0,
    var isSelected: Boolean = false,  // 뷰페이저 가운데 위치 여부
    var isClicked: Boolean = false,   // 투표 선택 여부
    var lastPlaceVote: Long = 1L,
    var firstPlaceVote: Long = 1L,
    var minPercent: Float = 0f,
    var diffVote: Long = 0L  // 1위와의 득표차
)
