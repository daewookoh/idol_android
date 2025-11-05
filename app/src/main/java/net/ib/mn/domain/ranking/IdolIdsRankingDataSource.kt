package net.ib.mn.domain.ranking

import kotlinx.coroutines.flow.Flow
import net.ib.mn.domain.model.ApiResult
import net.ib.mn.domain.repository.RankingRepository
import javax.inject.Inject

/**
 * Group/Solo 랭킹 데이터 소스
 *
 * charts/idol_ids/ API 사용
 * 남녀 변경에 영향을 받음
 *
 * @param rankingType "Group" 또는 "Solo" (로깅용)
 */
class IdolIdsRankingDataSource @Inject constructor(
    private val rankingRepository: RankingRepository,
    private val rankingType: String
) : RankingDataSource {

    override suspend fun loadIdolIds(chartCode: String): Flow<ApiResult<List<Int>>> {
        // charts/idol_ids/ API 호출 → idol_ids 리스트 직접 반환
        return rankingRepository.getChartIdolIds(chartCode)
    }

    override fun supportGenderChange(): Boolean = true

    override val type: String = rankingType

    companion object {
        /**
         * Factory method for Group ranking
         */
        fun forGroup(rankingRepository: RankingRepository): IdolIdsRankingDataSource {
            return IdolIdsRankingDataSource(rankingRepository, "Group")
        }

        /**
         * Factory method for Solo ranking
         */
        fun forSolo(rankingRepository: RankingRepository): IdolIdsRankingDataSource {
            return IdolIdsRankingDataSource(rankingRepository, "Solo")
        }
    }
}
