# 마이그레이션 가이드 01: 프로젝트 구조 비교 및 분석

## 📋 목차
1. [프로젝트 개요](#프로젝트-개요)
2. [빌드 시스템 비교](#빌드-시스템-비교)
3. [모듈 구조 비교](#모듈-구조-비교)
4. [패키지 구조 비교](#패키지-구조-비교)
5. [주요 차이점](#주요-차이점)

---

## 프로젝트 개요

### Old 프로젝트 (`old/`)
- **언어**: Kotlin (Java 혼재)
- **UI 프레임워크**: View System (XML Layouts) + ViewBinding + DataBinding
- **아키텍처**: MVVM + LiveData + Hilt
- **네비게이션**: Fragment 기반 + Activity 전환
- **상태 관리**: LiveData, MutableLiveData, StateFlow (부분적)
- **의존성 주입**: Hilt (Dagger)

### 현재 프로젝트 (`app/`)
- **언어**: Kotlin (100%)
- **UI 프레임워크**: Jetpack Compose
- **아키텍처**: MVI (Model-View-Intent) + Hilt
- **네비게이션**: Navigation Compose
- **상태 관리**: StateFlow + Channel (MVI 패턴)
- **의존성 주입**: Hilt (Dagger)

---

## 빌드 시스템 비교

### Old 프로젝트
```kotlin
// old/build.gradle.kts
- Multi-module 구조 (17개 모듈)
- JVM Target: 17
- Compose Compiler: 1.5.x
- AGP: 8.x
- Kotlin: 2.1.10
```

**주요 모듈**:
- `:app` - 메인 앱 모듈
- `:tnk_rwd` - 광고 모듈
- `:exodusimagepicker` - 이미지 피커
- `:admob` - AdMob 모듈
- `:core:data`, `:core:domain`, `:core:model` - 코어 모듈
- `:local`, `:data`, `:domain` - 레이어별 모듈
- `:common`, `:component`, `:bridge` - 공통 모듈

### 현재 프로젝트
```kotlin
// build.gradle.kts
- Single-module 구조 (app 모듈만)
- JVM Target: 11
- Compose Compiler: 2.0.21 (최신)
- AGP: 8.12.1
- Kotlin: 2.0.21
```

**버전 관리**:
- `gradle/libs.versions.toml` 사용 (Version Catalog)
- 모든 의존성 버전 중앙 관리

---

## 모듈 구조 비교

### Old 프로젝트 모듈 구조
```
old/
├── app/                    # 메인 앱 모듈
│   ├── src/
│   │   ├── app/           # app flavor 전용
│   │   ├── celeb/         # celeb flavor 전용
│   │   ├── china/         # china flavor 전용
│   │   ├── onestore/      # onestore flavor 전용
│   │   └── main/          # 공통 소스
│   └── build.gradle.kts
├── core/
│   ├── data/              # 데이터 레이어
│   ├── domain/           # 도메인 레이어
│   ├── model/            # 모델 정의
│   ├── designSystem/     # 디자인 시스템
│   └── utils/            # 유틸리티
├── data/                 # 데이터 구현
├── domain/              # 도메인 구현
├── local/               # 로컬 데이터소스
├── common/              # 공통 컴포넌트
└── component/          # UI 컴포넌트
```

### 현재 프로젝트 모듈 구조
```
app/
├── src/
│   ├── main/
│   │   ├── java/net/ib/mn/
│   │   │   ├── base/              # Base 클래스 (ViewModel, State 등)
│   │   │   ├── data/              # 데이터 레이어
│   │   │   │   ├── local/         # Room, DataStore
│   │   │   │   ├── remote/        # Retrofit, API
│   │   │   │   ├── model/         # 데이터 모델
│   │   │   │   └── repository/   # Repository 구현
│   │   │   ├── domain/            # 도메인 레이어
│   │   │   │   ├── model/         # 도메인 모델
│   │   │   │   ├── repository/    # Repository 인터페이스
│   │   │   │   └── usecase/      # UseCase
│   │   │   ├── di/                # Hilt 모듈
│   │   │   ├── presentation/      # Compose 화면
│   │   │   │   ├── startup/      # StartupScreen
│   │   │   │   ├── login/         # LoginScreen
│   │   │   │   ├── signup/        # SignUpScreen
│   │   │   │   └── main/          # MainScreen
│   │   │   ├── ui/                # UI 컴포넌트
│   │   │   │   ├── components/    # 재사용 가능한 Compose 컴포넌트
│   │   │   │   └── theme/         # Material Theme
│   │   │   ├── navigation/        # Navigation Graph
│   │   │   └── util/              # 유틸리티
│   │   ├── app/                   # app flavor 리소스
│   │   ├── celeb/                 # celeb flavor 리소스
│   │   ├── china/                 # china flavor 리소스
│   │   └── onestore/             # onestore flavor 리소스
│   └── test/
└── build.gradle.kts
```

---

## 패키지 구조 비교

### Old 프로젝트 패키지 구조
```
net.ib.mn/
├── activity/              # Activity 클래스들 (80+ 개)
│   ├── MainActivity.kt
│   ├── StartupActivity.kt
│   ├── FeedActivity.kt
│   ├── BaseActivity.kt
│   └── ...
├── fragment/             # Fragment 클래스들 (40+ 개)
├── viewmodel/            # ViewModel 클래스들
├── adapter/              # RecyclerView Adapter들
├── model/                # 데이터 모델들
├── utils/                # 유틸리티 클래스들
├── dialog/               # Dialog Fragment들
├── view/                 # Custom View들
└── ...
```

### 현재 프로젝트 패키지 구조
```
net.ib.mn/
├── base/                 # Base 클래스들
│   ├── BaseViewModel.kt  # MVI Base ViewModel
│   ├── UiState.kt
│   ├── UiIntent.kt
│   └── UiEffect.kt
├── data/                 # 데이터 레이어
│   ├── local/            # Room, DataStore
│   ├── remote/           # Retrofit APIs
│   ├── model/            # 데이터 모델
│   └── repository/       # Repository 구현체
├── domain/               # 도메인 레이어
│   ├── model/            # 도메인 모델
│   ├── repository/       # Repository 인터페이스
│   └── usecase/          # UseCase
├── presentation/         # Compose 화면들
│   ├── startup/         # Startup Screen (Contract, Screen, ViewModel)
│   ├── login/            # Login Screen
│   ├── signup/           # SignUp Screen
│   └── main/             # Main Screen
├── ui/                   # UI 컴포넌트
│   ├── components/      # 재사용 가능한 Compose 컴포넌트
│   └── theme/            # Material Theme
├── navigation/           # Navigation Graph
├── di/                   # Hilt 모듈들
└── util/                 # 유틸리티
```

---

## 주요 차이점

### 1. 아키텍처 패턴

#### Old 프로젝트
- **MVVM (Model-View-ViewModel)**
  - View: Activity/Fragment + XML Layout
  - ViewModel: LiveData 기반
  - Model: Repository + UseCase

```kotlin
// Old ViewModel 예시
@HiltViewModel
class MainViewModel @Inject constructor(
    private val repository: SomeRepository
) : ViewModel() {
    private val _state = MutableLiveData<State>()
    val state: LiveData<State> = _state
    
    fun loadData() {
        viewModelScope.launch {
            _state.value = State.Loading
            // ...
        }
    }
}
```

#### 현재 프로젝트
- **MVI (Model-View-Intent)**
  - View: Compose Screen
  - ViewModel: StateFlow + Channel 기반
  - Intent: 사용자 액션
  - Effect: Side Effect (Navigation, Toast 등)

```kotlin
// 현재 ViewModel 예시
@HiltViewModel
class StartUpViewModel @Inject constructor(
    private val getConfigUseCase: GetConfigStartupUseCase
) : BaseViewModel<StartUpContract.State, StartUpContract.Intent, StartUpContract.Effect>() {
    
    override fun createInitialState() = StartUpContract.State()
    
    override fun handleIntent(intent: StartUpContract.Intent) {
        when (intent) {
            is StartUpContract.Intent.Initialize -> initialize()
            is StartUpContract.Intent.Retry -> retry()
        }
    }
    
    private fun initialize() {
        setState { copy(isLoading = true) }
        // ...
    }
}
```

### 2. UI 구조

#### Old 프로젝트
- **View System**: XML Layouts
- **ViewBinding**: XML에서 자동 생성된 바인딩 클래스
- **DataBinding**: 양방향 데이터 바인딩
- **Fragment**: 화면 일부 또는 전체 화면 관리

```xml
<!-- activity_main.xml -->
<layout>
    <data>
        <variable name="viewModel" type="MainViewModel" />
    </data>
    <LinearLayout>
        <TextView android:text="@{viewModel.title}" />
    </LinearLayout>
</layout>
```

#### 현재 프로젝트
- **Jetpack Compose**: 선언적 UI
- **Stateless Composable**: UI 로직과 상태 분리
- **State Hoisting**: 상태를 상위로 끌어올림

```kotlin
@Composable
fun StartUpScreen(
    onNavigateToMain: () -> Unit,
    viewModel: StartUpViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    
    LaunchedEffect(Unit) {
        viewModel.effect.collectLatest { effect ->
            when (effect) {
                is StartUpContract.Effect.NavigateToMain -> onNavigateToMain()
            }
        }
    }
    
    StartUpContent(state = state)
}
```

### 3. 상태 관리

#### Old 프로젝트
- **LiveData**: 생명주기 인식 (Fragment/Activity)
- **MutableLiveData**: 변경 가능한 상태
- **Event Wrapper**: 일회성 이벤트 처리

```kotlin
// Old 방식
private val _errorToast = MutableLiveData<Event<String>>()
val errorToast: LiveData<Event<String>> = _errorToast

// 사용
_errorToast.value = Event("에러 메시지")
```

#### 현재 프로젝트
- **StateFlow**: Compose와 호환성 좋음
- **Channel**: 일회성 이벤트 (Effect)
- **State + Effect 분리**: 명확한 책임 분리

```kotlin
// 현재 방식
private val _uiState: MutableStateFlow<STATE> = MutableStateFlow(initialState)
val uiState: StateFlow<STATE> = _uiState.asStateFlow()

private val _effect: Channel<EFFECT> = Channel()
val effect = _effect.receiveAsFlow()

// 사용
setState { copy(error = "에러 메시지") }
setEffect { StartUpContract.Effect.ShowError("에러 메시지") }
```

### 4. 네비게이션

#### Old 프로젝트
- **Activity 전환**: Intent 기반
- **Fragment 전환**: FragmentManager
- **Deep Link**: AppLinkActivity에서 처리

```kotlin
// Old 방식
val intent = Intent(this, MainActivity::class.java)
intent.putExtra("key", value)
startActivity(intent)
```

#### 현재 프로젝트
- **Navigation Compose**: 타입 안전한 네비게이션
- **NavGraph**: 선언적 네비게이션 그래프
- **Deep Link**: NavGraph에서 선언

```kotlin
// 현재 방식
NavHost(
    navController = navController,
    startDestination = "startup"
) {
    composable("startup") { StartUpScreen(...) }
    composable("main") { MainScreen(...) }
}
```

### 5. 의존성 주입

#### Old 프로젝트
- **Hilt**: Dagger 기반
- **모듈별 분리**: core, data, local 등 각 모듈에 Module 클래스
- **복잡한 구조**: 여러 모듈 간 의존성

```kotlin
// Old 방식 - 여러 모듈에 분산
@Module
@InstallIn(SingletonComponent::class)
internal object ApiModule { ... }

@Module
@InstallIn(SingletonComponent::class)
internal object RepositoryModule { ... }
```

#### 현재 프로젝트
- **Hilt**: 동일하지만 단일 모듈 내 집중
- **명확한 구조**: NetworkModule, RepositoryModule, DatabaseModule
- **간단한 의존성**: 단일 모듈 내에서 관리

```kotlin
// 현재 방식 - app 모듈 내에 집중
@Module
@InstallIn(SingletonComponent::class)
object NetworkModule { ... }

@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule { ... }
```

---

## 마이그레이션 전략

### 1. 단계별 접근
1. **Screen 단위로 작업**: 한 번에 하나의 Activity를 Compose Screen으로 변환
2. **비즈니스 로직 보존**: ViewModel의 로직은 최대한 유지
3. **UI 완전 동일**: 레이아웃, 색상, 스타일 모두 동일하게 구현

### 2. 우선순위
1. **핵심 화면**: MainActivity, StartupActivity
2. **자주 사용되는 화면**: Login, SignUp
3. **나머지 화면**: 점진적으로 마이그레이션

### 3. 주의사항
- **Flavor별 차이**: app, celeb, china, onestore 각각 확인 필요
- **Fragment 관리**: Old는 Fragment 기반, 현재는 Compose Screen 단위
- **상태 관리**: LiveData → StateFlow 변환 주의
- **네비게이션**: Intent 기반 → Navigation Compose 변환

---

## 다음 문서
- [마이그레이션 가이드 02: 아키텍처 패턴 변환](./MIGRATION_GUIDE_02_ARCHITECTURE.md)
- [마이그레이션 가이드 03: Activity → Compose Screen](./MIGRATION_GUIDE_03_ACTIVITY_TO_COMPOSE.md)
- [마이그레이션 가이드 04: UI 컴포넌트 변환](./MIGRATION_GUIDE_04_UI_COMPONENTS.md)

