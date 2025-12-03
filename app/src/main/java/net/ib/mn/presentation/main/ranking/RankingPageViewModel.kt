package net.ib.mn.presentation.main.ranking

import net.ib.mn.util.logW
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import net.ib.mn.BuildConfig
import net.ib.mn.data.local.PreferencesManager
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
    private val savedStateHandle: SavedStateHandle,
    private val preferencesManager: PreferencesManager
) : ViewModel() {

    companion object {
        private const val KEY_SELECTED_TAB_INDEX = "selectedTabIndex"
        private const val DEFAULT_TAB_INDEX = 0
        const val MY_FAV_TOAST_MIN_RANK_LIMIT = 6  // 최애가 6위 이상일 때만 토스트 표시
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

    /**
     * AwardModel StateFlow를 직접 노출
     * - 이벤트 플로팅 버튼 이미지 URL 포함
     * - showAwardTab이 true일 때만 값이 있음
     */
    val awardModel: StateFlow<net.ib.mn.data.remote.dto.AwardModel?> = configRepository.observeAwardModel()

    /**
     * 이벤트 버튼 표시 여부 (showAwardTab && awardModel.mainFloatingImgUrl != null)
     */
    private val _showAwardButton = MutableStateFlow(false)
    val showAwardButton: StateFlow<Boolean> = _showAwardButton.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    // 최애 이동 토스트 표시 상태
    private val _showMyFavToast = MutableStateFlow(false)
    val showMyFavToast: StateFlow<Boolean> = _showMyFavToast.asStateFlow()

    // 최애 아이돌의 랭킹 리스트 내 위치 (스크롤 대상)
    private val _myFavIdolPosition = MutableStateFlow(-1)
    val myFavIdolPosition: StateFlow<Int> = _myFavIdolPosition.asStateFlow()

    // 최애 아이돌 ID
    private val _mostIdolId = MutableStateFlow<Int?>(null)
    val mostIdolId: StateFlow<Int?> = _mostIdolId.asStateFlow()

    // 웰컴 미션 버튼 표시 상태
    private val _showWelcomeMission = MutableStateFlow(false)
    val showWelcomeMission: StateFlow<Boolean> = _showWelcomeMission.asStateFlow()

    // NEW 뱃지 표시 상태 (하트픽, 원픽)
    private val _hasNewHeartPick = MutableStateFlow(false)
    val hasNewHeartPick: StateFlow<Boolean> = _hasNewHeartPick.asStateFlow()

    private val _hasNewOnePick = MutableStateFlow(false)
    val hasNewOnePick: StateFlow<Boolean> = _hasNewOnePick.asStateFlow()

    /**
     * 랭킹 페이지 내 메인 탭 선택 인덱스
     * SavedStateHandle을 사용하여 바텀 네비게이션 이동 시에도 유지
     */
    val selectedTabIndex: StateFlow<Int> = savedStateHandle.getStateFlow(KEY_SELECTED_TAB_INDEX, DEFAULT_TAB_INDEX)

    /** 앱 첫 실행 시 defaultChartCode를 읽어서 초기 탭 인덱스 설정 */
    private val _shouldInitializeTab = MutableStateFlow(true)
    val shouldInitializeTab: StateFlow<Boolean> = _shouldInitializeTab.asStateFlow()

    init {
        // 웰컴 미션 버튼 표시 여부 구독
        viewModelScope.launch {
            preferencesManager.showWelcomeMission.collect { _showWelcomeMission.value = it }
        }
        // NEW 뱃지 표시 여부 구독 (하트픽)
        viewModelScope.launch {
            preferencesManager.hasNewHeartPick.collect { _hasNewHeartPick.value = it }
        }
        // NEW 뱃지 표시 여부 구독 (원픽)
        viewModelScope.launch {
            preferencesManager.hasNewOnePick.collect { _hasNewOnePick.value = it }
        }
        // 이벤트 버튼 표시 여부 구독
        viewModelScope.launch {
            awardModel.collect { award ->
                _showAwardButton.value = configRepository.getShowAwardTab() &&
                    !award?.mainFloatingImgUrl.isNullOrEmpty()
            }
        }
        // 프로세스 복원 시 데이터가 없으면 재로드
        if (!BuildConfig.CELEB && configRepository.getMainChartModel() == null) {
            logW("RankingViewModel", "MainChartModel is null (process restored) - reloading data")
            reloadChartData()
        }
    }

    fun setSelectedTabIndex(index: Int) {
        savedStateHandle[KEY_SELECTED_TAB_INDEX] = index
    }

    fun initializeTabFromDefaultChartCode(tabDataList: List<*>, getCodeFromTab: (Any) -> String?) {
        if (!_shouldInitializeTab.value) return

        // 이미 SavedStateHandle에 값이 있으면 초기화하지 않음
        val savedIndex = savedStateHandle.get<Int>(KEY_SELECTED_TAB_INDEX)
        if (savedIndex != null && savedIndex != DEFAULT_TAB_INDEX) {
            _shouldInitializeTab.value = false
            return
        }

        viewModelScope.launch {
            configRepository.getDefaultChartCode()?.let { defaultChartCode ->
                val tabIndex = tabDataList.indexOfFirst { tab ->
                    tab?.let { getCodeFromTab(it) } == defaultChartCode
                }
                if (tabIndex >= 0) setSelectedTabIndex(tabIndex)
            }
            _shouldInitializeTab.value = false
        }
    }

    /**
     * 최애 이동 토스트 표시 여부 체크
     * old 프로젝트의 showRankingWithMyFavToast 로직
     *
     * @param rankItems 랭킹 아이템 리스트
     */
    fun checkMyFavToast(rankItems: List<net.ib.mn.ui.components.RankingItem>) {
        viewModelScope.launch {
            // 이미 토스트를 표시한 적 있으면 표시하지 않음
            val hasShown = preferencesManager.getHasShownMyFavToast()
            if (hasShown) {
                _showMyFavToast.value = false
                return@launch
            }

            // 최애 아이돌 ID 가져오기
            val mostId = preferencesManager.getMostIdolId()
            if (mostId == null) {
                _showMyFavToast.value = false
                return@launch
            }
            _mostIdolId.value = mostId

            // 랭킹 리스트에서 최애 아이돌 찾기
            val mostIdolIndex = rankItems.indexOfFirst { it.id.toIntOrNull() == mostId }
            if (mostIdolIndex < 0) {
                _showMyFavToast.value = false
                return@launch
            }

            // 최애 아이돌의 랭킹 (1-indexed)
            val mostIdolRank = rankItems.getOrNull(mostIdolIndex)?.rank ?: (mostIdolIndex + 1)

            // 6위 이상일 때만 토스트 표시 (화면에 안 보이는 경우)
            if (mostIdolRank < MY_FAV_TOAST_MIN_RANK_LIMIT) {
                _showMyFavToast.value = false
                return@launch
            }

            // 스크롤 위치 저장 (ExoTop3 고려하여 +1)
            _myFavIdolPosition.value = mostIdolIndex + 1
            _showMyFavToast.value = true
        }
    }

    /**
     * 최애 이동 토스트 클릭 처리
     * 토스트 숨기고, 표시 완료 기록
     */
    fun onMyFavToastClick() {
        viewModelScope.launch {
            _showMyFavToast.value = false
            preferencesManager.setHasShownMyFavToast(true)
        }
    }

    /**
     * 최애 이동 토스트 숨기기
     */
    fun hideMyFavToast() {
        _showMyFavToast.value = false
    }

    /**
     * 차트 데이터 재로드 (프로세스 복원 시 사용)
     */
    fun reloadChartData() {
        if (BuildConfig.CELEB) {
            return
        }

        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            try {
                val response = chartsApi.getChartsCurrent()

                if (response.isSuccessful && response.body() != null) {
                    val body = response.body()!!

                    if (body.success) {
                        // MainChartModel 저장
                        body.main?.let { mainChartModel ->
                            configRepository.setMainChartModel(mainChartModel)
                        }

                        // ChartObjects 저장 (MIRACLE, ROOKIE 등)
                        body.objects?.let { objects ->
                            configRepository.setChartObjects(objects)
                        }
                    } else {
                        _error.value = "API returned success=false"
                    }
                } else {
                    _error.value = "Failed to load chart data"
                }
            } catch (e: Exception) {
                _error.value = e.message
            } finally {
                _isLoading.value = false
            }
        }
    }
}
