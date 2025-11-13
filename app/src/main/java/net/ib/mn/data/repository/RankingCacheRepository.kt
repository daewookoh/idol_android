package net.ib.mn.data.repository

import android.content.Context
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import net.ib.mn.data.local.PreferencesManager
import net.ib.mn.data.local.dao.IdolDao
import net.ib.mn.data.local.entity.IdolEntity
import net.ib.mn.util.Constants
import net.ib.mn.util.ProcessedRankData
import net.ib.mn.util.RankingUtil
import java.text.NumberFormat
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 랭킹 데이터 인메모리 캐시 Repository
 *
 * StartUp 시점에 로드된 ProcessedRankData를 메모리에 캐싱하여
 * 앱 전역에서 빠르게 접근할 수 있도록 함.
 *
 * 주요 기능:
 * 1. 차트별 ProcessedRankData 캐싱 (Thread-safe)
 * 2. Flow를 통한 반응형 데이터 제공
 * 3. 투표 후 실시간 캐시 업데이트
 * 4. UDP 업데이트 시 스마트 부분 갱신
 */
@Singleton
class RankingCacheRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val rankingRepository: net.ib.mn.domain.repository.RankingRepository,
    private val idolDao: IdolDao,
    private val preferencesManager: PreferencesManager,
    private val userCacheRepository: dagger.Lazy<UserCacheRepository>
) {

    companion object {
        private const val TAG = "RankingCacheRepository"
        private const val MAX_CACHE_SIZE = 20

        // 기본 차트 코드 리스트 (재사용)
        private val DEFAULT_CHART_CODES = listOf("PR_S_F", "PR_S_M", "PR_G_F", "PR_G_M", "GLOBALS")
    }

    // 차트별 캐시된 데이터 (Thread-safe)
    private val cache = ConcurrentHashMap<String, ProcessedRankData>()

    // 차트별 Flow (UI에서 구독 가능)
    private val flows = ConcurrentHashMap<String, MutableStateFlow<ProcessedRankData?>>()

    /**
     * 차트 데이터 저장 및 Flow 업데이트
     */
    fun setChartData(chartCode: String, data: ProcessedRankData) {
        // LRU 캐시 관리
        if (cache.size >= MAX_CACHE_SIZE && !cache.containsKey(chartCode)) {
            cache.keys.firstOrNull()?.let { oldestKey ->
                cache.remove(oldestKey)
                flows.remove(oldestKey)
                Log.d(TAG, "🗑️ Evicted: $oldestKey")
            }
        }

        cache[chartCode] = data
        flows.getOrPut(chartCode) { MutableStateFlow(null) }.value = data

        Log.d(TAG, "💾 $chartCode: ${data.rankItems.size} items")
    }

    /**
     * 차트 데이터 가져오기 (동기)
     */
    fun getChartData(chartCode: String): ProcessedRankData? {
        return cache[chartCode]
    }

    /**
     * 차트 데이터 Flow로 구독 (반응형)
     *
     * @param chartCode 차트 코드
     * @return Flow<ProcessedRankData?>
     */
    fun observeChartData(chartCode: String): Flow<ProcessedRankData?> {
        return flows.getOrPut(chartCode) {
            MutableStateFlow(cache[chartCode])
        }.asStateFlow()
    }

    /**
     * 모든 캐시 클리어 (로그아웃 시 사용)
     */
    fun clearAll() {
        val size = cache.size
        cache.clear()
        flows.values.forEach { it.value = null }
        Log.d(TAG, "🗑️ Cleared $size caches")
    }

    /**
     * 특정 차트 캐시 제거
     */
    fun clearChart(chartCode: String) {
        cache.remove(chartCode)
        flows[chartCode]?.value = null
        Log.d(TAG, "🗑️ Cleared: $chartCode")
    }

    /** 캐시된 모든 차트 코드 */
    fun getCachedChartCodes(): Set<String> = cache.keys.toSet()

    /** 캐시 존재 여부 */
    fun hasCache(chartCode: String): Boolean = cache.containsKey(chartCode)

    /** 캐시 크기 */
    fun getCacheSize(): Int = cache.size

    /**
     * 캐시 상태 로깅 (디버깅용)
     */
    fun logCacheStatus() {
        Log.d(TAG, "========== Cache Status: ${cache.size} ==========")
        cache.forEach { (code, data) ->
            Log.d(TAG, "  $code: ${data.rankItems.size} items")
        }
        Log.d(TAG, "===========================================")
    }

    /**
     * 차트 데이터 로딩 및 캐싱 (공용 함수)
     *
     * 기본 5개 차트의 idol IDs를 병렬로 로드하고, 각 차트별 랭킹 데이터를 가공하여 캐시에 저장함.
     *
     * @param chartCodes 캐싱할 차트 코드 리스트
     */
    suspend fun cacheIdolsRanking(chartCodes: List<String> = DEFAULT_CHART_CODES) {
        Log.d(TAG, "📊 Loading ${chartCodes.size} charts in parallel...")

        try {
            // Step 1: 차트의 idol_ids를 병렬로 로드
            val chartIdolIdsMap = coroutineScope {
                chartCodes.map { chartCode ->
                    async { chartCode to fetchChartIdolIds(chartCode) }
                }.awaitAll().filter { it.second != null }.toMap()
            }

            Log.d(TAG, "✅ ${chartIdolIdsMap.size} charts loaded successfully")

            if (chartIdolIdsMap.isEmpty()) {
                Log.w(TAG, "⚠️ No chart data loaded")
                return
            }

            // Step 2: 각 차트별 rankingData 가공 및 캐싱 (병렬)
            val mostIdolId = userCacheRepository.get().getMostIdolId()

            coroutineScope {
                chartIdolIdsMap.map { (chartCode, idolIds) ->
                    async {
                        try {
                            rebuildChartCache(chartCode, idolIds!!, mostIdolId)
                            Log.d(TAG, "✅ Chart $chartCode cached")
                        } catch (e: Exception) {
                            Log.e(TAG, "❌ Chart $chartCode error: ${e.message}", e)
                        }
                    }
                }.awaitAll()
            }

            Log.d(TAG, "✅ All ranking data cached")
            logCacheStatus()
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to cache rankings: ${e.message}", e)
        }
    }

    /**
     * 단일 차트 데이터 갱신 (백그라운드 API 호출)
     *
     * @param chartCode 갱신할 차트 코드
     */
    suspend fun refreshChartData(chartCode: String) {
        Log.d(TAG, "🔄 Refreshing $chartCode...")

        try {
            val idolIds = fetchChartIdolIds(chartCode)
            if (idolIds == null) {
                Log.w(TAG, "⚠️ No idol IDs loaded for $chartCode")
                return
            }

            val mostIdolId = userCacheRepository.get().getMostIdolId()
            rebuildChartCache(chartCode, idolIds, mostIdolId)

            Log.d(TAG, "✅ $chartCode refreshed")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to refresh $chartCode: ${e.message}", e)
        }
    }

    /**
     * 투표 후 랭킹 업데이트 및 캐시 갱신
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
            // DB 업데이트 (항상 실행)
            updateIdolHeartInDb(idolId, voteCount)

            val cachedData = getChartData(chartCode)
            if (cachedData == null) {
                Log.w(TAG, "⚠️ No cache for $chartCode")

                // 캐시가 없어도 최애 아이돌이면 업데이트
                val mostIdolId = userCacheRepository.get().getMostIdolId()
                if (idolId == mostIdolId) {
                    userCacheRepository.get().updateMostFavoriteIdolHeart(voteCount)
                }
                return
            }

            // 캐시에서 해당 아이돌 하트 수 업데이트
            val updatedItems = cachedData.rankItems.map { item ->
                if (item.id == idolId.toString()) {
                    val newHeart = item.heartCount + voteCount

                    item.copy(
                        voteCount = formatHeartCount(newHeart.toInt()),
                        heartCount = newHeart
                    )
                } else {
                    item
                }
            }

            // 재랭킹 및 max/min 재계산
            val sortedItems = RankingUtil.sortAndRank(updatedItems)
            val maxHeart = sortedItems.maxOfOrNull { it.heartCount } ?: 0L
            val minHeart = sortedItems.minOfOrNull { it.heartCount } ?: 0L

            val finalItems = sortedItems.map { item ->
                item.copy(maxHeartCount = maxHeart, minHeartCount = minHeart)
            }

            // topIdol 업데이트
            val newTopIdol = finalItems.firstOrNull()?.id?.toIntOrNull()?.let {
                idolDao.getIdolById(it)
            } ?: cachedData.topIdol

            // 캐시 업데이트
            setChartData(
                chartCode,
                cachedData.copy(rankItems = finalItems, topIdol = newTopIdol)
            )

            Log.d(TAG, "✅ Vote updated: $chartCode")

            // 최애 아이돌인 경우 UserCacheRepository의 mostFavoriteIdol도 업데이트
            val mostIdolId = userCacheRepository.get().getMostIdolId()
            if (idolId == mostIdolId) {
                userCacheRepository.get().refreshMostFavoriteIdol()
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Vote update failed: ${e.message}", e)
        }
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

    /**
     * UDP 업데이트 시 특정 아이돌의 하트 수만 업데이트 (스마트 부분 갱신)
     *
     * 전체 캐시 재생성 대신 해당 아이돌이 포함된 차트만 선택적으로 업데이트.
     * 사용자 투표 데이터 보호: 캐시 하트 > DB 하트이면 skip (사용자가 방금 투표한 것)
     *
     * @param idolIds 업데이트할 아이돌 ID 리스트
     */
    suspend fun updateIdolsFromUdp(idolIds: Set<Int>) {
        try {
            val updatedIdols = idolDao.getIdolsByIds(idolIds.toList())
            if (updatedIdols.isEmpty()) {
                return
            }

            val updatedIdolMap = updatedIdols.associateBy { it.id }
            val mostIdolId = userCacheRepository.get().getMostIdolId()

            DEFAULT_CHART_CODES.forEach { chartCode ->
                if (shouldUpdateChartFromUdp(chartCode, idolIds, updatedIdolMap)) {
                    updateChartFromUdp(chartCode, mostIdolId)
                }
            }

            // 최애 아이돌이 업데이트된 경우 UserCacheRepository도 갱신
            if (mostIdolId != null && idolIds.contains(mostIdolId)) {
                userCacheRepository.get().refreshMostFavoriteIdol()
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ UDP update failed: ${e.message}", e)
        }
    }

    /**
     * UDP 업데이트가 필요한지 확인
     */
    private fun shouldUpdateChartFromUdp(
        chartCode: String,
        idolIds: Set<Int>,
        updatedIdolMap: Map<Int, IdolEntity>
    ): Boolean {
        val cachedData = getChartData(chartCode) ?: return false

        // 업데이트할 아이돌이 차트에 없으면 skip
        val hasUpdatedIdol = cachedData.rankItems.any { item ->
            idolIds.contains(item.id.toIntOrNull())
        }
        if (!hasUpdatedIdol) return false

        // 캐시와 DB 비교: DB가 더 크면 업데이트 필요
        for (cachedItem in cachedData.rankItems) {
            val idolId = cachedItem.id.toIntOrNull() ?: continue
            if (!idolIds.contains(idolId)) continue

            val dbIdol = updatedIdolMap[idolId] ?: continue
            if (dbIdol.heart > cachedItem.heartCount) {
                Log.d(TAG, "  $chartCode: idol=$idolId needs update")
                return true
            }
        }

        return false
    }

    /**
     * UDP 업데이트로 차트 캐시 갱신
     */
    private suspend fun updateChartFromUdp(chartCode: String, mostIdolId: Int?) {
        val cachedData = getChartData(chartCode) ?: return
        val chartIdolIds = cachedData.rankItems.map { it.id.toInt() }

        rebuildChartCache(chartCode, chartIdolIds, mostIdolId)
        Log.d(TAG, "✅ UDP: $chartCode updated")
    }

    // ==================== Private Helper Methods ====================

    /**
     * API에서 차트 Idol IDs 가져오기 (공통 로직)
     */
    private suspend fun fetchChartIdolIds(chartCode: String): List<Int>? {
        var idolIds: List<Int>? = null
        try {
            rankingRepository.getChartIdolIds(chartCode).collect { result ->
                when (result) {
                    is net.ib.mn.domain.model.ApiResult.Success -> {
                        idolIds = result.data
                        Log.d(TAG, "✅ Chart $chartCode: ${result.data.size} idol IDs loaded")
                    }
                    is net.ib.mn.domain.model.ApiResult.Error -> {
                        Log.w(TAG, "⚠️ Chart $chartCode failed: ${result.exception.message}")
                    }
                    is net.ib.mn.domain.model.ApiResult.Loading -> {
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
     * IdolEntity 리스트를 ProcessedRankData로 변환 (공통 로직)
     */
    private suspend fun processIdolEntities(
        idols: List<IdolEntity>,
        mostIdolId: Int?
    ): ProcessedRankData {
        val sortedIdols = idols.sortedByDescending { it.heart }
        return RankingUtil.processIdolsData(
            idols = sortedIdols,
            context = context,
            mostIdolId = mostIdolId,
            formatHeartCount = ::formatHeartCount
        )
    }

    /**
     * 차트 데이터 재생성 (공통 로직)
     */
    private suspend fun rebuildChartCache(
        chartCode: String,
        idolIds: List<Int>,
        mostIdolId: Int?
    ) {
        val idolEntities = idolDao.getIdolsByIds(idolIds)
        val processedData = processIdolEntities(idolEntities, mostIdolId)
        setChartData(chartCode, processedData)
    }

    /**
     * 하트 수 포맷팅 (NumberFormat 사용)
     */
    private fun formatHeartCount(count: Int): String {
        return NumberFormat.getNumberInstance(Locale.US).format(count)
    }
}
