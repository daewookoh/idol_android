package net.ib.mn.domain.repository

import kotlinx.coroutines.flow.Flow
import net.ib.mn.domain.model.ApiResult

/**
 * Ranking Repository 인터페이스
 *
 * Clean Architecture의 Repository 패턴을 따릅니다.
 * - Domain Layer에 위치하여 Data Layer의 구현 세부사항과 분리
 * - Flavor별 구현체를 통해 다른 동작을 제공할 수 있음
 */
interface RankingRepository {

    /**
     * 특정 차트 코드의 아이돌 ID 리스트 조회
     *
     * old 프로젝트와 동일한 방식
     * charts/idol_ids/ API를 사용하여 해당 차트에 속한 아이돌들의 ID 리스트 획득
     *
     * @param code 차트 코드 (예: "PR_S_M", "PR_G_F")
     * @return Flow<ApiResult<List<Int>>> 아이돌 ID 리스트
     */
    fun getChartIdolIds(code: String): Flow<ApiResult<List<Int>>>
}
