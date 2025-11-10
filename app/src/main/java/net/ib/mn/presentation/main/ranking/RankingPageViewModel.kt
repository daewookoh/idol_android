package net.ib.mn.presentation.main.ranking

import androidx.lifecycle.SavedStateHandle
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
 *
 * SavedStateHandle을 사용하여 메인 탭 선택을 저장:
 * - 앱을 내렸다 올려도 유지 (바텀 네비게이션 이동 시에도 유지)
 * - 앱을 재시작하면 리셋 (프로세스 종료 후)
 */
@HiltViewModel
class RankingPageViewModel @Inject constructor(
    val configRepository: ConfigRepository, // public으로 변경 (RankingPage에서 접근)
    private val chartsApi: net.ib.mn.data.remote.api.ChartsApi,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    companion object {
        private const val KEY_SELECTED_TAB_INDEX = "selectedTabIndex"
        private const val DEFAULT_TAB_INDEX = 0
    }

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

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    /**
     * 랭킹 페이지 내 메인 탭 선택 인덱스
     * SavedStateHandle을 사용하여 바텀 네비게이션 이동 시에도 유지
     *
     * 초기값 설정 우선순위:
     * 1. SavedStateHandle에 저장된 값 (이전에 선택한 탭)
     * 2. defaultChartCode로부터 계산된 인덱스 (앱 첫 실행 시)
     * 3. 0 (기본값)
     */
    val selectedTabIndex: StateFlow<Int> = savedStateHandle.getStateFlow(KEY_SELECTED_TAB_INDEX, DEFAULT_TAB_INDEX)

    /**
     * 앱 첫 실행 시 defaultChartCode를 읽어서 초기 탭 인덱스 설정
     */
    private val _shouldInitializeTab = MutableStateFlow(true)
    val shouldInitializeTab: StateFlow<Boolean> = _shouldInitializeTab.asStateFlow()

    /**
     * 선택된 탭 인덱스 업데이트
     */
    fun setSelectedTabIndex(index: Int) {
        savedStateHandle[KEY_SELECTED_TAB_INDEX] = index
        android.util.Log.d("RankingViewModel", "📌 Selected tab index updated: $index")
    }

    /**
     * defaultChartCode에 해당하는 탭 인덱스를 찾아서 설정
     */
    fun initializeTabFromDefaultChartCode(tabDataList: List<*>, getCodeFromTab: (Any) -> String?) {
        if (!_shouldInitializeTab.value) {
            android.util.Log.d("RankingViewModel", "⏭️ Tab already initialized, skipping")
            return
        }

        // 이미 SavedStateHandle에 값이 있으면 (이전에 선택한 탭이 있으면) 초기화하지 않음
        if (savedStateHandle.get<Int>(KEY_SELECTED_TAB_INDEX) != null && savedStateHandle.get<Int>(KEY_SELECTED_TAB_INDEX) != DEFAULT_TAB_INDEX) {
            android.util.Log.d("RankingViewModel", "⏭️ SavedStateHandle has previous tab selection, skipping initialization")
            _shouldInitializeTab.value = false
            return
        }

        viewModelScope.launch {
            val defaultChartCode = configRepository.getDefaultChartCode()
            android.util.Log.d("RankingViewModel", "========================================")
            android.util.Log.d("RankingViewModel", "[RankingViewModel] Initializing tab from defaultChartCode")
            android.util.Log.d("RankingViewModel", "  - defaultChartCode: $defaultChartCode")
            android.util.Log.d("RankingViewModel", "  - tabDataList size: ${tabDataList.size}")

            if (defaultChartCode != null) {
                // defaultChartCode에 해당하는 탭 찾기
                val tabIndex = tabDataList.indexOfFirst { tab ->
                    val code = tab?.let { getCodeFromTab(it) }
                    android.util.Log.d("RankingViewModel", "  - Checking tab: code=$code")
                    code == defaultChartCode
                }

                if (tabIndex >= 0) {
                    android.util.Log.d("RankingViewModel", "✅ Found matching tab at index: $tabIndex")
                    setSelectedTabIndex(tabIndex)
                } else {
                    android.util.Log.w("RankingViewModel", "⚠️ No matching tab found for chartCode: $defaultChartCode, using default index 0")
                }
            } else {
                android.util.Log.d("RankingViewModel", "ℹ️ No defaultChartCode set, using default index 0")
            }

            android.util.Log.d("RankingViewModel", "========================================")
            _shouldInitializeTab.value = false
        }
    }

    init {
        android.util.Log.d("RankingViewModel", "========================================")
        android.util.Log.d("RankingViewModel", "[RankingViewModel] Initialized")
        android.util.Log.d("RankingViewModel", "  - BuildConfig.CELEB: ${BuildConfig.CELEB}")
        android.util.Log.d("RankingViewModel", "  - Using direct StateFlow from ConfigRepository (zero-copy)")
        android.util.Log.d("RankingViewModel", "========================================")

        // 프로세스 복원 시 데이터가 없으면 재로드
        if (!BuildConfig.CELEB && configRepository.getMainChartModel() == null) {
            android.util.Log.w("RankingViewModel", "⚠️ MainChartModel is null (process restored) - reloading data")
            reloadChartData()
        }
    }

    /**
     * 차트 데이터 재로드 (프로세스 복원 시 사용)
     */
    fun reloadChartData() {
        if (BuildConfig.CELEB) {
            android.util.Log.d("RankingViewModel", "CELEB app - skipping chart reload")
            return
        }

        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            try {
                android.util.Log.d("RankingViewModel", "📡 Reloading ChartsCurrent...")
                val response = chartsApi.getChartsCurrent()

                if (response.isSuccessful && response.body() != null) {
                    val body = response.body()!!

                    if (body.success) {
                        // MainChartModel 저장
                        body.main?.let { mainChartModel ->
                            configRepository.setMainChartModel(mainChartModel)
                            android.util.Log.d("RankingViewModel", "✓ MainChartModel reloaded and cached")
                        }

                        // ChartObjects 저장 (MIRACLE, ROOKIE 등)
                        body.objects?.let { objects ->
                            configRepository.setChartObjects(objects)
                            android.util.Log.d("RankingViewModel", "✓ ChartObjects reloaded and cached")
                        }
                    } else {
                        _error.value = "API returned success=false"
                        android.util.Log.e("RankingViewModel", "❌ API returned success=false")
                    }
                } else {
                    _error.value = "Failed to load chart data"
                    android.util.Log.e("RankingViewModel", "❌ Chart API failed: ${response.code()}")
                }
            } catch (e: Exception) {
                _error.value = e.message
                android.util.Log.e("RankingViewModel", "❌ Exception: ${e.message}", e)
            } finally {
                _isLoading.value = false
            }
        }
    }
}
