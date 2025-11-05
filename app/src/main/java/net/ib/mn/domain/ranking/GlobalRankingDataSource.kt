package net.ib.mn.domain.ranking

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import net.ib.mn.domain.model.ApiResult
import net.ib.mn.domain.repository.RankingRepository
import javax.inject.Inject

/**
 * Global (기적) 랭킹 데이터 소스
 *
 * charts/ranks/ API 사용
 * 남녀 변경에 영향 받지 않음
 */
class GlobalRankingDataSource @Inject constructor(
    private val rankingRepository: RankingRepository
) : RankingDataSource {

    override suspend fun loadIdolIds(chartCode: String): Flow<ApiResult<List<Int>>> {
        // charts/ranks/ API 호출 → AggregateRankModel 리스트 → idol_ids 추출
        return rankingRepository.getChartRanks(chartCode).map { result ->
            when (result) {
                is ApiResult.Loading -> ApiResult.Loading
                is ApiResult.Success -> {
                    val idolIds = result.data.map { it.idolId }
                    ApiResult.Success(idolIds)
                }
                is ApiResult.Error -> ApiResult.Error(
                    exception = result.exception,
                    code = result.code,
                    message = result.message
                )
            }
        }
    }

    override fun supportGenderChange(): Boolean = false

    override val type: String = "Global"
}
