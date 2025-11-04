package net.ib.mn.domain.model

/**
 * 랭킹 아이템 도메인 모델
 *
 * UI에서 사용하는 순수한 도메인 모델입니다.
 * API Response DTO와 분리하여 비즈니스 로직에 집중합니다.
 */
data class RankingItem(
    val id: Int,
    val rank: Int,
    val name: String,
    val imageUrl: String?,
    val hearts: Long,
    val rankChange: RankChange,
    val isTop3: Boolean = rank <= 3,

    // 추가 정보 (선택적)
    val group: String? = null,
    val debutDate: String? = null,
    val category: String? = null
) {
    /**
     * 순위 변동 정보
     */
    enum class RankChange {
        UP,      // 순위 상승
        DOWN,    // 순위 하락
        SAME,    // 변동 없음
        NEW      // 신규 진입
    }
}

/**
 * 랭킹 리스트 응답
 */
data class RankingResponse(
    val type: String,
    val items: List<RankingItem>,
    val totalCount: Int,
    val lastUpdated: String
)
