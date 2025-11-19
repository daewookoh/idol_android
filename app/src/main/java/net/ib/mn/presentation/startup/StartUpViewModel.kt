package net.ib.mn.presentation.startup

import android.content.Context
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import net.ib.mn.base.BaseViewModel
import net.ib.mn.data.remote.dto.toEntity
import net.ib.mn.domain.model.ApiResult
import net.ib.mn.domain.usecase.GetAdTypeListUseCase
import net.ib.mn.domain.usecase.GetBlocksUseCase
import net.ib.mn.domain.usecase.GetConfigSelfUseCase
import net.ib.mn.domain.usecase.GetConfigStartupUseCase
import net.ib.mn.domain.usecase.GetIabKeyUseCase
import net.ib.mn.domain.usecase.GetIdolsUseCase
import net.ib.mn.domain.usecase.GetMessageCouponUseCase
import net.ib.mn.domain.usecase.GetTypeListUseCase
import net.ib.mn.domain.usecase.GetUpdateInfoUseCase
import net.ib.mn.domain.usecase.GetUserSelfUseCase
import net.ib.mn.domain.usecase.GetUserStatusUseCase
import net.ib.mn.domain.usecase.UpdateTimezoneUseCase
import net.ib.mn.util.Constants
import java.text.NumberFormat
import java.util.Locale
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
    @ApplicationContext private val context: Context,
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
    private val rankingRepository: net.ib.mn.domain.repository.RankingRepository,
    private val userRepository: net.ib.mn.domain.repository.UserRepository,
    private val favoritesRepository: net.ib.mn.domain.repository.FavoritesRepository,
    private val chartsApi: net.ib.mn.data.remote.api.ChartsApi,
    private val configsApi: net.ib.mn.data.remote.api.ConfigsApi,
    private val preferencesManager: net.ib.mn.data.local.PreferencesManager,
    private val authRepository: net.ib.mn.data.repository.AuthRepository,
    private val idolDao: net.ib.mn.data.local.dao.IdolDao,
    private val chartDatabaseRepository: net.ib.mn.data.repository.ChartRankingRepository,
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

                // Step 0: 저장된 인증 정보 확인 (old 프로젝트의 IdolAccount.getAccount() 역할)
                // AuthRepository를 통해 인증 정보 유효성 확인
                android.util.Log.d("USER_INFO", "========================================")
                android.util.Log.d("USER_INFO", "[StartUpViewModel] Checking auth credentials via AuthRepository...")

                val hasValidCredentials = authRepository.hasValidCredentialsAsync()

                if (hasValidCredentials) {
                    android.util.Log.d("USER_INFO", "[StartUpViewModel] ✓ Valid auth credentials found")
                    android.util.Log.d("USER_INFO", "[StartUpViewModel] AuthRepository will automatically provide credentials to AuthInterceptor")
                    android.util.Log.d("USER_INFO", "[StartUpViewModel] Ready to make authenticated API calls")
                    android.util.Log.d("USER_INFO", "========================================")

                    android.util.Log.d(TAG, "✓ Auth credentials validated via AuthRepository")
                } else {
                    android.util.Log.w("USER_INFO", "========================================")
                    android.util.Log.w("USER_INFO", "[StartUpViewModel] ⚠️ Auth credentials incomplete or missing")
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

        // Phase 1-1: InAppBanner 로드
        android.util.Log.d(TAG, "Phase 1-1: Loading InAppBanner...")
        loadInAppBanner()

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

        loadIdols()

        // Phase 3: 나머지 APIs 병렬 호출
        coroutineScope {
            val tasks = mutableListOf(
                async { loadUpdateInfo() },
                async { loadAndSaveUserSelf() },
                async { loadAndSaveFavoriteSelf() },
                async { loadUserStatus() },
                async { loadAdTypeList() },
                async { loadMessageCoupon() },
                async { loadTimezone() },
                async { loadChartsCurrent() },
                async { fetchChartIdols() }
                // 조건부: loadBlocks() - 첫 사용자만
            )

            // CELEB 전용: TypeList API 호출
            if (net.ib.mn.BuildConfig.CELEB) {
                tasks.add(async { loadTypeList() })
            }

            awaitAll(*tasks.toTypedArray())
        }

        android.util.Log.d(TAG, "✓ All APIs completed successfully")

        // Phase 4: Initialize chart rankings in SharedPreference
        android.util.Log.d(TAG, "Phase 4: Initializing chart rankings...")
        chartDatabaseRepository.initializeChartsInDatabase()
        android.util.Log.d(TAG, "✓ Chart rankings initialized")
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
                    android.util.Log.d(TAG, "Video Heart: ${data.videoHeart}")
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

                    // Video Heart 저장
                    preferencesManager.setVideoHeart(data.videoHeart)
                    android.util.Log.d(TAG, "✓ Video Heart saved: ${data.videoHeart}")

                    // Menu Config 저장
                    preferencesManager.setMenuNoticeMain(data.menuNoticeMain)
                    preferencesManager.setMenuStoreMain(data.menuStoreMain)
                    preferencesManager.setMenuFreeBoardMain(data.menuFreeBoardMain)
                    preferencesManager.setShowStoreEventMarker(data.showStoreEventMarker)
                    preferencesManager.setShowFreeChargeMarker(data.showFreeChargeMarker)
                    preferencesManager.setShowLiveStreamingTab(data.showLiveStreamingTab)

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
     * InAppBanner API 호출
     */
    private suspend fun loadInAppBanner() {
        try {
            android.util.Log.d(TAG, "Calling InAppBanner API...")
            val response = configsApi.getInAppBanner()

            if (response.isSuccessful && response.body() != null) {
                val responseBody = response.body()!!.string()
                android.util.Log.d(TAG, "InAppBanner response: $responseBody")

                // JSON 파싱
                val jsonObject = org.json.JSONObject(responseBody)
                val success = jsonObject.optBoolean("success", false)

                if (success) {
                    val objectsArray = jsonObject.optJSONArray("objects")

                    if (objectsArray != null && objectsArray.length() > 0) {
                        val bannerList = mutableListOf<net.ib.mn.data.remote.dto.InAppBannerDto>()

                        for (i in 0 until objectsArray.length()) {
                            val bannerObj = objectsArray.getJSONObject(i)
                            bannerList.add(
                                net.ib.mn.data.remote.dto.InAppBannerDto(
                                    id = bannerObj.getInt("id"),
                                    imageUrl = bannerObj.getString("image_url"),
                                    link = if (bannerObj.has("link")) bannerObj.getString("link") else null,
                                    section = if (bannerObj.has("section")) bannerObj.getString("section") else "M"
                                )
                            )
                        }

                        android.util.Log.d(TAG, "✓ InAppBanner API success (${bannerList.size} banners)")

                        // section별로 그룹화
                        val bannersBySection = bannerList.groupBy { it.section }

                        // 메뉴 섹션 배너만 추출
                        val menuBanners = bannersBySection["M"] ?: emptyList()

                        // JSON으로 변환하여 저장
                        if (menuBanners.isNotEmpty()) {
                            val menuBannersJson = Gson().toJson(menuBanners.map { dto ->
                                net.ib.mn.domain.model.InAppBanner(
                                    id = dto.id,
                                    imageUrl = dto.imageUrl,
                                    link = dto.link,
                                    section = dto.section
                                )
                            })
                            preferencesManager.setInAppBannerMenu(menuBannersJson)
                            android.util.Log.d(TAG, "✓ Menu banners saved (${menuBanners.size} banners)")
                        } else {
                            preferencesManager.setInAppBannerMenu(null)
                            android.util.Log.d(TAG, "✓ No menu banners found")
                        }
                    } else {
                        preferencesManager.setInAppBannerMenu(null)
                        android.util.Log.d(TAG, "✓ No banners in response")
                    }
                } else {
                    android.util.Log.e(TAG, "InAppBanner API success=false")
                }
            } else {
                android.util.Log.e(TAG, "InAppBanner API error: ${response.code()}")
            }
        } catch (e: Exception) {
            android.util.Log.e(TAG, "InAppBanner exception: ${e.message}", e)
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
     * 최초 로드이므로 isInitialLoad = true로 호출하여 최애 성별로 defaultCategory 설정
     */
    private suspend fun loadAndSaveUserSelf() {
        try {
            val loadResult = userRepository.loadAndSaveUserSelf(isInitialLoad = true)

            if (loadResult.isFailure) {
                val exception = loadResult.exceptionOrNull()
                if (exception?.message == "Unauthorized") {
                    setEffect { StartUpContract.Effect.NavigateToLogin }
                }
            }
        } catch (e: Exception) {
            android.util.Log.e(TAG, "UserSelf exception: ${e.message}", e)
        }
    }

    /**
     * FavoritesSelf API 호출
     */
    private suspend fun loadAndSaveFavoriteSelf() {
        try {
            favoritesRepository.loadAndSaveFavoriteSelf()
        } catch (e: Exception) {
            android.util.Log.e(TAG, "FavoritesSelf exception: ${e.message}", e)
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
                        // Top3 데이터 로깅 (디버깅용 - 첫 5개만)
                        idolList.take(5).forEach { idol ->
                            android.util.Log.d(TAG, "🖼️ API Data - Idol ${idol.id} (${idol.name}): top3=${idol.top3}, top3Type=${idol.top3Type}, top3ImageVer=${idol.top3ImageVer}")
                        }

                        val entities = idolList.map { it.toEntity() }
                        idolDao.insert(entities)  // old 프로젝트와 동일한 메서드명
                        android.util.Log.d(TAG, "✓ ${entities.size} idols saved to Room Database")

                        // 저장된 데이터 검증 (디버깅용 - 첫 5개만)
                        entities.take(5).forEach { entity ->
                            android.util.Log.d(TAG, "🖼️ Saved to DB - Idol ${entity.id} (${entity.name}): top3=${entity.top3}, top3Type=${entity.top3Type}")
                        }
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
     * 차트별 아이돌 ID 목록을 Room DB에 미리 로드
     *
     * 퍼포먼스 최적화: MyFavorite 페이지에서 5개 차트의 데이터를 빠르게 표시하기 위해
     * StartUp 시점에 미리 Room DB에 저장
     *
     * 변경사항:
     * - RankingCacheRepository (인메모리 캐시) 제거
     * - ChartRankingRepository (Room DB) 사용 - Single Source of Truth
     */
    /**
     * 5개 차트의 아이돌 ID 리스트를 가져와서 SharedPreference에 저장
     *
     * 저장되는 차트:
     * 1. SOLO_M - 개인 남성
     * 2. SOLO_F - 개인 여성
     * 3. GROUP_M - 그룹 남성
     * 4. GROUP_F - 그룹 여성
     * 5. GLOBAL - 글로벌
     */
    private suspend fun fetchChartIdols() {
        android.util.Log.d(TAG, "========================================")
        android.util.Log.d(TAG, "📊 Fetching chart idol IDs...")
        android.util.Log.d(TAG, "========================================")

        try {
            // 5개 차트 코드 정의
            val chartCodes = listOf("SOLO_M", "SOLO_F", "GROUP_M", "GROUP_F", "GLOBAL")

            coroutineScope {
                chartCodes.map { code ->
                    async {
                        try {
                            android.util.Log.d(TAG, "🔄 Fetching idol IDs for chart: $code")
                            val response = chartsApi.getChartIdolIds(code)

                            if (response.isSuccessful && response.body() != null) {
                                val body = response.body()!!
                                if (body.success && body.data != null) {
                                    // SharedPreference에 저장
                                    preferencesManager.saveChartIdolIds(code, body.data)
                                    android.util.Log.d(TAG, "✅ Saved ${body.data.size} idol IDs for $code")
                                } else {
                                    android.util.Log.w(TAG, "⚠️ No data for chart: $code")
                                }
                            } else {
                                android.util.Log.e(TAG, "❌ Failed to fetch chart $code: ${response.code()}")
                            }
                        } catch (e: Exception) {
                            android.util.Log.e(TAG, "❌ Error fetching chart $code: ${e.message}", e)
                        }
                    }
                }.awaitAll()
            }

            android.util.Log.d(TAG, "========================================")
            android.util.Log.d(TAG, "✅ All chart idol IDs fetched and saved")
            android.util.Log.d(TAG, "========================================")
        } catch (e: Exception) {
            android.util.Log.e(TAG, "❌ Failed to fetch chart idols: ${e.message}", e)
        }
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

    /**
     * 하트 수를 포맷팅 (천 단위 콤마)
     */
    private fun formatHeartCount(count: Int): String {
        return NumberFormat.getNumberInstance(Locale.US).format(count)
    }
}
