package net.ib.mn.domain.repository

import kotlinx.coroutines.flow.Flow
import net.ib.mn.domain.model.ApiResult
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
}

/**
 * 이붙그램 응답 데이터
 */
data class TrendsResponse(
    val items: List<TrendsModel>,
    val totalCount: Int,
    val hasMore: Boolean
)
