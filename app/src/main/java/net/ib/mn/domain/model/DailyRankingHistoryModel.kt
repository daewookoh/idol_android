package net.ib.mn.domain.model

import com.google.gson.annotations.SerializedName
import net.ib.mn.util.IdolImageUtil.toSecureUrl

/**
 * 일일 순위 히스토리 모델
 * Old 프로젝트의 HallTopModel 참고
 */
data class DailyRankingHistoryModel(
    @SerializedName("idol")
    val idol: DailyRankingIdol? = null,
    val heart: Long = 0,
    val rank: Int = 0,
    val difference: Int = 0,
    val status: String = "",  // "new", "increase", "decrease", "same"
    @SerializedName("image_url")
    val imageUrl: String? = null
) {
    // 아이돌 이름 (idol 객체에서 가져옴)
    val idolName: String
        get() = idol?.name ?: ""

    // 아이돌 ID
    val idolId: Int
        get() = idol?.id ?: 0

    // 보안 URL 처리된 이미지
    val secureImageUrl: String
        get() = imageUrl.toSecureUrl()
}

/**
 * 일일 순위 히스토리의 아이돌 정보
 */
data class DailyRankingIdol(
    val id: Int = 0,
    val name: String = "",
    @SerializedName("image_url")
    val imageUrl: String? = null
)
