package net.ib.mn.presentation.community

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import net.ib.mn.domain.model.ApiResult
import net.ib.mn.domain.model.TrendsModel
import net.ib.mn.domain.repository.TrendsRepository
import net.ib.mn.util.logE
import javax.inject.Inject

private const val PAGE_SIZE = 30

/**
 * UI 상태
 */
data class TrendsUiState(
    val isLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    val items: List<TrendsModel> = emptyList(),
    val hasMore: Boolean = false,
    val error: String? = null
)

/**
 * TrendsViewModel - 이붙그램 ViewModel
 */
@HiltViewModel
class TrendsViewModel @Inject constructor(
    private val trendsRepository: TrendsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(TrendsUiState())
    val uiState: StateFlow<TrendsUiState> = _uiState.asStateFlow()

    private var currentIdolId: Int = 0
    private var currentOffset: Int = 0
    private var isLoadingNextPage: Boolean = false

    /**
     * 이붙그램 목록 로드
     */
    fun loadTrends(idolId: Int) {
        this.currentIdolId = idolId
        this.currentOffset = 0

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            trendsRepository.getRecent(idolId, 0, PAGE_SIZE).collect { result ->
                when (result) {
                    is ApiResult.Loading -> {
                        // 이미 isLoading = true
                    }
                    is ApiResult.Success -> {
                        currentOffset = result.data.items.size
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            items = result.data.items,
                            hasMore = result.data.hasMore,
                            error = null
                        )
                    }
                    is ApiResult.Error -> {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            error = result.message ?: "Unknown error"
                        )
                        logE("loadTrends error: ${result.message}")
                    }
                }
            }
        }
    }

    /**
     * 다음 페이지 로드 (무한 스크롤)
     */
    fun loadNextPage() {
        if (!_uiState.value.hasMore || isLoadingNextPage) return

        isLoadingNextPage = true

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoadingMore = true)

            trendsRepository.getRecent(currentIdolId, currentOffset, PAGE_SIZE).collect { result ->
                when (result) {
                    is ApiResult.Loading -> {
                        // 이미 isLoadingMore = true
                    }
                    is ApiResult.Success -> {
                        currentOffset += result.data.items.size
                        val currentItems = _uiState.value.items
                        _uiState.value = _uiState.value.copy(
                            isLoadingMore = false,
                            items = currentItems + result.data.items,
                            hasMore = result.data.hasMore
                        )
                    }
                    is ApiResult.Error -> {
                        _uiState.value = _uiState.value.copy(
                            isLoadingMore = false,
                            error = result.message
                        )
                        logE("loadNextPage error: ${result.message}")
                    }
                }
                isLoadingNextPage = false
            }
        }
    }

    /**
     * 새로고침
     */
    fun refresh() {
        loadTrends(currentIdolId)
    }
}
