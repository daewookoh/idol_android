package net.ib.mn.data.repository

import android.content.Context
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import net.ib.mn.data.local.ChartRankingItem
import net.ib.mn.data.local.PreferencesManager
import net.ib.mn.data.local.dao.IdolDao
import net.ib.mn.domain.model.ApiResult
import net.ib.mn.ui.components.RankingItemData
import net.ib.mn.util.IdolImageUtil
import net.ib.mn.util.ProcessedRankData
import net.ib.mn.util.RankingUtil
import java.text.NumberFormat
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 차트 랭킹 데이터를 SharedPreference에 저장하고 관리하는 Repository (SharedPreference 기반)
 *
 * Room DB 대신 SharedPreference (DataStore)를 사용:
 * - 각 차트별 랭킹 데이터를 JSON으로 저장
 * - Flow를 통한 실시간 리스닝
 * - 가볍고 빠른 데이터 접근
 *
 * 5개 차트 관리:
 * - SOLO_M (PR_S_M): Male Solo
 * - SOLO_F (PR_S_F): Female Solo
 * - GROUP_M (PR_G_M): Male Group
 * - GROUP_F (PR_G_F): Female Group
 * - GLOBAL (GLOBALS): Global
 */
@Singleton
class ChartRankingRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val rankingRepository: net.ib.mn.domain.repository.RankingRepository,
    private val idolDao: IdolDao,
    private val preferencesManager: PreferencesManager,
    private val userCacheRepository: dagger.Lazy<UserCacheRepository>
) {

    companion object {
        private const val TAG = "ChartRankingRepo"

        // 기본 5개 차트 코드
        val DEFAULT_CHART_CODES = listOf("PR_S_F", "PR_S_M", "PR_G_F", "PR_G_M", "GLOBALS")

        // 차트 업데이트 디바운싱 시간 (5초)
        private const val CHART_UPDATE_DEBOUNCE_MS = 5000L
    }

    // 마지막 차트 업데이트 시간
    @Volatile
    private var lastChartUpdateTime = 0L

    // Coroutine Scope for background operations
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // ==================== Public API (UI Layer) ====================

    /**
     * 차트 데이터 Flow로 구독 (반응형)
     *
     * SharedPreference의 Flow를 구독하여 실시간으로 변경사항을 감지합니다.
     *
     * @param chartCode 차트 코드
     * @return Flow<ProcessedRankData?>
     */
    fun observeChartData(chartCode: String): Flow<ProcessedRankData?> {
        return preferencesManager.observeChartRanking(chartCode).map { items ->
            if (items.isEmpty()) {
                Log.d(TAG, "⚠️ No data in SharedPreference for chart: $chartCode")
                null
            } else {
                convertToProcessedRankData(chartCode, items)
            }
        }
    }

    /**
     * 차트 데이터 가져오기 (일회성)
     *
     * @param chartCode 차트 코드
     * @return ProcessedRankData?
     */
    suspend fun getChartData(chartCode: String): ProcessedRankData? {
        val items = preferencesManager.getChartRanking(chartCode)
        return if (items.isEmpty()) {
            Log.d(TAG, "⚠️ No data in SharedPreference for chart: $chartCode")
            null
        } else {
            convertToProcessedRankData(chartCode, items)
        }
    }

    /**
     * 차트 새로고침 (idol DB의 최신 데이터로 재생성)
     *
     * @param chartCode 차트 코드
     */
    suspend fun refreshChart(chartCode: String) {
        try {
            val startTime = System.currentTimeMillis()
            Log.d(TAG, "🔄 [$chartCode] Refreshing chart from idol DB...")

            // 1. SharedPreference에서 아이돌 ID 목록 가져오기
            val existingRankings = preferencesManager.getChartRanking(chartCode)
            if (existingRankings.isEmpty()) {
                Log.w(TAG, "⚠️ No existing rankings for $chartCode, skipping refresh")
                return
            }

            val idolIds = existingRankings.map { it.idolId }
            Log.d(TAG, "🔄 [$chartCode] Refreshing with ${idolIds.size} idols from idol DB")

            // 2. idol DB의 최신 데이터로 랭킹 재생성
            buildAndSaveChartRankings(chartCode, idolIds)

            val elapsed = System.currentTimeMillis() - startTime
            Log.d(TAG, "✅ [$chartCode] Refreshed in ${elapsed}ms (${idolIds.size} idols)")

        } catch (e: Exception) {
            Log.e(TAG, "❌ [$chartCode] Failed to refresh: ${e.message}", e)
        }
    }

    /**
     * 투표 후 차트 재정렬 (즉시 반영)
     *
     * 동작 순서:
     * 1. idol DB 하트 수 업데이트 (Single Source of Truth)
     * 2. 업데이트된 DB 데이터를 기반으로 차트 재정렬
     *
     * @param idolId 투표한 아이돌 ID
     * @param newHeartCount 새로운 하트 수
     * @param chartCode 차트 코드 (nullable)
     */
    suspend fun updateVoteAndRerank(idolId: Int, newHeartCount: Long, chartCode: String?) {
        try {
            Log.d(TAG, "💝 Updating vote: idol=$idolId, hearts=$newHeartCount, chart=$chartCode")

            // 1. 먼저 idol DB 업데이트 (Single Source of Truth)
            idolDao.updateIdolHeart(idolId, newHeartCount)
            Log.d(TAG, "✅ Updated idol DB: idol=$idolId, hearts=$newHeartCount")

            // 2. 업데이트된 DB 데이터를 기반으로 차트 재정렬
            if (chartCode != null) {
                // 특정 차트만 리프레시
                refreshChart(chartCode)
                Log.d(TAG, "✅ Refreshed chart: $chartCode")
            } else {
                // 모든 차트 리프레시
                coroutineScope {
                    DEFAULT_CHART_CODES.map { code ->
                        async {
                            try {
                                refreshChart(code)
                            } catch (e: Exception) {
                                Log.e(TAG, "❌ Failed to refresh chart $code: ${e.message}", e)
                            }
                        }
                    }.awaitAll()
                }
                Log.d(TAG, "✅ Refreshed all charts")
            }

        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to update vote and rerank: ${e.message}", e)
        }
    }

    /**
     * UDP 업데이트 시 호출 (변경된 아이돌들만 업데이트)
     *
     * 5초 디바운싱 적용:
     * - 마지막 업데이트가 5초 이내이면 스킵
     * - 5초가 지났으면 모든 차트를 새로고침하여 정렬까지 반영
     *
     * @param changedIdolIds 변경된 아이돌 ID 리스트
     */
    suspend fun updateIdolsFromUdp(changedIdolIds: Set<Int>) {
        try {
            val currentTime = System.currentTimeMillis()
            val timeSinceLastUpdate = currentTime - lastChartUpdateTime

            // 5초 디바운싱 체크
            if (timeSinceLastUpdate < CHART_UPDATE_DEBOUNCE_MS) {
                Log.d(TAG, "⏭️ UDP update skipped (last update was ${timeSinceLastUpdate}ms ago, need ${CHART_UPDATE_DEBOUNCE_MS}ms)")
                return
            }

            Log.d(TAG, "📡 UDP update for ${changedIdolIds.size} idols (${timeSinceLastUpdate}ms since last update)")

            // 마지막 업데이트 시간 갱신
            lastChartUpdateTime = currentTime

            // 모든 차트를 새로고침하여 정렬까지 반영
            coroutineScope {
                DEFAULT_CHART_CODES.map { chartCode ->
                    async {
                        try {
                            refreshChart(chartCode)
                        } catch (e: Exception) {
                            Log.e(TAG, "❌ Failed to refresh chart $chartCode: ${e.message}", e)
                        }
                    }
                }.awaitAll()
            }

            Log.d(TAG, "✅ All 5 charts refreshed with new rankings")

        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to update from UDP: ${e.message}", e)
        }
    }

    /**
     * 모든 차트 데이터 삭제
     */
    suspend fun clearAll() {
        try {
            Log.d(TAG, "🗑️ Clearing all chart rankings...")

            DEFAULT_CHART_CODES.forEach { chartCode ->
                preferencesManager.saveChartRanking(chartCode, emptyList())
            }

            Log.d(TAG, "✅ All chart rankings cleared")

        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to clear rankings: ${e.message}", e)
        }
    }

    /**
     * 5개 차트 초기화 (Startup 시 호출)
     */
    suspend fun initializeChartsInDatabase() {
        try {
            Log.d(TAG, "========================================")
            Log.d(TAG, "🚀 Initializing 5 charts in SharedPreference...")
            Log.d(TAG, "========================================")

            val startTime = System.currentTimeMillis()

            coroutineScope {
                DEFAULT_CHART_CODES.map { chartCode ->
                    async {
                        try {
                            Log.d(TAG, "📊 [$chartCode] Fetching idol IDs from API...")
                            fetchAndSaveChart(chartCode)
                        } catch (e: Exception) {
                            Log.e(TAG, "❌ [$chartCode] Failed: ${e.message}", e)
                        }
                    }
                }.awaitAll()
            }

            val elapsed = System.currentTimeMillis() - startTime
            Log.d(TAG, "========================================")
            Log.d(TAG, "✅ All 5 charts initialized in ${elapsed}ms")
            Log.d(TAG, "========================================")

        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to initialize charts: ${e.message}", e)
        }
    }

    // ==================== Private Helper Methods ====================

    /**
     * API에서 차트 아이돌 ID 리스트를 가져와서 랭킹 데이터 저장
     */
    private suspend fun fetchAndSaveChart(chartCode: String) {
        try {
            // API에서 아이돌 ID 리스트 가져오기
            rankingRepository.getChartIdolIds(chartCode).collect { result ->
                when (result) {
                    is ApiResult.Success -> {
                        val idolIds = result.data
                        if (idolIds.isNotEmpty()) {
                            Log.d(TAG, "✅ [$chartCode] Got ${idolIds.size} idol IDs from API")
                            buildAndSaveChartRankings(chartCode, idolIds)
                        } else {
                            Log.w(TAG, "⚠️ [$chartCode] No idol IDs from API")
                        }
                    }
                    is ApiResult.Error -> {
                        Log.e(TAG, "❌ [$chartCode] API error: ${result.message}")
                    }
                    is ApiResult.Loading -> {
                        // Loading state
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ [$chartCode] Failed to fetch: ${e.message}", e)
        }
    }

    /**
     * idol DB 데이터로 차트 랭킹 빌드 및 저장
     */
    private suspend fun buildAndSaveChartRankings(chartCode: String, idolIds: List<Int>) {
        try {
            // idol DB에서 아이돌 정보 가져오기
            val idols = idolDao.getIdolsByIds(idolIds)

            if (idols.isEmpty()) {
                Log.w(TAG, "⚠️ [$chartCode] No idols found in idol DB")
                return
            }

            // 하트 수로 정렬
            val sortedIdols = idols.sortedByDescending { it.heart }

            // 최대/최소 하트 수 계산
            val maxHeart = sortedIdols.firstOrNull()?.heart ?: 0L
            val minHeart = sortedIdols.lastOrNull()?.heart ?: 0L

            // ChartRankingItem 리스트 생성
            val rankings = sortedIdols.mapIndexed { index, idol ->
                // IdolImageUtil을 사용하여 top3 이미지/비디오 URL 가져오기
                val imageUrls = IdolImageUtil.getTop3ImageUrls(idol).filterNotNull()
                val videoUrls = IdolImageUtil.getTop3VideoUrls(idol).filterNotNull()

                // Top3 파싱 결과 로깅 (디버깅용 - 상위 3명만)
                if (index < 3) {
                    Log.d(TAG, "🖼️ [$chartCode] Rank ${index+1} (${idol.name}): images=${imageUrls.size}, videos=${videoUrls.size}")
                    imageUrls.forEachIndexed { i, url -> Log.d(TAG, "    Image[$i]: $url") }
                    videoUrls.forEachIndexed { i, url -> Log.d(TAG, "    Video[$i]: $url") }
                }

                ChartRankingItem(
                    idolId = idol.id,
                    rank = index + 1,
                    heartCount = idol.heart,
                    voteCount = NumberFormat.getNumberInstance(Locale.US).format(idol.heart),
                    maxHeartCount = maxHeart,
                    minHeartCount = minHeart,
                    name = idol.name,
                    photoUrl = idol.imageUrl,
                    miracleCount = idol.miracleCount,
                    fairyCount = idol.fairyCount,
                    angelCount = idol.angelCount,
                    rookieCount = idol.rookieCount,
                    anniversary = idol.anniversary,
                    anniversaryDays = idol.anniversaryDays ?: 0,
                    top3Image1 = imageUrls.getOrNull(0),
                    top3Image2 = imageUrls.getOrNull(1),
                    top3Image3 = imageUrls.getOrNull(2),
                    top3Video1 = videoUrls.getOrNull(0),
                    top3Video2 = videoUrls.getOrNull(1),
                    top3Video3 = videoUrls.getOrNull(2)
                )
            }

            // SharedPreference에 저장
            preferencesManager.saveChartRanking(chartCode, rankings)

            Log.d(TAG, "✅ [$chartCode] Saved ${rankings.size} rankings to SharedPreference")

        } catch (e: Exception) {
            Log.e(TAG, "❌ [$chartCode] Failed to build rankings: ${e.message}", e)
        }
    }

    /**
     * ChartRankingItem 리스트를 ProcessedRankData로 변환
     */
    private fun convertToProcessedRankData(
        chartCode: String,
        items: List<ChartRankingItem>
    ): ProcessedRankData {
        val rankingItems = items.map { item ->
            RankingItemData(
                id = item.idolId.toString(),  // String으로 변환
                rank = item.rank,
                name = item.name,
                photoUrl = item.photoUrl,
                voteCount = item.voteCount,
                heartCount = item.heartCount,
                maxHeartCount = item.maxHeartCount,
                minHeartCount = item.minHeartCount,
                top3ImageUrls = listOfNotNull(item.top3Image1, item.top3Image2, item.top3Image3),
                top3VideoUrls = listOfNotNull(item.top3Video1, item.top3Video2, item.top3Video3),
                miracleCount = item.miracleCount,
                fairyCount = item.fairyCount,
                angelCount = item.angelCount,
                rookieCount = item.rookieCount,
                superRookieCount = 0,  // ChartRankingItem에 없는 필드
                anniversary = item.anniversary,
                anniversaryDays = item.anniversaryDays
            )
        }

        // topIdol은 1등 아이돌의 IdolEntity를 가져와야 하지만, 여기서는 null로 설정
        // 필요하다면 idolDao.getIdolById()를 사용하여 가져올 수 있음
        return ProcessedRankData(
            rankItems = rankingItems,
            topIdol = null
        )
    }
}
