package net.ib.mn.presentation.main.ranking

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import net.ib.mn.data.model.TypeListModel
import net.ib.mn.domain.model.RankingItem
import net.ib.mn.domain.repository.RankingRepository

/**
 * RankingSubPage ViewModel
 *
 * Best Practice:
 * 1. AssistedInject를 사용하여 런타임에 type 파라미터 주입
 * 2. StateFlow로 UI 상태 관리
 * 3. UiState 패턴으로 로딩/성공/실패 상태 명확하게 구분
 * 4. Repository를 통한 데이터 접근으로 Testability 향상
 */
@HiltViewModel(assistedFactory = RankingSubPageViewModel.Factory::class)
class RankingSubPageViewModel @AssistedInject constructor(
    @Assisted private val type: TypeListModel,
    private val rankingRepository: RankingRepository
) : ViewModel() {

    /**
     * UI 상태 정의
     */
    sealed interface UiState {
        data object Loading : UiState
        data class Success(val items: List<RankingItem>) : UiState
        data class Error(val message: String) : UiState
    }

    private val _uiState = MutableStateFlow<UiState>(UiState.Loading)
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    init {
        loadRankingData()
    }

    /**
     * 랭킹 데이터 로드
     */
    private fun loadRankingData() {
        viewModelScope.launch {
            _uiState.value = UiState.Loading

            rankingRepository.getRankingByType(type.type ?: "")
                .catch { e ->
                    _uiState.value = UiState.Error(
                        e.message ?: "알 수 없는 오류가 발생했습니다."
                    )
                }
                .collect { result ->
                    result.fold(
                        onSuccess = { response ->
                            _uiState.value = UiState.Success(response.items)
                        },
                        onFailure = { e ->
                            _uiState.value = UiState.Error(
                                e.message ?: "데이터를 불러오는데 실패했습니다."
                            )
                        }
                    )
                }
        }
    }

    /**
     * 랭킹 데이터 새로고침
     */
    fun refresh() {
        viewModelScope.launch {
            _isRefreshing.value = true

            val result = rankingRepository.refreshRanking(type.type ?: "")
            result.fold(
                onSuccess = {
                    loadRankingData()
                },
                onFailure = { e ->
                    _uiState.value = UiState.Error(
                        e.message ?: "새로고침에 실패했습니다."
                    )
                }
            )

            _isRefreshing.value = false
        }
    }

    /**
     * AssistedFactory for Hilt
     *
     * 런타임에 type 파라미터를 주입받기 위해 필요
     */
    @AssistedFactory
    interface Factory {
        fun create(type: TypeListModel): RankingSubPageViewModel
    }
}
