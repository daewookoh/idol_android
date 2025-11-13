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
import kotlinx.coroutines.launch
import net.ib.mn.domain.ranking.RankingDataSource
import net.ib.mn.ui.components.RankingItemData

/**
 * 통합 랭킹 ViewModel (Global, Group, Solo 모두 지원)
 *
 * RankingCacheRepository를 구독하여 캐시된 데이터만 표시하는 간소화된 버전
 *
 * 주요 기능:
 * 1. RankingCacheRepository 구독 → 실시간 데이터 반영
 * 2. 차트 변경 (남녀 토글) 지원
 * 3. 캐시 데이터 즉시 표시 (빠른 로딩)
 *
 * 데이터 로딩은 StartUpViewModel에서 처리:
 * - StartUp 시점에 5개 차트 데이터를 미리 캐싱
 * - API 호출 및 데이터 가공은 StartUpViewModel이 담당
 * - 이 ViewModel은 캐시된 데이터를 구독하여 표시만 함
 *
 * @param chartCode 초기 차트 코드
 * @param dataSource 랭킹 데이터 소스 (Global/Group/Solo)
 */
@HiltViewModel(assistedFactory = UnifiedRankingSubPageViewModel.Factory::class)
class UnifiedRankingSubPageViewModel @AssistedInject constructor(
    @Assisted private val chartCode: String,
    @Assisted private val dataSource: RankingDataSource,
    private val rankingCacheRepository: net.ib.mn.data.repository.RankingCacheRepository
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

    private val logTag = "UnifiedRankingVM[${dataSource.type}]"

    init {
        android.util.Log.d(logTag, "🆕 ViewModel created for chartCode: $chartCode")

        // 캐시 데이터를 구독하여 실시간 반영
        subscribeToCacheData()

        // 캐시에서 즉시 데이터 로드
        loadFromCache()
    }

    /**
     * RankingCacheRepository를 구독하여 캐시 변경 시 자동 업데이트
     */
    private fun subscribeToCacheData() {
        // 모든 차트 코드 변경을 감지하기 위해 각 차트별로 구독
        listOf("PR_S_F", "PR_S_M", "PR_G_F", "PR_G_M", "GLOBALS").forEach { code ->
            viewModelScope.launch {
                android.util.Log.d(logTag, "👂 Starting subscription for chart: $code")
                rankingCacheRepository.observeChartData(code).collect { processedData ->
                    android.util.Log.d(logTag, "📥 Received update for chart $code: data=${processedData?.rankItems?.size} items, currentChartCode=$currentChartCode")

                    // 현재 활성화된 차트 코드와 일치하는 경우에만 UI 업데이트
                    if (code == currentChartCode && processedData != null) {
                        android.util.Log.d(logTag, "🔄 Cache updated for $code: ${processedData.rankItems.size} items - UPDATING UI")
                        _uiState.value = UiState.Success(
                            items = processedData.rankItems,
                            topIdol = processedData.rankItems.firstOrNull()
                        )
                        android.util.Log.d(logTag, "✅ UI state updated successfully")
                    } else {
                        android.util.Log.d(logTag, "⏭️ Skipping UI update: code mismatch or null data")
                    }
                }
            }
        }
    }

    /**
     * 캐시에서 데이터 로드 (즉시 표시)
     */
    private fun loadFromCache() {
        val cachedData = rankingCacheRepository.getChartData(currentChartCode)
        if (cachedData != null) {
            android.util.Log.d(logTag, "✅ Loaded from cache: ${cachedData.rankItems.size} items")
            _uiState.value = UiState.Success(
                items = cachedData.rankItems,
                topIdol = cachedData.rankItems.firstOrNull()
            )
        } else {
            android.util.Log.d(logTag, "⚠️ No cache available for $currentChartCode - showing loading state")
            _uiState.value = UiState.Loading
        }
    }

    /**
     * 화면이 보일 때 호출
     * 백그라운드에서 API 호출하여 캐시 갱신
     */
    fun onScreenVisible() {
        android.util.Log.d(logTag, "👁️ Screen became visible for chartCode: $currentChartCode")

        // 백그라운드에서 API 호출하여 캐시 갱신
        viewModelScope.launch {
            rankingCacheRepository.refreshChartData(currentChartCode)
        }
    }

    /**
     * 화면이 사라질 때 호출
     * (캐시 구독 방식이므로 특별한 처리 불필요)
     */
    fun onScreenHidden() {
        android.util.Log.d(logTag, "🙈 Screen hidden for chartCode: $currentChartCode")
        // Flow 구독은 viewModelScope에 의해 자동 관리됨
    }

    /**
     * 남녀 변경 (차트 코드 변경)
     *
     * @param isMale true면 남자, false면 여자
     */
    fun changeGender(isMale: Boolean) {
        android.util.Log.d(logTag, "🔄 Changing gender to ${if (isMale) "Male" else "Female"}")

        // Global 랭킹은 변경 없음
        if (currentChartCode == "GLOBALS") {
            android.util.Log.d(logTag, "⚠️ Global ranking doesn't support gender change")
            return
        }

        // 남녀 변경 지원하지 않으면 무시
        if (!dataSource.supportGenderChange()) {
            android.util.Log.d(logTag, "⚠️ This data source doesn't support gender change")
            return
        }

        // 차트 코드 변환 (PR_S_F ↔ PR_S_M, PR_G_F ↔ PR_G_M)
        val newCode = when {
            currentChartCode.startsWith("PR_S_") -> if (isMale) "PR_S_M" else "PR_S_F"
            currentChartCode.startsWith("PR_G_") -> if (isMale) "PR_G_M" else "PR_G_F"
            else -> {
                android.util.Log.e(logTag, "❌ Unknown chart code pattern: $currentChartCode")
                return
            }
        }

        // 같은 코드면 무시
        if (newCode == currentChartCode) {
            android.util.Log.d(logTag, "⚠️ Same code, ignoring: $newCode")
            return
        }

        android.util.Log.d(logTag, "🔄 Changing chartCode: $currentChartCode → $newCode")

        // 새로운 코드로 업데이트
        currentChartCode = newCode

        // 캐시에서 즉시 로드
        loadFromCache()

        // 백그라운드에서 API 호출하여 새 차트 데이터 갱신
        viewModelScope.launch {
            rankingCacheRepository.refreshChartData(newCode)
        }

        android.util.Log.d(logTag, "✅ Gender changed to $newCode")
    }

    /**
     * 투표 후 호출 - 캐시 업데이트 및 재랭킹
     */
    fun updateVote(idolId: Int, votedHeart: Long) {
        android.util.Log.d(logTag, "📊 Vote updated: idolId=$idolId, hearts=$votedHeart, chartCode=$currentChartCode")

        // 백그라운드에서 캐시 업데이트 및 재랭킹
        viewModelScope.launch {
            android.util.Log.d(logTag, "🚀 Starting updateVoteAndRefreshCache...")
            rankingCacheRepository.updateVoteAndRefreshCache(
                chartCode = currentChartCode,
                idolId = idolId,
                voteCount = votedHeart
            )
            android.util.Log.d(logTag, "✅ updateVoteAndRefreshCache completed")
        }
    }

    /**
     * 필요 시 재로드 (더미 메서드 - 캐시 구독 방식이므로 자동 업데이트됨)
     */
    fun reloadIfNeeded() {
        android.util.Log.d(logTag, "🔄 Reload requested")
        // 캐시에서 다시 로드
        loadFromCache()
    }

    /**
     * 새로운 차트 코드로 재로드 (더미 메서드 - changeGender 사용)
     */
    fun reloadWithNewCode(newChartCode: String) {
        android.util.Log.d(logTag, "🔄 Reload with new code: $newChartCode")
        // 차트 코드 변경
        currentChartCode = newChartCode
        loadFromCache()
    }

    @AssistedFactory
    interface Factory {
        fun create(
            chartCode: String,
            dataSource: RankingDataSource
        ): UnifiedRankingSubPageViewModel
    }
}
