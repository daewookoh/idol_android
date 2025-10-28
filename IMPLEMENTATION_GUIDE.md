# StartUp 비즈니스 로직 구현 가이드

## 📋 목차

1. [완성된 구현 예시](#완성된-구현-예시)
2. [아키텍처 개요](#아키텍처-개요)
3. [레이어별 확장 가이드](#레이어별-확장-가이드)
4. [나머지 API 구현 체크리스트](#나머지-api-구현-체크리스트)
5. [데이터 흐름](#데이터-흐름)
6. [보안 및 IAB 구현](#보안-및-iab-구현)

---

## ✅ 완성된 구현 예시

### ConfigStartup API 플로우 (100% 완성)

전체 플로우를 하나의 완벽한 예시로 구현했습니다:

```
ConfigApi 인터페이스
    ↓
ConfigRepository (인터페이스)
    ↓
ConfigRepositoryImpl (구현)
    ↓
GetConfigStartupUseCase
    ↓
StartUpViewModel
    ↓
StartUpScreen (UI)
```

### 구현된 파일들:

1. **API Layer**
   - `data/remote/dto/ConfigStartupResponse.kt` - API 모델
   - `data/remote/api/ConfigApi.kt` - API 인터페이스
   - `data/remote/interceptor/AuthInterceptor.kt` - 인증 인터셉터

2. **Repository Layer**
   - `domain/repository/ConfigRepository.kt` - 인터페이스
   - `data/repository/ConfigRepositoryImpl.kt` - 구현체

3. **UseCase Layer**
   - `domain/usecase/GetConfigStartupUseCase.kt`

4. **ViewModel Layer**
   - `presentation/startup/StartUpViewModel.kt` - 통합 완료

5. **DI Layer**
   - `di/NetworkModule.kt` - Retrofit, OkHttp 설정
   - `di/RepositoryModule.kt` - Repository 바인딩

6. **Utilities**
   - `util/Constants.kt` - 상수 정의
   - `domain/model/ApiResult.kt` - 결과 래퍼
   - `data/local/PreferencesManager.kt` - DataStore

---

## 🏗️ 아키텍처 개요

### Clean Architecture + MVVM

```
┌─────────────────────────────────────────┐
│           Presentation Layer            │
│  ┌──────────────────────────────────┐   │
│  │  Screen (Composable)             │   │
│  └────────────┬─────────────────────┘   │
│               │                          │
│  ┌────────────▼─────────────────────┐   │
│  │  ViewModel (MVI)                 │   │
│  │  - State                         │   │
│  │  - Intent                        │   │
│  │  - Effect                        │   │
│  └────────────┬─────────────────────┘   │
└───────────────┼─────────────────────────┘
                │
┌───────────────▼─────────────────────────┐
│            Domain Layer                  │
│  ┌──────────────────────────────────┐   │
│  │  UseCase                         │   │
│  │  - Business Logic                │   │
│  └────────────┬─────────────────────┘   │
│               │                          │
│  ┌────────────▼─────────────────────┐   │
│  │  Repository Interface            │   │
│  └────────────┬─────────────────────┘   │
└───────────────┼─────────────────────────┘
                │
┌───────────────▼─────────────────────────┐
│             Data Layer                   │
│  ┌──────────────────────────────────┐   │
│  │  Repository Implementation       │   │
│  └──────┬───────────────┬───────────┘   │
│         │               │                │
│  ┌──────▼──────┐ ┌──────▼──────┐        │
│  │ Remote      │ │ Local       │        │
│  │ (Retrofit)  │ │ (Room/      │        │
│  │             │ │  DataStore) │        │
│  └─────────────┘ └─────────────┘        │
└─────────────────────────────────────────┘
```

---

## 🔧 레이어별 확장 가이드

### 1. API 모델 추가 (Data Layer)

**위치**: `data/remote/dto/`

**패턴**:
```kotlin
// 1. Response 모델
data class XXXResponse(
    @SerializedName("success")
    val success: Boolean,

    @SerializedName("data")
    val data: XXXData?
)

// 2. Data 모델
data class XXXData(
    @SerializedName("field1")
    val field1: String,

    @SerializedName("field2")
    val field2: Int?
)
```

**예시**: `ConfigStartupResponse.kt` 참고

**TODO 목록**:
- [ ] UpdateInfoResponse (완성도: 50%, OtherResponses.kt에 기본 구조 있음)
- [ ] UserSelfResponse (완성도: 50%)
- [ ] UserStatusResponse (완성도: 50%)
- [ ] AdTypeListResponse (완성도: 50%)
- [ ] MessageCouponResponse (완성도: 50%)
- [ ] TimezoneUpdateResponse (완성도: 50%)
- [ ] IdolListResponse (완성도: 50%)
- [ ] IabKeyResponse (완성도: 50%)
- [ ] BlockListResponse (완성도: 50%)
- [ ] OfferWallRewardResponse (없음)
- [ ] TypeListResponse (CELEB flavor, 없음)
- [ ] PaymentVerificationResponse (없음)
- [ ] SubscriptionCheckResponse (없음)

---

### 2. API 인터페이스 추가 (Data Layer)

**위치**: `data/remote/api/`

**패턴**:
```kotlin
interface XXXApi {
    @GET("endpoint/")
    suspend fun getXXX(
        @Header("Authorization") authorization: String
    ): Response<XXXResponse>

    @POST("endpoint/")
    suspend fun postXXX(
        @Body request: XXXRequest
    ): Response<XXXResponse>
}
```

**예시**: `ConfigApi.kt` 참고

**TODO 목록**:
- [x] ConfigApi (100% 완성)
- [ ] UserApi (기본 구조만 있음, ApiInterfaces.kt)
- [ ] IdolApi (기본 구조만 있음)
- [ ] AdApi (기본 구조만 있음)
- [ ] MessageApi (기본 구조만 있음)
- [ ] UtilityApi (기본 구조만 있음)
- [ ] PaymentApi (없음)

---

### 3. Repository 추가 (Domain + Data Layer)

**위치**: 
- Interface: `domain/repository/`
- Implementation: `data/repository/`

**패턴**:

```kotlin
// domain/repository/XXXRepository.kt
interface XXXRepository {
    fun getXXX(): Flow<ApiResult<XXXResponse>>
}

// data/repository/XXXRepositoryImpl.kt
class XXXRepositoryImpl @Inject constructor(
    private val xxxApi: XXXApi
) : XXXRepository {

    override fun getXXX(): Flow<ApiResult<XXXResponse>> = flow {
        emit(ApiResult.Loading)

        try {
            val response = xxxApi.getXXX()

            if (response.isSuccessful && response.body() != null) {
                val body = response.body()!!

                if (body.success) {
                    emit(ApiResult.Success(body))
                } else {
                    emit(ApiResult.Error(
                        exception = Exception("API returned success=false"),
                        code = response.code()
                    ))
                }
            } else {
                emit(ApiResult.Error(
                    exception = HttpException(response),
                    code = response.code()
                ))
            }
        } catch (e: HttpException) {
            emit(ApiResult.Error(
                exception = e,
                code = e.code(),
                message = "HTTP ${e.code()}: ${e.message()}"
            ))
        } catch (e: IOException) {
            emit(ApiResult.Error(
                exception = e,
                message = "Network error: ${e.message}"
            ))
        } catch (e: Exception) {
            emit(ApiResult.Error(
                exception = e,
                message = "Unknown error: ${e.message}"
            ))
        }
    }
}
```

**DI 바인딩** (`di/RepositoryModule.kt`):
```kotlin
@Binds
@Singleton
abstract fun bindXXXRepository(
    impl: XXXRepositoryImpl
): XXXRepository
```

**예시**: `ConfigRepositoryImpl.kt` 참고

**TODO 목록**:
- [x] ConfigRepository (100% 완성)
- [ ] UserRepository
- [ ] IdolRepository
- [ ] AdRepository
- [ ] MessageRepository
- [ ] PaymentRepository

---

### 4. UseCase 추가 (Domain Layer)

**위치**: `domain/usecase/`

**패턴**:
```kotlin
class GetXXXUseCase @Inject constructor(
    private val xxxRepository: XXXRepository
) {
    operator fun invoke(): Flow<ApiResult<XXXResponse>> {
        return xxxRepository.getXXX()
    }

    // 또는 파라미터가 있는 경우:
    operator fun invoke(param1: String): Flow<ApiResult<XXXResponse>> {
        return xxxRepository.getXXX(param1)
    }
}
```

**예시**: `GetConfigStartupUseCase.kt` 참고

**TODO 목록**:
- [x] GetConfigStartupUseCase (100% 완성)
- [ ] GetConfigSelfUseCase
- [ ] GetUpdateInfoUseCase
- [ ] GetUserSelfUseCase
- [ ] GetUserStatusUseCase
- [ ] GetAdTypeListUseCase
- [ ] GetMessageCouponUseCase
- [ ] UpdateTimezoneUseCase
- [ ] GetIdolsUseCase
- [ ] GetIabKeyUseCase
- [ ] GetBlocksUseCase
- [ ] VerifyIabPurchaseUseCase
- [ ] CheckSubscriptionUseCase

---

### 5. ViewModel 통합 (Presentation Layer)

**위치**: `presentation/startup/StartUpViewModel.kt`

**패턴**:
```kotlin
// 1. UseCase 주입
@HiltViewModel
class StartUpViewModel @Inject constructor(
    private val getXXXUseCase: GetXXXUseCase,
) : BaseViewModel<...>() {

    // 2. API 호출 메서드
    private suspend fun loadXXX() {
        getXXXUseCase().collect { result ->
            when (result) {
                is ApiResult.Loading -> {
                    // 로딩 처리
                }
                is ApiResult.Success -> {
                    val data = result.data.data
                    // 데이터 처리 (DataStore 저장 등)
                }
                is ApiResult.Error -> {
                    // 에러 처리
                    Log.e(TAG, "XXX error: ${result.message}")
                }
            }
        }
    }

    // 3. initialize() 메서드에서 호출
    private fun initialize() {
        viewModelScope.launch {
            try {
                updateProgress(0.3f, "Loading XXX...")
                loadXXX()
                updateProgress(0.4f, "XXX loaded")
                // ...
            } catch (e: Exception) {
                handleError(e)
            }
        }
    }
}
```

**예시**: `StartUpViewModel.kt`의 `loadConfigStartup()` 참고

**TODO 목록**:
- [x] loadConfigStartup() (100% 완성)
- [ ] loadConfigSelf()
- [ ] loadUpdateInfo()
- [ ] loadUserSelf()
- [ ] loadUserStatus()
- [ ] loadAdTypeList()
- [ ] loadMessageCoupon()
- [ ] updateTimezone()
- [ ] loadIdols()
- [ ] checkIAB()
- [ ] checkSubscriptions()

---

## 📝 나머지 API 구현 체크리스트

### Priority 1: 필수 API (앱 시작에 필요)

1. **ConfigSelf** (사용자 설정)
   - [ ] Model: `ConfigSelfResponse`
   - [ ] API: `ConfigApi.getConfigSelf()`
   - [ ] Repository: `ConfigRepository.getConfigSelf()`
   - [ ] UseCase: `GetConfigSelfUseCase`
   - [ ] ViewModel: `loadConfigSelf()`

2. **UpdateInfo** (Idol 업데이트 플래그)
   - [ ] Model: `UpdateInfoResponse`
   - [ ] API: `IdolApi.getUpdateInfo()`
   - [ ] Repository: `IdolRepository.getUpdateInfo()`
   - [ ] UseCase: `GetUpdateInfoUseCase`
   - [ ] ViewModel: `loadUpdateInfo()`

3. **UserSelf** (사용자 프로필)
   - [ ] Model: `UserSelfResponse`
   - [ ] API: `UserApi.getUserSelf()`
   - [ ] Repository: `UserRepository.getUserSelf()`
   - [ ] UseCase: `GetUserSelfUseCase`
   - [ ] ViewModel: `loadUserSelf()`
   - [ ] 특이사항: ETag 캐싱 지원 필요

### Priority 2: 중요 API

4. **UserStatus** (튜토리얼 상태)
5. **AdTypeList** (광고 타입)
6. **MessageCoupon** (쿠폰 메시지)
7. **TimezoneUpdate** (타임존 동기화)
8. **Idols** (아이돌 리스트)

### Priority 3: 선택적 API

9. **IabKey** (IAB 공개키)
10. **Blocks** (차단 사용자)
11. **OfferWallReward** (조건부)
12. **TypeList** (CELEB flavor만)

---

## 🔄 데이터 흐름

### 1. API → DataStore 저장

```kotlin
// ViewModel에서
private suspend fun loadConfigStartup() {
    getConfigStartupUseCase().collect { result ->
        when (result) {
            is ApiResult.Success -> {
                val data = result.data.data

                // DataStore에 저장
                preferencesManager.setBadWords(data.badWords)
                preferencesManager.setNoticeList(data.noticeList)
                // ...
            }
            // ...
        }
    }
}
```

### 2. DataStore → UI 표시

```kotlin
// ViewModel에서
val badWords: StateFlow<List<String>> = preferencesManager.badWords
    .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )
```

---

## 🔒 보안 및 IAB 구현

### 1. VM/Rooting 감지

**위치**: `util/SecurityUtil.kt` (TODO)

```kotlin
object SecurityUtil {
    fun isRunningOnEmulator(): Boolean {
        val packages = Constants.EMULATOR_PACKAGES
        // 패키지 설치 여부 체크
        return packages.any { /* check if installed */ }
    }

    fun isDeviceRooted(): Boolean {
        // Root 권한 감지 로직
        return false // TODO
    }
}
```

### 2. IAB 공개키 복호화

**위치**: `util/BillingUtil.kt` (TODO)

```kotlin
object BillingUtil {
    /**
     * XOR 방식으로 암호화된 IAB 공개키 복호화
     *
     * old 프로젝트의 checkKey() 로직
     */
    fun decryptIabKey(encryptedKey: String): String {
        val key1 = encryptedKey.substring(encryptedKey.length - 7)
        val data = encryptedKey.substring(0, encryptedKey.length - 7)
        val pKey = xor(data.toByteArray(), key1.toByteArray())
        return String(pKey)
    }

    private fun xor(data: ByteArray, key: ByteArray): ByteArray {
        val result = ByteArray(data.size)
        for (i in data.indices) {
            result[i] = (data[i].toInt() xor key[i % key.size].toInt()).toByte()
        }
        return result
    }
}
```

### 3. Billing Manager 통합

**위치**: `util/BillingManager.kt` (TODO)

```kotlin
class BillingManager(
    private val activity: Activity,
    private val publicKey: String,
    private val listener: BillingUpdatesListener
) {
    private val billingClient: BillingClient = // ...

    fun startConnection() {
        // BillingClient 연결
    }

    fun queryPurchases() {
        // 구매 내역 조회
    }

    fun consumeAsync(purchaseToken: String) {
        // 소비 처리
    }

    interface BillingUpdatesListener {
        fun onBillingClientSetupFinished()
        fun onPurchasesUpdated(
            billingResult: BillingResult,
            purchases: List<Purchase>?
        )
    }
}
```

---

## 📚 추가 참고 자료

### 1. old 프로젝트 분석 문서
- **STARTUP_ANALYSIS.md** (1,447줄)
  - 전체 플로우 상세 분석
  - API 엔드포인트 목록
  - 코드 스니펫

### 2. 아키텍처 패턴
- **MVI (Model-View-Intent)**
  - State: UI 상태
  - Intent: 사용자 액션
  - Effect: 일회성 이벤트

- **Clean Architecture**
  - Domain: 비즈니스 로직
  - Data: 데이터 소스
  - Presentation: UI

### 3. Kotlin Coroutines
- `Flow`: 비동기 스트림
- `StateFlow`: 상태 관리
- `viewModelScope`: ViewModel 생명주기 연동

---

## 🎯 다음 단계

1. **Priority 1 API 구현** (ConfigSelf, UpdateInfo, UserSelf)
   - 패턴: ConfigStartup 플로우 참고
   - 예상 시간: 각 2-3시간

2. **DataStore 확장**
   - 나머지 40개 키 추가
   - JSON 리스트 저장 로직

3. **Room Database 구현**
   - IdolEntity, NotificationEntity
   - DAO 생성
   - Migration 전략

4. **IAB 및 보안 구현**
   - BillingManager
   - SecurityUtil
   - VM 감지

5. **에러 핸들링 개선**
   - HTTP 401 → 재인증
   - HTTP 88888 → 점검 화면
   - HTTP 8000 → 구독 문제

---

## 💡 팁

1. **한 번에 하나씩**: ConfigStartup 패턴을 따라 한 API씩 완성
2. **테스트 우선**: MockRepository로 ViewModel 테스트
3. **로그 활용**: 각 단계마다 로그 출력하여 디버깅
4. **점진적 확장**: 기본 플로우 완성 → 에러 처리 → 최적화

---

**작성일**: 2025-01-XX
**버전**: 1.0
**작성자**: Claude Code Assistant
