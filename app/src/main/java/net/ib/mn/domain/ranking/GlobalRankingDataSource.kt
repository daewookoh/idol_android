package net.ib.mn.domain.ranking

import kotlinx.coroutines.flow.Flow
import net.ib.mn.domain.model.ApiResult
import net.ib.mn.domain.repository.RankingRepository
import javax.inject.Inject

/**
 * Global (기적) 랭킹 데이터 소스
 *
 * charts/idol_ids/ API 사용
 * 남녀 변경에 영향 받지 않음
 */
class GlobalRankingDataSource @Inject constructor(
    private val rankingRepository: RankingRepository
) : RankingDataSource {

    override suspend fun loadIdolIds(chartCode: String): Flow<ApiResult<List<Int>>> {
        // charts/idol_ids/ API 호출 → idol_ids 리스트 직접 반환
        return rankingRepository.getChartIdolIds(chartCode)
    }

    override fun supportGenderChange(): Boolean = false

    override val type: String = "Global"
}
