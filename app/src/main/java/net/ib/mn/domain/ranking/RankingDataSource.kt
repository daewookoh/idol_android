package net.ib.mn.domain.ranking

import kotlinx.coroutines.flow.Flow
import net.ib.mn.domain.model.ApiResult

/**
 * 랭킹 데이터 소스 인터페이스 (Strategy Pattern)
 *
 * 다양한 랭킹 타입(Global, Group, Solo)에 대해 통일된 인터페이스를 제공합니다.
 * 각 타입별로 API 호출 방식은 다르지만, 최종적으로 idol_ids (List<Int>)만 반환하면
 * 동일한 로직으로 UI를 렌더링할 수 있습니다.
 */
interface RankingDataSource {
    /**
     * 차트 코드로 아이돌 ID 리스트 로드
     *
     * @param chartCode 차트 코드 (예: "SOLO_M", "GROUP_F", "GLOBAL")
     * @return 아이돌 ID 리스트 Flow
     */
    suspend fun loadIdolIds(chartCode: String): Flow<ApiResult<List<Int>>>

    /**
     * 남녀 변경 지원 여부
     *
     * @return true: 남녀 변경 시 새로운 차트 코드로 데이터 재로드 필요 (Group, Solo)
     *         false: 남녀 변경 영향 없음 (Global)
     */
    fun supportGenderChange(): Boolean

    /**
     * 데이터 소스 타입 (로깅용)
     */
    val type: String
}
