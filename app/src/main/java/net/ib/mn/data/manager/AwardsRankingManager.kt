package net.ib.mn.data.manager

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import net.ib.mn.data.local.dao.IdolDao
import net.ib.mn.data.remote.dto.AwardIdolItem
import net.ib.mn.util.logD
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

/**
 * AwardsRankingManager - 어워즈 실시간 랭킹 관리
 *
 * 기능:
 * 1. API 성공 시 chartCode별 데이터 저장
 * 2. 2초마다 DB에서 최신 heart 값 조회 후 정렬 및 Flow emit
 * 3. UDP로 DB가 업데이트되면 자동으로 반영
 */
@Singleton
class AwardsRankingManager @Inject constructor(
    private val idolDao: IdolDao
) {

    companion object {
        private const val TAG = "AwardsRankingManager"
        private const val SORT_INTERVAL_MS = 2000L
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    // 차트별 원본 데이터 (heart 포함)
    private val chartDataMap = ConcurrentHashMap<String, MutableList<AwardIdolItem>>()

    // 차트별 idol IDs (API 응답 순서 유지)
    private val chartIdolIdsMap = ConcurrentHashMap<String, List<Int>>()

    // 정렬된 랭킹 Flow (chartCode -> 정렬된 리스트)
    private val _rankingFlow = MutableStateFlow<Map<String, List<AwardIdolItem>>>(emptyMap())
    val rankingFlow: StateFlow<Map<String, List<AwardIdolItem>>> = _rankingFlow.asStateFlow()

    // 특정 차트의 랭킹 Flow
    private val chartRankingFlows = ConcurrentHashMap<String, MutableStateFlow<List<AwardIdolItem>>>()

    // 정렬 Job 활성화 여부
    private var isSortingActive = false

    init {
        logD(TAG, "AwardsRankingManager initialized")
    }

    /**
     * API 성공 시 호출 - 차트 데이터 저장
     *
     * @param chartCode 차트 코드 (예: "MSM", "FSF")
     * @param items API 응답 아이템 리스트
     */
    fun updateChartData(chartCode: String, items: List<AwardIdolItem>) {
        logD(TAG, "updateChartData: chartCode=$chartCode, items=${items.size}")

        // idol IDs 저장
        val idolIds = items.map { it.id }
        chartIdolIdsMap[chartCode] = idolIds
        logD(TAG, "Saved idol IDs for $chartCode: $idolIds")

        // 데이터 저장 (mutable copy)
        chartDataMap[chartCode] = items.map { it.copy() }.toMutableList()

        // 개별 차트 Flow 초기화
        if (!chartRankingFlows.containsKey(chartCode)) {
            chartRankingFlows[chartCode] = MutableStateFlow(emptyList())
        }

        // 즉시 정렬 및 emit
        sortAndEmit()

        // 정렬 Job 시작 (아직 시작 안됐으면)
        startSortingJobIfNeeded()
    }

    /**
     * 특정 차트의 랭킹 Flow 가져오기
     */
    fun getChartRankingFlow(chartCode: String): StateFlow<List<AwardIdolItem>> {
        return chartRankingFlows.getOrPut(chartCode) {
            MutableStateFlow(emptyList())
        }.asStateFlow()
    }

    /**
     * heart 업데이트 (투표 완료 시 호출)
     * 로컬 DB 업데이트 후 DB 기준으로 리랭킹
     *
     * @param chartCode 차트 코드
     * @param idolId 아이돌 ID
     * @param heart 새로운 heart 값
     */
    fun updateHeart(chartCode: String, idolId: Int, heart: Long) {
        scope.launch(Dispatchers.IO) {
            try {
                // 로컬 DB 업데이트
                idolDao.updateIdolHeart(idolId, heart)
                logD(TAG, "Updated heart in DB: idolId=$idolId, heart=$heart")

                // DB 기준으로 리랭킹
                sortAndEmit()
            } catch (e: Exception) {
                logD(TAG, "Failed to update heart in DB: ${e.message}")
            }
        }
    }

    /**
     * 모든 데이터 삭제
     */
    fun clearAll() {
        chartDataMap.clear()
        chartIdolIdsMap.clear()
        chartRankingFlows.values.forEach { it.value = emptyList() }
        _rankingFlow.value = emptyMap()
        logD(TAG, "Cleared all data")
    }

    /**
     * 정렬 Job 시작 (한 번만)
     */
    private fun startSortingJobIfNeeded() {
        if (isSortingActive) return

        isSortingActive = true
        scope.launch {
            logD(TAG, "Starting sorting job (interval: ${SORT_INTERVAL_MS}ms)")
            while (true) {
                delay(SORT_INTERVAL_MS)
                sortAndEmit()
            }
        }
    }

    /**
     * DB에서 최신 heart 값 조회 후 정렬 및 Flow emit
     */
    private fun sortAndEmit() {
        val sortedMap = mutableMapOf<String, List<AwardIdolItem>>()

        chartDataMap.forEach { (chartCode, items) ->
            // DB에서 최신 heart 값 조회
            val idolIds = items.map { it.id }
            if (idolIds.isNotEmpty()) {
                try {
                    val dbIdols = runBlocking(Dispatchers.IO) {
                        idolDao.getIdolsByIds(idolIds)
                    }
                    val heartMap = dbIdols.associate { it.id to it.heart }

                    // 메모리 데이터에 DB heart 값 반영
                    items.forEach { item ->
                        heartMap[item.id]?.let { dbHeart ->
                            if (item.heart != dbHeart) {
                                item.heart = dbHeart
                                logD(TAG, "Updated heart from DB: id=${item.id}, heart=$dbHeart")
                            }
                        }
                    }
                } catch (e: Exception) {
                    logD(TAG, "Failed to fetch hearts from DB: ${e.message}")
                }
            }

            // heart 기준 내림차순 정렬 후 rank 재계산
            val sorted = items
                .sortedByDescending { it.heart }
                .mapIndexed { index, item ->
                    item.copy().apply { rank = index }
                }

            sortedMap[chartCode] = sorted

            // 개별 차트 Flow 업데이트
            chartRankingFlows[chartCode]?.value = sorted
        }

        // 전체 Flow 업데이트
        _rankingFlow.value = sortedMap
    }
}
