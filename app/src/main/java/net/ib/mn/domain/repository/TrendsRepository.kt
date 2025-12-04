package net.ib.mn.domain.repository

import kotlinx.coroutines.flow.Flow
import net.ib.mn.domain.model.ApiResult
import net.ib.mn.domain.model.DailyRankingHistoryModel
import net.ib.mn.domain.model.IdolRankingHistoryModel
import net.ib.mn.domain.model.TrendsModel

/**
 * Trends Repository 인터페이스 (이붙그램)
 */
interface TrendsRepository {

    /**
     * 이붙그램 목록 조회
     *
     * @param idolId 아이돌 ID
     * @param offset 페이지 오프셋
     * @param limit 한 번에 가져올 개수
     */
    fun getRecent(
        idolId: Int,
        offset: Int = 0,
        limit: Int = 30
    ): Flow<ApiResult<TrendsResponse>>

    /**
     * 랭킹 변동 히스토리 조회
     * Old: HallOfFameAggHistoryActivity의 trendsRepository.recent() 참고
     *
     * @param idolId 아이돌 ID
     */
    fun getIdolRankingHistory(idolId: Int): Flow<ApiResult<IdolRankingHistoryResponse>>

    /**
     * 일일 순위 히스토리 조회 (특정 날짜의 전체 랭킹)
     * Old: HallOfFameTopHistoryActivity의 trendsRepository.dailyHistory() 참고
     *
     * @param historyParam 날짜 파라미터 (예: "2024-01-01")
     * @param type 타입 (예: "S", "G") - CELEB용
     * @param category 카테고리 (예: "M", "F") - CELEB용
     * @param chartCode 차트 코드 - 애돌용
     */
    fun getDailyRankingHistory(
        historyParam: String,
        type: String = "",
        category: String = "",
        chartCode: String = ""
    ): Flow<ApiResult<DailyRankingHistoryResponse>>
}

/**
 * 이붙그램 응답 데이터
 */
data class TrendsResponse(
    val items: List<TrendsModel>,
    val totalCount: Int,
    val hasMore: Boolean
)

/**
 * 랭킹 히스토리 응답 데이터
 */
data class IdolRankingHistoryResponse(
    val items: List<IdolRankingHistoryModel>,
    val totalCount: Int,
    val chartCode: String = ""  // 애돌용 차트 코드
)

/**
 * 일일 순위 히스토리 응답 데이터
 */
data class DailyRankingHistoryResponse(
    val items: List<DailyRankingHistoryModel>,
    val totalCount: Int
)
