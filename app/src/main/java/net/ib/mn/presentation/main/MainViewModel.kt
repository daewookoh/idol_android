package net.ib.mn.presentation.main

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import net.ib.mn.R
import net.ib.mn.util.Constants
import net.ib.mn.data.local.PreferencesManager
import net.ib.mn.data.local.UserInfo
import net.ib.mn.data.remote.udp.IdolBroadcastManager
import net.ib.mn.domain.model.ApiResult
import net.ib.mn.domain.repository.UserRepository
import net.ib.mn.data.repository.WikiRepository
import net.ib.mn.util.DeviceUtil
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    val preferencesManager: PreferencesManager,
    private val chartDatabaseRepository: net.ib.mn.data.repository.ChartRankingRepository,
    private val userRepository: UserRepository,
    private val deviceUtil: DeviceUtil,
    private val idolBroadcastManager: IdolBroadcastManager,
    private val idolRepository: net.ib.mn.domain.repository.IdolRepository,
    val wikiRepository: WikiRepository,
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
            android.util.Log.d(TAG, "========================================")
            android.util.Log.d(TAG, "[MainViewModel] Subscribing to DataStore userInfo")
            android.util.Log.d(TAG, "========================================")

            // DataStore의 userInfo를 구독하여 _userInfo 업데이트
            preferencesManager.userInfo.collect { info ->
                android.util.Log.d(TAG, "[MainViewModel] ========================================")
                android.util.Log.d(TAG, "[MainViewModel] DataStore userInfo received")
                android.util.Log.d(TAG, "[MainViewModel] ========================================")

                if (info != null) {
                    android.util.Log.d(TAG, "[MainViewModel] ✓ User info updated from DataStore:")
                    android.util.Log.d(TAG, "[MainViewModel]   - ID: ${info.id}")
                    android.util.Log.d(TAG, "[MainViewModel]   - Email: ${info.email}")
                    android.util.Log.d(TAG, "[MainViewModel]   - Username: ${info.username}")
                    android.util.Log.d(TAG, "[MainViewModel]   - Nickname: ${info.nickname}")
                    android.util.Log.d(TAG, "[MainViewModel]   - ProfileImage: ${info.profileImage}")
                    android.util.Log.d(TAG, "[MainViewModel]   - Hearts: ${info.heart}")
                    android.util.Log.d(TAG, "[MainViewModel]   - Diamond: ${info.diamond}")
                    android.util.Log.d(TAG, "[MainViewModel]   - StrongHeart: ${info.strongHeart}")
                    android.util.Log.d(TAG, "[MainViewModel]   - WeakHeart: ${info.weakHeart}")
                    android.util.Log.d(TAG, "[MainViewModel]   - Level: ${info.level}")
                    android.util.Log.d(TAG, "[MainViewModel]   - LevelHeart: ${info.levelHeart}")
                    android.util.Log.d(TAG, "[MainViewModel]   - Power: ${info.power}")
                    android.util.Log.d(TAG, "[MainViewModel]   - ResourceUri: ${info.resourceUri}")
                    android.util.Log.d(TAG, "[MainViewModel]   - PushKey: ${info.pushKey}")
                    android.util.Log.d(TAG, "[MainViewModel]   - CreatedAt: ${info.createdAt}")
                    android.util.Log.d(TAG, "[MainViewModel]   - PushFilter: ${info.pushFilter}")
                    android.util.Log.d(TAG, "[MainViewModel]   - StatusMessage: ${info.statusMessage}")
                    android.util.Log.d(TAG, "[MainViewModel]   - TS: ${info.ts}")
                    android.util.Log.d(TAG, "[MainViewModel]   - ItemNo: ${info.itemNo}")
                    android.util.Log.d(TAG, "[MainViewModel]   - Domain: ${info.domain}")
                    android.util.Log.d(TAG, "[MainViewModel]   - GiveHeart: ${info.giveHeart}")
                    android.util.Log.d(TAG, "[MainViewModel] ========================================")
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
                android.util.Log.d(TAG, "[MainViewModel] ✓ Category updated: $category")
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
        android.util.Log.d(TAG, "[MainViewModel] 📖 Opening Community for: ${rankingItem.name}")
    }

    /**
     * CommunityScreen 닫기
     */
    fun closeCommunity() {
        _selectedRankingItem.value = null
        android.util.Log.d(TAG, "[MainViewModel] 📕 Closing Community")
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
        android.util.Log.d(TAG, "[MainViewModel] ========================================")
        android.util.Log.d(TAG, "[MainViewModel] 👁️ App resumed")
        android.util.Log.d(TAG, "[MainViewModel] ========================================")

        // UDP 연결 상태 확인
        val isUdpConnected = idolBroadcastManager.isConnected()
        android.util.Log.d(TAG, "[MainViewModel] UDP connection status: ${if (isUdpConnected) "CONNECTED" else "DISCONNECTED"}")

        // UDP 연결이 끊어진 경우에만 API로 데이터 복구
        if (!isUdpConnected) {
            android.util.Log.d(TAG, "[MainViewModel] 🔄 UDP disconnected - refreshing all charts from API")
            viewModelScope.launch(Dispatchers.IO) {
                chartDatabaseRepository.refreshAllChartsFromApi(idolRepository)
            }
        } else {
            android.util.Log.d(TAG, "[MainViewModel] ✅ UDP connected - no API refresh needed")
        }
    }

    /**
     * 앱이 포그라운드에서 백그라운드로 갈 때 호출
     */
    fun onAppPause() {
        android.util.Log.d(TAG, "[MainViewModel] 🙈 App paused")
    }

    /**
     * 로그아웃 처리.
     *
     * 모든 저장된 데이터를 삭제하고 로그아웃 완료 상태를 업데이트합니다.
     */
    fun logout() {
        viewModelScope.launch {
            try {
                android.util.Log.d(TAG, "[MainViewModel] 🔴 Logging out - clearing all data")

                // DataStore의 모든 데이터 삭제
                preferencesManager.clearAll()

                // Room DB의 모든 차트 데이터 삭제
                chartDatabaseRepository.clearAll()

                // 로그아웃 완료 플래그 설정
                _logoutCompleted.value = true

                android.util.Log.d(TAG, "[MainViewModel] ✓ Logout completed successfully")
            } catch (e: Exception) {
                android.util.Log.e(TAG, "[MainViewModel] ❌ Logout failed: ${e.message}", e)
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
                android.util.Log.d(TAG, "[MainViewModel] 📌 Tab: $tabName - UDP alive, no API refresh needed")
            } else {
                // UDP가 죽어있음 → UDP 활성화 + API 호출
                android.util.Log.d(TAG, "[MainViewModel] 📌 Tab: $tabName - UDP dead, enabling + API refresh")
                idolBroadcastManager.setReactionEnabled(true, "MainViewModel.onTabSelected($tabName)")

                viewModelScope.launch(Dispatchers.IO) {
                    chartDatabaseRepository.refreshAllChartsFromApi(idolRepository)
                }
            }
        } else {
            // 다른 탭 진입: 15초 후 UDP 종료 (기존 Job이 있으면 유지)
            if (udpStopJob == null) {
                android.util.Log.d(TAG, "[MainViewModel] 📌 Tab: $tabName - scheduling UDP stop in ${Constants.UDP_STOP_DELAY_MS / 1000}s")

                udpStopJob = viewModelScope.launch {
                    delay(Constants.UDP_STOP_DELAY_MS)
                    android.util.Log.d(TAG, "[MainViewModel] ⏰ ${Constants.UDP_STOP_DELAY_MS / 1000}s passed - stopping UDP reaction")
                    idolBroadcastManager.setReactionEnabled(false, "MainViewModel.delayedStop($tabName)")
                    udpStopJob = null
                }
            } else {
                android.util.Log.d(TAG, "[MainViewModel] 📌 Tab: $tabName - UDP stop already scheduled")
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

                android.util.Log.d(TAG, "[MainViewModel] 📡 Calling checkEvent API...")

                userRepository.checkEvent(
                    version = version,
                    gmail = gmail,
                    isVM = false,  // TODO: VM 감지 로직 추가 시 변경
                    isRooted = false,  // TODO: 루팅 감지 로직 추가 시 변경
                    deviceId = deviceId
                ).collect { result ->
                    when (result) {
                        is ApiResult.Loading -> {
                            android.util.Log.d(TAG, "[MainViewModel] checkEvent loading...")
                        }
                        is ApiResult.Success -> {
                            val showWelcomeMission = result.data.showWelcomeMission ?: false
                            android.util.Log.d(TAG, "[MainViewModel] ✓ checkEvent success: showWelcomeMission=$showWelcomeMission")

                            // PreferencesManager에 저장
                            preferencesManager.setShowWelcomeMission(showWelcomeMission)
                        }
                        is ApiResult.Error -> {
                            android.util.Log.e(TAG, "[MainViewModel] ❌ checkEvent error: ${result.message}")
                        }
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e(TAG, "[MainViewModel] ❌ checkEvent exception: ${e.message}", e)
            }
        }
    }
}
