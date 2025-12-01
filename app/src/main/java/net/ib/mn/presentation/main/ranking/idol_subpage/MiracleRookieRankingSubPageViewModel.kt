package net.ib.mn.presentation.main.ranking.idol_subpage

import android.content.Context
import androidx.lifecycle.SavedStateHandle
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
import net.ib.mn.domain.model.ApiResult
import net.ib.mn.domain.ranking.RankingDataSource
import net.ib.mn.ui.components.RankingItem
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
 * SavedStateHandle을 사용하여 탭 선택을 저장:
 * - 앱을 내렸다 올려도 유지 (바텀 네비게이션 이동 시에도 유지)
 * - 앱을 재시작하면 리셋 (프로세스 종료 후)
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
    private val configsApi: net.ib.mn.data.remote.api.ConfigsApi,
    private val savedStateHandle: SavedStateHandle,
    private val preferencesManager: net.ib.mn.data.local.PreferencesManager,
    private val userCacheRepository: net.ib.mn.data.repository.UserCacheRepository
) : ViewModel() {

    sealed interface UiState {
        data object Loading : UiState
        data class Success(
            val items: List<RankingItem>,
            val bannerUrl: String? = null,
            val accumulatedChartCode: String? = null,
            val accumulatedBannerUrl: String? = null,
            val infoEventId: Int = 0
        ) : UiState
        data class Error(val message: String) : UiState
    }

    companion object {
        private const val KEY_SELECTED_TAB_INDEX = "selectedTabIndex"
        private const val DEFAULT_TAB_INDEX = 1  // 기본값: 실시간 랭킹
    }

    private val _uiState = MutableStateFlow<UiState>(UiState.Loading)
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    /**
     * 탭 선택 인덱스: 0 = 누적 랭킹, 1 = 실시간 랭킹
     * SavedStateHandle을 사용하여 바텀 네비게이션 이동 시에도 유지
     */
    val selectedTabIndex: StateFlow<Int> = savedStateHandle.getStateFlow(KEY_SELECTED_TAB_INDEX, DEFAULT_TAB_INDEX)

    /**
     * 탭 선택 변경
     */
    fun setSelectedTabIndex(index: Int) {
        savedStateHandle[KEY_SELECTED_TAB_INDEX] = index
    }

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
        loadConfigInfo()
        loadChartInfo()
        loadRankingData()

        // UDP updateEvent 구독 (MainScreen에서 heartbeat 관리)
        subscribeToUpdates()
    }

    /**
     * 화면이 보일 때 호출 - 데이터 새로고침
     */
    fun onScreenVisible() {
        isScreenVisible = true

        // DB에서 최신 데이터 로드
        val cachedIds = cachedIdolIds
        if (cachedIds != null && cachedIds.isNotEmpty()) {
            viewModelScope.launch(Dispatchers.IO) {
                queryIdolsByIdsFromDb(cachedIds)
            }
        }
    }

    /**
     * 화면이 사라질 때 호출
     */
    fun onScreenHidden() {
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
                    return@collect
                }


                // 캐시된 ID 리스트가 있으면 DB에서 전체 재조회
                val cachedIds = cachedIdolIds
                if (cachedIds != null && cachedIds.isNotEmpty()) {
                    // 변경된 아이돌 중 현재 차트에 포함된 아이돌이 있는지 확인
                    val hasRelevantChanges = changedIds.any { it in cachedIds }

                    if (hasRelevantChanges) {

                        launch(Dispatchers.IO) {
                            queryIdolsByIdsFromDb(cachedIds)
                        }
                    } else {
                    }
                }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        udpSubscriptionJob?.cancel()
    }

    /**
     * 캐시된 데이터가 있으면 사용하고, 없으면 새로 로드
     */
    fun reloadIfNeeded() {
        val cachedIds = cachedIdolIds
        if (cachedIds != null && cachedIds.isNotEmpty()) {
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


                    // 이미 Success 상태면 infoEventId 포함하여 재업데이트
                    val currentState = _uiState.value
                    if (currentState is UiState.Success) {
                        _uiState.value = currentState.copy(infoEventId = infoEventId)
                    }
                }
            } catch (e: Exception) {
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

                val response = chartsApi.getChartsCurrent()

                if (!response.isSuccessful || response.body()?.success != true) {
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
            }
        }
    }

    private fun loadRankingData() {
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.value = UiState.Loading


            // DataSource를 통해 idol_ids 로드
            dataSource.loadIdolIds(chartCode).collect { result ->
                when (result) {
                    is ApiResult.Loading -> {
                    }
                    is ApiResult.Success -> {
                        cachedIdolIds = result.data
                        queryIdolsByIdsFromDb(result.data)
                    }
                    is ApiResult.Error -> {
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
            val mostIdolId = userCacheRepository.getMostIdolId()

            val result = net.ib.mn.util.RankingUtil.processIdolsData(
                idols = idols,
                context = context,
                mostIdolId = mostIdolId,
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


            _uiState.value = UiState.Success(
                items = finalItems,
                bannerUrl = bannerUrl,
                accumulatedChartCode = accumulatedChartCode,
                accumulatedBannerUrl = accumulatedBannerUrl,
                infoEventId = infoEventId
            )
        } catch (e: Exception) {
            _uiState.value = UiState.Error(e.message ?: "Error")
        }
    }

    /**
     * 투표 성공 시 로컬 데이터 업데이트 및 재정렬
     */
    fun updateVote(idolId: Int, voteCount: Long) {
        val currentState = _uiState.value
        if (currentState !is UiState.Success) return


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
