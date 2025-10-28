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
     * 모든 Startup API를 병렬로 호출
     *
     * old 프로젝트의 getStartApi() 로직:
     * - ConfigStartup (필수, 먼저 호출)
     * - 나머지 API들 병렬 호출
     */
    private suspend fun loadAllStartupAPIs() {
        // Phase 1: ConfigStartup 먼저 (필수)
        loadConfigStartup()

        // Phase 2: ConfigSelf (사용자 설정 - 선행 필요)
        loadConfigSelf()

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
    }

    /**
     * ConfigStartup API 호출
     *
     * 앱 시작 시 필요한 설정 정보를 조회:
     * - 욕설 필터 리스트
     * - 공지사항, 이벤트 목록
     * - SNS 채널 정보
     * - 업로드 제한 사양
     * - 도움말 정보 등
     */
    private suspend fun loadConfigStartup() {
        getConfigStartupUseCase().collect { result ->
            when (result) {
                is ApiResult.Loading -> {
                    // 로딩 중 (이미 프로그레스로 표시 중)
                }
                is ApiResult.Success -> {
                    val data = result.data.data

                    // TODO: DataStore에 저장
                    // - 욕설 필터: data.badWords
                    // - 공지사항: data.noticeList
                    // - 이벤트: data.eventList
                    // - SNS 채널: data.snsChannels
                    // ... etc

                    // TODO: 메모리에 캐싱 (Application 클래스 또는 싱글톤)
                    // AppConfig.badWords = data.badWords
                    // AppConfig.notices = data.noticeList
                    // ... etc

                    // 성공 로그
                    android.util.Log.d("StartUpViewModel", "ConfigStartup loaded successfully")
                }
                is ApiResult.Error -> {
                    // 에러 처리
                    android.util.Log.e("StartUpViewModel", "ConfigStartup error: ${result.message}")

                    // 치명적이지 않은 에러면 계속 진행
                    // 치명적이면 에러 표시 및 중단
                    // throw result.exception
                }
            }
        }
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
                    android.util.Log.d(TAG, "ConfigSelf loaded")
                    // TODO: DataStore에 저장
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
                    android.util.Log.d(TAG, "UpdateInfo loaded")
                    // TODO: 기존 플래그와 비교하여 동기화 필요 여부 결정
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
        // TODO: DataStore에서 저장된 ETag 가져오기
        val etag = null

        getUserSelfUseCase(etag).collect { result ->
            when (result) {
                is ApiResult.Loading -> {}
                is ApiResult.Success -> {
                    incrementApiProgress()
                    android.util.Log.d(TAG, "UserSelf loaded")
                    // TODO: 사용자 정보 DataStore 저장
                }
                is ApiResult.Error -> {
                    if (result.code == 304) {
                        // 캐시 유효 - 로컬 데이터 사용
                        android.util.Log.d(TAG, "UserSelf cache valid")
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
                    android.util.Log.d(TAG, "UserStatus loaded")
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
                    android.util.Log.d(TAG, "AdTypeList loaded")
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
                    android.util.Log.d(TAG, "MessageCoupon loaded")
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
                    android.util.Log.d(TAG, "Timezone updated")
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
                    android.util.Log.d(TAG, "Idols loaded: ${result.data.data?.size} items")
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
