package net.ib.mn.presentation.community

import androidx.lifecycle.SavedStateHandle
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
import net.ib.mn.BuildConfig
import net.ib.mn.domain.model.ApiResult
import net.ib.mn.domain.model.DailyRankingHistoryModel
import net.ib.mn.domain.repository.TrendsRepository

/**
 * DailyRankingHistory 화면의 UI 상태
 */
sealed interface DailyRankingHistoryUiState {
    data object Loading : DailyRankingHistoryUiState
    data class Success(
        val items: List<DailyRankingHistoryModel>,
        val totalCount: Int = 0
    ) : DailyRankingHistoryUiState
    data class Error(val message: String) : DailyRankingHistoryUiState
}

/**
 * DailyRankingHistory ViewModel
 *
 * Old 프로젝트의 HallOfFameTopHistoryActivity를 참고하여 작성
 * 특정 날짜의 전체 랭킹 히스토리
 */
@HiltViewModel(assistedFactory = DailyRankingHistoryViewModel.Factory::class)
class DailyRankingHistoryViewModel @AssistedInject constructor(
    private val trendsRepository: TrendsRepository,
    @Assisted private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    @AssistedFactory
    interface Factory {
        fun create(savedStateHandle: SavedStateHandle): DailyRankingHistoryViewModel
    }

    private val historyParam: String = savedStateHandle.get<String>("historyParam") ?: ""
    private val type: String = savedStateHandle.get<String>("type") ?: ""
    private val category: String = savedStateHandle.get<String>("category") ?: ""
    private val chartCode: String = savedStateHandle.get<String>("chartCode") ?: ""

    private val _uiState = MutableStateFlow<DailyRankingHistoryUiState>(DailyRankingHistoryUiState.Loading)
    val uiState: StateFlow<DailyRankingHistoryUiState> = _uiState.asStateFlow()

    init {
        if (historyParam.isNotEmpty()) {
            loadDailyRankingHistory()
        } else {
            _uiState.value = DailyRankingHistoryUiState.Error("Invalid parameters")
        }
    }

    /**
     * 일일 랭킹 히스토리 데이터 로드
     *
     * BuildConfig.CELEB에 따라 분기:
     * - CELEB=true: type + category 사용
     * - CELEB=false: chartCode 사용
     */
    private fun loadDailyRankingHistory() {
        viewModelScope.launch {
            trendsRepository.getDailyRankingHistory(
                historyParam = historyParam,
                type = if (BuildConfig.CELEB) type else "",
                category = if (BuildConfig.CELEB) category else "",
                chartCode = if (BuildConfig.CELEB) "" else chartCode
            ).collect { result ->
                _uiState.value = when (result) {
                    is ApiResult.Loading -> DailyRankingHistoryUiState.Loading
                    is ApiResult.Success -> DailyRankingHistoryUiState.Success(
                        items = result.data.items,
                        totalCount = result.data.totalCount
                    )
                    is ApiResult.Error -> DailyRankingHistoryUiState.Error(
                        result.message ?: "Unknown error"
                    )
                }
            }
        }
    }
}
