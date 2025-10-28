package com.example.idol_android.presentation.startup

import androidx.lifecycle.viewModelScope
import com.example.idol_android.base.BaseViewModel
import com.example.idol_android.domain.model.ApiResult
import com.example.idol_android.domain.usecase.*
import com.example.idol_android.util.Constants
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * StartUp 화면의 ViewModel.
 * old 프로젝트의 StartupActivity 비즈니스 로직을 MVI 패턴으로 구현.
 *
 * 주요 기능:
 * 1. 프로그레스바 업데이트 (0 -> 100)
 * 2. API 초기화 (ConfigStartup, UserSelf 등)
 * 3. 필요한 데이터 프리로드
 * 4. 초기화 완료 후 메인 화면으로 이동
 *
 * 완성된 플로우: ConfigStartup API
 * TODO 플로우: 나머지 14개 API
 */
@HiltViewModel
class StartUpViewModel @Inject constructor(
    private val getConfigStartupUseCase: GetConfigStartupUseCase,
    private val getConfigSelfUseCase: GetConfigSelfUseCase,
    private val getUpdateInfoUseCase: GetUpdateInfoUseCase,
    private val getUserSelfUseCase: GetUserSelfUseCase,
    private val getUserStatusUseCase: GetUserStatusUseCase,
    private val getAdTypeListUseCase: GetAdTypeListUseCase,
    private val getMessageCouponUseCase: GetMessageCouponUseCase,
    private val updateTimezoneUseCase: UpdateTimezoneUseCase,
    private val getIdolsUseCase: GetIdolsUseCase,
    private val getIabKeyUseCase: GetIabKeyUseCase,
    private val getBlocksUseCase: GetBlocksUseCase,
    private val preferencesManager: com.example.idol_android.data.local.PreferencesManager,
    private val authInterceptor: com.example.idol_android.data.remote.interceptor.AuthInterceptor,
) : BaseViewModel<StartUpContract.State, StartUpContract.Intent, StartUpContract.Effect>() {

    companion object {
        private const val TAG = "StartUpViewModel"
        private const val TOTAL_API_CALLS = 11 // 병렬 호출되는 API 수
    }

    private var apiCallsCompleted = 0

    override fun createInitialState(): StartUpContract.State {
        return StartUpContract.State(
            progress = 0f,
            isLoading = true,
            currentStep = "Starting..."
        )
    }

    init {
        initialize()
    }

    override fun handleIntent(intent: StartUpContract.Intent) {
        when (intent) {
            is StartUpContract.Intent.Initialize -> initialize()
            is StartUpContract.Intent.Retry -> initialize()
        }
    }

    /**
     * 초기화 프로세스.
     * old 프로젝트의 StartupThread 로직을 코루틴으로 구현.
     */
    private fun initialize() {
        viewModelScope.launch {
            try {
                setState { copy(isLoading = true, progress = 0f, error = null) }

                // Step 0: 저장된 토큰 로드 (old 프로젝트의 IdolAccount.getAccount() 역할)
                // 인증이 필요한 API 호출 전에 반드시 토큰을 먼저 로드해야 함
                val savedToken = preferencesManager.accessToken.first()
                if (savedToken != null) {
                    authInterceptor.setToken(savedToken)
                    android.util.Log.d(TAG, "✓ Access token loaded from DataStore")
                } else {
                    android.util.Log.w(TAG, "⚠️  No saved token - user not logged in (guest mode)")
                }

                // Step 1: 기본 설정 확인 (0-20%)
                updateProgress(0.1f, "Loading configuration...")
                delay(300)
                // TODO: 서버 설정 확인, SharedPreferences 로드 등

                updateProgress(0.2f, "Checking settings...")
                delay(300)

                // Step 2: 광고 ID 및 디바이스 정보 수집 (20-40%)
                updateProgress(0.3f, "Collecting device info...")
                delay(300)
                // TODO: AdvertisingIdClient.getAdvertisingIdInfo() 호출

                updateProgress(0.4f, "Device info collected")
                delay(300)

                // Step 3: 인앱 배너 정보 가져오기 (40-60%)
                updateProgress(0.5f, "Loading banners...")
                delay(300)
                // TODO: viewModel.getInAppBanner() 구현

                updateProgress(0.6f, "Banners loaded")
                delay(300)

                // Step 4: 모든 API 병렬 호출 (60-85%)
                updateProgress(0.6f, "Loading startup APIs...")

                setState { copy(totalApiCalls = TOTAL_API_CALLS) }

                // old 프로젝트처럼 병렬 호출
                loadAllStartupAPIs()

                updateProgress(0.85f, "All APIs loaded")
                delay(300)

                // Step 5: 구독 정보 확인 (80-90%)
                updateProgress(0.85f, "Checking subscriptions...")
                delay(300)
                // TODO: checkSubscriptions() 구현

                updateProgress(0.9f, "Subscriptions checked")
                delay(300)

                // Step 6: IAB 정보 확인 (90-100%)
                updateProgress(0.95f, "Checking IAB...")
                delay(300)
                // TODO: checkIAB() 구현

                updateProgress(1.0f, "Initialization complete")
                delay(300)

                // 초기화 완료
                setState {
                    copy(
                        isLoading = false,
                        progress = 1f,
                        currentStep = "Complete"
                    )
                }

                // 메인 화면으로 이동
//                delay(200)
//                setEffect { StartUpContract.Effect.NavigateToMain }

            } catch (e: Exception) {
                handleError(e)
            }
        }
    }

    /**
     * 프로그레스 업데이트.
     */
    private fun updateProgress(progress: Float, step: String) {
        setState {
            copy(
                progress = progress,
                currentStep = step
            )
        }
    }

    /**
     * 에러 처리.
     */
    private fun handleError(exception: Exception) {
        val errorMessage = exception.message ?: "Unknown error occurred"

        setState {
            copy(
                isLoading = false,
                error = errorMessage,
                currentStep = "Error"
            )
        }

        setEffect { StartUpContract.Effect.ShowError(errorMessage) }
    }

    // ============================================================
    // 실제 API 호출 메서드들
    // ============================================================

    /**
     * 모든 Startup API를 호출 (old 프로젝트 순서 준수)
     *
     * old 프로젝트의 getStartApi() 로직:
     * Phase 1: getConfigSelf() - 먼저 호출 (필수 전제조건)
     * Phase 2: getConfigStartup() - 두 번째 호출 (실패 시 전체 중단)
     * Phase 3: 나머지 API들 병렬 호출
     */
    private suspend fun loadAllStartupAPIs() {
        android.util.Log.d(TAG, "========================================")
        android.util.Log.d(TAG, "Starting API Initialization (Old Project Order)")
        android.util.Log.d(TAG, "Server: ${Constants.BASE_URL}")
        android.util.Log.d(TAG, "========================================")

        // Phase 1: ConfigSelf 먼저 호출 (필수 전제조건)
        // old 코드: async { getConfigSelf(context) }.await()
        android.util.Log.d(TAG, "Phase 1: Loading ConfigSelf (prerequisite)...")
        loadConfigSelf()

        // Phase 2: ConfigStartup (critical path - 실패 시 중단)
        // old 코드: val isStartupSuccess = async { getConfigStartup(context) }.await()
        android.util.Log.d(TAG, "Phase 2: Loading ConfigStartup (critical)...")
        val isStartupSuccess = loadConfigStartup()

        if (!isStartupSuccess) {
            android.util.Log.e(TAG, "❌ ConfigStartup failed - aborting initialization")
            android.util.Log.w(TAG, "⚠️  This is likely because BASE_URL points to a non-existent server")
            android.util.Log.w(TAG, "⚠️  Check Constants.BASE_URL = \"${Constants.BASE_URL}\"")
            android.util.Log.w(TAG, "⚠️  To continue development, you can:")
            android.util.Log.w(TAG, "    1. Set up a mock API server")
            android.util.Log.w(TAG, "    2. Update BASE_URL to a working server")
            android.util.Log.w(TAG, "    3. Temporarily skip this check (development only)")

            handleError(Exception("ConfigStartup API failed - Server not available"))
            return
        }

        android.util.Log.d(TAG, "Phase 3: Loading remaining APIs in parallel...")

        // Phase 3: 나머지 APIs 병렬 호출
        coroutineScope {
            awaitAll(
                async { loadUpdateInfo() },
                async { loadUserSelf() },
                async { loadUserStatus() },
                async { loadAdTypeList() },
                async { loadMessageCoupon() },
                async { loadTimezone() },
                async { loadIdols() },
                // 조건부: loadBlocks() - 첫 사용자만
            )
        }

        android.util.Log.d(TAG, "✓ All APIs completed successfully")
    }

    /**
     * ConfigStartup API 호출 (critical path)
     *
     * 앱 시작 시 필요한 설정 정보를 조회:
     * - 욕설 필터 리스트
     * - 공지사항, 이벤트 목록
     * - SNS 채널 정보
     * - 업로드 제한 사양
     * - 도움말 정보 등
     *
     * @return Boolean - 성공 여부 (실패 시 전체 초기화 중단)
     */
    private suspend fun loadConfigStartup(): Boolean {
        var isSuccess = false

        getConfigStartupUseCase().collect { result ->
            when (result) {
                is ApiResult.Loading -> {
                    // 로딩 중 (이미 프로그레스로 표시 중)
                }
                is ApiResult.Success -> {
                    isSuccess = true
                    val data = result.data.data

                    android.util.Log.d(TAG, "========================================")
                    android.util.Log.d(TAG, "ConfigStartup API Response")
                    android.util.Log.d(TAG, "========================================")
                    android.util.Log.d(TAG, "BadWords count: ${data?.badWords?.size ?: 0}")
                    android.util.Log.d(TAG, "BadWords: ${data?.badWords?.joinToString(", ")}")
                    android.util.Log.d(TAG, "----------------------------------------")
                    android.util.Log.d(TAG, "Board Tags count: ${data?.boardTags?.size ?: 0}")
                    android.util.Log.d(TAG, "Board Tags: ${data?.boardTags?.joinToString(", ")}")
                    android.util.Log.d(TAG, "----------------------------------------")
                    android.util.Log.d(TAG, "SNS Channels count: ${data?.snsChannels?.size ?: 0}")
                    data?.snsChannels?.forEach { channel ->
                        android.util.Log.d(TAG, "  - ${channel.name}: ${channel.url}")
                    }
                    android.util.Log.d(TAG, "----------------------------------------")
                    android.util.Log.d(TAG, "Notices count: ${data?.noticeList?.size ?: 0}")
                    data?.noticeList?.take(3)?.forEach { notice ->
                        android.util.Log.d(TAG, "  - [${notice.id}] ${notice.title}")
                    }
                    android.util.Log.d(TAG, "----------------------------------------")
                    android.util.Log.d(TAG, "Events count: ${data?.eventList?.size ?: 0}")
                    data?.eventList?.take(3)?.forEach { event ->
                        android.util.Log.d(TAG, "  - [${event.id}] ${event.title}")
                    }
                    android.util.Log.d(TAG, "----------------------------------------")
                    android.util.Log.d(TAG, "Family Apps count: ${data?.familyAppList?.size ?: 0}")
                    data?.familyAppList?.forEach { app ->
                        android.util.Log.d(TAG, "  - ${app.name} (${app.packageName})")
                    }
                    android.util.Log.d(TAG, "----------------------------------------")
                    android.util.Log.d(TAG, "Upload Video Spec:")
                    android.util.Log.d(TAG, "  - Max Duration: ${data?.uploadVideoSpec?.maxDurationSec}s")
                    android.util.Log.d(TAG, "  - Max Size: ${data?.uploadVideoSpec?.maxSizeMb} MB")
                    android.util.Log.d(TAG, "  - Allowed Formats: ${data?.uploadVideoSpec?.allowedFormats?.joinToString(", ")}")
                    android.util.Log.d(TAG, "----------------------------------------")
                    android.util.Log.d(TAG, "End Popup: ${data?.endPopup?.title ?: "None"}")
                    android.util.Log.d(TAG, "New Picks count: ${data?.newPicks?.size ?: 0}")
                    android.util.Log.d(TAG, "Help Infos count: ${data?.helpInfos?.size ?: 0}")
                    android.util.Log.d(TAG, "========================================")

                    // DataStore에 저장
                    data?.let { configData ->
                        configData.badWords?.let { preferencesManager.setBadWords(it) }
                        configData.boardTags?.let { preferencesManager.setBoardTags(it) }
                        configData.noticeList?.let { preferencesManager.setNotices(it) }
                        configData.eventList?.let { preferencesManager.setEvents(it) }

                        android.util.Log.d(TAG, "✓ ConfigStartup data saved to DataStore")
                    }
                    // TODO: 메모리에 캐싱 (Application 클래스 또는 싱글톤)
                }
                is ApiResult.Error -> {
                    // 에러 처리
                    isSuccess = false
                    android.util.Log.e("StartUpViewModel", "ConfigStartup error: ${result.message}")

                    // ConfigStartup은 critical path이므로 실패 시 전체 초기화 중단
                }
            }
        }

        return isSuccess
    }

    /**
     * ConfigSelf API 호출 (사용자 설정)
     */
    private suspend fun loadConfigSelf() {
        getConfigSelfUseCase().collect { result ->
            when (result) {
                is ApiResult.Loading -> {}
                is ApiResult.Success -> {
                    incrementApiProgress()
                    val data = result.data.data

                    android.util.Log.d(TAG, "========================================")
                    android.util.Log.d(TAG, "ConfigSelf API Response")
                    android.util.Log.d(TAG, "========================================")
                    android.util.Log.d(TAG, "Language: ${data?.language}")
                    android.util.Log.d(TAG, "Theme: ${data?.theme}")
                    android.util.Log.d(TAG, "Push Enabled: ${data?.pushEnabled}")
                    android.util.Log.d(TAG, "========================================")

                    // DataStore에 저장
                    data?.let { configData ->
                        // TODO: language, theme, pushEnabled 저장 로직 추가
                        android.util.Log.d(TAG, "✓ ConfigSelf data saved to DataStore")
                    }
                }
                is ApiResult.Error -> {
                    android.util.Log.e(TAG, "ConfigSelf error: ${result.message}")
                }
            }
        }
    }

    /**
     * UpdateInfo API 호출 (Idol 업데이트 플래그)
     */
    private suspend fun loadUpdateInfo() {
        getUpdateInfoUseCase().collect { result ->
            when (result) {
                is ApiResult.Loading -> {}
                is ApiResult.Success -> {
                    incrementApiProgress()
                    val data = result.data.data

                    android.util.Log.d(TAG, "========================================")
                    android.util.Log.d(TAG, "UpdateInfo API Response")
                    android.util.Log.d(TAG, "========================================")
                    android.util.Log.d(TAG, "All Idol Update: ${data?.allIdolUpdate}")
                    android.util.Log.d(TAG, "Daily Idol Update: ${data?.dailyIdolUpdate}")
                    android.util.Log.d(TAG, "SNS Channel Update: ${data?.snsChannelUpdate}")
                    android.util.Log.d(TAG, "========================================")

                    // DataStore에 저장 및 기존 플래그와 비교
                    data?.let { updateData ->
                        updateData.allIdolUpdate?.let { preferencesManager.setAllIdolUpdate(it) }
                        updateData.dailyIdolUpdate?.let { preferencesManager.setDailyIdolUpdate(it) }
                        updateData.snsChannelUpdate?.let { preferencesManager.setSnsChannelUpdate(it) }

                        android.util.Log.d(TAG, "✓ UpdateInfo flags saved to DataStore")
                        // TODO: 기존 플래그와 비교하여 동기화 필요 여부 결정
                    }
                }
                is ApiResult.Error -> {
                    android.util.Log.e(TAG, "UpdateInfo error: ${result.message}")
                }
            }
        }
    }

    /**
     * UserSelf API 호출 (사용자 프로필, ETag 지원)
     */
    private suspend fun loadUserSelf() {
        // DataStore에서 저장된 ETag 가져오기
        val etag = preferencesManager.userSelfETag.first()

        getUserSelfUseCase(etag).collect { result ->
            when (result) {
                is ApiResult.Loading -> {}
                is ApiResult.Success -> {
                    incrementApiProgress()
                    val data = result.data.data

                    android.util.Log.d(TAG, "========================================")
                    android.util.Log.d(TAG, "UserSelf API Response")
                    android.util.Log.d(TAG, "========================================")
                    android.util.Log.d(TAG, "User ID: ${data?.id}")
                    android.util.Log.d(TAG, "Username: ${data?.username}")
                    android.util.Log.d(TAG, "Email: ${data?.email}")
                    android.util.Log.d(TAG, "Nickname: ${data?.nickname}")
                    android.util.Log.d(TAG, "Profile Image: ${data?.profileImage}")
                    android.util.Log.d(TAG, "Hearts: ${data?.hearts}")
                    android.util.Log.d(TAG, "========================================")

                    // 사용자 정보 DataStore 저장
                    data?.let { userData ->
                        preferencesManager.setUserInfo(
                            id = userData.id,
                            email = userData.email,
                            username = userData.username,
                            nickname = userData.nickname,
                            profileImage = userData.profileImage,
                            hearts = userData.hearts
                        )

                        // ETag 저장 (다음 요청 시 캐싱에 사용)
                        // TODO: Response에서 ETag 헤더 추출하여 저장
                        // val newETag = response.headers()["ETag"]
                        // newETag?.let { preferencesManager.setUserSelfETag(it) }

                        android.util.Log.d(TAG, "✓ UserSelf data saved to DataStore")
                    }
                }
                is ApiResult.Error -> {
                    if (result.code == 304) {
                        // 캐시 유효 - 로컬 데이터 사용
                        android.util.Log.d(TAG, "UserSelf cache valid (304 Not Modified)")
                    } else {
                        android.util.Log.e(TAG, "UserSelf error: ${result.message}")
                    }
                }
            }
        }
    }

    /**
     * UserStatus API 호출 (튜토리얼 상태)
     */
    private suspend fun loadUserStatus() {
        getUserStatusUseCase().collect { result ->
            when (result) {
                is ApiResult.Loading -> {}
                is ApiResult.Success -> {
                    incrementApiProgress()
                    val data = result.data.data

                    android.util.Log.d(TAG, "========================================")
                    android.util.Log.d(TAG, "UserStatus API Response")
                    android.util.Log.d(TAG, "========================================")
                    android.util.Log.d(TAG, "Tutorial Completed: ${data?.tutorialCompleted}")
                    android.util.Log.d(TAG, "First Login: ${data?.firstLogin}")
                    android.util.Log.d(TAG, "========================================")

                    // 사용자 상태 DataStore 저장
                    data?.let { statusData ->
                        statusData.tutorialCompleted?.let { preferencesManager.setTutorialCompleted(it) }
                        statusData.firstLogin?.let { preferencesManager.setFirstLogin(it) }

                        android.util.Log.d(TAG, "✓ UserStatus data saved to DataStore")
                    }
                }
                is ApiResult.Error -> {
                    android.util.Log.e(TAG, "UserStatus error: ${result.message}")
                }
            }
        }
    }

    /**
     * AdTypeList API 호출
     */
    private suspend fun loadAdTypeList() {
        getAdTypeListUseCase().collect { result ->
            when (result) {
                is ApiResult.Loading -> {}
                is ApiResult.Success -> {
                    incrementApiProgress()
                    val data = result.data.data

                    android.util.Log.d(TAG, "========================================")
                    android.util.Log.d(TAG, "AdTypeList API Response")
                    android.util.Log.d(TAG, "========================================")
                    android.util.Log.d(TAG, "Ad Types count: ${data?.size ?: 0}")
                    data?.forEach { adType ->
                        android.util.Log.d(TAG, "  - ${adType.type} (ID: ${adType.id})")
                        android.util.Log.d(TAG, "    Reward: ${adType.reward}")
                    }
                    android.util.Log.d(TAG, "========================================")
                }
                is ApiResult.Error -> {
                    android.util.Log.e(TAG, "AdTypeList error: ${result.message}")
                }
            }
        }
    }

    /**
     * MessageCoupon API 호출
     */
    private suspend fun loadMessageCoupon() {
        getMessageCouponUseCase().collect { result ->
            when (result) {
                is ApiResult.Loading -> {}
                is ApiResult.Success -> {
                    incrementApiProgress()
                    val data = result.data.data

                    android.util.Log.d(TAG, "========================================")
                    android.util.Log.d(TAG, "MessageCoupon API Response")
                    android.util.Log.d(TAG, "========================================")
                    android.util.Log.d(TAG, "Coupon Messages count: ${data?.size ?: 0}")
                    data?.forEach { coupon ->
                        android.util.Log.d(TAG, "  - [${coupon.id}] ${coupon.message}")
                        android.util.Log.d(TAG, "    Code: ${coupon.couponCode}")
                    }
                    android.util.Log.d(TAG, "========================================")
                }
                is ApiResult.Error -> {
                    android.util.Log.e(TAG, "MessageCoupon error: ${result.message}")
                }
            }
        }
    }

    /**
     * Timezone 업데이트
     */
    private suspend fun loadTimezone() {
        val timezone = java.util.TimeZone.getDefault().id

        updateTimezoneUseCase(timezone).collect { result ->
            when (result) {
                is ApiResult.Loading -> {}
                is ApiResult.Success -> {
                    incrementApiProgress()

                    android.util.Log.d(TAG, "========================================")
                    android.util.Log.d(TAG, "Timezone Update API Response")
                    android.util.Log.d(TAG, "========================================")
                    android.util.Log.d(TAG, "Device Timezone: $timezone")
                    android.util.Log.d(TAG, "Update Success: ${result.data.success}")
                    android.util.Log.d(TAG, "========================================")
                }
                is ApiResult.Error -> {
                    android.util.Log.e(TAG, "Timezone error: ${result.message}")
                }
            }
        }
    }

    /**
     * Idols 리스트 조회
     */
    private suspend fun loadIdols() {
        getIdolsUseCase().collect { result ->
            when (result) {
                is ApiResult.Loading -> {}
                is ApiResult.Success -> {
                    incrementApiProgress()
                    val data = result.data.data

                    android.util.Log.d(TAG, "========================================")
                    android.util.Log.d(TAG, "Idols API Response")
                    android.util.Log.d(TAG, "========================================")
                    android.util.Log.d(TAG, "Total Idols count: ${data?.size ?: 0}")
                    android.util.Log.d(TAG, "----------------------------------------")

                    // 상위 10개만 로그 출력 (너무 많을 수 있음)
                    data?.take(10)?.forEach { idol ->
                        android.util.Log.d(TAG, "Idol: ${idol.name}")
                        android.util.Log.d(TAG, "  - ID: ${idol.id}")
                        android.util.Log.d(TAG, "  - Type: ${idol.type}")
                        android.util.Log.d(TAG, "  - Image: ${idol.imageUrl}")
                        android.util.Log.d(TAG, "  - Debut Date: ${idol.debutDate}")
                        android.util.Log.d(TAG, "----------------------------------------")
                    }

                    if ((data?.size ?: 0) > 10) {
                        android.util.Log.d(TAG, "... and ${data!!.size - 10} more idols")
                    }
                    android.util.Log.d(TAG, "========================================")

                    // TODO: Room Database에 저장
                }
                is ApiResult.Error -> {
                    android.util.Log.e(TAG, "Idols error: ${result.message}")
                }
            }
        }
    }

    /**
     * API 완료 카운트 증가 및 프로그레스 업데이트
     */
    private fun incrementApiProgress() {
        apiCallsCompleted++
        val progress = 0.6f + (apiCallsCompleted.toFloat() / TOTAL_API_CALLS) * 0.25f

        setState {
            copy(
                apiCallsCompleted = apiCallsCompleted,
                progress = progress
            )
        }
    }

    /**
     * 구독 정보 확인.
     */
    private suspend fun checkSubscriptions() {
        // TODO: 구독 정보 확인 로직
        delay(300) // 시뮬레이션
    }

    /**
     * IAB 정보 확인.
     */
    private suspend fun checkIAB() {
        // TODO: IAB 정보 확인 로직
        delay(300) // 시뮬레이션
    }
}
