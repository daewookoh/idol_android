package net.ib.mn.presentation.main.ranking.idol_subpage

import androidx.lifecycle.ViewModel
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import net.ib.mn.data.local.entity.IdolEntity
import net.ib.mn.ui.components.RankingItemData

/**
 * HallOfFame (명예전당) 랭킹 ViewModel
 *
 * 탭 선택 상태만 관리하는 단순 ViewModel
 * 실제 데이터 로딩은 각 서브 페이지에서 처리
 */
@HiltViewModel(assistedFactory = HallOfFameRankingSubPageViewModel.Factory::class)
class HallOfFameRankingSubPageViewModel @AssistedInject constructor(
    @Assisted private val chartCode: String
) : ViewModel() {

    sealed interface UiState {
        data object Loading : UiState
        data class Success(
            val items: List<RankingItemData>,
            val topIdol: IdolEntity? = null
        ) : UiState
        data class Error(val message: String) : UiState
    }

    // 탭 인덱스: 0 = 30일 누적, 1 = 일일
    private val _selectedTabIndex = MutableStateFlow(0)
    val selectedTabIndex: StateFlow<Int> = _selectedTabIndex.asStateFlow()

    init {
        android.util.Log.d("HallOfFameRankingVM", "🆕 ViewModel created for chartCode: $chartCode")
    }

    /**
     * 탭 선택 변경
     * @param index 0 = 30일 누적, 1 = 일일
     */
    fun onTabSelected(index: Int) {
        android.util.Log.d("HallOfFameRankingVM", "🔄 Tab selected: $index (${if (index == 0) "30일 누적" else "일일"})")
        _selectedTabIndex.value = index
    }

    override fun onCleared() {
        super.onCleared()
        android.util.Log.d("HallOfFameRankingVM", "♻️ ViewModel cleared")
    }

    @AssistedFactory
    interface Factory {
        fun create(chartCode: String): HallOfFameRankingSubPageViewModel
    }
}
