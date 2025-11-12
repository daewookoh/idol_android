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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import net.ib.mn.data.local.dao.IdolDao
import net.ib.mn.data.local.entity.IdolEntity
import net.ib.mn.domain.model.ApiResult
import net.ib.mn.domain.ranking.RankingDataSource
import net.ib.mn.ui.components.RankingItemData
import java.text.NumberFormat
import java.util.Locale

/**
 * 통합 랭킹 ViewModel (Global, Group, Solo 모두 지원)
 *
 * Strategy Pattern을 사용하여 세 개의 ViewModel을 하나로 통합:
 * - GlobalRankingSubPageViewModel
 * - GroupRankingSubPageViewModel
 * - SoloRankingSubPageViewModel
 *
 * 주요 기능:
 * 1. RankingDataSource를 통한 유연한 데이터 로딩
 * 2. UDP 리스닝 (화면 visible 시에만)
 * 3. 남녀 변경 지원 (dataSource에서 결정)
 * 4. 데이터 캐싱
 *
 * @param chartCode 초기 차트 코드
 * @param dataSource 랭킹 데이터 소스 (Global/Group/Solo)
 */
@HiltViewModel(assistedFactory = UnifiedRankingSubPageViewModel.Factory::class)
class UnifiedRankingSubPageViewModel @AssistedInject constructor(
    @Assisted private val chartCode: String,
    @Assisted private val dataSource: RankingDataSource,
    @ApplicationContext private val context: Context,
    private val idolDao: IdolDao,
    private val broadcastManager: net.ib.mn.data.remote.udp.IdolBroadcastManager,
    private val preferencesManager: net.ib.mn.data.local.PreferencesManager
) : ViewModel() {

    sealed interface UiState {
        data object Loading : UiState
        data class Success(
            val items: List<RankingItemData>,
            val topIdol: RankingItemData? = null
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

    private val logTag = "UnifiedRankingVM[${dataSource.type}]"

    init {
        android.util.Log.d(logTag, "🆕 ViewModel created for chartCode: $chartCode")
        loadRankingData()

        // UDP updateEvent 구독 (MainScreen에서 heartbeat 관리)
        subscribeToUpdates()
    }

    /**
     * 화면이 보일 때 호출 - 데이터 새로고침
     */
    fun onScreenVisible() {
        android.util.Log.d(logTag, "👁️ Screen became visible for chartCode: $currentChartCode")
        isScreenVisible = true

        // DB에서 최신 데이터 로드
        val cachedIds = codeToIdListMap[currentChartCode]
        if (cachedIds != null && cachedIds.isNotEmpty()) {
            android.util.Log.d(logTag, "🔄 Refreshing data from DB (${cachedIds.size} items)")
            viewModelScope.launch(Dispatchers.IO) {
                queryIdolsByIdsFromDb(cachedIds)
            }
        }
    }

    /**
     * 화면이 사라질 때 호출
     */
    fun onScreenHidden() {
        android.util.Log.d(logTag, "🙈 Screen hidden for chartCode: $currentChartCode")
        isScreenVisible = false
    }

    /**
     * UDP updateEvent 구독 (heartbeat는 MainScreen에서 관리)
     */
    private fun subscribeToUpdates() {
        udpSubscriptionJob = viewModelScope.launch {
            broadcastManager.updateEvent.collect { changedIds ->
                // 화면이 보이지 않으면 무시
                if (!isScreenVisible) {
                    android.util.Log.d(logTag, "⏭️ Screen not visible, ignoring UDP update")
                    return@collect
                }

                android.util.Log.d(logTag, "🔄 UDP update event received - ${changedIds.size} idols changed")

                // 현재 캐시된 ID 리스트가 있으면 DB에서 전체 재조회
                val cachedIds = codeToIdListMap[currentChartCode]
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

    override fun onCleared() {
        super.onCleared()
        udpSubscriptionJob?.cancel()
        android.util.Log.d(logTag, "♻️ ViewModel cleared")
    }

    /**
     * 남녀 변경 시 호출 - 새로운 차트 코드로 데이터 로드
     * (Group/Solo에서만 사용, Global은 reloadIfNeeded 사용)
     */
    fun reloadWithNewCode(newCode: String) {
        if (!dataSource.supportGenderChange()) {
            android.util.Log.w(logTag, "⚠️ Gender change not supported for ${dataSource.type}")
            return
        }

        android.util.Log.d(logTag, "🔄 Reloading with new code: $newCode (previous: $currentChartCode)")

        // 같은 코드면 캐시된 데이터 사용
        if (newCode == currentChartCode) {
            val cachedIds = codeToIdListMap[newCode]
            if (cachedIds != null && cachedIds.isNotEmpty()) {
                android.util.Log.d(logTag, "✓ Using cached data for $newCode")
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
            android.util.Log.d(logTag, "✓ Using cached data for $newCode")
            viewModelScope.launch(Dispatchers.IO) {
                queryIdolsByIdsFromDb(cachedIds)
            }
        } else {
            android.util.Log.d(logTag, "📡 Fetching new data for $newCode")
            loadRankingData()
        }
    }

    /**
     * 캐시된 데이터가 있으면 사용하고, 없으면 새로 로드
     * (Global에서 사용)
     */
    fun reloadIfNeeded() {
        val cachedIds = codeToIdListMap[currentChartCode]
        if (cachedIds != null && cachedIds.isNotEmpty()) {
            android.util.Log.d(logTag, "✓ Using cached data")
            viewModelScope.launch(Dispatchers.IO) {
                queryIdolsByIdsFromDb(cachedIds)
            }
        } else {
            loadRankingData()
        }
    }

    private fun loadRankingData() {
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.value = UiState.Loading

            android.util.Log.d(logTag, "========================================")
            android.util.Log.d(logTag, "[${dataSource.type}] Loading ranking data")
            android.util.Log.d(logTag, "  - currentChartCode: $currentChartCode")

            // DataSource를 통해 idol_ids 로드
            dataSource.loadIdolIds(currentChartCode).collect { result ->
                when (result) {
                    is ApiResult.Loading -> {
                        android.util.Log.d(logTag, "⏳ Loading...")
                    }
                    is ApiResult.Success -> {
                        android.util.Log.d(logTag, "✅ SUCCESS - IDs count: ${result.data.size}")
                        val ids = ArrayList(result.data)
                        codeToIdListMap[currentChartCode] = ids
                        queryIdolsByIdsFromDb(ids)
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

            // 최애 ID 가져오기
            val mostIdolId = preferencesManager.mostIdolId.first()
            android.util.Log.d(logTag, "💗 Most idol ID from PreferencesManager: $mostIdolId")

            val result = net.ib.mn.util.RankingUtil.processIdolsData(
                idols = idols,
                context = context,
                mostIdolId = mostIdolId,
                formatHeartCount = ::formatHeartCount
            )

            // isFavorite가 설정된 아이템 확인
            val favoriteItems = result.rankItems.filter { it.isFavorite }
            android.util.Log.d(logTag, "💗 Favorite items count: ${favoriteItems.size}")
            favoriteItems.forEach { item ->
                android.util.Log.d(logTag, "💗 Favorite item: id=${item.id}, name=${item.name}, rank=${item.rank}")
            }

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
                topIdol = finalItems.firstOrNull()
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

            // State 업데이트 -> 자동 리컴포지션
            _uiState.value = UiState.Success(
                items = finalItems,
                topIdol = finalItems.firstOrNull()
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
        ): UnifiedRankingSubPageViewModel
    }
}
