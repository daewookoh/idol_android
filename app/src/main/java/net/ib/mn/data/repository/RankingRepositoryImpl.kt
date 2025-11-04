package net.ib.mn.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import net.ib.mn.domain.model.RankingItem
import net.ib.mn.domain.model.RankingResponse
import net.ib.mn.domain.repository.RankingRepository
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Ranking Repository 구현체
 *
 * TODO: 실제 API 연동 시 ApiService를 주입받아 사용
 * 현재는 Mock 데이터로 구현
 */
@Singleton
class RankingRepositoryImpl @Inject constructor(
    // private val apiService: ApiService,
    // private val rankingDao: RankingDao
) : RankingRepository {

    override fun getRankingByType(
        type: String,
        page: Int,
        limit: Int
    ): Flow<Result<RankingResponse>> = flow {
        try {
            // TODO: 실제 API 호출
            // val response = apiService.getRanking(type, page, limit)

            // Mock 데이터 (개발용)
            val mockData = createMockRankingData(type)
            emit(Result.success(mockData))
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }

    override fun observeRanking(type: String): Flow<Result<RankingResponse>> = flow {
        try {
            // TODO: Room DB나 실시간 데이터 소스 관찰
            // rankingDao.observeRankingByType(type).collect { ... }

            val mockData = createMockRankingData(type)
            emit(Result.success(mockData))
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }

    override suspend fun refreshRanking(type: String): Result<Unit> {
        return try {
            // TODO: 서버에서 최신 데이터 가져와서 캐시 업데이트
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Mock 랭킹 데이터 생성 (개발/테스트용)
     */
    private fun createMockRankingData(type: String): RankingResponse {
        val items = (1..20).map { index ->
            RankingItem(
                id = index,
                rank = index,
                name = when (type) {
                    "SOLO" -> "아이돌 $index"
                    "GROUP" -> "그룹 $index"
                    "MALE_ACTOR" -> "남자배우 $index"
                    "FEMALE_ACTOR" -> "여자배우 $index"
                    "MALE_SINGER" -> "남자가수 $index"
                    "FEMALE_SINGER" -> "여자가수 $index"
                    else -> "인물 $index"
                },
                imageUrl = "https://via.placeholder.com/150",
                hearts = (1000000L - (index * 10000)),
                rankChange = when (index % 4) {
                    0 -> RankingItem.RankChange.UP
                    1 -> RankingItem.RankChange.DOWN
                    2 -> RankingItem.RankChange.SAME
                    else -> RankingItem.RankChange.NEW
                }
            )
        }

        return RankingResponse(
            type = type,
            items = items,
            totalCount = items.size,
            lastUpdated = "2025-01-04 12:00:00"
        )
    }
}
