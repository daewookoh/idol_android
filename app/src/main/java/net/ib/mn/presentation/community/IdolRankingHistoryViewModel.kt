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
import net.ib.mn.domain.model.ApiResult
import net.ib.mn.domain.model.IdolRankingHistoryModel
import net.ib.mn.domain.repository.TrendsRepository
import net.ib.mn.util.logD
import net.ib.mn.util.logE

/**
 * IdolRankingHistory 화면의 UI 상태
 */
sealed interface IdolRankingHistoryUiState {
    data object Loading : IdolRankingHistoryUiState
    data class Success(
        val items: List<IdolRankingHistoryModel>,
        val averageHeart: Long = 0
    ) : IdolRankingHistoryUiState
    data class Error(val message: String) : IdolRankingHistoryUiState
}

/**
 * IdolRankingHistory ViewModel
 *
 * Old 프로젝트의 HallOfFameAggHistoryActivity를 참고하여 작성
 * 아이돌의 누적 순위 변동 히스토리
 */
@HiltViewModel(assistedFactory = IdolRankingHistoryViewModel.Factory::class)
class IdolRankingHistoryViewModel @AssistedInject constructor(
    private val trendsRepository: TrendsRepository,
    @Assisted private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    @AssistedFactory
    interface Factory {
        fun create(savedStateHandle: SavedStateHandle): IdolRankingHistoryViewModel
    }

    private val idolId: Int = savedStateHandle.get<Int>("idolId") ?: 0

    private val _uiState = MutableStateFlow<IdolRankingHistoryUiState>(IdolRankingHistoryUiState.Loading)
    val uiState: StateFlow<IdolRankingHistoryUiState> = _uiState.asStateFlow()

    init {
        if (idolId > 0) {
            loadIdolRankingHistory()
        } else {
            _uiState.value = IdolRankingHistoryUiState.Error("Invalid idol ID")
        }
    }

    /**
     * 랭킹 히스토리 데이터 로드
     *
     * Old 프로젝트의 HallOfFameAggHistoryActivity.onCreate() 참고
     */
    fun loadIdolRankingHistory() {
        viewModelScope.launch {
            trendsRepository.getIdolRankingHistory(idolId).collect { result ->
                when (result) {
                    is ApiResult.Loading -> {
                        _uiState.value = IdolRankingHistoryUiState.Loading
                    }
                    is ApiResult.Success -> {
                        val items = result.data.items
                        logD("IdolRankingHistoryVM: Loaded ${items.size} items")

                        // 평균 하트 수 계산 (Old 프로젝트와 동일)
                        val totalHeart = items.sumOf { it.heart }
                        val averageHeart = if (items.isNotEmpty()) {
                            Math.round(totalHeart.toDouble() / items.size)
                        } else {
                            0L
                        }

                        _uiState.value = IdolRankingHistoryUiState.Success(
                            items = items,
                            averageHeart = averageHeart
                        )
                    }
                    is ApiResult.Error -> {
                        logE("IdolRankingHistoryVM: API error: ${result.message}")
                        _uiState.value = IdolRankingHistoryUiState.Error(result.message ?: "Unknown error")
                    }
                }
            }
        }
    }
}
