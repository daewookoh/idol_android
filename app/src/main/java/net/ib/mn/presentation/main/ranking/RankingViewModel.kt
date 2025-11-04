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
 * RankingPage ViewModel
 *
 * CELEB: typeList 사용 (StartupViewModel.loadTypeList에서 캐시됨)
 * 일반: MainChartModel 사용 (old 프로젝트와 동일하게 성별에 따라 males/females 선택)
 *
 * old 프로젝트와 동일하게 StartupView에서 모든 데이터를 처리하고
 * RankingPage에서는 뿌려주기만 함
 */
@HiltViewModel
class RankingViewModel @Inject constructor(
    val configRepository: ConfigRepository // public으로 변경 (RankingPage에서 접근)
) : ViewModel() {

    private val _typeList = MutableStateFlow<List<TypeListModel>>(emptyList())
    val typeList: StateFlow<List<TypeListModel>> = _typeList.asStateFlow()

    private val _mainChartModel = MutableStateFlow<MainChartModel?>(null)
    val mainChartModel: StateFlow<MainChartModel?> = _mainChartModel.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    init {
        loadRankingData()
    }

    /**
     * 랭킹 데이터 로드
     *
     * CELEB: typeList 사용
     * 일반: MainChartModel 사용
     */
    private fun loadRankingData() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            android.util.Log.d("RankingViewModel", "========================================")
            android.util.Log.d("RankingViewModel", "[RankingViewModel] Loading ranking data")
            android.util.Log.d("RankingViewModel", "  - BuildConfig.CELEB: ${BuildConfig.CELEB}")

            try {
                if (BuildConfig.CELEB) {
                    // CELEB: typeList 사용 (StartupViewModel에서 캐시됨)
                    loadTypeListForCeleb()
                } else {
                    // 일반: MainChartModel 사용 (old 프로젝트와 동일)
                    loadMainChartModelForIdol()
                }
            } catch (e: Exception) {
                android.util.Log.e("RankingViewModel", "Exception: ${e.message}", e)
                _error.value = "Failed to load ranking data: ${e.message}"
            } finally {
                _isLoading.value = false
                android.util.Log.d("RankingViewModel", "========================================")
            }
        }
    }

    /**
     * CELEB 전용: typeList 로드
     *
     * StartupViewModel에서 이미 처리한 캐시 데이터 사용
     */
    private suspend fun loadTypeListForCeleb() {
        android.util.Log.d("RankingViewModel", "[CELEB] Loading typeList from cache")

        configRepository.getTypeList(forceRefresh = false).collect { typeListData ->
            android.util.Log.d("RankingViewModel", "[CELEB] TypeList received: ${typeListData.size} items")
            _typeList.value = typeListData
        }
    }

    /**
     * 일반 앱 전용: MainChartModel 로드
     *
     * old 프로젝트와 동일하게 MainChartModel 사용
     * RankingPage에서 성별에 따라 males/females 선택하여 탭 생성
     */
    private fun loadMainChartModelForIdol() {
        android.util.Log.d("RankingViewModel", "[IDOL] Loading MainChartModel from cache")

        val mainChartModel = configRepository.getMainChartModel()
        if (mainChartModel != null) {
            android.util.Log.d("RankingViewModel", "[IDOL] MainChartModel received")
            android.util.Log.d("RankingViewModel", "  - males: ${mainChartModel.males?.size ?: 0}")
            android.util.Log.d("RankingViewModel", "  - females: ${mainChartModel.females?.size ?: 0}")
            _mainChartModel.value = mainChartModel
        } else {
            android.util.Log.w("RankingViewModel", "[IDOL] MainChartModel not found in cache")
        }
    }
}
