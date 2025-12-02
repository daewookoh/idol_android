package net.ib.mn.presentation.main

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import net.ib.mn.R
import net.ib.mn.data.local.PreferencesManager
import net.ib.mn.data.local.UserInfo
import net.ib.mn.data.remote.udp.IdolBroadcastManager
import net.ib.mn.data.repository.UserCacheRepository
import net.ib.mn.data.repository.UsersRepository
import net.ib.mn.domain.model.ApiResult
import net.ib.mn.domain.repository.UserRepository
import net.ib.mn.util.Constants
import net.ib.mn.util.DeviceUtil

@HiltViewModel
class MainViewModel @Inject constructor(
    val preferencesManager: PreferencesManager,
    private val chartDatabaseRepository: net.ib.mn.data.repository.ChartRankingRepository,
    private val userRepository: UserRepository,
    val usersRepository: UsersRepository,
    val userCacheRepository: UserCacheRepository,
    private val deviceUtil: DeviceUtil,
    private val idolBroadcastManager: IdolBroadcastManager,
    private val idolRepository: net.ib.mn.domain.repository.IdolRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    companion object {
        private const val TAG = "USER_INFO"
    }

    private val _userInfo = MutableStateFlow<UserInfo?>(null)
    val userInfo: StateFlow<UserInfo?> = _userInfo.asStateFlow()

    private val _logoutCompleted = MutableStateFlow(false)
    val logoutCompleted: StateFlow<Boolean> = _logoutCompleted.asStateFlow()

    // CommunityScreen 표시 상태 (RankingItem 클릭 시 사용)
    private val _selectedRankingItem = MutableStateFlow<net.ib.mn.ui.components.RankingItem?>(null)
    val selectedRankingItem: StateFlow<net.ib.mn.ui.components.RankingItem?> = _selectedRankingItem.asStateFlow()

    // 즉시 반응하는 로컬 카테고리 상태 (UI 반응성 개선)
    private val _currentCategory = MutableStateFlow<String?>(null)
    val currentCategory: StateFlow<String?> = _currentCategory.asStateFlow()

    // UDP 종료 지연 Job (다른 탭에 15초 이상 머물 때만 UDP 종료)
    private var udpStopJob: Job? = null

    init {
        viewModelScope.launch {

            // DataStore의 userInfo를 구독하여 _userInfo 업데이트
            preferencesManager.userInfo.collect { info ->

                if (info != null) {
                    _userInfo.value = info
                } else {
                    android.util.Log.w(TAG, "[MainViewModel] ⚠️ UserInfo is null")
                    _userInfo.value = null
                }
            }
        }

        // DataStore의 카테고리를 구독
        viewModelScope.launch {
            preferencesManager.defaultCategory.collect { category ->
                _currentCategory.value = category
            }
        }

        // 기존 콜백 제거 - 새 전략에서는 onTabSelected()에서 직접 API 호출 여부를 판단
        // idolBroadcastManager.setOnReactionEnabledCallback { ... }
    }

    /**
     * CommunityScreen 열기 (RankingItem 클릭 시)
     */
    fun openCommunity(rankingItem: net.ib.mn.ui.components.RankingItem) {
        _selectedRankingItem.value = rankingItem
    }

    /**
     * 채팅 탭 표시 여부 계산 (old 프로젝트의 setIsShowChattingTab과 동일)
     * 조건: 최애이거나, 최애의 그룹이거나, 관리자인 경우
     *
     * @param rankingItem 현재 커뮤니티의 아이돌
     * @return 채팅 탭 표시 여부
     */
    suspend fun shouldShowChattingTab(rankingItem: net.ib.mn.ui.components.RankingItem): Boolean {
        val userHeart = _userInfo.value?.heart ?: 0
        val mostIdolId = preferencesManager.getMostIdolId()

        val idolId = rankingItem.id.toIntOrNull() ?: 0
        val groupId = rankingItem.groupId

        // 관리자인 경우
        if (userHeart == Constants.LEVEL_ADMIN) {
            return true
        }

        // 최애인 경우
        if (mostIdolId != null && mostIdolId == idolId) {
            return true
        }

        // 최애의 그룹인 경우 (현재 아이돌이 그룹이고, 최애가 해당 그룹의 멤버인 경우)
        if (groupId != null && mostIdolId != null && groupId == mostIdolId) {
            return true
        }

        return false
    }

    /**
     * CommunityScreen 닫기
     */
    fun closeCommunity() {
        _selectedRankingItem.value = null
    }

    /**
     * 카테고리 변경 (즉시 UI 업데이트 + 백그라운드 DataStore 저장)
     */
    fun setCategory(category: String) {
        // 1. 즉시 로컬 상태 업데이트 (UI가 바로 반응)
        _currentCategory.value = category

        // 2. 백그라운드에서 DataStore에 저장
        viewModelScope.launch {
            preferencesManager.setDefaultCategory(category)
        }
    }

    /**
     * 앱이 백그라운드에서 포그라운드로 돌아올 때 호출
     * UDP 연결이 끊어진 경우에만 백그라운드 동안 놓친 데이터를 API로 복구
     */
    fun onAppResume() {

        // UDP 연결 상태 확인
        val isUdpConnected = idolBroadcastManager.isConnected()

        // UDP 연결이 끊어진 경우에만 API로 데이터 복구
        if (!isUdpConnected) {
            viewModelScope.launch(Dispatchers.IO) {
                chartDatabaseRepository.refreshAllChartsFromApi(idolRepository)
            }
        } else {
        }
    }

    /**
     * 앱이 포그라운드에서 백그라운드로 갈 때 호출
     */
    fun onAppPause() {
    }

    /**
     * 로그아웃 처리.
     *
     * 모든 저장된 데이터를 삭제하고 로그아웃 완료 상태를 업데이트합니다.
     */
    fun logout() {
        viewModelScope.launch {
            try {

                // DataStore의 모든 데이터 삭제
                preferencesManager.clearAll()

                // Room DB의 모든 차트 데이터 삭제
                chartDatabaseRepository.clearAll()

                // 로그아웃 완료 플래그 설정
                _logoutCompleted.value = true

            } catch (e: Exception) {
            }
        }
    }

    /**
     * 바텀 네비게이션 탭 변경 시 호출
     *
     * 새 전략 (서버 비용 절감):
     * - 랭킹(0)/나의최애(1) 탭: UDP 즉시 활성화 + 지연 종료 Job 취소 + UDP 상태 기반 API 호출
     * - 다른 탭: 15초 후 UDP 종료 (15초 내 복귀 시 UDP 유지)
     *
     * API 호출 기준:
     * - UDP가 살아있으면 → API 호출 불필요 (실시간 데이터 유지)
     * - UDP가 죽어있으면 → API 호출 필요 (놓친 데이터 복구)
     *
     * @param tabIndex 선택된 탭 인덱스 (0: 랭킹, 1: 나의 최애, 2: 내정보, 3: 자유게시판, 4: 메뉴)
     */
    fun onTabSelected(tabIndex: Int) {
        val tabName = when (tabIndex) {
            0 -> "Ranking"
            1 -> "MyFavorite"
            2 -> "MyInfo"
            3 -> "FreeBoard"
            4 -> "Menu"
            else -> "Unknown"
        }

        val isRankingOrFavoriteTab = tabIndex == 0 || tabIndex == 1

        if (isRankingOrFavoriteTab) {
            // 랭킹/나의최애 탭 진입: 지연 종료 취소 + UDP 상태 확인
            udpStopJob?.cancel()
            udpStopJob = null

            val isUdpAlive = idolBroadcastManager.isReactionEnabled()

            if (isUdpAlive) {
                // UDP가 살아있음 → API 호출 불필요
            } else {
                // UDP가 죽어있음 → UDP 활성화 + API 호출
                idolBroadcastManager.setReactionEnabled(true, "MainViewModel.onTabSelected($tabName)")

                viewModelScope.launch(Dispatchers.IO) {
                    chartDatabaseRepository.refreshAllChartsFromApi(idolRepository)
                }
            }
        } else {
            // 다른 탭 진입: 15초 후 UDP 종료 (기존 Job이 있으면 유지)
            if (udpStopJob == null) {

                udpStopJob = viewModelScope.launch {
                    delay(Constants.UDP_STOP_DELAY_MS)
                    idolBroadcastManager.setReactionEnabled(false, "MainViewModel.delayedStop($tabName)")
                    udpStopJob = null
                }
            } else {
            }
        }
    }

    /**
     * 이벤트 체크 API 호출 (웰컴 미션, 배너 등)
     * old 프로젝트의 MainViewModel.requestEvent()와 동일
     *
     * MainScreen 진입 시 호출하여 showWelcomeMission 값을 가져옴
     */
    fun checkEvent() {
        viewModelScope.launch {
            try {
                val version = context.getString(R.string.app_version)
                val gmail = deviceUtil.getGmail()
                val deviceId = deviceUtil.getDeviceUUID()


                userRepository.checkEvent(
                    version = version,
                    gmail = gmail,
                    isVM = false,  // TODO: VM 감지 로직 추가 시 변경
                    isRooted = false,  // TODO: 루팅 감지 로직 추가 시 변경
                    deviceId = deviceId
                ).collect { result ->
                    when (result) {
                        is ApiResult.Loading -> {
                        }
                        is ApiResult.Success -> {
                            val showWelcomeMission = result.data.showWelcomeMission ?: false

                            // PreferencesManager에 저장
                            preferencesManager.setShowWelcomeMission(showWelcomeMission)
                        }
                        is ApiResult.Error -> {
                        }
                    }
                }
            } catch (e: Exception) {
            }
        }
    }

}
