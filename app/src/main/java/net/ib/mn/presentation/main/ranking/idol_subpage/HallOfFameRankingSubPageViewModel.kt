package net.ib.mn.presentation.main.ranking.idol_subpage

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import net.ib.mn.data.local.entity.IdolEntity
import net.ib.mn.ui.components.RankingItem

/**
 * HallOfFame (명예전당) 랭킹 ViewModel
 *
 * 탭 선택 상태만 관리하는 단순 ViewModel
 * 실제 데이터 로딩은 각 서브 페이지에서 처리
 *
 * SavedStateHandle을 사용하여 탭 선택을 저장:
 * - 앱을 내렸다 올려도 유지 (프로세스가 살아있을 때)
 * - 앱을 재시작하면 리셋 (프로세스 종료 후)
 */
@HiltViewModel(assistedFactory = HallOfFameRankingSubPageViewModel.Factory::class)
class HallOfFameRankingSubPageViewModel @AssistedInject constructor(
    @Assisted private val chartCode: String,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    sealed interface UiState {
        data object Loading : UiState
        data class Success(
            val items: List<RankingItem>,
            val topIdol: IdolEntity? = null
        ) : UiState
        data class Error(val message: String) : UiState
    }

    companion object {
        private const val KEY_SELECTED_TAB_INDEX = "selectedTabIndex"
        private const val KEY_ACCUMULATIVE_SUB_TAB_INDEX = "accumulativeSubTabIndex"
        private const val KEY_DAILY_SUB_TAB_INDEX = "dailySubTabIndex"
    }

    // 탭 인덱스: 0 = 30일 누적, 1 = 일일
    // SavedStateHandle을 사용하여 자동으로 저장/복원
    val selectedTabIndex: StateFlow<Int> = savedStateHandle.getStateFlow(KEY_SELECTED_TAB_INDEX, 0)

    // 30일 누적 페이지의 하위 탭 인덱스: 0 = 개인, 1 = 그룹, 2 = 글로벌
    val accumulativeSubTabIndex: StateFlow<Int> = savedStateHandle.getStateFlow(KEY_ACCUMULATIVE_SUB_TAB_INDEX, 0)

    // 일일 페이지의 하위 탭 인덱스: 0 = 개인, 1 = 그룹, 2 = 글로벌
    val dailySubTabIndex: StateFlow<Int> = savedStateHandle.getStateFlow(KEY_DAILY_SUB_TAB_INDEX, 0)

    init {
    }

    /**
     * 탭 선택 변경
     * @param index 0 = 30일 누적, 1 = 일일
     */
    fun onTabSelected(index: Int) {
        savedStateHandle[KEY_SELECTED_TAB_INDEX] = index
    }

    /**
     * 30일 누적 페이지의 하위 탭 선택 변경
     * @param index 0 = 개인, 1 = 그룹, 2 = 글로벌
     */
    fun setAccumulativeSubTabIndex(index: Int) {
        savedStateHandle[KEY_ACCUMULATIVE_SUB_TAB_INDEX] = index
    }

    /**
     * 일일 페이지의 하위 탭 선택 변경
     * @param index 0 = 개인, 1 = 그룹, 2 = 글로벌
     */
    fun setDailySubTabIndex(index: Int) {
        savedStateHandle[KEY_DAILY_SUB_TAB_INDEX] = index
    }

    override fun onCleared() {
        super.onCleared()
    }

    @AssistedFactory
    interface Factory {
        fun create(chartCode: String): HallOfFameRankingSubPageViewModel
    }
}
