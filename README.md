# Idol Android - MVI Architecture Sample

세련된 Jetpack Compose와 MVI 아키텍처를 사용한 Android 샘플 프로젝트입니다.

## 기술 스택

### UI Layer
- **Jetpack Compose** - 최신 선언형 UI 프레임워크
- **Material 3** - 최신 Material Design 구현
- **Coil** - 이미지 로딩 라이브러리

### Architecture
- **MVI (Model-View-Intent)** - 단방향 데이터 흐름 아키텍처
- **Clean Architecture** - Domain, Data, Presentation 레이어 분리
- **MVVM + MVI** - ViewModel 기반 상태 관리

### Dependency Injection
- **Hilt** - Dagger 기반 의존성 주입 프레임워크

### Networking
- **Retrofit** - REST API 클라이언트
- **OkHttp** - HTTP 클라이언트
- **Kotlinx Serialization** - JSON 직렬화/역직렬화

### Async
- **Kotlin Coroutines** - 비동기 프로그래밍
- **Flow** - 반응형 데이터 스트림

## 프로젝트 구조

```
app/src/main/java/com/example/idol_android/
├── base/                          # MVI 기본 구조
│   ├── BaseViewModel.kt           # MVI ViewModel 베이스 클래스
│   ├── UiState.kt                 # UI 상태 인터페이스
│   ├── UiIntent.kt                # 사용자 인텐트 인터페이스
│   └── UiEffect.kt                # 사이드 이펙트 인터페이스
│
├── domain/                        # Domain Layer (비즈니스 로직)
│   ├── model/                     # 도메인 모델
│   │   ├── User.kt
│   │   └── Result.kt              # 성공/실패/로딩 래퍼
│   ├── repository/                # Repository 인터페이스
│   │   └── UserRepository.kt
│   └── usecase/                   # 유스케이스
│       └── GetUsersUseCase.kt
│
├── data/                          # Data Layer (데이터 소스)
│   ├── model/                     # 데이터 전송 객체 (DTO)
│   │   └── UserDto.kt
│   ├── remote/                    # 원격 데이터 소스
│   │   └── ApiService.kt
│   └── repository/                # Repository 구현체
│       └── UserRepositoryImpl.kt
│
├── presentation/                  # Presentation Layer (UI)
│   └── users/                     # Users 화면
│       ├── UserContract.kt        # State, Intent, Effect 정의
│       ├── UserViewModel.kt       # ViewModel 구현
│       └── UserScreen.kt          # Compose UI
│
├── di/                            # Dependency Injection
│   ├── NetworkModule.kt           # 네트워크 관련 의존성
│   └── RepositoryModule.kt        # Repository 의존성
│
├── ui/theme/                      # Compose 테마
│   ├── Color.kt
│   ├── Theme.kt
│   └── Type.kt
│
├── IdolApplication.kt             # Application 클래스
└── MainActivity.kt                # Main Activity
```

## MVI 아키텍처 패턴

### State (상태)
UI의 현재 상태를 나타내는 불변 데이터 클래스입니다.

```kotlin
data class State(
    val users: List<User> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
) : UiState
```

### Intent (의도)
사용자의 액션이나 이벤트를 나타냅니다.

```kotlin
sealed class Intent : UiIntent {
    data object LoadUsers : Intent()
    data object RetryLoadUsers : Intent()
    data class OnUserClick(val user: User) : Intent()
}
```

### Effect (사이드 이펙트)
일회성 이벤트 (네비게이션, 토스트 등)를 나타냅니다.

```kotlin
sealed class Effect : UiEffect {
    data class ShowToast(val message: String) : Effect()
    data class NavigateToUserDetail(val userId: Int) : Effect()
}
```

## 주요 기능

### BaseViewModel
MVI 패턴을 구현한 기본 ViewModel 클래스로, 모든 ViewModel이 상속합니다.

**주요 메서드:**
- `setState()`: 새로운 상태 설정
- `setEffect()`: 사이드 이펙트 발생
- `sendIntent()`: 사용자 인텐트 처리

### Clean Architecture Layers

**Domain Layer (비즈니스 로직)**
- 프레임워크에 독립적인 순수 비즈니스 로직
- Use Case 패턴을 사용한 단일 책임 원칙
- Repository 인터페이스 정의 (DIP)

**Data Layer (데이터 소스)**
- Repository 인터페이스 구현
- API 통신 및 데이터 변환
- DTO → Domain Model 매핑

**Presentation Layer (UI)**
- Compose를 사용한 선언형 UI
- MVI 패턴 구현
- ViewModel을 통한 상태 관리

## 샘플 앱 기능

현재 프로젝트는 [JSONPlaceholder API](https://jsonplaceholder.typicode.com/)를 사용하여 사용자 목록을 표시하는 샘플 앱입니다.

**주요 기능:**
- 사용자 목록 조회
- 로딩 상태 표시
- 에러 핸들링 및 재시도
- Material 3 디자인
- 반응형 UI (Flow 기반)

## 빌드 및 실행

### 요구사항
- Android Studio Hedgehog 이상
- JDK 11 이상
- Android SDK 26 이상

### 빌드
```bash
./gradlew build
```

### 실행
Android Studio에서 프로젝트를 열고 Run 버튼을 클릭하거나:
```bash
./gradlew installDebug
```

## 의존성

주요 라이브러리 버전:
- Kotlin: 2.0.21
- Compose BOM: 2024.09.00
- Hilt: 2.51.1
- Retrofit: 2.11.0
- Coroutines: 1.9.0

전체 의존성은 `gradle/libs.versions.toml` 파일을 참조하세요.

## 코드 스타일

- **Kotlin Coding Conventions** 준수
- **단방향 데이터 흐름** (Unidirectional Data Flow)
- **불변성** (Immutability) 우선
- **의존성 역전 원칙** (Dependency Inversion Principle)
- **단일 책임 원칙** (Single Responsibility Principle)

## 확장 가능성

이 프로젝트는 다음과 같이 확장할 수 있습니다:

1. **Navigation Component** 추가로 다중 화면 구현
2. **Room Database** 추가로 로컬 캐싱
3. **DataStore** 추가로 설정 저장
4. **WorkManager** 추가로 백그라운드 작업
5. **Paging 3** 추가로 무한 스크롤

## 라이선스

이 프로젝트는 샘플 목적으로 만들어졌습니다.
# idol_android
