package net.ib.mn.presentation.main.ranking.idol_subpage

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import net.ib.mn.domain.model.ApiResult
import net.ib.mn.domain.repository.RankingRepository

/**
 * 명예전당 - 30일 누적 순위 ViewModel
 *
 * charts/ranks/ API를 호출하여 30일 누적 순위 데이터를 가져옵니다.
 */
@HiltViewModel(assistedFactory = HallOfFameRankingSecondSubAccumulativePageViewModel.Factory::class)
class HallOfFameRankingSecondSubAccumulativePageViewModel @AssistedInject constructor(
    @Assisted private val chartCode: String,
    @Assisted private val exoTabSwitchType: Int,
    private val rankingRepository: RankingRepository,
    private val preferencesManager: net.ib.mn.data.local.PreferencesManager
) : ViewModel() {

    @AssistedFactory
    interface Factory {
        fun create(chartCode: String, exoTabSwitchType: Int): HallOfFameRankingSecondSubAccumulativePageViewModel
    }

    private val _rankingData = MutableStateFlow<List<net.ib.mn.data.remote.dto.AggregateRankModel>>(emptyList())
    val rankingData: StateFlow<List<net.ib.mn.data.remote.dto.AggregateRankModel>> = _rankingData.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    // CDN URL (PreferencesManager에서 가져옴, 기본값: https://hswnpegrwdch3017979.gcdn.ntruss.com)
    val cdnUrl: StateFlow<String> = preferencesManager.cdnUrl
        .map { it ?: "https://hswnpegrwdch3017979.gcdn.ntruss.com" }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = "https://hswnpegrwdch3017979.gcdn.ntruss.com"
        )

    init {

        loadData()
    }

    fun loadData(newChartCode: String? = null) {
        val codeToUse = newChartCode ?: chartCode

        viewModelScope.launch {

            rankingRepository.getChartRanks(codeToUse).collect { result ->
                when (result) {
                    is ApiResult.Loading -> {
                        _isLoading.value = true
                        _error.value = null
                    }
                    is ApiResult.Success -> {
                        _isLoading.value = false
                        _error.value = null

                        // 급상승 표시 계산 (old 프로젝트 HallOfFameAggFragment 로직과 동일)
                        val processedData = calculateSuddenIncrease(result.data)
                        _rankingData.value = processedData

                    }
                    is ApiResult.Error -> {
                        _isLoading.value = false
                        _error.value = result.message ?: "Unknown error"
                    }
                }
            }
        }
    }

    /**
     * 급상승 표시 계산
     *
     * old 프로젝트 HallOfFameAggFragment의 로직과 동일:
     * 1. difference 값으로 내림차순 정렬
     * 2. status == "increase"인 아이템 중 가장 높은 difference 찾기
     * 3. 최대 difference와 동일한 값을 가진 모든 아이템을 suddenIncrease = true로 설정
     *
     * @param data 원본 랭킹 데이터
     * @return 급상승 표시가 설정된 랭킹 데이터
     */
    private fun calculateSuddenIncrease(
        data: List<net.ib.mn.data.remote.dto.AggregateRankModel>
    ): List<net.ib.mn.data.remote.dto.AggregateRankModel> {
        if (data.isEmpty()) return data

        // 1. difference로 내림차순 정렬
        val sortedByDiff = data.sortedWith(
            compareByDescending<net.ib.mn.data.remote.dto.AggregateRankModel> { it.difference }
                .thenBy { it.scoreRank }
        )

        // 2. status == "increase"인 아이템 중 최대 difference 찾기
        val maxDifference = sortedByDiff
            .firstOrNull { it.status.equals("increase", ignoreCase = true) }
            ?.difference

        if (maxDifference == null || maxDifference <= 0) {
            return data
        }


        // 3. 최대 difference와 동일한 값을 가진 모든 "increase" 아이템을 suddenIncrease = true로 설정
        return data.map { item ->
            if (item.status.equals("increase", ignoreCase = true) && item.difference == maxDifference) {
                item.copy(suddenIncrease = true)
            } else {
                item
            }
        }
    }
}
