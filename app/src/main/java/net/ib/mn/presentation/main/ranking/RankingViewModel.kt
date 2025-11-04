package net.ib.mn.presentation.main.ranking

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import net.ib.mn.BuildConfig
import net.ib.mn.data.model.TypeListModel
import net.ib.mn.data.remote.dto.MainChartModel
import net.ib.mn.domain.repository.ConfigRepository
import javax.inject.Inject

/**
 * RankingPage ViewModel (Optimized for real-time data)
 *
 * CELEB: typeList 사용 (StartupViewModel.loadTypeList에서 캐시됨)
 * 일반: MainChartModel 사용 (old 프로젝트와 동일하게 성별에 따라 males/females 선택)
 *
 * 최적화:
 * 1. ConfigRepository의 StateFlow를 직접 노출 (zero-copy, 중복 collect 방지)
 * 2. 캐시 업데이트 시 자동으로 UI 업데이트 (reactive)
 * 3. 불필요한 중간 StateFlow 제거 (메모리 효율)
 */
@HiltViewModel
class RankingViewModel @Inject constructor(
    val configRepository: ConfigRepository // public으로 변경 (RankingPage에서 접근)
) : ViewModel() {

    /**
     * CELEB 전용: typeList StateFlow를 직접 노출
     * - ConfigRepository의 StateFlow를 그대로 사용
     * - StartupViewModel에서 캐시 업데이트 시 자동으로 UI 업데이트
     * - Zero-copy, 최대 효율
     */
    val typeList: StateFlow<List<TypeListModel>> = configRepository.observeTypeList()

    /**
     * 일반 앱 전용: MainChartModel StateFlow를 직접 노출
     * - ConfigRepository의 StateFlow를 그대로 사용
     * - 실시간 업데이트 자동 반영
     */
    val mainChartModel: StateFlow<MainChartModel?> = configRepository.observeMainChartModel()

    val isLoading: StateFlow<Boolean> = MutableStateFlow(false)
    val error: StateFlow<String?> = MutableStateFlow<String?>(null)

    init {
        android.util.Log.d("RankingViewModel", "========================================")
        android.util.Log.d("RankingViewModel", "[RankingViewModel] Initialized")
        android.util.Log.d("RankingViewModel", "  - BuildConfig.CELEB: ${BuildConfig.CELEB}")
        android.util.Log.d("RankingViewModel", "  - Using direct StateFlow from ConfigRepository (zero-copy)")
        android.util.Log.d("RankingViewModel", "========================================")
    }
}
