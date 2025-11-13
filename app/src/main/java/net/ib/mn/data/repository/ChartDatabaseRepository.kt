package net.ib.mn.data.repository

import android.content.Context
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import net.ib.mn.data.local.dao.ChartRankingDao
import net.ib.mn.data.local.dao.IdolDao
import net.ib.mn.data.local.entity.ChartRankingEntity
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
 * 차트 랭킹 데이터를 Room DB에 저장하고 관리하는 Repository (통합 버전)
 *
 * RankingCacheRepository의 모든 기능을 통합하여 단일 Repository로 관리
 *
 * 5개 차트 관리:
 * - PR_S_F: Female Solo
 * - PR_S_M: Male Solo
 * - PR_G_F: Female Group
 * - PR_G_M: Male Group
 * - GLOBALS: Global
 *
 * 주요 기능:
 * 1. Startup 시 5개 차트 데이터를 API에서 로드하여 DB에 저장
 * 2. Flow를 통한 실시간 스트리밍
 * 3. 투표 후 DB 업데이트 및 자동 UI 갱신
 * 4. UDP 업데이트 시 DB 실시간 업데이트
 * 5. idol DB 변경 감지 및 자동 chart_rankings 업데이트 (5초 debounce)
 */
@Singleton
class ChartDatabaseRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val rankingRepository: net.ib.mn.domain.repository.RankingRepository,
    private val idolDao: IdolDao,
    private val chartRankingDao: ChartRankingDao,
    private val userCacheRepository: dagger.Lazy<UserCacheRepository>
) {

    companion object {
        private const val TAG = "ChartDatabaseRepo"

        // 기본 5개 차트 코드
        val DEFAULT_CHART_CODES = listOf("PR_S_F", "PR_S_M", "PR_G_F", "PR_G_M", "GLOBALS")

        // idol DB 변경 감지 debounce 시간 (밀리초)
        private const val IDOL_DB_DEBOUNCE_MS = 5000L
    }

    // Coroutine Scope for background operations
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    init {
        // idol DB 변경 감지 및 자동 chart_rankings 업데이트
        startIdolDbChangeObserver()
    }

    // ==================== Idol DB Change Observer ====================

    /**
     * idol DB 변경 감지 및 자동 chart_rankings 업데이트
     *
     * idol 테이블의 heart 합계를 관찰하여:
     * 1. 변경 감지 (distinctUntilChanged)
     * 2. 5초 동안 추가 변경이 없을 때만 실행 (debounce)
     * 3. 모든 차트 재생성
     *
     * 장점:
     * - UDP 등으로 idol DB가 변경되면 자동으로 chart_rankings 업데이트
     * - debounce로 불필요한 재생성 방지 (대량 업데이트 시 효율적)
     * - Single Source of Truth: idol DB → chart_rankings
     *
     * 참고:
     * - 투표 시에는 updateVoteAndRefreshCache()에서 즉시 업데이트하므로
     *   이 자동 업데이트는 UDP 등 다른 경우에만 실행됩니다.
     */
    @OptIn(FlowPreview::class)
    private fun startIdolDbChangeObserver() {
        scope.launch {
            Log.d(TAG, "🚀 Starting idol DB change observer (${IDOL_DB_DEBOUNCE_MS}ms debounce)")

            idolDao.observeTotalHearts()
                .drop(1) // 첫 번째 값(초기 로드)은 무시
                .distinctUntilChanged() // 실제로 값이 변경된 경우만
                .debounce(IDOL_DB_DEBOUNCE_MS) // 5초 동안 추가 변경이 없을 때만 실행
                .collect { totalHearts ->
                    if (totalHearts != null) {
                        Log.d(TAG, "========================================")
                        Log.d(TAG, "🔄 idol DB 자동 업데이트 트리거")
                        Log.d(TAG, "   - totalHearts: $totalHearts")
                        Log.d(TAG, "   - debounce: ${IDOL_DB_DEBOUNCE_MS}ms 경과 (추가 변경 없음)")
                        Log.d(TAG, "   - 모든 차트 재생성 시작...")
                        Log.d(TAG, "========================================")

                        try {
                            val startTime = System.currentTimeMillis()

                            // 모든 차트를 idol DB 최신 데이터로 재생성
                            DEFAULT_CHART_CODES.forEach { chartCode ->
                                refreshChartFromIdolDb(chartCode)
                            }

                            val elapsed = System.currentTimeMillis() - startTime

                            Log.d(TAG, "========================================")
                            Log.d(TAG, "✅ 자동 업데이트 완료 (${elapsed}ms)")
                            Log.d(TAG, "   - ${DEFAULT_CHART_CODES.size}개 차트 재생성")
                            Log.d(TAG, "   - UI는 Flow를 통해 자동 반영됨")
                            Log.d(TAG, "========================================")
                        } catch (e: Exception) {
                            Log.e(TAG, "❌ 자동 업데이트 실패: ${e.message}", e)
                        }
                    }
                }
        }
    }

    /**
     * idol DB 데이터로 chart_rankings를 직접 재생성 (API 호출 없음)
     *
     * 기존 chart_rankings의 idol ID 목록을 사용하여 idol DB의 최신 데이터로 업데이트합니다.
     * 완전히 로컬 DB만으로 동작하며, API 호출이 없습니다.
     *
     * @param chartCode 갱신할 차트 코드
     */
    private suspend fun refreshChartFromIdolDb(chartCode: String) {
        try {
            val startTime = System.currentTimeMillis()

            // 1. 기존 chart_rankings에서 아이돌 ID 목록 가져오기 (API 호출 없음)
            val existingRankings = chartRankingDao.getChartRankings(chartCode)
            if (existingRankings.isEmpty()) {
                Log.w(TAG, "⚠️ No existing rankings for $chartCode, skipping refresh")
                return
            }

            val idolIds = existingRankings.map { it.idolId }
            Log.d(TAG, "🔄 [$chartCode] Refreshing with ${idolIds.size} idols from idol DB")

            // 2. idol DB의 최신 데이터로 chart_rankings 재생성 (완전 로컬 동작)
            buildAndSaveChartRankings(chartCode, idolIds)

            val elapsed = System.currentTimeMillis() - startTime
            Log.d(TAG, "✅ [$chartCode] Refreshed in ${elapsed}ms (${idolIds.size} idols)")
            Log.d(TAG, "   → chart_rankings 업데이트 완료, UI Flow에 emit됨")

        } catch (e: Exception) {
            Log.e(TAG, "❌ [$chartCode] Failed to refresh: ${e.message}", e)
        }
    }

    // ==================== Public API (UI Layer) ====================

    /**
     * 차트 데이터 Flow로 구독 (반응형)
     *
     * Room DB의 Flow를 구독하여 실시간으로 변경사항을 감지합니다.
     * ChartRankingEntity를 ProcessedRankData로 변환하여 반환합니다.
     *
     * @param chartCode 차트 코드
     * @return Flow<ProcessedRankData?>
     */
    fun observeChartData(chartCode: String): Flow<ProcessedRankData?> {
        return chartRankingDao.observeChartRankings(chartCode).map { entities ->
            if (entities.isEmpty()) {
                Log.d(TAG, "⚠️ No data in DB for chart: $chartCode")
                null
            } else {
                convertToProcessedRankData(chartCode, entities)
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
        val entities = chartRankingDao.getChartRankings(chartCode)
        return if (entities.isEmpty()) {
            Log.d(TAG, "⚠️ No data in DB for chart: $chartCode")
            null
        } else {
            convertToProcessedRankData(chartCode, entities)
        }
    }

    /**
     * 차트가 DB에 존재하는지 확인
     *
     * @param chartCode 차트 코드
     * @return Boolean
     */
    suspend fun hasCache(chartCode: String): Boolean {
        return chartRankingDao.getChartSize(chartCode) > 0
    }

    /**
     * 모든 캐시 클리어 (로그아웃 시 사용)
     */
    suspend fun clearAll() {
        chartRankingDao.deleteAll()
        Log.d(TAG, "🗑️ All chart data cleared from DB")
    }

    /**
     * 특정 차트 캐시 제거
     *
     * @param chartCode 차트 코드
     */
    suspend fun clearChart(chartCode: String) {
        chartRankingDao.deleteChartRankings(chartCode)
        Log.d(TAG, "🗑️ Cleared: $chartCode")
    }

    /**
     * 투표 후 랭킹 업데이트 및 캐시 갱신 (즉시 반영)
     *
     * idol DB 업데이트 후 즉시 chart_rankings를 재생성하여 UI에 즉시 반영됩니다.
     * API 호출 없이 완전히 로컬 DB만으로 동작합니다.
     *
     * @param chartCode 업데이트할 차트 코드
     * @param idolId 투표한 아이돌 ID
     * @param voteCount 투표한 하트 수
     */
    suspend fun updateVoteAndRefreshCache(
        chartCode: String,
        idolId: Int,
        voteCount: Long
    ) {
        Log.d(TAG, "📊 Vote: idol=$idolId, chart=$chartCode, +$voteCount")

        try {
            // 1️⃣ idol DB 먼저 업데이트 (Single Source of Truth)
            updateIdolHeartInDb(idolId, voteCount)
            Log.d(TAG, "✅ idol DB updated: idol=$idolId")

            // 2️⃣ chart_rankings 즉시 재생성 (API 호출 없이, idol DB에서 직접)
            refreshChartFromIdolDb(chartCode)

            Log.d(TAG, "✅ Vote updated: $chartCode (Immediate UI refresh via Flow)")

            // 3️⃣ 최애 아이돌인 경우 UserCacheRepository도 업데이트
            val mostIdolId = userCacheRepository.get().getMostIdolId()
            if (idolId == mostIdolId) {
                userCacheRepository.get().refreshMostFavoriteIdol()
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Vote update failed: ${e.message}", e)
        }
    }

    /**
     * UDP 업데이트 시 특정 아이돌의 하트 수만 업데이트
     *
     * Room DB를 업데이트하면 observeChartData()를 구독 중인 UI가 자동으로 갱신됩니다.
     *
     * @param idolIds 업데이트할 아이돌 ID 리스트
     */
    suspend fun updateIdolsFromUdp(idolIds: Set<Int>) {
        try {
            Log.d(TAG, "🔄 UDP update: ${idolIds.size} idols")

            // 업데이트된 아이돌 정보 조회
            val updatedIdols = idolDao.getIdolsByIds(idolIds.toList())
            if (updatedIdols.isEmpty()) {
                return
            }

            // 각 아이돌의 하트 수를 DB에 업데이트
            updatedIdols.forEach { idol ->
                chartRankingDao.updateIdolHeartCount(idol.id, idol.heart)
            }

            Log.d(TAG, "✅ UDP: ${updatedIdols.size} idols updated in DB (Auto UI refresh via Flow)")

            // 최애 아이돌이 업데이트된 경우 UserCacheRepository도 갱신
            val mostIdolId = userCacheRepository.get().getMostIdolId()
            if (mostIdolId != null && idolIds.contains(mostIdolId)) {
                userCacheRepository.get().refreshMostFavoriteIdol()
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ UDP update failed: ${e.message}", e)
        }
    }

    // ==================== Initialization ====================

    /**
     * Startup 시 5개 차트 데이터를 DB에 초기화
     *
     * 동작:
     * 1. 각 차트별 아이돌 ID 목록을 API에서 병렬로 로드
     * 2. 각 아이돌의 하트 수를 Room DB에서 조회
     * 3. 랭킹을 계산하여 ChartRankingEntity로 변환
     * 4. DB에 저장 (기존 데이터 교체)
     *
     * @param chartCodes 초기화할 차트 코드 리스트 (기본: 5개 차트)
     */
    suspend fun initializeChartsInDatabase(chartCodes: List<String> = DEFAULT_CHART_CODES) {
        Log.d(TAG, "========================================")
        Log.d(TAG, "📊 Initializing ${chartCodes.size} charts in database...")
        Log.d(TAG, "========================================")

        try {
            // Step 1: 차트별 아이돌 ID 목록을 병렬로 로드
            val chartIdolIdsMap = coroutineScope {
                chartCodes.map { chartCode ->
                    async {
                        val idolIds = fetchChartIdolIds(chartCode)
                        chartCode to idolIds
                    }
                }.awaitAll().filter { it.second != null }.associate { it }
            }

            Log.d(TAG, "✅ ${chartIdolIdsMap.size} charts loaded from API")

            if (chartIdolIdsMap.isEmpty()) {
                Log.w(TAG, "⚠️ No chart data loaded from API")
                return
            }

            // Step 2: 각 차트별 랭킹 데이터 생성 및 DB 저장 (병렬)
            coroutineScope {
                chartIdolIdsMap.map { (chartCode, idolIds) ->
                    async {
                        try {
                            buildAndSaveChartRankings(chartCode, idolIds!!)
                            Log.d(TAG, "✅ Chart $chartCode saved to DB")
                        } catch (e: Exception) {
                            Log.e(TAG, "❌ Chart $chartCode error: ${e.message}", e)
                        }
                    }
                }.awaitAll()
            }

            Log.d(TAG, "========================================")
            Log.d(TAG, "✅ All chart rankings initialized in database")
            Log.d(TAG, "========================================")

            // 통계 로깅
            logChartStatistics()

        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to initialize charts: ${e.message}", e)
        }
    }

    /**
     * 특정 차트의 랭킹 데이터를 갱신
     *
     * @param chartCode 갱신할 차트 코드
     */
    suspend fun refreshChart(chartCode: String) {
        Log.d(TAG, "🔄 Refreshing chart: $chartCode")

        try {
            val idolIds = fetchChartIdolIds(chartCode)
            if (idolIds == null) {
                Log.w(TAG, "⚠️ No idol IDs loaded for $chartCode")
                return
            }

            buildAndSaveChartRankings(chartCode, idolIds)
            Log.d(TAG, "✅ Chart $chartCode refreshed")

        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to refresh $chartCode: ${e.message}", e)
        }
    }

    /**
     * UDP 업데이트 시 특정 아이돌의 하트 수 업데이트
     *
     * @param idolId 업데이트할 아이돌 ID
     * @param newHeartCount 새로운 하트 수
     */
    suspend fun updateIdolHeartFromUdp(idolId: Int, newHeartCount: Long) {
        try {
            // 모든 차트에서 해당 아이돌의 하트 수 업데이트
            chartRankingDao.updateIdolHeartCount(idolId, newHeartCount)
            Log.d(TAG, "✅ UDP: Updated idol $idolId heart to $newHeartCount in all charts")

            // 랭킹 재계산이 필요한 경우 (선택적)
            // 각 차트별로 해당 아이돌이 속한 차트만 재정렬
            // TODO: 필요시 구현

        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to update idol $idolId from UDP: ${e.message}", e)
        }
    }

    /**
     * 투표 후 특정 차트의 아이돌 하트 수 업데이트
     *
     * @param chartCode 차트 코드
     * @param idolId 아이돌 ID
     * @param newHeartCount 새로운 하트 수
     */
    suspend fun updateIdolHeartAfterVote(chartCode: String, idolId: Int, newHeartCount: Long) {
        try {
            chartRankingDao.updateChartIdolHeartCount(chartCode, idolId, newHeartCount)
            Log.d(TAG, "✅ Vote: Updated idol $idolId heart to $newHeartCount in chart $chartCode")

            // 랭킹 재계산 (해당 차트만)
            refreshChart(chartCode)

        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to update idol $idolId after vote: ${e.message}", e)
        }
    }

    // ==================== Private Helper Methods ====================

    /**
     * API에서 차트의 아이돌 ID 목록 가져오기
     */
    private suspend fun fetchChartIdolIds(chartCode: String): List<Int>? {
        var idolIds: List<Int>? = null
        try {
            rankingRepository.getChartIdolIds(chartCode).collect { result ->
                when (result) {
                    is ApiResult.Success -> {
                        idolIds = result.data
                        Log.d(TAG, "✅ Chart $chartCode: ${result.data.size} idol IDs loaded")
                    }
                    is ApiResult.Error -> {
                        Log.w(TAG, "⚠️ Chart $chartCode failed: ${result.exception.message}")
                    }
                    is ApiResult.Loading -> {
                        // 로딩 중
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Chart $chartCode error: ${e.message}", e)
        }
        return idolIds
    }

    /**
     * 차트 랭킹 데이터 생성 및 DB 저장
     *
     * idol DB에서 최신 데이터를 가져와 랭킹을 재계산하고,
     * chart_rankings 테이블을 atomic하게 업데이트합니다.
     *
     * @param chartCode 차트 코드
     * @param idolIds 아이돌 ID 리스트
     */
    private suspend fun buildAndSaveChartRankings(chartCode: String, idolIds: List<Int>) {
        // 1. idol DB에서 아이돌 정보 조회 (하트 수 포함)
        val idolEntities = idolDao.getIdolsByIds(idolIds)
        if (idolEntities.isEmpty()) {
            Log.w(TAG, "⚠️ [$chartCode] No idols found in idol DB")
            return
        }

        Log.d(TAG, "   → [$chartCode] idol DB에서 ${idolEntities.size}개 아이돌 로드")

        // 2. 하트 수 기준 정렬 (내림차순) - 랭킹 재집계
        val sortedIdols = idolEntities.sortedByDescending { it.heart }

        // 3. 최대/최소 하트 수 계산
        val maxHeart = sortedIdols.firstOrNull()?.heart ?: 0L
        val minHeart = sortedIdols.lastOrNull()?.heart ?: 0L

        Log.d(TAG, "   → [$chartCode] 랭킹 재집계: 1위=${sortedIdols.firstOrNull()?.name}(${maxHeart} hearts)")

        // 4. ChartRankingEntity 생성 (동점자 처리 + 모든 필드 포함)
        val rankingEntities = mutableListOf<ChartRankingEntity>()
        var currentRank = 1

        sortedIdols.forEachIndexed { index, idol ->
            // 동점 처리: 이전 아이돌과 하트 수가 같으면 같은 순위
            if (index > 0 && sortedIdols[index - 1].heart == idol.heart) {
                currentRank = rankingEntities[index - 1].rank
            } else {
                currentRank = index + 1
            }

            // 다국어 처리된 이름
            val localizedName = RankingUtil.getLocalizedName(idol, context)

            // Top3 이미지/비디오 URL
            val top3ImageUrls = IdolImageUtil.getTop3ImageUrls(idol)
            val top3VideoUrls = IdolImageUtil.getTop3VideoUrls(idol)

            val entity = ChartRankingEntity(
                chartCode = chartCode,
                idolId = idol.id,
                rank = currentRank,
                heartCount = idol.heart,
                maxHeartCount = maxHeart,
                minHeartCount = minHeart,
                voteCount = formatHeartCount(idol.heart.toInt()),
                name = localizedName,
                photoUrl = idol.imageUrl,
                miracleCount = idol.miracleCount,
                fairyCount = idol.fairyCount,
                angelCount = idol.angelCount,
                rookieCount = idol.rookieCount,
                superRookieCount = 0, // IdolEntity에 superRookieCount 필드 없음
                anniversary = null, // TODO: 기념일 정보가 있으면 추가
                anniversaryDays = 0,
                top3Image1 = top3ImageUrls.getOrNull(0),
                top3Image2 = top3ImageUrls.getOrNull(1),
                top3Image3 = top3ImageUrls.getOrNull(2),
                top3Video1 = top3VideoUrls.getOrNull(0),
                top3Video2 = top3VideoUrls.getOrNull(1),
                top3Video3 = top3VideoUrls.getOrNull(2),
                updatedAt = System.currentTimeMillis()
            )
            rankingEntities.add(entity)
        }

        // 5. chart_rankings 테이블에 atomic하게 저장 (기존 데이터 삭제 후 삽입)
        chartRankingDao.replaceChartRankings(chartCode, rankingEntities)
        Log.d(TAG, "   → [$chartCode] chart_rankings 테이블 atomic 업데이트 (${rankingEntities.size}개)")
        Log.d(TAG, "   → [$chartCode] Flow emit 완료 → UI 자동 갱신")
    }

    /**
     * 하트 수 포맷팅 (NumberFormat 사용)
     */
    private fun formatHeartCount(count: Int): String {
        return NumberFormat.getNumberInstance(Locale.US).format(count)
    }

    /**
     * 차트 통계 로깅 (디버깅용)
     */
    private suspend fun logChartStatistics() {
        try {
            val allChartCodes = chartRankingDao.getAllChartCodes()

            Log.d(TAG, "========================================")
            Log.d(TAG, "📊 Chart Database Statistics")
            Log.d(TAG, "========================================")
            Log.d(TAG, "Total charts: ${allChartCodes.size}")

            allChartCodes.forEach { chartCode ->
                val size = chartRankingDao.getChartSize(chartCode)
                Log.d(TAG, "  - $chartCode: $size idols")
            }

            Log.d(TAG, "========================================")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to log statistics: ${e.message}", e)
        }
    }

    /**
     * ChartRankingEntity 리스트를 ProcessedRankData로 변환 (최적화)
     *
     * Entity에 모든 정보가 포함되어 있어 IdolDao 조회 불필요
     *
     * @param chartCode 차트 코드
     * @param entities ChartRankingEntity 리스트
     * @return ProcessedRankData
     */
    private suspend fun convertToProcessedRankData(
        chartCode: String,
        entities: List<ChartRankingEntity>
    ): ProcessedRankData {
        // 1. 최애 아이돌 ID 조회
        val mostIdolId = userCacheRepository.get().getMostIdolId()

        // 2. RankingItemData 생성 (Entity에서 바로 변환)
        val rankItems = entities.map { entity ->
            // 최애 여부 (동적 계산)
            val isFavorite = mostIdolId != null && entity.idolId == mostIdolId

            // Top3 이미지/비디오 URL 리스트로 변환
            val top3ImageUrls = listOf(
                entity.top3Image1,
                entity.top3Image2,
                entity.top3Image3
            )

            val top3VideoUrls = listOf(
                entity.top3Video1,
                entity.top3Video2,
                entity.top3Video3
            )

            RankingItemData(
                rank = entity.rank,
                name = entity.name,
                voteCount = entity.voteCount,
                photoUrl = entity.photoUrl,
                id = entity.idolId.toString(),
                miracleCount = entity.miracleCount,
                fairyCount = entity.fairyCount,
                angelCount = entity.angelCount,
                rookieCount = entity.rookieCount,
                superRookieCount = entity.superRookieCount,
                heartCount = entity.heartCount,
                maxHeartCount = entity.maxHeartCount,
                minHeartCount = entity.minHeartCount,
                isFavorite = isFavorite,
                anniversary = entity.anniversary,
                anniversaryDays = entity.anniversaryDays,
                top3ImageUrls = top3ImageUrls,
                top3VideoUrls = top3VideoUrls
            )
        }

        // 3. 1위 아이돌 정보 (IdolDao 조회 필요)
        val topIdol = entities.firstOrNull()?.let { topEntity ->
            idolDao.getIdolById(topEntity.idolId)
        }

        return ProcessedRankData(
            rankItems = rankItems,
            topIdol = topIdol
        )
    }

    /**
     * DB에서 아이돌 하트 수 업데이트
     */
    private suspend fun updateIdolHeartInDb(idolId: Int, voteCount: Long) {
        try {
            val idol = idolDao.getIdolById(idolId)
            if (idol != null) {
                val newHeart = idol.heart + voteCount
                idolDao.updateIdolHeart(idolId, newHeart)
                Log.d(TAG, "✅ DB: idol=$idolId, heart=${idol.heart} → $newHeart")
            } else {
                Log.w(TAG, "⚠️ Idol $idolId not found in DB")
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ DB update failed: ${e.message}", e)
        }
    }
}
