package net.ib.mn.domain.model

import com.google.gson.annotations.SerializedName

/**
 * 이미지픽 후보 아이돌 모델
 * old 프로젝트: OnepickIdolModel
 */
data class ImagePickIdolModel(
    @SerializedName("id") val id: Int = 0,
    @SerializedName("idol") val idol: IdolModel? = null,
    @SerializedName("image_url") val imageUrl: String? = null,
    @SerializedName("vote") val vote: Long = 0L,
    @SerializedName("count") val count: Long = 0L,  // 일부 API에서 vote 대신 count 사용
    var rank: Int = 0,
    var lastPlaceVoteCount: Long = 1,
    var firstPlaceVoteCount: Long = 1,
    var minPercent: Float = 1f
) {
    /**
     * 실제 투표수 반환 (vote 또는 count 중 0이 아닌 값)
     */
    val voteCount: Long
        get() = if (vote > 0) vote else count

    /**
     * 빈 후보 (그리드 채우기용)
     */
    val isEmpty: Boolean
        get() = id == 0 && idol == null
}
