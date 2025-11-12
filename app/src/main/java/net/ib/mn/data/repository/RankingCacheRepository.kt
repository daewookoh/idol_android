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
    private val preferencesManager: PreferencesManager
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
     * 차트 데이터 저장
     *
     * @param chartCode 차트 코드 (예: "PR_S_F", "PR_G_M", "GLOBALS")
     * @param data ProcessedRankData (rankItems + topIdol)
     */
    fun setChartData(chartCode: String, data: ProcessedRankData) {
        // LRU 방식으로 오래된 캐시 제거 (메모리 관리)
        if (cache.size >= MAX_CACHE_SIZE && !cache.containsKey(chartCode)) {
            val oldestKey = cache.keys.firstOrNull()
            if (oldestKey != null) {
                cache.remove(oldestKey)
                flows.remove(oldestKey)
                android.util.Log.d(TAG, "🗑️ Evicted oldest cache: $oldestKey")
            }
        }

        cache[chartCode] = data

        // Flow 업데이트 - 새로운 값을 emit
        val flow = flows.getOrPut(chartCode) { MutableStateFlow(null) }
        flow.value = data

        android.util.Log.d(
            TAG,
            "✅ Cached: $chartCode with ${data.rankItems.size} items, topIdol=${data.topIdol?.name}, flowHashCode=${flow.hashCode()}"
        )
    }

    /**
     * 차트 데이터 가져오기 (동기)
     *
     * @param chartCode 차트 코드
     * @return ProcessedRankData 또는 null (캐시 미스)
     */
    fun getChartData(chartCode: String): ProcessedRankData? {
        val data = cache[chartCode]
        if (data != null) {
            android.util.Log.d(TAG, "✅ Cache hit: $chartCode")
        } else {
            android.util.Log.d(TAG, "❌ Cache miss: $chartCode")
        }
        return data
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
     * 모든 캐시 클리어
     * (로그아웃 시 사용)
     */
    fun clearAll() {
        val size = cache.size
        cache.clear()
        flows.values.forEach { it.value = null }
        android.util.Log.d(TAG, "🗑️ Cleared all cache ($size items)")
    }

    /**
     * 특정 차트 캐시 제거
     *
     * @param chartCode 차트 코드
     */
    fun clearChart(chartCode: String) {
        cache.remove(chartCode)
        flows[chartCode]?.value = null
        android.util.Log.d(TAG, "🗑️ Cleared cache: $chartCode")
    }

    /**
     * 캐시된 모든 차트 코드
     *
     * @return Set<String>
     */
    fun getCachedChartCodes(): Set<String> {
        return cache.keys.toSet()
    }

    /**
     * 캐시 상태 확인
     *
     * @param chartCode 차트 코드
     * @return Boolean (캐시 존재 여부)
     */
    fun hasCache(chartCode: String): Boolean {
        return cache.containsKey(chartCode)
    }

    /**
     * 캐시 크기
     *
     * @return Int
     */
    fun getCacheSize(): Int {
        return cache.size
    }

    /**
     * 캐시 상태 로깅 (디버깅용)
     */
    fun logCacheStatus() {
        android.util.Log.d(TAG, "========================================")
        android.util.Log.d(TAG, "Cache Status: ${cache.size} items")
        cache.forEach { (chartCode, data) ->
            android.util.Log.d(
                TAG,
                "  - $chartCode: ${data.rankItems.size} items, topIdol=${data.topIdol?.name}"
            )
        }
        android.util.Log.d(TAG, "========================================")
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
            val mostIdolId = preferencesManager.mostIdolId.first()

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

            val mostIdolId = preferencesManager.mostIdolId.first()
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
            val cachedData = getChartData(chartCode) ?: run {
                Log.w(TAG, "⚠️ No cache for $chartCode")
                return
            }

            // DB 업데이트
            updateIdolHeartInDb(idolId, voteCount)

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
     * UDP 업데이트 시 특정 아이돌의 하트 수만 업데이트 (캐시 부분 갱신)
     *
     * 전체 캐시를 재생성하지 않고, 해당 아이돌이 포함된 차트의 캐시만 업데이트
     * 사용자가 방금 투표한 데이터와 충돌하지 않도록 처리:
     * - 캐시의 하트 수가 DB보다 크면 사용자가 방금 투표한 것이므로 skip
     * - DB의 하트 수가 크거나 같으면 서버에서 온 최신 데이터이므로 업데이트
     *
     * @param idolIds 업데이트할 아이돌 ID 리스트
     */
    suspend fun updateIdolsFromUdp(idolIds: Set<Int>) {
        android.util.Log.d(TAG, "📡 UDP update for ${idolIds.size} idols")

        try {
            // 업데이트된 아이돌들을 DB에서 가져오기
            val updatedIdols = idolDao.getIdolsByIds(idolIds.toList())
            if (updatedIdols.isEmpty()) {
                android.util.Log.w(TAG, "⚠️ No idols found in DB for UDP update")
                return
            }

            android.util.Log.d(TAG, "✅ Found ${updatedIdols.size} idols in DB")
            val updatedIdolMap = updatedIdols.associateBy { it.id }

            // 각 차트의 캐시를 확인하고 해당 아이돌이 포함되어 있으면 업데이트
            val mostIdolId = preferencesManager.mostIdolId.first()
            val chartCodes = listOf("PR_S_F", "PR_S_M", "PR_G_F", "PR_G_M", "GLOBALS")

            chartCodes.forEach { chartCode ->
                val cachedData = getChartData(chartCode)
                if (cachedData == null) {
                    android.util.Log.d(TAG, "⏭️ No cache for $chartCode, skipping")
                    return@forEach
                }

                // 업데이트할 아이돌이 이 차트에 포함되어 있는지 확인
                val hasUpdatedIdol = cachedData.rankItems.any { item ->
                    idolIds.contains(item.id.toIntOrNull())
                }

                if (!hasUpdatedIdol) {
                    android.util.Log.d(TAG, "⏭️ $chartCode doesn't contain updated idols, skipping")
                    return@forEach
                }

                android.util.Log.d(TAG, "🔄 Checking if $chartCode needs update...")

                // 캐시와 DB의 하트 수 비교
                var needsUpdate = false
                for (cachedItem in cachedData.rankItems) {
                    val idolId = cachedItem.id.toIntOrNull() ?: continue
                    if (!idolIds.contains(idolId)) continue

                    val dbIdol = updatedIdolMap[idolId] ?: continue
                    val cachedHeart = cachedItem.heartCount
                    val dbHeart = dbIdol.heart

                    if (dbHeart > cachedHeart) {
                        android.util.Log.d(TAG, "  Idol $idolId: cache=$cachedHeart < db=$dbHeart → needs update")
                        needsUpdate = true
                        break
                    } else if (dbHeart < cachedHeart) {
                        android.util.Log.d(TAG, "  Idol $idolId: cache=$cachedHeart > db=$dbHeart → skip (user just voted)")
                    } else {
                        android.util.Log.d(TAG, "  Idol $idolId: cache=$cachedHeart == db=$dbHeart → no change")
                    }
                }

                if (!needsUpdate) {
                    android.util.Log.d(TAG, "⏭️ $chartCode doesn't need update, skipping")
                    return@forEach
                }

                android.util.Log.d(TAG, "🔄 Updating $chartCode cache...")

                // DB에서 해당 차트의 모든 아이돌 정보 가져오기 (최신 하트 수 반영)
                val chartIdolIds = cachedData.rankItems.map { it.id.toInt() }
                val allIdols = idolDao.getIdolsByIds(chartIdolIds)

                // 재정렬 및 재랭킹
                val sortedIdols = allIdols.sortedByDescending { it.heart }
                val processedData = net.ib.mn.util.RankingUtil.processIdolsData(
                    idols = sortedIdols,
                    context = context,
                    mostIdolId = mostIdolId,
                    formatHeartCount = ::formatHeartCount
                )

                // 캐시 업데이트
                setChartData(chartCode, processedData)
                android.util.Log.d(TAG, "✅ $chartCode cache updated via UDP")
            }

            android.util.Log.d(TAG, "✅ UDP update complete")
        } catch (e: Exception) {
            android.util.Log.e(TAG, "❌ Failed to update from UDP: ${e.message}", e)
        }
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
