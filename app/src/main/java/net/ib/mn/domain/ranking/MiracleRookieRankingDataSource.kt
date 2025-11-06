package net.ib.mn.domain.ranking

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import net.ib.mn.domain.model.ApiResult
import net.ib.mn.domain.repository.RankingRepository
import javax.inject.Inject

/**
 * Miracle/Rookie 랭킹 데이터 소스
 *
 * charts/ranks/ API 사용 (Global과 동일한 방식)
 * 남녀 변경에 영향 받지 않음
 *
 * @param rankingType "Miracle" 또는 "Rookie" (로깅용)
 */
class MiracleRookieRankingDataSource @Inject constructor(
    private val rankingRepository: RankingRepository,
    private val rankingType: String
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

    override val type: String = rankingType

    companion object {
        /**
         * Factory method for Miracle ranking
         */
        fun forMiracle(rankingRepository: RankingRepository): MiracleRookieRankingDataSource {
            return MiracleRookieRankingDataSource(rankingRepository, "Miracle")
        }

        /**
         * Factory method for Rookie ranking
         */
        fun forRookie(rankingRepository: RankingRepository): MiracleRookieRankingDataSource {
            return MiracleRookieRankingDataSource(rankingRepository, "Rookie")
        }
    }
}
