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
import net.ib.mn.util.logD
import net.ib.mn.util.logW
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
import net.ib.mn.util.NumberFormatUtil
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

    // init에서 initialize() 호출하지 않음
    // Navigation3에서 ViewModel이 재사용될 수 있으므로 Screen의 LaunchedEffect에서 호출

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

                // 앱 시작 시 최애 이동 토스트 초기화 (old 프로젝트의 StartupActivity와 동일)
                preferencesManager.resetMyFavToast()

                // 앱 시작 시 하트박스 표시 가능하도록 초기화 (old 프로젝트의 StartupActivity/MainActivity와 동일)
                // 하트박스 안나온다는 사람이 많아서 앱 실행 때마다 true로 설정
                preferencesManager.setHeartBoxViewable(true)

                // Step 0: 저장된 인증 정보 확인 (old 프로젝트의 IdolAccount.getAccount() 역할)
                // AuthRepository를 통해 인증 정보 유효성 확인

                val hasValidCredentials = authRepository.hasValidCredentialsAsync()

                if (hasValidCredentials) {

                } else {
                    logW("USER_INFO", "========================================")
                    logW("USER_INFO", "[StartUpViewModel] ⚠️ Auth credentials incomplete or missing")
                    logW("USER_INFO", "[StartUpViewModel] User not logged in - navigating to Login screen")
                    logW("USER_INFO", "========================================")

                    logW(TAG, "⚠️  Auth credentials incomplete - user not logged in (guest mode)")
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

        // Phase 1: ConfigSelf 먼저 호출 (필수 전제조건)
        // old 코드: async { getConfigSelf(context) }.await()
        loadConfigSelf()

        // Phase 1-1: InAppBanner 로드
        loadInAppBanner()

        // Phase 2: ConfigStartup (critical path - 실패 시 중단)
        // old 코드: val isStartupSuccess = async { getConfigStartup(context) }.await()
        val isStartupSuccess = loadConfigStartup()

        if (!isStartupSuccess) {
            logW(TAG, "⚠️  This is likely because BASE_URL points to a non-existent server")
            logW(TAG, "⚠️  Check Constants.BASE_URL = \"${Constants.BASE_URL}\"")
            logW(TAG, "⚠️  Clearing all auth credentials and local data...")

            // 모든 인증 정보 및 로컬 데이터 삭제
            preferencesManager.clearAll()

            // 로그인 페이지로 이동
            setState { copy(isLoading = false, progress = 0f, currentStep = "Login required") }
            setEffect { StartUpContract.Effect.NavigateToLogin }
            return
        }


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


        // Phase 4: Initialize chart rankings in SharedPreference
        chartDatabaseRepository.initializeChartsInDatabase()
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

                    data?.badWords?.take(3)?.forEach { badWord ->
                    }
                    data?.boardTags?.take(3)?.forEach { tag ->
                    }
                    data?.snsChannels?.forEach { channel ->
                    }
                    data?.familyAppList?.forEach { app ->
                    }

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

                        // New Picks (하트픽, 원픽 NEW 뱃지)
                        configData.newPicks?.let { newPicks ->
                            preferencesManager.setNewPicks(
                                heartpick = newPicks.heartpick,
                                onepick = newPicks.onepick,
                                themepick = newPicks.themepick
                            )
                        }

                    }
                    // NOTE: 메모리 캐싱이 필요한 경우 구현 방법:
                    // 1. Application 클래스에 ConfigCache 싱글톤 생성
                    // 2. 또는 Hilt SingletonComponent로 ConfigRepository 제공
                    // 3. 현재는 DataStore만 사용하며, 필요시 Flow로 실시간 데이터 접근 가능
                }
                    is ApiResult.Error -> {
                        // 에러 처리
                        isSuccess = false

                        // ConfigStartup은 critical path이므로 실패 시 전체 초기화 중단
                    }
                }
            }
        } catch (e: Exception) {
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


                    // DataStore에 UDP 설정 저장
                    data.udpBroadcastUrl?.let {
                        preferencesManager.setUdpBroadcastUrl(it)
                    }

                    preferencesManager.setUdpStage(data.udpStage)

                    // CDN URL 저장
                    data.cdnUrl?.let {
                        preferencesManager.setCdnUrl(it)
                    }

                    // Video Heart 저장
                    preferencesManager.setVideoHeart(data.videoHeart)

                    // Menu Config 저장
                    preferencesManager.setMenuNoticeMain(data.menuNoticeMain)
                    preferencesManager.setMenuStoreMain(data.menuStoreMain)
                    preferencesManager.setMenuFreeBoardMain(data.menuFreeBoardMain)
                    preferencesManager.setShowStoreEventMarker(data.showStoreEventMarker)
                    preferencesManager.setShowFreeChargeMarker(data.showFreeChargeMarker)
                    preferencesManager.setShowLiveStreamingTab(data.showLiveStreamingTab)

                }
                    is ApiResult.Error -> {
                    }
                }
            }
        } catch (e: Exception) {
        }
    }

    /**
     * InAppBanner API 호출
     */
    private suspend fun loadInAppBanner() {
        try {
            val response = configsApi.getInAppBanner()

            if (response.isSuccessful && response.body() != null) {
                val responseBody = response.body()!!.string()

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
                        } else {
                            preferencesManager.setInAppBannerMenu(null)
                        }
                    } else {
                        preferencesManager.setInAppBannerMenu(null)
                    }
                } else {
                }
            } else {
            }
        } catch (e: Exception) {
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


                    // DataStore에 저장 및 기존 플래그와 비교
                    data?.let { updateData ->
                        // 기존 플래그 가져오기
                        val oldAllIdolUpdate = preferencesManager.allIdolUpdate.first()
                        val oldDailyIdolUpdate = preferencesManager.dailyIdolUpdate.first()
                        val oldSnsChannelUpdate = preferencesManager.snsChannelUpdate.first()

                        // 플래그 비교 및 동기화 필요 여부 로그
                        updateData.allIdolUpdate?.let { newFlag ->
                            if (oldAllIdolUpdate != newFlag) {
                            }
                            preferencesManager.setAllIdolUpdate(newFlag)
                        }

                        updateData.dailyIdolUpdate?.let { newFlag ->
                            if (oldDailyIdolUpdate != newFlag) {
                            }
                            preferencesManager.setDailyIdolUpdate(newFlag)
                        }

                        updateData.snsChannelUpdate?.let { newFlag ->
                            if (oldSnsChannelUpdate != newFlag) {
                            }
                            preferencesManager.setSnsChannelUpdate(newFlag)
                        }

                    }
                }
                is ApiResult.Error -> {
                }
            }
        }
        } catch (e: Exception) {
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
        }
    }

    /**
     * FavoritesSelf API 호출
     */
    private suspend fun loadAndSaveFavoriteSelf() {
        try {
            favoritesRepository.loadAndSaveFavoriteSelf()
        } catch (e: Exception) {
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


                    // 사용자 상태 DataStore 저장
                    data?.let { statusData ->
                        statusData.tutorialCompleted?.let { preferencesManager.setTutorialCompleted(it) }
                        statusData.firstLogin?.let { preferencesManager.setFirstLogin(it) }

                    }
                }
                is ApiResult.Error -> {
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

                    data?.forEach { adType ->
                    }
                }
                is ApiResult.Error -> {
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

                    data?.forEach { coupon ->
                    }
                }
                is ApiResult.Error -> {
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
                }
                is ApiResult.Error -> {
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


                    // Room Database에 저장
                    data?.let { idolList ->
                        // Top3 데이터 로깅 (디버깅용 - 첫 5개만)
                        idolList.take(5).forEach { idol ->
                        }

                        val entities = idolList.map { it.toEntity() }
                        idolDao.insert(entities)  // old 프로젝트와 동일한 메서드명

                        // 저장된 데이터 검증 (디버깅용 - 첫 5개만)
                        entities.take(5).forEach { entity ->
                        }
                    }
                }
                is ApiResult.Error -> {
                }
            }
        }
        } catch (e: Exception) {
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

        try {
            getTypeListUseCase(forceRefresh = true).collect { typeListData ->

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

                arrayTypeList.forEachIndexed { index, type ->
                }

            }
        } catch (e: Exception) {
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

        try {
            val response = chartsApi.getChartsCurrent()


            if (response.isSuccessful && response.body() != null) {
                val body = response.body()!!

                if (body.success) {
                    // MainChartModel 저장 (old 프로젝트와 동일)
                    body.main?.let { mainChartModel ->
                        configRepository.setMainChartModel(mainChartModel)
                    }

                    // ChartObjects 저장 (MIRACLE, ROOKIE 등)
                    body.objects?.let { objects ->
                        configRepository.setChartObjects(objects)
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

                    typeListData.forEachIndexed { index, type ->
                    }
                } else {
                }
            } else {
            }
        } catch (e: Exception) {
        }

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

        try {
            // 5개 차트 코드 정의
            val chartCodes = listOf("SOLO_M", "SOLO_F", "GROUP_M", "GROUP_F", "GLOBAL")

            coroutineScope {
                chartCodes.map { code ->
                    async {
                        try {
                            val response = chartsApi.getChartIdolIds(code)

                            if (response.isSuccessful && response.body() != null) {
                                val body = response.body()!!
                                if (body.success && body.data != null) {
                                    // SharedPreference에 저장
                                    preferencesManager.saveChartIdolIds(code, body.data)
                                } else {
                                    logW(TAG, "⚠️ No data for chart: $code")
                                }
                            } else {
                            }
                        } catch (e: Exception) {
                        }
                    }
                }.awaitAll()
            }

        } catch (e: Exception) {
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
        return NumberFormatUtil.formatWithComma(count)
    }
}
