package net.ib.mn.domain.ranking

import kotlinx.coroutines.flow.Flow
import net.ib.mn.domain.model.ApiResult
import net.ib.mn.domain.repository.RankingRepository
import net.ib.mn.util.logD

/**
 * My Favorite 랭킹 데이터 소스
 *
 * charts/idol_ids/ API를 호출하여 FULL 리스트를 반환
 * (필터링은 ViewModel에서 RankingItem 생성 후 처리)
 *
 * 중요: 순위는 전체 목록에서 계산되며, 노출만 favoriteIds로 필터링
 *
 * @param rankingRepository RankingRepository 인스턴스
 * @param favoriteIds 즐겨찾기 아이돌 ID Set (ViewModel에서 필터링용)
 * @param chartCode 차트 코드 (로깅용)
 */
class MyFavoriteRankingDataSource(
    private val rankingRepository: RankingRepository,
    val favoriteIds: Set<Int>, // public으로 변경하여 ViewModel에서 접근 가능
    private val chartCode: String
) : RankingDataSource {

    override suspend fun loadIdolIds(chartCode: String): Flow<ApiResult<List<Int>>> {
        // 전체 idol IDs를 반환 (필터링하지 않음)
        // 순위는 전체 목록 기준으로 계산되어야 하므로
        logD(
            "MyFavoriteDataSource",
            "📊 Chart $chartCode: Loading FULL idol list for ranking calculation"
        )
        return rankingRepository.getChartIdolIds(chartCode)
    }

    override fun supportGenderChange(): Boolean = false

    override val type: String = "MyFavorite[$chartCode]"
}
