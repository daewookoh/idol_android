package net.ib.mn.presentation.startup

import androidx.lifecycle.viewModelScope
import net.ib.mn.base.BaseViewModel
import net.ib.mn.data.remote.dto.toEntity
import net.ib.mn.domain.model.ApiResult
import net.ib.mn.domain.usecase.*
import net.ib.mn.util.Constants
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
 * UseCases:
 * - GetConfigStartupUseCase: 앱 전역 설정 (욕설 필터, 공지사항, 이벤트 등)
 * - GetConfigSelfUseCase: 사용자 앱 설정 (언어, 테마, 푸시 알림)
 * - GetUpdateInfoUseCase: 아이돌 업데이트 플래그 (전체/일일/SNS)
 * - GetUserSelfUseCase: 사용자 프로필 정보 (ETag 캐싱 지원)
 * - GetUserStatusUseCase: 사용자 상태 (튜토리얼, 첫 로그인)
 * - GetAdTypeListUseCase: 광고 타입 목록
 * - GetMessageCouponUseCase: 쿠폰 메시지 목록
 * - UpdateTimezoneUseCase: 타임존 업데이트
 * - GetIdolsUseCase: 전체 아이돌 목록 (Room DB 저장)
 * - GetIabKeyUseCase: IAB 공개키 (미사용)
 * - GetBlocksUseCase: 차단 사용자 목록 (미사용)
 *
 * 호출 API 및 사용 Field:
 * - GET /config/startup - badWords, boardTags, noticeList, eventList, snsChannels, uploadVideoSpec
 * - GET /config/self - language, theme, pushEnabled
 * - GET /update/info - allIdolUpdate, dailyIdolUpdate, snsChannelUpdate
 * - GET /user/self - id, username, email, nickname, profileImage, hearts (ETag 헤더)
 * - GET /user/status - tutorialCompleted, firstLogin
 * - GET /ad/types - id, type, reward
 * - GET /message/coupon - id, message, couponCode
 * - PUT /user/timezone - timezone (request body)
 * - GET /idols - id, name, group, imageUrl, type, debutDate
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
    private val getTypeListUseCase: GetTypeListUseCase,
    private val configRepository: net.ib.mn.domain.repository.ConfigRepository,
    private val chartsApi: net.ib.mn.data.remote.api.ChartsApi,
    private val preferencesManager: net.ib.mn.data.local.PreferencesManager,
    private val authInterceptor: net.ib.mn.data.remote.interceptor.AuthInterceptor,
    private val idolDao: net.ib.mn.data.local.dao.IdolDao,
) : BaseViewModel<StartUpContract.State, StartUpContract.Intent, StartUpContract.Effect>() {

    companion object {
        private const val TAG = "StartUpViewModel"
    }

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

                // Step 0: 저장된 인증 정보 로드 (old 프로젝트의 IdolAccount.getAccount() 역할)
                // old 프로젝트와 동일: email, domain, token을 모두 로드
                android.util.Log.d("USER_INFO", "========================================")
                android.util.Log.d("USER_INFO", "[StartUpViewModel] Loading auth credentials from DataStore...")

                val savedEmail = preferencesManager.loginEmail.first()
                val savedDomain = preferencesManager.loginDomain.first()
                val savedToken = preferencesManager.accessToken.first()

                if (savedEmail != null && savedDomain != null && savedToken != null) {
                    android.util.Log.d("USER_INFO", "[StartUpViewModel] ✓ Auth credentials found in DataStore")
                    android.util.Log.d("USER_INFO", "[StartUpViewModel]   - Email: $savedEmail")
                    android.util.Log.d("USER_INFO", "[StartUpViewModel]   - Domain: $savedDomain")
                    android.util.Log.d("USER_INFO", "[StartUpViewModel]   - Token: ${savedToken.take(20)}...")
                    android.util.Log.d("USER_INFO", "[StartUpViewModel] Setting credentials in AuthInterceptor...")

                    // old 프로젝트와 동일: email, domain, token을 AuthInterceptor에 설정
                    authInterceptor.setAuthCredentials(savedEmail, savedDomain, savedToken)

                    android.util.Log.d("USER_INFO", "[StartUpViewModel] ✓ AuthInterceptor credentials set successfully")
                    android.util.Log.d("USER_INFO", "[StartUpViewModel] Ready to make authenticated API calls")
                    android.util.Log.d("USER_INFO", "========================================")

                    android.util.Log.d(TAG, "✓ Auth credentials loaded from DataStore")
                } else {
                    android.util.Log.w("USER_INFO", "========================================")
                    android.util.Log.w("USER_INFO", "[StartUpViewModel] ⚠️ Auth credentials incomplete")
                    android.util.Log.w("USER_INFO", "[StartUpViewModel]   - Email: $savedEmail")
                    android.util.Log.w("USER_INFO", "[StartUpViewModel]   - Domain: $savedDomain")
                    android.util.Log.w("USER_INFO", "[StartUpViewModel]   - Token: ${if (savedToken != null) "present" else "null"}")
                    android.util.Log.w("USER_INFO", "[StartUpViewModel] User not logged in - navigating to Login screen")
                    android.util.Log.w("USER_INFO", "========================================")

                    android.util.Log.w(TAG, "⚠️  Auth credentials incomplete - user not logged in (guest mode)")
                    // Guest mode - Navigate to Login screen
                    setState { copy(isLoading = false, progress = 0f, currentStep = "Login required") }

                    setEffect { StartUpContract.Effect.NavigateToLogin }
                    return@launch
                }

                // 실제 작업: API 병렬 호출
                updateProgress(0.2f, "Loading startup APIs...")
                loadAllStartupAPIs()

                // 초기화 완료
                updateProgress(1.0f, "Initialization complete")

                // 초기화 완료
                setState {
                    copy(
                        isLoading = false,
                        progress = 1f,
                        currentStep = "Complete"
                    )
                }

                setEffect { StartUpContract.Effect.NavigateToMain }

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
            android.util.Log.w(TAG, "⚠️  Clearing all auth credentials and local data...")

            // 모든 인증 정보 및 로컬 데이터 삭제
            preferencesManager.clearAll()
            android.util.Log.d(TAG, "✓ All auth credentials and local data cleared")

            // 로그인 페이지로 이동
            setState { copy(isLoading = false, progress = 0f, currentStep = "Login required") }
            setEffect { StartUpContract.Effect.NavigateToLogin }
            return
        }

        android.util.Log.d(TAG, "Phase 3: Loading remaining APIs in parallel...")

        // Phase 3: 나머지 APIs 병렬 호출
        coroutineScope {
            val tasks = mutableListOf(
                async { loadUpdateInfo() },
                async { loadUserSelf() },
                async { loadUserStatus() },
                async { loadAdTypeList() },
                async { loadMessageCoupon() },
                async { loadTimezone() },
                async { loadIdols() }
                // 조건부: loadBlocks() - 첫 사용자만
            )

            // CELEB 전용: TypeList API 호출
            if (net.ib.mn.BuildConfig.CELEB) {
                tasks.add(async { loadTypeList() })
            }

            // 모든 앱: ChartsCurrent API 호출
            tasks.add(async { loadChartsCurrent() })

            awaitAll(*tasks.toTypedArray())
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

        try {
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
                    data?.badWords?.take(3)?.forEach { badWord ->
                        android.util.Log.d(TAG, "  - ${badWord.word} (type: ${badWord.type}, exc: ${badWord.exc.size})")
                    }
                    android.util.Log.d(TAG, "----------------------------------------")
                    android.util.Log.d(TAG, "Board Tags count: ${data?.boardTags?.size ?: 0}")
                    data?.boardTags?.take(3)?.forEach { tag ->
                        android.util.Log.d(TAG, "  - [${tag.id}] ${tag.name}")
                    }
                    android.util.Log.d(TAG, "----------------------------------------")
                    android.util.Log.d(TAG, "SNS Channels count: ${data?.snsChannels?.size ?: 0}")
                    data?.snsChannels?.forEach { channel ->
                        android.util.Log.d(TAG, "  - ${channel.name}: ${channel.url}")
                    }
                    android.util.Log.d(TAG, "----------------------------------------")
                    android.util.Log.d(TAG, "Notice List: ${if (data?.noticeList.isNullOrEmpty()) "Empty" else "JSON String (${data?.noticeList?.length} chars)"}")
                    android.util.Log.d(TAG, "----------------------------------------")
                    android.util.Log.d(TAG, "Event List: ${if (data?.eventList.isNullOrEmpty()) "Empty" else "JSON String (${data?.eventList?.length} chars)"}")
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
                    android.util.Log.d(TAG, "New Picks: ${data?.newPicks}")
                    android.util.Log.d(TAG, "Help Infos: ${data?.helpInfos}")
                    android.util.Log.d(TAG, "========================================")

                    // DataStore에 저장
                    data?.let { configData ->
                        // BadWords는 List<BadWord>를 word 필드만 추출하여 List<String>으로 변환
                        configData.badWords?.let { badWords ->
                            val badWordStrings = badWords.map { it.word }
                            preferencesManager.setBadWords(badWordStrings)
                        }
                        configData.boardTags?.let { preferencesManager.setBoardTags(it) }
                        configData.noticeList?.let { preferencesManager.setNotices(it) }
                        configData.eventList?.let { preferencesManager.setEvents(it) }

                        android.util.Log.d(TAG, "✓ ConfigStartup data saved to DataStore")
                    }
                    // NOTE: 메모리 캐싱이 필요한 경우 구현 방법:
                    // 1. Application 클래스에 ConfigCache 싱글톤 생성
                    // 2. 또는 Hilt SingletonComponent로 ConfigRepository 제공
                    // 3. 현재는 DataStore만 사용하며, 필요시 Flow로 실시간 데이터 접근 가능
                }
                    is ApiResult.Error -> {
                        // 에러 처리
                        isSuccess = false
                        android.util.Log.e("StartUpViewModel", "ConfigStartup error: ${result.message}")

                        // ConfigStartup은 critical path이므로 실패 시 전체 초기화 중단
                    }
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("StartUpViewModel", "ConfigStartup exception: ${e.message}", e)
            isSuccess = false
        }

        return isSuccess
    }

    /**
     * ConfigSelf API 호출 (사용자 설정)
     *
     * UDP 설정 포함:
     * - udpBroadcastUrl: UDP 브로드캐스트 서버 URL (테스트/실서버 구분)
     * - udpStage: UDP 활성화 플래그 (> 0일 때만 UDP 연결)
     */
    private suspend fun loadConfigSelf() {
        try {
            getConfigSelfUseCase().collect { result ->
                when (result) {
                    is ApiResult.Loading -> {}
                    is ApiResult.Success -> {
                    val data = result.data

                    android.util.Log.d(TAG, "========================================")
                    android.util.Log.d(TAG, "ConfigSelf API Response")
                    android.util.Log.d(TAG, "========================================")
                    android.util.Log.d(TAG, "UDP Broadcast URL: ${data.udpBroadcastUrl}")
                    android.util.Log.d(TAG, "UDP Stage: ${data.udpStage}")
                    android.util.Log.d(TAG, "CDN URL: ${data.cdnUrl}")
                    android.util.Log.d(TAG, "----------------------------------------")
                    android.util.Log.d(TAG, "Daily Idol Update: ${data.dailyIdolUpdate}")
                    android.util.Log.d(TAG, "All Idol Update: ${data.allIdolUpdate}")
                    android.util.Log.d(TAG, "Show Miracle Tab: ${data.showMiracleTab}")
                    android.util.Log.d(TAG, "Show Award Tab: ${data.showAwardTab}")
                    android.util.Log.d(TAG, "========================================")

                    // DataStore에 UDP 설정 저장
                    data.udpBroadcastUrl?.let {
                        preferencesManager.setUdpBroadcastUrl(it)
                        android.util.Log.d(TAG, "✓ UDP Broadcast URL saved: $it")
                    }

                    preferencesManager.setUdpStage(data.udpStage)
                    android.util.Log.d(TAG, "✓ UDP Stage saved: ${data.udpStage}")

                    // CDN URL 저장
                    data.cdnUrl?.let {
                        preferencesManager.setCdnUrl(it)
                        android.util.Log.d(TAG, "✓ CDN URL saved: $it")
                    }

                    android.util.Log.d(TAG, "✓ ConfigSelf data saved to DataStore")
                }
                    is ApiResult.Error -> {
                        android.util.Log.e(TAG, "ConfigSelf error: ${result.message}")
                    }
                }
            }
        } catch (e: Exception) {
            android.util.Log.e(TAG, "ConfigSelf exception: ${e.message}", e)
        }
    }

    /**
     * UpdateInfo API 호출 (Idol 업데이트 플래그)
     */
    private suspend fun loadUpdateInfo() {
        try {
            getUpdateInfoUseCase().collect { result ->
            when (result) {
                is ApiResult.Loading -> {}
                is ApiResult.Success -> {
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
                        // 기존 플래그 가져오기
                        val oldAllIdolUpdate = preferencesManager.allIdolUpdate.first()
                        val oldDailyIdolUpdate = preferencesManager.dailyIdolUpdate.first()
                        val oldSnsChannelUpdate = preferencesManager.snsChannelUpdate.first()

                        // 플래그 비교 및 동기화 필요 여부 로그
                        updateData.allIdolUpdate?.let { newFlag ->
                            if (oldAllIdolUpdate != newFlag) {
                                android.util.Log.d(TAG, "⚠️  AllIdolUpdate changed: $oldAllIdolUpdate -> $newFlag (sync needed)")
                            }
                            preferencesManager.setAllIdolUpdate(newFlag)
                        }

                        updateData.dailyIdolUpdate?.let { newFlag ->
                            if (oldDailyIdolUpdate != newFlag) {
                                android.util.Log.d(TAG, "⚠️  DailyIdolUpdate changed: $oldDailyIdolUpdate -> $newFlag (sync needed)")
                            }
                            preferencesManager.setDailyIdolUpdate(newFlag)
                        }

                        updateData.snsChannelUpdate?.let { newFlag ->
                            if (oldSnsChannelUpdate != newFlag) {
                                android.util.Log.d(TAG, "⚠️  SnsChannelUpdate changed: $oldSnsChannelUpdate -> $newFlag (sync needed)")
                            }
                            preferencesManager.setSnsChannelUpdate(newFlag)
                        }

                        android.util.Log.d(TAG, "✓ UpdateInfo flags saved to DataStore")
                    }
                }
                is ApiResult.Error -> {
                    android.util.Log.e(TAG, "UpdateInfo error: ${result.message}")
                }
            }
        }
        } catch (e: Exception) {
            android.util.Log.e(TAG, "UpdateInfo exception: ${e.message}", e)
        }
    }

    /**
     * UserSelf API 호출 (사용자 프로필, ETag 지원)
     */
    private suspend fun loadUserSelf() {
        try {
            android.util.Log.d("USER_INFO", "========================================")
            android.util.Log.d("USER_INFO", "[StartUpViewModel] Loading user info from server (NO CACHE)")
            android.util.Log.d("USER_INFO", "========================================")

            // cacheControl을 전달하여 캐시 비활성화
            getUserSelfUseCase(
                etag = null,
                cacheControl = "no-cache",
                timestamp = (System.currentTimeMillis() / 1000).toInt()
            ).collect { result ->
            when (result) {
                is ApiResult.Loading -> {
                    android.util.Log.d("USER_INFO", "[StartUpViewModel] Loading user info...")
                }
                is ApiResult.Success -> {
                    // NOTE: UserSelfResponse 구조: {objects: [UserSelfData], ...}
                    // 사용자 데이터는 objects 배열의 첫 번째 요소
                    val data = result.data.objects.firstOrNull()

                    android.util.Log.d("USER_INFO", "========================================")
                    android.util.Log.d("USER_INFO", "[StartUpViewModel] ✓ UserSelf API Response received")
                    android.util.Log.d("USER_INFO", "[StartUpViewModel] Objects count: ${result.data.objects.size}")
                    android.util.Log.d("USER_INFO", "========================================")
                    android.util.Log.d("USER_INFO", "[StartUpViewModel] id: ${data?.id}")
                    android.util.Log.d("USER_INFO", "[StartUpViewModel] email: ${data?.email}")
                    android.util.Log.d("USER_INFO", "[StartUpViewModel] username: ${data?.username}")
                    android.util.Log.d("USER_INFO", "[StartUpViewModel] nickname: ${data?.nickname}")
                    android.util.Log.d("USER_INFO", "[StartUpViewModel] profileImage: ${data?.profileImage}")
                    android.util.Log.d("USER_INFO", "[StartUpViewModel] hearts: ${data?.hearts}")
                    android.util.Log.d("USER_INFO", "[StartUpViewModel] diamond: ${data?.diamond}")
                    android.util.Log.d("USER_INFO", "[StartUpViewModel] strongHeart: ${data?.strongHeart}")
                    android.util.Log.d("USER_INFO", "[StartUpViewModel] weakHeart: ${data?.weakHeart}")
                    android.util.Log.d("USER_INFO", "[StartUpViewModel] level: ${data?.level}")
                    android.util.Log.d("USER_INFO", "[StartUpViewModel] levelHeart: ${data?.levelHeart}")
                    android.util.Log.d("USER_INFO", "[StartUpViewModel] power: ${data?.power}")
                    android.util.Log.d("USER_INFO", "[StartUpViewModel] resourceUri: ${data?.resourceUri}")
                    android.util.Log.d("USER_INFO", "[StartUpViewModel] pushKey: ${data?.pushKey}")
                    android.util.Log.d("USER_INFO", "[StartUpViewModel] createdAt: ${data?.createdAt}")
                    android.util.Log.d("USER_INFO", "[StartUpViewModel] pushFilter: ${data?.pushFilter}")
                    android.util.Log.d("USER_INFO", "[StartUpViewModel] statusMessage: ${data?.statusMessage}")
                    android.util.Log.d("USER_INFO", "[StartUpViewModel] ts: ${data?.ts}")
                    android.util.Log.d("USER_INFO", "[StartUpViewModel] itemNo: ${data?.itemNo}")
                    android.util.Log.d("USER_INFO", "[StartUpViewModel] domain: ${data?.domain}")
                    android.util.Log.d("USER_INFO", "[StartUpViewModel] giveHeart: ${data?.giveHeart}")
                    android.util.Log.d("USER_INFO", "[StartUpViewModel] most: ${data?.most?.id} (${data?.most?.name})")
                    android.util.Log.d("USER_INFO", "========================================")

                    // 사용자 정보 DataStore 저장
                    data?.let { userData ->
                        android.util.Log.d("USER_INFO", "[StartUpViewModel] Saving user info to DataStore...")

                        // domain이 null이면 저장된 loginDomain 사용 (old 프로젝트와 동일)
                        val userDomain = userData.domain ?: preferencesManager.loginDomain.first()

                        // 로그인 시 저장한 이메일 보존 (getUserSelf 응답의 이메일로 덮어쓰지 않음)
                        val savedLoginEmail = preferencesManager.loginEmail.first()
                        val emailToSave = savedLoginEmail ?: userData.email

                        android.util.Log.d("USER_INFO", "[StartUpViewModel] Email preservation:")
                        android.util.Log.d("USER_INFO", "  - Login email (saved): $savedLoginEmail")
                        android.util.Log.d("USER_INFO", "  - API response email: ${userData.email}")
                        android.util.Log.d("USER_INFO", "  - Email to save: $emailToSave")

                        // setUserInfo 호출 (suspend 함수이므로 완료될 때까지 기다림)
                        preferencesManager.setUserInfo(
                            id = userData.id,
                            email = emailToSave,  // 로그인 시 저장한 이메일 사용
                            username = userData.username,
                            nickname = userData.nickname,
                            profileImage = userData.profileImage,
                            hearts = userData.hearts,
                            diamond = userData.diamond,
                            strongHeart = userData.strongHeart,
                            weakHeart = userData.weakHeart,
                            level = userData.level,
                            levelHeart = userData.levelHeart,
                            power = userData.power,
                            resourceUri = userData.resourceUri,
                            pushKey = userData.pushKey,
                            createdAt = userData.createdAt,
                            pushFilter = userData.pushFilter,
                            statusMessage = userData.statusMessage,
                            ts = userData.ts,
                            itemNo = userData.itemNo,
                            domain = userDomain,
                            giveHeart = userData.giveHeart
                        )

                        // 최애 정보 저장 (old 프로젝트의 IdolAccount.getAccount(context)?.most와 동일)
                        // chartCodes에서 Award 코드(AW_로 시작)를 제외한 첫 번째 유효한 값을 저장
                        // Award 코드는 특수 이벤트/시상식 관련이므로 일반 랭킹 탭에 매칭되지 않음
                        val chartCode = userData.most?.chartCodes
                            ?.firstOrNull { !it.startsWith("AW_") && !it.startsWith("DF_") }  // Award/DF 코드 제외
                            ?: userData.most?.chartCodes?.firstOrNull()  // 모두 특수 코드면 첫 번째 사용

                        val category = userData.most?.category

                        android.util.Log.d("USER_INFO", "[StartUpViewModel] chartCodes from server: ${userData.most?.chartCodes}")
                        android.util.Log.d("USER_INFO", "[StartUpViewModel] Selected chartCode (filtered): $chartCode")
                        android.util.Log.d("USER_INFO", "[StartUpViewModel] Most idol category: $category")

                        preferencesManager.setMostIdol(
                            idolId = userData.most?.id,
                            chartCode = chartCode,
                            category = category
                        )
                        android.util.Log.d("USER_INFO", "[StartUpViewModel] ✓ Most idol saved: id=${userData.most?.id}, chartCode=$chartCode, category=$category")

                        // Most 아이돌 데이터를 로컬 DB에 upsert
                        userData.most?.let { most ->
                            val idolEntity = most.toEntity()
                            idolDao.upsert(idolEntity)
                            android.util.Log.d("USER_INFO", "[StartUpViewModel] ✓ Most idol upserted to local DB: id=${most.id}, name=${most.name}")
                        }

                        // mostCategory를 defaultCategory로 설정 (GLOBALS 탭 초기 필터링에 사용)
                        // 앱 첫 진입 시 최애의 성별에 맞는 랭킹이 표시되도록 함
                        if (category != null) {
                            preferencesManager.setDefaultCategory(category)
                            android.util.Log.d("USER_INFO", "[StartUpViewModel] ✓ Default category set from mostCategory: $category")
                            android.util.Log.d("USER_INFO", "[StartUpViewModel]   → GLOBALS 탭에서 이 성별의 랭킹이 표시됩니다")
                        } else {
                            android.util.Log.w("USER_INFO", "[StartUpViewModel] ⚠️ mostCategory is null, defaultCategory not set")
                        }

                        // mostChartCode를 defaultChartCode로 설정 (랭킹 탭 초기 선택에 사용)
                        // 앱 첫 진입 시 최애의 차트에 해당하는 탭이 선택되도록 함
                        if (chartCode != null) {
                            preferencesManager.setDefaultChartCode(chartCode)
                            android.util.Log.d("USER_INFO", "[StartUpViewModel] ✓ Default chartCode set from mostChartCode: $chartCode")
                            android.util.Log.d("USER_INFO", "[StartUpViewModel]   → 랭킹 탭에서 이 차트가 기본으로 선택됩니다")
                        } else {
                            android.util.Log.w("USER_INFO", "[StartUpViewModel] ⚠️ mostChartCode is null, defaultChartCode not set")
                        }

                        // setUserInfo 완료 후 DataStore 업데이트가 완료되기를 보장하기 위해 약간의 지연
                        // DataStore는 비동기적으로 업데이트되므로, 업데이트가 반영되기까지 시간이 필요할 수 있음
                        kotlinx.coroutines.delay(100)

                        android.util.Log.d("USER_INFO", "[StartUpViewModel] ✓ Full user info saved to DataStore and update confirmed")
                        android.util.Log.d("USER_INFO", "[StartUpViewModel]   - All fields saved: id, email, username, nickname, profileImage")
                        android.util.Log.d("USER_INFO", "[StartUpViewModel]   - All fields saved: hearts, diamond, strongHeart, weakHeart")
                        android.util.Log.d("USER_INFO", "[StartUpViewModel]   - All fields saved: level, levelHeart, power, resourceUri")
                        android.util.Log.d("USER_INFO", "[StartUpViewModel]   - All fields saved: pushKey, createdAt, pushFilter, statusMessage")
                        android.util.Log.d("USER_INFO", "[StartUpViewModel]   - All fields saved: ts, itemNo, domain, giveHeart")
                        android.util.Log.d("USER_INFO", "[StartUpViewModel]   - DataStore will emit updated userInfo to subscribers")
                        android.util.Log.d("USER_INFO", "[StartUpViewModel]   - MainViewModel will receive the updated data")
                    } ?: run {
                        android.util.Log.w("USER_INFO", "[StartUpViewModel] ⚠️ UserSelf API returned null data")
                    }
                }
                is ApiResult.Error -> {
                    if (result.code == 401) {
                        // 토큰이 유효하지 않음 - 토큰 삭제 및 로그인 페이지로 이동
                        android.util.Log.e("USER_INFO", "[StartUpViewModel] ❌ Token invalid (401 Unauthorized)")
                        android.util.Log.e("USER_INFO", "[StartUpViewModel] Clearing auth credentials and navigating to Login")

                        // 토큰 및 로그인 정보 삭제
                        preferencesManager.setAccessToken("")
                        // loginEmail과 loginDomain도 삭제하기 위해 clearAll 호출 후 네비게이션
                        preferencesManager.clearAll()

                        android.util.Log.d("USER_INFO", "[StartUpViewModel] ✓ Auth credentials cleared")

                        // 로그인 페이지로 이동
                        setEffect { StartUpContract.Effect.NavigateToLogin }

                        // 초기화 중단
                        return@collect
                    } else {
                        android.util.Log.e("USER_INFO", "[StartUpViewModel] ❌ UserSelf API error: ${result.message} (code: ${result.code})")
                        android.util.Log.e(TAG, "UserSelf error: ${result.message}")
                    }
                }
            }
        }
        } catch (e: Exception) {
            android.util.Log.e(TAG, "UserSelf exception: ${e.message}", e)
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
        try {
            getIdolsUseCase().collect { result ->
            when (result) {
                is ApiResult.Loading -> {}
                is ApiResult.Success -> {
                    val response = result.data
                    val data = response.data

                    android.util.Log.d(TAG, "========================================")
                    android.util.Log.d(TAG, "Idols API Response")
                    android.util.Log.d(TAG, "========================================")
                    android.util.Log.d(TAG, "All Idol Update: ${response.allIdolUpdate}")
                    android.util.Log.d(TAG, "Daily Idol Update: ${response.dailyIdolUpdate}")
                    android.util.Log.d(TAG, "Meta - Total Count: ${response.meta?.totalCount}")
                    android.util.Log.d(TAG, "Meta - Limit: ${response.meta?.limit}")
                    android.util.Log.d(TAG, "Meta - Offset: ${response.meta?.offset}")
                    android.util.Log.d(TAG, "Total Idols count: ${data?.size ?: 0}")
                    android.util.Log.d(TAG, "========================================")

                    // Room Database에 저장
                    data?.let { idolList ->
                        val entities = idolList.map { it.toEntity() }
                        idolDao.insert(entities)  // old 프로젝트와 동일한 메서드명
                        android.util.Log.d(TAG, "✓ ${entities.size} idols saved to Room Database")
                    }
                }
                is ApiResult.Error -> {
                    android.util.Log.e(TAG, "Idols error: ${result.message}")
                }
            }
        }
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Idols exception: ${e.message}", e)
        }
    }

    /**
     * TypeList API 호출 (old 프로젝트와 동일)
     *
     * CELEB 전용 API
     * 랭킹 탭 타입 목록을 조회하고 ConfigRepository 캐시에 저장
     * old 프로젝트의 StartupViewModel.getTypeList()와 동일한 로직
     */
    private suspend fun loadTypeList() {
        android.util.Log.d(TAG, "========================================")
        android.util.Log.d(TAG, "Loading TypeList (old logic)...")

        try {
            getTypeListUseCase(forceRefresh = true).collect { typeListData ->
                android.util.Log.d("API_RESPONSE", "TypeList received: ${typeListData.size} items")

                // old 프로젝트와 동일: A, S 타입은 isDivided = "Y"로 설정
                val arrayTypeList = typeListData.toMutableList()

                for (i in arrayTypeList.indices) {
                    arrayTypeList[i].type?.let {
                        if (it == "A" || it == "S") {
                            arrayTypeList[i].isDivided = "Y"
                        }
                    }
                }

                // old 프로젝트와 동일: isDivided == "Y"인 경우 여성 버전 추가
                var insertOffset = 0
                for (i in 0 until arrayTypeList.size + insertOffset) {
                    if (i < arrayTypeList.size && arrayTypeList[i].isDivided == "Y") {
                        val model = arrayTypeList[i].copy()
                        model.isDivided = "N" // N으로 만드는 이유는 Y로 했을 경우 무한루프가 돌 수 있음
                        model.isFemale = true // Y인 경우 여자가 있는 경우이므로 추가
                        model.showDivider = true // 구분선 보여주기
                        arrayTypeList.add(i + 1, model)
                        insertOffset++
                    }
                }

                // old 프로젝트와 동일: 해외 배우 카테고리(G) 끼워넣기
                val globalIndex = arrayTypeList.indexOfFirst { it.type == "G" }
                if (globalIndex != -1) {
                    val globalModel = arrayTypeList[globalIndex]
                    globalModel.showDivider = true

                    // type이 A이고 isFemale이 true인 카테고리를 찾는다
                    val insertIndex = arrayTypeList.indexOfFirst { it.type == "A" && it.isFemale }
                    if (insertIndex != -1) {
                        arrayTypeList.removeAt(globalIndex)
                        arrayTypeList.add(insertIndex + 1, globalModel)
                        arrayTypeList[insertIndex].showDivider = false
                    }
                }

                // ConfigRepository 캐시에 처리된 typeList 저장
                configRepository.setTypeListCache(arrayTypeList)
                android.util.Log.d("API_RESPONSE", "✓ TypeList cached in ConfigRepository (${arrayTypeList.size} items)")

                arrayTypeList.forEachIndexed { index, type ->
                    android.util.Log.d("API_RESPONSE", "  [$index] id=${type.id}, name=${type.name}, type=${type.type}, isDivided=${type.isDivided}, isFemale=${type.isFemale}, showDivider=${type.showDivider}")
                }

                android.util.Log.d("API_RESPONSE", "========================================")
            }
        } catch (e: Exception) {
            android.util.Log.e(TAG, "TypeList error: ${e.message}", e)
        }
    }

    /**
     * ChartsCurrent API 호출 (old 프로젝트와 동일)
     *
     * 일반 앱 전용 API
     * charts/current/를 호출하여 main.males/females를 TypeListModel로 변환
     * ConfigRepository 캐시에 저장
     */
    private suspend fun loadChartsCurrent() {
        android.util.Log.d(TAG, "========================================")
        android.util.Log.d(TAG, "Loading ChartsCurrent (non-CELEB logic)...")

        try {
            val response = chartsApi.getChartsCurrent()

            android.util.Log.d("API_RESPONSE", "ChartsCurrent Response code: ${response.code()}")
            android.util.Log.d("API_RESPONSE", "ChartsCurrent Response successful: ${response.isSuccessful}")

            if (response.isSuccessful && response.body() != null) {
                val body = response.body()!!

                if (body.success) {
                    // MainChartModel 저장 (old 프로젝트와 동일)
                    body.main?.let { mainChartModel ->
                        configRepository.setMainChartModel(mainChartModel)
                        android.util.Log.d("API_RESPONSE", "✓ MainChartModel saved to cache")
                    }

                    // ChartObjects 저장 (MIRACLE, ROOKIE 등)
                    body.objects?.let { objects ->
                        configRepository.setChartObjects(objects)
                        android.util.Log.d("API_RESPONSE", "✓ ChartObjects saved to cache")
                    }

                    // main.males/females를 TypeListModel로 변환
                    val typeListData = mutableListOf<net.ib.mn.data.model.TypeListModel>()

                    // males 차트 변환 (예: SOLO_M, GROUP_M)
                    body.main?.males?.forEach { chartInfo ->
                        chartInfo.code?.let { code ->
                            val typeListModel = net.ib.mn.data.model.TypeListModel(
                                id = 0,
                                name = chartInfo.name ?: "",
                                type = extractTypeFromCode(code), // "SOLO", "GROUP" 등
                                isDivided = "N",
                                isFemale = false,
                                showDivider = false
                            )
                            typeListData.add(typeListModel)
                        }
                    }

                    // females 차트 변환 (예: SOLO_F, GROUP_F)
                    body.main?.females?.forEach { chartInfo ->
                        chartInfo.code?.let { code ->
                            val typeListModel = net.ib.mn.data.model.TypeListModel(
                                id = 0,
                                name = chartInfo.name ?: "",
                                type = extractTypeFromCode(code), // "SOLO", "GROUP" 등
                                isDivided = "N",
                                isFemale = true,
                                showDivider = false
                            )
                            typeListData.add(typeListModel)
                        }
                    }

                    // objects에서 추가 차트 정보 (MIRACLE, ROOKIE, HEARTPICK 등)
                    body.objects?.forEach { chart ->
                        chart.type?.let { type ->
                            val typeListModel = net.ib.mn.data.model.TypeListModel(
                                id = 0,
                                name = chart.type ?: "", // type을 name으로 사용
                                type = type,
                                isDivided = "N",
                                isFemale = false,
                                showDivider = false
                            )
                            typeListData.add(typeListModel)
                        }
                    }

                    // ConfigRepository 캐시에 처리된 typeList 저장
                    configRepository.setTypeListCache(typeListData)
                    android.util.Log.d("API_RESPONSE", "✓ ChartsCurrent converted and cached (${typeListData.size} items)")

                    typeListData.forEachIndexed { index, type ->
                        android.util.Log.d("API_RESPONSE", "  [$index] name=${type.name}, type=${type.type}, isFemale=${type.isFemale}")
                    }
                } else {
                    android.util.Log.e("API_RESPONSE", "ChartsCurrent API returned success=false")
                }
            } else {
                android.util.Log.e("API_RESPONSE", "ChartsCurrent Response not successful: ${response.code()}")
                android.util.Log.e("API_RESPONSE", "Error body: ${response.errorBody()?.string()}")
            }
        } catch (e: Exception) {
            android.util.Log.e(TAG, "ChartsCurrent error: ${e.message}", e)
        }

        android.util.Log.d("API_RESPONSE", "========================================")
    }

    /**
     * 차트 코드에서 타입 추출
     *
     * 예: "SOLO_M" -> "SOLO", "GROUP_F" -> "GROUP"
     */
    private fun extractTypeFromCode(code: String): String {
        return when {
            code.startsWith("SOLO") -> "SOLO"
            code.startsWith("GROUP") -> "GROUP"
            else -> code
        }
    }
}
