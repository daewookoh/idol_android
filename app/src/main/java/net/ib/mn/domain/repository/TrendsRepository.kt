package net.ib.mn.domain.repository

import kotlinx.coroutines.flow.Flow
import net.ib.mn.domain.model.ApiResult
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
    val totalCount: Int
)
