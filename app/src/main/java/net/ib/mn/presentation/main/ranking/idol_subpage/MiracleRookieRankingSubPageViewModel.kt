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
import net.ib.mn.domain.model.ApiResult
import net.ib.mn.domain.ranking.RankingDataSource
import net.ib.mn.ui.components.RankingItemData
import java.text.NumberFormat
import java.util.Locale

/**
 * 통합 Miracle/Rookie 랭킹 ViewModel
 *
 * 주요 기능:
 * 1. RankingDataSource를 통한 데이터 로딩
 * 2. UDP 리스닝 (화면 visible 시에만)
 * 3. 성별 변경 미지원 (고정된 차트 코드 사용)
 * 4. 단순 캐싱
 *
 * @param chartCode 차트 코드 (고정, 성별 변경 없음)
 * @param dataSource 랭킹 데이터 소스 (Miracle/Rookie)
 */
@HiltViewModel(assistedFactory = MiracleRookieRankingSubPageViewModel.Factory::class)
class MiracleRookieRankingSubPageViewModel @AssistedInject constructor(
    @Assisted private val chartCode: String,
    @Assisted private val dataSource: RankingDataSource,
    @ApplicationContext private val context: Context,
    private val idolDao: IdolDao,
    private val broadcastManager: net.ib.mn.data.remote.udp.IdolBroadcastManager,
    private val chartsApi: net.ib.mn.data.remote.api.ChartsApi,
    private val configsApi: net.ib.mn.data.remote.api.ConfigsApi
) : ViewModel() {

    sealed interface UiState {
        data object Loading : UiState
        data class Success(
            val items: List<RankingItemData>,
            val bannerUrl: String? = null,
            val accumulatedChartCode: String? = null,
            val accumulatedBannerUrl: String? = null,
            val infoEventId: Int = 0
        ) : UiState
        data class Error(val message: String) : UiState
    }

    private val _uiState = MutableStateFlow<UiState>(UiState.Loading)
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    // 캐시된 아이돌 ID 리스트
    private var cachedIdolIds: List<Int>? = null

    // UDP 구독 Job (화면에 보일 때만 활성화)
    private var udpSubscriptionJob: Job? = null

    // 화면 가시성 상태
    private var isScreenVisible = false

    // 배너 URL, 누적 차트 정보, 정보 이벤트 ID (한 번만 로드)
    private var bannerUrl: String? = null
    private var accumulatedChartCode: String? = null
    private var accumulatedBannerUrl: String? = null
    private var infoEventId: Int = 0

    private val logTag = "MiracleRookieVM[${dataSource.type}]"

    init {
        android.util.Log.d(logTag, "🆕 ViewModel created for chartCode: $chartCode")
        loadConfigInfo()
        loadChartInfo()
        loadRankingData()
    }

    /**
     * 화면이 보일 때 호출 - UDP 구독 시작 및 데이터 새로고침
     */
    fun onScreenVisible() {
        android.util.Log.d(logTag, "👁️ Screen became visible")
        isScreenVisible = true

        // DB에서 최신 데이터 로드
        val cachedIds = cachedIdolIds
        if (cachedIds != null && cachedIds.isNotEmpty()) {
            android.util.Log.d(logTag, "🔄 Refreshing data from DB (${cachedIds.size} items)")
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
        android.util.Log.d(logTag, "🙈 Screen hidden")
        isScreenVisible = false
        stopUdpSubscription()
    }

    /**
     * UDP 구독 시작
     */
    private fun startUdpSubscription() {
        // 이미 구독 중이면 중복 방지
        if (udpSubscriptionJob?.isActive == true) {
            android.util.Log.d(logTag, "⚠️ UDP already subscribed, skipping")
            return
        }

        android.util.Log.d(logTag, "📡 Starting UDP subscription")
        udpSubscriptionJob = viewModelScope.launch {
            broadcastManager.updateEvent.collect { changedIds ->
                // 화면이 보이지 않으면 무시
                if (!isScreenVisible) {
                    android.util.Log.d(logTag, "⏭️ Screen not visible, ignoring UDP update")
                    return@collect
                }

                android.util.Log.d(logTag, "🔄 UDP update event received - ${changedIds.size} idols changed")

                // 캐시된 ID 리스트가 있으면 DB에서 전체 재조회
                val cachedIds = cachedIdolIds
                if (cachedIds != null && cachedIds.isNotEmpty()) {
                    // 변경된 아이돌 중 현재 차트에 포함된 아이돌이 있는지 확인
                    val hasRelevantChanges = changedIds.any { it in cachedIds }

                    if (hasRelevantChanges) {
                        android.util.Log.d(logTag, "📊 Reloading all ${cachedIds.size} idols from DB")
                        android.util.Log.d(logTag, "   → Changed IDs in this chart: ${changedIds.filter { it in cachedIds }}")
                        android.util.Log.d(logTag, "   → Full ranking recalculation (순위 변경 가능)")

                        launch(Dispatchers.IO) {
                            queryIdolsByIdsFromDb(cachedIds)
                        }
                    } else {
                        android.util.Log.d(logTag, "⏭️ No relevant changes for this chart - skipping update")
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
        android.util.Log.d(logTag, "🛑 Stopped UDP subscription")
    }

    override fun onCleared() {
        super.onCleared()
        stopUdpSubscription()
        android.util.Log.d(logTag, "♻️ ViewModel cleared")
    }

    /**
     * 캐시된 데이터가 있으면 사용하고, 없으면 새로 로드
     */
    fun reloadIfNeeded() {
        val cachedIds = cachedIdolIds
        if (cachedIds != null && cachedIds.isNotEmpty()) {
            android.util.Log.d(logTag, "✓ Using cached data")
            viewModelScope.launch(Dispatchers.IO) {
                queryIdolsByIdsFromDb(cachedIds)
            }
        } else {
            loadRankingData()
        }
    }

    /**
     * configs/self/ API를 호출하여 정보 이벤트 ID 로드
     */
    private fun loadConfigInfo() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val response = configsApi.getConfigSelf()

                if (response.isSuccessful && response.body()?.success == true) {
                    val config = response.body()

                    // dataSource.type에 따라 적절한 정보 ID 선택
                    infoEventId = when (dataSource.type) {
                        "Miracle" -> config?.showMiracleInfo ?: 0
                        "Rookie" -> config?.showRookieInfo ?: 0
                        else -> 0
                    }

                    android.util.Log.d(logTag, "✅ Config info loaded: infoEventId=$infoEventId")

                    // 이미 Success 상태면 infoEventId 포함하여 재업데이트
                    val currentState = _uiState.value
                    if (currentState is UiState.Success) {
                        _uiState.value = currentState.copy(infoEventId = infoEventId)
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e(logTag, "❌ Exception loading config info: ${e.message}", e)
            }
        }
    }

    /**
     * charts/current/ API를 호출하여 배너 URL 및 누적 차트 정보 로드
     * Old 프로젝트의 MiracleMainFragment 로직 기반:
     * - realTimeChartModel.imageUrl -> 실시간 배너
     * - accumulateChartModel?.imageRankUrl -> 누적 배너
     * - aggregateType "A" = 누적, "D" = 실시간
     */
    private fun loadChartInfo() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                android.util.Log.d(logTag, "🖼️ Loading chart info from charts/current/ API")

                val response = chartsApi.getChartsCurrent()

                if (!response.isSuccessful || response.body()?.success != true) {
                    android.util.Log.e(logTag, "❌ Failed to load chart info: ${response.code()}")
                    return@launch
                }

                val chartModels = response.body()?.objects ?: emptyList()

                // 현재 차트 타입에 맞는 ChartModel 찾기
                // API는 "M"/"R"을 사용하므로 변환 필요
                val targetType = when (dataSource.type) {
                    "Miracle" -> "M"
                    "Rookie" -> "R"
                    else -> dataSource.type
                }

                // API는 aggregateType=[D, A]로 한 차트에 실시간/누적 모두 포함
                // targetType과 일치하는 차트를 찾고, 해당 차트에서:
                // - imageUrl: 실시간 배너
                // - imageRankUrl: 누적 배너
                val targetChart = chartModels.find { chart ->
                    chart.type.equals(targetType, ignoreCase = true) &&
                    chart.aggregateType?.contains("D") == true &&
                    chart.aggregateType?.contains("A") == true
                }

                // 배너 URL 저장
                bannerUrl = targetChart?.imageUrl  // 실시간 배너
                accumulatedChartCode = targetChart?.code
                accumulatedBannerUrl = targetChart?.imageRankUrl  // 누적 배너

                // 이미 Success 상태면 배너 URL 포함하여 재업데이트
                val currentState = _uiState.value
                if (currentState is UiState.Success) {
                    _uiState.value = currentState.copy(
                        bannerUrl = bannerUrl,
                        accumulatedChartCode = accumulatedChartCode,
                        accumulatedBannerUrl = accumulatedBannerUrl,
                        infoEventId = infoEventId
                    )
                }

            } catch (e: Exception) {
                android.util.Log.e(logTag, "❌ Exception loading chart info: ${e.message}", e)
            }
        }
    }

    private fun loadRankingData() {
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.value = UiState.Loading

            android.util.Log.d(logTag, "========================================")
            android.util.Log.d(logTag, "[${dataSource.type}] Loading ranking data")
            android.util.Log.d(logTag, "  - chartCode: $chartCode")

            // DataSource를 통해 idol_ids 로드
            dataSource.loadIdolIds(chartCode).collect { result ->
                when (result) {
                    is ApiResult.Loading -> {
                        android.util.Log.d(logTag, "⏳ Loading...")
                    }
                    is ApiResult.Success -> {
                        android.util.Log.d(logTag, "✅ SUCCESS - IDs count: ${result.data.size}")
                        cachedIdolIds = result.data
                        queryIdolsByIdsFromDb(result.data)
                    }
                    is ApiResult.Error -> {
                        android.util.Log.e(logTag, "❌ ERROR: ${result.message}")
                        _uiState.value = UiState.Error(result.message ?: "Error loading data")
                    }
                }
            }
        }
    }

    private suspend fun queryIdolsByIdsFromDb(ids: List<Int>) {
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

            val result = net.ib.mn.util.RankingUtil.processIdolsData(
                idols = idols,
                formatHeartCount = ::formatHeartCount
            )

            // 정렬 및 순위 계산
            val sortedItems = net.ib.mn.util.RankingUtil.sortAndRank(result.rankItems)

            // max/min 하트 수 계산
            val maxHeart = sortedItems.maxOfOrNull { it.heartCount } ?: 0L
            val minHeart = sortedItems.minOfOrNull { it.heartCount } ?: 0L

            // 모든 아이템에 max/min 적용
            val finalItems = sortedItems.map { item ->
                item.copy(
                    maxHeartCount = maxHeart,
                    minHeartCount = minHeart
                )
            }

            android.util.Log.d(logTag, "✅ Processed ${finalItems.size} items (sorted, max=$maxHeart, min=$minHeart)")

            _uiState.value = UiState.Success(
                items = finalItems,
                bannerUrl = bannerUrl,
                accumulatedChartCode = accumulatedChartCode,
                accumulatedBannerUrl = accumulatedBannerUrl,
                infoEventId = infoEventId
            )
        } catch (e: Exception) {
            android.util.Log.e(logTag, "❌ Exception: ${e.message}", e)
            _uiState.value = UiState.Error(e.message ?: "Error")
        }
    }

    /**
     * 투표 성공 시 로컬 데이터 업데이트 및 재정렬
     */
    fun updateVote(idolId: Int, voteCount: Long) {
        val currentState = _uiState.value
        if (currentState !is UiState.Success) return

        android.util.Log.d(logTag, "💗 Updating vote: idol=$idolId, votes=$voteCount")

        viewModelScope.launch(Dispatchers.IO) {
            // RankingUtil을 사용하여 투표 업데이트 및 재정렬 (DB + 메모리)
            val finalItems = net.ib.mn.util.RankingUtil.updateVoteAndRerank(
                items = currentState.items,
                idolId = idolId,
                voteCount = voteCount,
                idolDao = idolDao,
                formatHeartCount = { count -> formatHeartCount(count.toInt()) }
            )

            // State 업데이트 -> 자동 리컴포지션 (배너 정보 유지)
            _uiState.value = UiState.Success(
                items = finalItems,
                bannerUrl = bannerUrl,
                accumulatedChartCode = accumulatedChartCode,
                accumulatedBannerUrl = accumulatedBannerUrl,
                infoEventId = infoEventId
            )

            val maxHeart = finalItems.firstOrNull()?.maxHeartCount ?: 0L
            val minHeart = finalItems.firstOrNull()?.minHeartCount ?: 0L
            android.util.Log.d(logTag, "✅ Vote updated and re-ranked (${finalItems.size} items)")
            android.util.Log.d(logTag, "   → New max: $maxHeart, min: $minHeart")
        }
    }

    private fun formatHeartCount(count: Int): String {
        return NumberFormat.getNumberInstance(Locale.US).format(count)
    }

    @AssistedFactory
    interface Factory {
        fun create(
            chartCode: String,
            dataSource: RankingDataSource
        ): MiracleRookieRankingSubPageViewModel
    }
}
