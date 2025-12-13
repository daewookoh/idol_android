package net.ib.mn.presentation.community.history.idol

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
import net.ib.mn.domain.repository.IdolRepository
import net.ib.mn.domain.repository.TrendsRepository

/**
 * IdolRankingHistory 화면의 UI 상태
 */
sealed interface IdolRankingHistoryUiState {
    data object Loading : IdolRankingHistoryUiState
    data class Success(
        val items: List<IdolRankingHistoryModel>,
        val averageHeart: Long = 0,
        val category: String = "",      // celeb 플레이버용 (M/F)
        val chartCode: String = ""      // app 플레이버용
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
    private val idolRepository: IdolRepository,
    @Assisted private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    @AssistedFactory
    interface Factory {
        fun create(savedStateHandle: SavedStateHandle): IdolRankingHistoryViewModel
    }

    private val idolId: Int = savedStateHandle.get<Int>("idolId") ?: 0

    private val _uiState = MutableStateFlow<IdolRankingHistoryUiState>(IdolRankingHistoryUiState.Loading)
    val uiState: StateFlow<IdolRankingHistoryUiState> = _uiState.asStateFlow()

    // Idol category (M: Male, F: Female)
    private var idolCategory: String = ""

    init {
        if (idolId > 0) {
            viewModelScope.launch {
                // 먼저 idol 정보를 로드하여 category를 가져옴
                loadIdolInfo()
                // 그 다음 랭킹 히스토리 로드
                loadIdolRankingHistory()
            }
        } else {
            _uiState.value = IdolRankingHistoryUiState.Error("Invalid idol ID")
        }
    }

    private suspend fun loadIdolInfo() {
        val idol = idolRepository.getIdolById(idolId)
        idolCategory = idol?.category ?: ""
    }

    private fun loadIdolRankingHistory() {
        viewModelScope.launch {
            trendsRepository.getIdolRankingHistory(idolId).collect { result ->
                _uiState.value = when (result) {
                    is ApiResult.Loading -> IdolRankingHistoryUiState.Loading
                    is ApiResult.Success -> {
                        val items = result.data.items
                        val totalHeart = items.sumOf { it.heart }
                        val averageHeart = if (items.isNotEmpty()) {
                            Math.round(totalHeart.toDouble() / items.size)
                        } else {
                            0L
                        }
                        IdolRankingHistoryUiState.Success(
                            items = items,
                            averageHeart = averageHeart,
                            category = idolCategory,
                            chartCode = result.data.chartCode
                        )
                    }
                    is ApiResult.Error -> IdolRankingHistoryUiState.Error(
                        result.message ?: "Unknown error"
                    )
                }
            }
        }
    }
}
