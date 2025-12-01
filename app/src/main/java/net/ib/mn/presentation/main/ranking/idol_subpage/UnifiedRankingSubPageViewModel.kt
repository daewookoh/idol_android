package net.ib.mn.presentation.main.ranking.idol_subpage

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.launch
import net.ib.mn.domain.ranking.RankingDataSource
import net.ib.mn.ui.components.RankingItem

/**
 * 통합 랭킹 ViewModel (Global, Group, Solo 모두 지원)
 *
 * ChartRankingRepository를 구독하여 Room DB 데이터를 실시간으로 표시
 *
 * 주요 기능:
 * 1. ChartRankingRepository 구독 → Room DB Flow 기반 실시간 데이터 반영
 * 2. 차트 변경 (남녀 토글) 지원
 * 3. 캐시 데이터 즉시 표시 (빠른 로딩)
 *
 * 데이터 로딩은 StartUpViewModel에서 처리:
 * - StartUp 시점에 5개 차트 데이터를 미리 DB에 저장
 * - API 호출 및 데이터 가공은 StartUpViewModel이 담당
 * - 이 ViewModel은 DB 데이터를 구독하여 표시만 함
 *
 * @param chartCode 초기 차트 코드
 * @param dataSource 랭킹 데이터 소스 (Global/Group/Solo)
 */
@HiltViewModel(assistedFactory = UnifiedRankingSubPageViewModel.Factory::class)
class UnifiedRankingSubPageViewModel @AssistedInject constructor(
    @Assisted private val chartCode: String,
    @Assisted private val dataSource: RankingDataSource,
    private val chartDatabaseRepository: net.ib.mn.data.repository.ChartRankingRepository
) : ViewModel() {

    sealed interface UiState {
        data object Loading : UiState
        data class Success(
            val items: List<RankingItem>,
            val topIdol: RankingItem? = null
        ) : UiState
        data class Error(val message: String) : UiState
    }

    private val _uiState = MutableStateFlow<UiState>(UiState.Loading)
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    // 현재 사용 중인 차트 코드 (남녀 변경 시 업데이트됨)
    private val _currentChartCode = MutableStateFlow(chartCode)
    private val currentChartCode: StateFlow<String> = _currentChartCode.asStateFlow()

    private val logTag = "UnifiedRankingVM[${dataSource.type}]"

    init {

        // 현재 차트만 구독하여 실시간 반영
        subscribeToCacheData()

        // 캐시에서 즉시 데이터 로드
        viewModelScope.launch {
            loadFromCache()
        }
    }

    /**
     * ChartRankingRepository를 구독하여 DB 변경 시 자동 업데이트
     *
     * flatMapLatest를 사용하여 currentChartCode가 변경되면
     * 자동으로 새로운 차트를 구독합니다.
     */
    private fun subscribeToCacheData() {
        viewModelScope.launch {
            _currentChartCode
                .flatMapLatest { code ->
                    chartDatabaseRepository.observeChartData(code)
                }
                .collect { processedData ->
                    val code = _currentChartCode.value

                    if (processedData != null) {
                        _uiState.value = UiState.Success(
                            items = processedData.rankItems,
                            topIdol = processedData.rankItems.firstOrNull()
                        )
                    } else {
                    }
                }
        }
    }

    /**
     * DB에서 데이터 로드 (즉시 표시)
     */
    private suspend fun loadFromCache() {
        val code = _currentChartCode.value
        val cachedData = chartDatabaseRepository.getChartData(code)
        if (cachedData != null) {
            _uiState.value = UiState.Success(
                items = cachedData.rankItems,
                topIdol = cachedData.rankItems.firstOrNull()
            )
        } else {
            _uiState.value = UiState.Loading
        }
    }

    /**
     * 화면이 보일 때 호출
     * 백그라운드에서 API 호출하여 DB 갱신
     */
    fun onScreenVisible() {
        val code = _currentChartCode.value

        // 백그라운드에서 API 호출하여 DB 갱신
        viewModelScope.launch {
            chartDatabaseRepository.refreshChart(code)
        }
    }

    /**
     * 화면이 사라질 때 호출
     * (DB Flow 구독 방식이므로 특별한 처리 불필요)
     */
    fun onScreenHidden() {
        val code = _currentChartCode.value
        // Flow 구독은 viewModelScope에 의해 자동 관리됨
    }

    /**
     * 남녀 변경 (차트 코드 변경)
     *
     * @param isMale true면 남자, false면 여자
     */
    fun changeGender(isMale: Boolean) {
        val currentCode = _currentChartCode.value

        // Global 랭킹은 변경 없음
        if (currentCode == "GLOBALS") {
            return
        }

        // 남녀 변경 지원하지 않으면 무시
        if (!dataSource.supportGenderChange()) {
            return
        }

        // 차트 코드 변환 (PR_S_F ↔ PR_S_M, PR_G_F ↔ PR_G_M)
        val newCode = when {
            currentCode.startsWith("PR_S_") -> if (isMale) "PR_S_M" else "PR_S_F"
            currentCode.startsWith("PR_G_") -> if (isMale) "PR_G_M" else "PR_G_F"
            else -> {
                return
            }
        }

        // 같은 코드면 무시
        if (newCode == currentCode) {
            return
        }


        // 새로운 코드로 업데이트 (flatMapLatest가 자동으로 새 차트 구독)
        _currentChartCode.value = newCode

        // 백그라운드에서 API 호출하여 새 차트 데이터 갱신
        viewModelScope.launch {
            chartDatabaseRepository.refreshChart(newCode)
        }

    }

    /**
     * 투표 후 호출 - DB 업데이트 및 재랭킹
     */
    fun updateVote(idolId: Int, votedHeart: Long) {
        val code = _currentChartCode.value

        // 백그라운드에서 DB 업데이트 및 재랭킹
        viewModelScope.launch {
            chartDatabaseRepository.updateVoteAndRerank(
                idolId = idolId,
                votedHeartCount = votedHeart,
                chartCode = code
            )
        }
    }

    /**
     * 필요 시 재로드 (더미 메서드 - 캐시 구독 방식이므로 자동 업데이트됨)
     */
    fun reloadIfNeeded() {
        // 캐시에서 다시 로드
        viewModelScope.launch {
            loadFromCache()
        }
    }

    /**
     * 새로운 차트 코드로 재로드 (더미 메서드 - changeGender 사용)
     */
    fun reloadWithNewCode(newChartCode: String) {
        // 차트 코드 변경 (flatMapLatest가 자동으로 새 차트 구독)
        _currentChartCode.value = newChartCode
    }

    @AssistedFactory
    interface Factory {
        fun create(
            chartCode: String,
            dataSource: RankingDataSource
        ): UnifiedRankingSubPageViewModel
    }
}
