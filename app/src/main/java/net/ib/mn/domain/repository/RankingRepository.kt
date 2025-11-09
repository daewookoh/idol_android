package net.ib.mn.domain.repository

import kotlinx.coroutines.flow.Flow
import net.ib.mn.data.remote.dto.AggregateRankModel
import net.ib.mn.data.remote.dto.VoteResponse
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
     * 사용 탭: Solo (개인), Group (그룹)
     *
     * @param code 차트 코드 (예: "PR_S_M", "PR_G_F")
     * @return Flow<ApiResult<List<Int>>> 아이돌 ID 리스트
     */
    fun getChartIdolIds(code: String): Flow<ApiResult<List<Int>>>

    /**
     * 특정 차트 코드의 누적 랭킹 조회
     *
     * old 프로젝트와 동일한 방식
     * charts/ranks/ API를 사용하여 해당 차트의 누적 집계 결과 획득
     *
     * 사용 탭: Miracle (기적), Rookie (루키), HeartPick (하트픽), OnePick (원픽), HallOfFame (명예전당-30일 누적순위)
     *
     * @param code 차트 코드 (예: "PR_G_M", "HEARTPICK", "ONEPICK", "HOF_M")
     * @return Flow<ApiResult<List<AggregateRankModel>>> 누적 랭킹 리스트
     */
    fun getChartRanks(code: String): Flow<ApiResult<List<AggregateRankModel>>>

    /**
     * 아이돌에게 하트 투표
     *
     * old 프로젝트의 GiveHeartToIdolUseCase와 동일
     * idols/{idol_id}/vote/ API를 사용하여 하트 투표
     *
     * @param idolId 아이돌 ID
     * @param heart 투표할 하트 개수
     * @return Flow<ApiResult<VoteResponse>> 투표 결과
     */
    fun voteIdol(idolId: Int, heart: Long): Flow<ApiResult<VoteResponse>>

    /**
     * 명예전당 일일 데이터 조회
     *
     * old 프로젝트의 hofs/ API와 동일
     * 명예전당 일일 순위 데이터 조회
     *
     * @param code 차트 코드 (예: "HOF_M", "HOF_F")
     * @param historyParam 이전 기간 조회용 파라미터 (optional)
     * @return Flow<ApiResult<String>> JSON 형식의 응답
     */
    fun getHofs(code: String, historyParam: String? = null): Flow<ApiResult<String>>
}
