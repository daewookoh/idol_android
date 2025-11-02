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
                        configData.language?.let { preferencesManager.setLanguage(it) }
                        configData.theme?.let { preferencesManager.setTheme(it) }
                        configData.pushEnabled?.let { preferencesManager.setPushEnabled(it) }
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
    }

    /**
     * UserSelf API 호출 (사용자 프로필, ETag 지원)
     */
    private suspend fun loadUserSelf() {
        // DataStore에서 저장된 ETag 가져오기
        val etag = preferencesManager.userSelfETag.first()

        android.util.Log.d("USER_INFO", "========================================")
        android.util.Log.d("USER_INFO", "[StartUpViewModel] Loading user info from server")
        android.util.Log.d("USER_INFO", "[StartUpViewModel] ETag: $etag")
        android.util.Log.d("USER_INFO", "========================================")

        getUserSelfUseCase(etag).collect { result ->
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
                    android.util.Log.d("USER_INFO", "========================================")

                    // 사용자 정보 DataStore 저장
                    data?.let { userData ->
                        android.util.Log.d("USER_INFO", "[StartUpViewModel] Saving user info to DataStore...")

                        // domain이 null이면 저장된 loginDomain 사용 (old 프로젝트와 동일)
                        val userDomain = userData.domain ?: preferencesManager.loginDomain.first()

                        // setUserInfo 호출 (suspend 함수이므로 완료될 때까지 기다림)
                        preferencesManager.setUserInfo(
                            id = userData.id,
                            email = userData.email,
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

                    // Success나 Error가 나오면 수집 종료 (Flow 완료)
                    return@collect
                }
                is ApiResult.Error -> {
                    if (result.code == 304) {
                        // 캐시 유효 - 로컬 데이터 사용
                        android.util.Log.d("USER_INFO", "[StartUpViewModel] UserSelf cache valid (304 Not Modified)")
                        android.util.Log.d("USER_INFO", "[StartUpViewModel] Using cached user info from DataStore")
                        android.util.Log.d(TAG, "UserSelf cache valid (304 Not Modified)")
                    } else {
                        android.util.Log.e("USER_INFO", "[StartUpViewModel] ❌ UserSelf API error: ${result.message}")
                        android.util.Log.e(TAG, "UserSelf error: ${result.message}")
                    }
                    // Success나 Error가 나오면 수집 종료 (Flow 완료)
                    return@collect
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
        getIdolsUseCase().collect { result ->
            when (result) {
                is ApiResult.Loading -> {}
                is ApiResult.Success -> {
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

                    // Room Database에 저장
                    data?.let { idolList ->
                        val entities = idolList.map { it.toEntity() }
                        idolDao.insertIdols(entities)
                        android.util.Log.d(TAG, "✓ ${entities.size} idols saved to Room Database")
                    }
                }
                is ApiResult.Error -> {
                    android.util.Log.e(TAG, "Idols error: ${result.message}")
                }
            }
        }
    }
}
