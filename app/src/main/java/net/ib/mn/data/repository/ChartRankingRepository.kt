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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import net.ib.mn.data.local.PreferencesManager
import net.ib.mn.data.local.dao.IdolDao
import net.ib.mn.data.remote.dto.toEntity
import net.ib.mn.domain.model.ApiResult
import net.ib.mn.ui.components.RankingItem
import net.ib.mn.util.Constants
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

    // mostFavoriteIdolRankingItem Flow
    private val _mostFavoriteIdolRankingItem = MutableStateFlow<RankingItem?>(null)
    val mostFavoriteIdolRankingItem: StateFlow<RankingItem?> = _mostFavoriteIdolRankingItem.asStateFlow()

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
                ProcessedRankData(
                    rankItems = items,
                    topIdol = null
                )
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
            ProcessedRankData(
                rankItems = items,
                topIdol = null
            )
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

            val idolIds = existingRankings.map { it.id.toIntOrNull() ?: 0 }
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
     * 1. idol DB 하트 수 업데이트 (기존 하트 수 + 투표 수)
     * 2. 업데이트된 DB 데이터를 기반으로 차트 재정렬
     *
     * @param idolId 투표한 아이돌 ID
     * @param votedHeartCount 투표한 하트 수 (기존 하트에 더해질 값)
     * @param chartCode 차트 코드 (nullable)
     */
    suspend fun updateVoteAndRerank(idolId: Int, votedHeartCount: Long, chartCode: String?) {
        try {
            Log.d(TAG, "💝 Updating vote: idol=$idolId, votedHearts=$votedHeartCount, chart=$chartCode")

            // 1. 기존 하트 수 가져오기
            val currentIdol = idolDao.getIdolById(idolId)
            val currentHeart = currentIdol?.heart ?: 0L
            val newTotalHeart = currentHeart + votedHeartCount

            Log.d(TAG, "📊 Heart calculation: current=$currentHeart + voted=$votedHeartCount = total=$newTotalHeart")

            // 2. idol DB 업데이트 (기존 하트 + 투표 하트)
            idolDao.updateIdolHeart(idolId, newTotalHeart)
            Log.d(TAG, "✅ Updated idol DB: idol=$idolId, hearts=$newTotalHeart")

            // 3. 투표한 아이돌이 최애돌인 경우 mostFavoriteIdolRankingItem도 즉시 업데이트
            val mostIdolId = userCacheRepository.get().mostIdolId.first()
            if (mostIdolId == idolId) {
                Log.d(TAG, "💖 Voted idol is mostFavorite - updating mostFavoriteIdolRankingItem")
                val currentMostIdol = _mostFavoriteIdolRankingItem.value
                if (currentMostIdol != null) {
                    // 기존 mostFavoriteIdol의 heartCount와 voteCount만 업데이트
                    val updatedMostIdol = currentMostIdol.copy(
                        heartCount = newTotalHeart,
                        voteCount = NumberFormat.getNumberInstance(Locale.US).format(newTotalHeart)
                    )
                    _mostFavoriteIdolRankingItem.value = updatedMostIdol
                    Log.d(TAG, "✅ Updated mostFavoriteIdol: hearts=$newTotalHeart")
                }
            }

            // 4. 업데이트된 DB 데이터를 기반으로 차트 재정렬
            if (chartCode != null) {
                // 특정 차트만 리프레시
                refreshChart(chartCode)
                Log.d(TAG, "✅ Refreshed chart: $chartCode")
            } else if(idolId != Constants.SECRET_ROOM_IDOL_ID){
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
     * API를 통해 모든 차트의 아이돌 데이터 새로고침
     * UDP 비활성화 상태에서 활성화로 전환 시 호출하여 놓친 데이터를 복구
     *
     * old 프로젝트의 NewRankingViewModel.refreshData()와 동일한 패턴
     *
     * @param idolRepository IdolRepository (API 호출용)
     */
    suspend fun refreshAllChartsFromApi(idolRepository: net.ib.mn.domain.repository.IdolRepository) {
        try {
            val startTime = System.currentTimeMillis()

            // 1. 모든 차트에서 아이돌 ID 수집
            val allIdolIds = mutableSetOf<Int>()
            DEFAULT_CHART_CODES.forEach { chartCode ->
                preferencesManager.getChartRanking(chartCode).forEach { item ->
                    item.id.toIntOrNull()?.let { allIdolIds.add(it) }
                }
            }

            if (allIdolIds.isEmpty()) {
                Log.d(TAG, "⚠️ refreshAllChartsFromApi: No idol IDs - charts may not be initialized yet")
                return
            }

            // 2. HTTP 414 방지를 위해 100개씩 청크로 나누어 API 호출
            val chunks = allIdolIds.toList().chunked(100)
            var totalUpdatedIdols = 0
            var apiSuccess = false

            for (chunk in chunks) {
                idolRepository.getIdolsByIds(chunk, fields = null).collect { result ->
                    when (result) {
                        is ApiResult.Success -> {
                            result.data.data?.let { idolDataList ->
                                if (idolDataList.isNotEmpty()) {
                                    idolDao.upsertIdols(idolDataList.map { it.toEntity() })
                                    totalUpdatedIdols += idolDataList.size
                                    apiSuccess = true
                                }
                            }
                        }
                        is ApiResult.Error -> Log.e(TAG, "❌ refreshAllChartsFromApi error: ${result.message}")
                        is ApiResult.Loading -> { }
                    }
                }
            }

            // 3. API 성공 시 모든 차트 새로고침 (정렬 재계산)
            if (apiSuccess) {
                coroutineScope {
                    DEFAULT_CHART_CODES.map { chartCode ->
                        async { refreshChart(chartCode) }
                    }.awaitAll()
                }
                Log.d(TAG, "✅ refreshAllChartsFromApi: Updated $totalUpdatedIdols idols in ${System.currentTimeMillis() - startTime}ms")
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ refreshAllChartsFromApi error", e)
        }
    }

    /**
     * 모든 차트 데이터 삭제
     */
    suspend fun clearAll() {
        try {
            // 1. SharedPreference의 모든 차트 데이터 삭제
            DEFAULT_CHART_CODES.forEach { chartCode ->
                preferencesManager.saveChartRanking(chartCode, emptyList())
            }

            // 2. 메모리 캐시 초기화
            _mostFavoriteIdolRankingItem.value = null

        } catch (e: Exception) {
            Log.e(TAG, "Failed to clear rankings: ${e.message}", e)
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

            // mostIdolId 가져오기 (isFavorite 설정용)
            val mostIdolId = try {
                userCacheRepository.get().getMostIdolId()
            } catch (e: Exception) {
                Log.w(TAG, "⚠️ Failed to get mostIdolId: ${e.message}")
                null
            }

            // RankingItem 리스트 생성 (같은 투표수면 같은 랭킹)
            var currentRank = 1
            var previousHeart: Long? = null

            val rankings = sortedIdols.mapIndexed { index, idol ->
                // 투표수가 이전 아이돌과 다르면 현재 index + 1을 랭킹으로 사용
                // 투표수가 같으면 이전 랭킹 유지
                val rank = if (previousHeart != null && previousHeart == idol.heart) {
                    currentRank  // 같은 투표수면 같은 랭킹
                } else {
                    index + 1  // 다른 투표수면 현재 순서를 랭킹으로
                }

                currentRank = rank
                previousHeart = idol.heart

                // IdolImageUtil을 사용하여 top3 이미지/비디오 URL 가져오기
                val imageUrls = IdolImageUtil.getTop3ImageUrls(idol).filterNotNull()
                val videoUrls = IdolImageUtil.getTop3VideoUrls(idol).filterNotNull()

                // Top3 파싱 결과 로깅 (디버깅용 - 상위 3명만)
                if (index < 3) {
                    Log.d(TAG, "🖼️ [$chartCode] Rank $rank (${idol.name}): hearts=${idol.heart}, images=${imageUrls.size}, videos=${videoUrls.size}")
                    imageUrls.forEachIndexed { i, url -> Log.d(TAG, "    Image[$i]: $url") }
                    videoUrls.forEachIndexed { i, url -> Log.d(TAG, "    Video[$i]: $url") }
                }

                // 다국어 이름 가져오기 (old 프로젝트와 동일)
                val localizedName = RankingUtil.getLocalizedName(idol, context)

                RankingItem(
                    id = idol.id.toString(),
                    rank = rank,
                    heartCount = idol.heart,
                    voteCount = NumberFormat.getNumberInstance(Locale.US).format(idol.heart),
                    maxHeartCount = maxHeart,
                    minHeartCount = minHeart,
                    name = localizedName,
                    photoUrl = idol.imageUrl ?: "",
                    miracleCount = idol.miracleCount,
                    fairyCount = idol.fairyCount,
                    angelCount = idol.angelCount,
                    rookieCount = idol.rookieCount,
                    superRookieCount = 0,
                    anniversary = if (idol.anniversary == "Y") idol.anniversary else null,
                    anniversaryDays = idol.anniversaryDays ?: 0,
                    top3ImageUrls = imageUrls,
                    top3VideoUrls = videoUrls,
                    isFavorite = mostIdolId?.let { idol.id == it } ?: false
                )
            }

            // SharedPreference에 저장
            preferencesManager.saveChartRanking(chartCode, rankings)

            Log.d(TAG, "✅ [$chartCode] Saved ${rankings.size} rankings to SharedPreference")

            // mostFavoriteIdolRankingItem 업데이트 (해당하는 아이돌이 있으면)
            updateMostFavoriteIdolRankingItem(chartCode, rankings)

        } catch (e: Exception) {
            Log.e(TAG, "❌ [$chartCode] Failed to build rankings: ${e.message}", e)
        }
    }


    /**
     * mostFavoriteIdolRankingItem 업데이트
     *
     * refreshChart 시 호출되어 mostIdolId에 해당하는 아이돌의 RankingItem를 설정
     * - 최애 아이돌의 차트 코드와 현재 차트 코드가 일치할 때만 업데이트
     * - rankings에서 찾으면: 해당 아이템을 그대로 설정
     * - rankings에 없으면: 업데이트하지 않음 (다른 차트일 가능성)
     * - 비밀의 방(chartCodes=[]) 특수 처리: idol DB에서 직접 가져오기
     */
    private suspend fun updateMostFavoriteIdolRankingItem(
        chartCode: String,
        rankings: List<RankingItem>
    ) {
        try {
            // UserCacheRepository에서 mostIdolId와 mostIdolChartCode 가져오기
            val mostIdolId: Int? = userCacheRepository.get().mostIdolId.first()
            val mostIdolChartCode: String? = userCacheRepository.get().getMostIdolChartCode()

            if (mostIdolId == null) {
                Log.d(TAG, "⚠️ No mostIdolId set - clearing mostFavoriteIdolRankingItem")
                _mostFavoriteIdolRankingItem.value = null
                return
            }

            // ✅ 비밀의 방 (chartCodes=[] 인 경우) 특수 처리
            // mostIdolChartCode가 null이면 차트에 속하지 않는 특수 아이돌 (비밀의 방)
            if (mostIdolChartCode == null) {
                Log.d(TAG, "🔐 Special idol (비밀의 방) detected - loading from DB")
                val idolEntity = idolDao.getIdolById(mostIdolId)
                if (idolEntity != null) {
                    // 다국어 이름 가져오기 (old 프로젝트와 동일)
                    val localizedName = RankingUtil.getLocalizedName(idolEntity, context)
                    val specialItem = RankingItem(
                        rank = 0,  // 비밀의 방은 순위 없음
                        name = localizedName,
                        voteCount = NumberFormat.getNumberInstance(Locale.US).format(idolEntity.heart),
                        photoUrl = idolEntity.imageUrl,
                        id = idolEntity.id.toString(),
                        heartCount = idolEntity.heart,
                        top3ImageUrls = listOfNotNull(
                            idolEntity.imageUrl,
                            idolEntity.imageUrl2,
                            idolEntity.imageUrl3
                        ),
                        top3VideoUrls = emptyList(),
                        isFavorite = true  // 비밀의 방은 항상 최애
                    )
                    _mostFavoriteIdolRankingItem.value = specialItem
                    Log.d(TAG, "✅ Loaded special idol: ${specialItem.name}, heart=${specialItem.heartCount}")
                } else {
                    Log.w(TAG, "⚠️ Special idol not found in DB: id=$mostIdolId")
                    _mostFavoriteIdolRankingItem.value = null
                }
                return
            }

            // 최애 아이돌의 차트 코드와 현재 차트 코드가 일치하지 않으면 스킵
            if (mostIdolChartCode != chartCode) {
                Log.d(TAG, "⏭️ Skipping chart $chartCode (mostIdol chart is $mostIdolChartCode)")
                return
            }

            Log.d(TAG, "🔍 Looking for mostIdolId=$mostIdolId in chart $chartCode rankings")

            // rankings에서 해당 아이돌 찾기
            val foundItem = rankings.find { it.id.toIntOrNull() == mostIdolId }

            if (foundItem != null) {
                // rankings에 있으면 그대로 설정
                Log.d(TAG, "✅ Found mostIdol in rankings: ${foundItem.name}, rank=${foundItem.rank}")

                _mostFavoriteIdolRankingItem.value = foundItem
            } else {
                // rankings에 없으면 로그만 남기고 업데이트하지 않음
                // (다른 차트이거나 순위권 밖일 수 있음)
                Log.d(TAG, "⚠️ mostIdol not found in rankings for chart $chartCode")
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to update mostFavoriteIdolRankingItem: ${e.message}", e)
        }
    }
}
