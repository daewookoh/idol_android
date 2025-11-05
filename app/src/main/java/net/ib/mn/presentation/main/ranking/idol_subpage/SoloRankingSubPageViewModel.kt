package net.ib.mn.presentation.main.ranking.idol_subpage

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import net.ib.mn.data.local.dao.IdolDao
import net.ib.mn.data.local.entity.IdolEntity
import net.ib.mn.domain.model.ApiResult
import net.ib.mn.domain.repository.RankingRepository
import net.ib.mn.ui.components.RankingItemData
import net.ib.mn.util.IdolImageUtil
import java.text.Collator
import java.text.NumberFormat
import java.util.Locale

/**
 * Solo (개인) 랭킹 ViewModel
 *
 * charts/idol_ids/ API 사용
 * 남녀 변경에 영향을 받음
 */
@HiltViewModel(assistedFactory = SoloRankingSubPageViewModel.Factory::class)
class SoloRankingSubPageViewModel @AssistedInject constructor(
    @Assisted private val chartCode: String,
    @ApplicationContext private val context: Context,
    private val rankingRepository: RankingRepository,
    private val idolDao: IdolDao,
    private val broadcastManager: net.ib.mn.data.remote.udp.IdolBroadcastManager
) : ViewModel() {

    sealed interface UiState {
        data object Loading : UiState
        data class Success(
            val items: List<RankingItemData>,
            val topIdol: IdolEntity? = null
        ) : UiState
        data class Error(val message: String) : UiState
    }

    private val _uiState = MutableStateFlow<UiState>(UiState.Loading)
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    // 현재 사용 중인 차트 코드 (남녀 변경 시 업데이트됨)
    private var currentChartCode: String = chartCode

    // 코드별 캐시 (남녀 변경 시에도 이전 데이터 유지)
    private val codeToIdListMap = mutableMapOf<String, ArrayList<Int>>()

    // UDP 구독 Job (화면에 보일 때만 활성화)
    private var udpSubscriptionJob: Job? = null

    // 화면 가시성 상태
    private var isScreenVisible = false

    init {
        android.util.Log.d("SoloRankingVM", "🆕 ViewModel created for chartCode: $chartCode")
        loadRankingData()
    }

    /**
     * 화면이 보일 때 호출 - UDP 구독 시작 및 데이터 새로고침
     */
    fun onScreenVisible() {
        android.util.Log.d("SoloRankingVM", "👁️ Screen became visible for chartCode: $currentChartCode")
        isScreenVisible = true

        // DB에서 최신 데이터 로드
        val cachedIds = codeToIdListMap[currentChartCode]
        if (cachedIds != null && cachedIds.isNotEmpty()) {
            android.util.Log.d("SoloRankingVM", "🔄 Refreshing data from DB (${cachedIds.size} items)")
            viewModelScope.launch(Dispatchers.IO) {
                queryIdolsByIdsFromDb(cachedIds)
            }
        }

        // UDP 구독 시작
        startUdpSubscription()
    }

    /**
     * 화면이 사라질 때 호출 - UDP 구독 중지
     */
    fun onScreenHidden() {
        android.util.Log.d("SoloRankingVM", "🙈 Screen hidden for chartCode: $currentChartCode")
        isScreenVisible = false
        stopUdpSubscription()
    }

    /**
     * UDP 구독 시작
     */
    private fun startUdpSubscription() {
        // 이미 구독 중이면 중복 방지
        if (udpSubscriptionJob?.isActive == true) {
            android.util.Log.d("SoloRankingVM", "⚠️ UDP already subscribed, skipping")
            return
        }

        android.util.Log.d("SoloRankingVM", "📡 Starting UDP subscription")
        udpSubscriptionJob = viewModelScope.launch {
            broadcastManager.updateEvent.collect { changedIds ->
                // 화면이 보이지 않으면 무시
                if (!isScreenVisible) {
                    android.util.Log.d("SoloRankingVM", "⏭️ Screen not visible, ignoring UDP update")
                    return@collect
                }

                android.util.Log.d("SoloRankingVM", "🔄 UDP update event received - ${changedIds.size} idols changed")

                // 현재 캐시된 ID 리스트가 있으면 DB에서 전체 재조회
                // → 전체 순위 재계산 → data class의 equals로 변경된 아이템만 리컴포지션
                val cachedIds = codeToIdListMap[currentChartCode]
                if (cachedIds != null && cachedIds.isNotEmpty()) {
                    // 변경된 아이돌 중 현재 차트에 포함된 아이돌이 있는지 확인
                    val hasRelevantChanges = changedIds.any { it in cachedIds }

                    if (hasRelevantChanges) {
                        android.util.Log.d("SoloRankingVM", "📊 Reloading all ${cachedIds.size} idols from DB")
                        android.util.Log.d("SoloRankingVM", "   → Changed IDs in this chart: ${changedIds.filter { it in cachedIds }}")
                        android.util.Log.d("SoloRankingVM", "   → Full ranking recalculation (순위 변경 가능)")
                        android.util.Log.d("SoloRankingVM", "   → StateFlow emit → LazyColumn diff → 변경된 아이템만 리컴포지션")

                        launch(Dispatchers.IO) {
                            queryIdolsByIdsFromDb(cachedIds, isUdpUpdate = true)
                        }
                    } else {
                        android.util.Log.d("SoloRankingVM", "⏭️ No relevant changes for this chart - skipping update")
                    }
                }
            }
        }
    }

    /**
     * UDP 구독 중지
     */
    private fun stopUdpSubscription() {
        udpSubscriptionJob?.cancel()
        udpSubscriptionJob = null
        android.util.Log.d("SoloRankingVM", "🛑 Stopped UDP subscription")
    }

    override fun onCleared() {
        super.onCleared()
        stopUdpSubscription()
        android.util.Log.d("SoloRankingVM", "♻️ ViewModel cleared")
    }

    /**
     * 남녀 변경 시 호출 - 새로운 차트 코드로 데이터 로드
     */
    fun reloadWithNewCode(newCode: String) {
        android.util.Log.d("SoloRankingVM", "🔄 Reloading with new code: $newCode (previous: $currentChartCode)")

        // 같은 코드면 캐시된 데이터 사용
        if (newCode == currentChartCode) {
            val cachedIds = codeToIdListMap[newCode]
            if (cachedIds != null && cachedIds.isNotEmpty()) {
                android.util.Log.d("SoloRankingVM", "✓ Using cached data for $newCode")
                viewModelScope.launch(Dispatchers.IO) {
                    queryIdolsByIdsFromDb(cachedIds)
                }
                return
            }
        }

        // 새로운 코드로 업데이트하고 데이터 로드
        currentChartCode = newCode

        val cachedIds = codeToIdListMap[newCode]
        if (cachedIds != null && cachedIds.isNotEmpty()) {
            android.util.Log.d("SoloRankingVM", "✓ Using cached data for $newCode")
            viewModelScope.launch(Dispatchers.IO) {
                queryIdolsByIdsFromDb(cachedIds)
            }
        } else {
            android.util.Log.d("SoloRankingVM", "📡 Fetching new data for $newCode")
            loadRankingData()
        }
    }

    private fun loadRankingData() {
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.value = UiState.Loading

            android.util.Log.d("SoloRankingVM", "========================================")
            android.util.Log.d("SoloRankingVM", "[Solo] Loading ranking data")
            android.util.Log.d("SoloRankingVM", "  - currentChartCode: $currentChartCode")
            android.util.Log.d("SoloRankingVM", "  - API: charts/idol_ids/")

            // charts/idol_ids/ API 호출
            rankingRepository.getChartIdolIds(currentChartCode).collect { result ->
                when (result) {
                    is ApiResult.Loading -> {
                        android.util.Log.d("SoloRankingVM", "⏳ Loading...")
                    }
                    is ApiResult.Success -> {
                        android.util.Log.d("SoloRankingVM", "✅ SUCCESS - IDs count: ${result.data.size}")
                        val ids = ArrayList(result.data)
                        codeToIdListMap[currentChartCode] = ids
                        queryIdolsByIdsFromDb(ids)
                    }
                    is ApiResult.Error -> {
                        android.util.Log.e("SoloRankingVM", "❌ ERROR: ${result.message}")
                        _uiState.value = UiState.Error(result.message ?: "Error loading data")
                    }
                }
            }
        }
    }

    private suspend fun queryIdolsByIdsFromDb(ids: List<Int>, isUdpUpdate: Boolean = false) {
        if (ids.isEmpty()) {
            _uiState.value = UiState.Success(emptyList())
            return
        }

        try {
            val idols = idolDao.getIdolsByIds(ids)

            if (idols.isEmpty()) {
                _uiState.value = UiState.Error("DB에 아이돌 데이터가 없습니다.")
                return
            }

            // Heart 기준 정렬 및 순위 계산
            val sortedIdols = sortAndRankIdols(idols)

            // 프로그레스 바 계산을 위한 최대/최소 점수
            val maxScore = sortedIdols.maxOfOrNull { it.idol.heart } ?: 0L
            val minScore = sortedIdols.minOfOrNull { it.idol.heart } ?: 0L

            // RankingItemData로 변환
            val rankItems = sortedIdols.mapIndexed { index, idolWithRank ->
                val item = RankingItemData(
                    rank = idolWithRank.rank,
                    name = idolWithRank.idol.name,  // "이름_그룹명" 형식 그대로 사용
                    voteCount = formatHeartCount(idolWithRank.idol.heart.toInt()),
                    photoUrl = idolWithRank.idol.imageUrl,
                    id = idolWithRank.idol.id.toString(),
                    miracleCount = idolWithRank.idol.miracleCount,
                    fairyCount = idolWithRank.idol.fairyCount,
                    angelCount = idolWithRank.idol.angelCount,
                    rookieCount = idolWithRank.idol.rookieCount,
                    heartCount = idolWithRank.idol.heart,
                    maxHeartCount = maxScore,
                    minHeartCount = minScore,
                    top3ImageUrls = IdolImageUtil.getTop3ImageUrls(idolWithRank.idol),
                    top3VideoUrls = IdolImageUtil.getTop3VideoUrls(idolWithRank.idol)
                )

                // 첫 3개 아이템 상세 로그
                if (index < 3) {
                    android.util.Log.d("SoloRankingVM", "=== Item #$index ===")
                    android.util.Log.d("SoloRankingVM", "  DB idol.name: '${idolWithRank.idol.name}'")
                    android.util.Log.d("SoloRankingVM", "  DB idol.groupId: ${idolWithRank.idol.groupId}")
                    android.util.Log.d("SoloRankingVM", "  DB idol.heart: ${idolWithRank.idol.heart}")
                    android.util.Log.d("SoloRankingVM", "  → RankingItemData.name: '${item.name}'")
                    android.util.Log.d("SoloRankingVM", "  → RankingItemData.voteCount: '${item.voteCount}'")
                }

                item
            }

            android.util.Log.d("SoloRankingVM", "✅ Processed ${rankItems.size} items")

            _uiState.value = UiState.Success(
                items = rankItems,
                topIdol = sortedIdols.firstOrNull()?.idol
            )
        } catch (e: Exception) {
            android.util.Log.e("SoloRankingVM", "❌ Exception: ${e.message}", e)
            _uiState.value = UiState.Error(e.message ?: "Error")
        }
    }

    private fun sortAndRankIdols(idols: List<IdolEntity>): List<IdolWithRank> {
        val collator = Collator.getInstance(Locale.ROOT).apply {
            strength = Collator.PRIMARY
        }

        val sorted = idols.sortedWith(
            compareByDescending<IdolEntity> { it.heart }
                .thenComparator { a, b -> collator.compare(a.name, b.name) }
        )

        val result = mutableListOf<IdolWithRank>()
        sorted.forEachIndexed { index, idol ->
            val rank = if (index > 0 && sorted[index - 1].heart == idol.heart) {
                result[index - 1].rank
            } else {
                index + 1
            }
            result.add(IdolWithRank(idol, rank))
        }

        return result
    }

    private fun formatHeartCount(count: Int): String {
        return NumberFormat.getNumberInstance(Locale.US).format(count)
    }

    data class IdolWithRank(val idol: IdolEntity, val rank: Int)

    @AssistedFactory
    interface Factory {
        fun create(chartCode: String): SoloRankingSubPageViewModel
    }
}
