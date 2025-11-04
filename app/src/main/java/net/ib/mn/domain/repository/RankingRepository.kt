package net.ib.mn.domain.repository

import kotlinx.coroutines.flow.Flow
import net.ib.mn.domain.model.RankingResponse

/**
 * Ranking Repository 인터페이스
 *
 * Clean Architecture의 Repository 패턴을 따릅니다.
 * - Domain Layer에 위치하여 Data Layer의 구현 세부사항과 분리
 * - Flavor별 구현체를 통해 다른 동작을 제공할 수 있음
 */
interface RankingRepository {

    /**
     * 특정 타입의 랭킹 데이터를 가져옵니다.
     *
     * @param type 랭킹 타입 (SOLO, GROUP, MALE_ACTOR 등)
     * @param page 페이지 번호 (기본값: 1)
     * @param limit 페이지당 아이템 수 (기본값: 100)
     * @return Flow<Result<RankingResponse>> 랭킹 응답
     */
    fun getRankingByType(
        type: String,
        page: Int = 1,
        limit: Int = 100
    ): Flow<Result<RankingResponse>>

    /**
     * 랭킹 데이터를 실시간으로 관찰합니다.
     *
     * @param type 랭킹 타입
     * @return Flow<Result<RankingResponse>> 실시간 랭킹 스트림
     */
    fun observeRanking(type: String): Flow<Result<RankingResponse>>

    /**
     * 랭킹 데이터를 새로고침합니다.
     *
     * @param type 랭킹 타입
     */
    suspend fun refreshRanking(type: String): Result<Unit>
}
