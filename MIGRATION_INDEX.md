# 마이그레이션 가이드 인덱스

이 문서는 Old 프로젝트를 Compose로 마이그레이션하기 위한 전체 가이드의 인덱스입니다.

## 📚 가이드 문서 목록

### 1. [프로젝트 구조 비교 및 분석](./MIGRATION_GUIDE_01_PROJECT_STRUCTURE.md)
- Old 프로젝트와 현재 프로젝트의 구조 비교
- 빌드 시스템 및 모듈 구조 분석
- 패키지 구조 비교
- 주요 차이점 정리

### 2. [아키텍처 패턴 변환](./MIGRATION_GUIDE_02_ARCHITECTURE.md)
- MVVM → MVI 패턴 변환
- ViewModel 변환 가이드
- 상태 관리 변환 (LiveData → StateFlow)
- Effect 처리 변환
- 실제 변환 예시

### 3. [Activity → Compose Screen 변환](./MIGRATION_GUIDE_03_ACTIVITY_TO_COMPOSE.md)
- Activity 분석 방법
- Compose Screen 생성 가이드
- UI 레이아웃 변환 (XML → Compose)
- 이벤트 처리 변환
- 실제 변환 예시

### 4. [UI 컴포넌트 변환](./MIGRATION_GUIDE_04_UI_COMPONENTS.md)
- UI 컴포넌트 매핑 테이블
- Custom View → Composable 변환
- RecyclerView → LazyColumn 변환
- Dialog 변환
- 리소스 및 테마 변환

### 5. [의존성 및 라이브러리 매핑](./MIGRATION_GUIDE_05_DEPENDENCIES.md)
- 의존성 버전 비교
- 라이브러리 매핑 (Glide → Coil, RxJava → Coroutines 등)
- 의존성 추가 가이드
- 주의사항

### 6. [네비게이션 변환](./MIGRATION_GUIDE_06_NAVIGATION.md)
- Intent 기반 → Navigation Compose 변환
- Deep Link 처리
- 파라미터 전달
- Back Stack 관리
- 실제 변환 예시

---

## 🚀 빠른 시작 가이드

### 새로운 Screen 마이그레이션 시작하기

1. **Activity 분석**
   - [MIGRATION_GUIDE_03_ACTIVITY_TO_COMPOSE.md](./MIGRATION_GUIDE_03_ACTIVITY_TO_COMPOSE.md)의 "Activity 분석 및 구조 파악" 섹션 참조

2. **Contract 정의**
   - [MIGRATION_GUIDE_02_ARCHITECTURE.md](./MIGRATION_GUIDE_02_ARCHITECTURE.md)의 "Contract 정의" 섹션 참조

3. **ViewModel 변환**
   - [MIGRATION_GUIDE_02_ARCHITECTURE.md](./MIGRATION_GUIDE_02_ARCHITECTURE.md)의 "ViewModel 변환 가이드" 섹션 참조

4. **Compose Screen 생성**
   - [MIGRATION_GUIDE_03_ACTIVITY_TO_COMPOSE.md](./MIGRATION_GUIDE_03_ACTIVITY_TO_COMPOSE.md)의 "Compose Screen 생성" 섹션 참조

5. **UI 변환**
   - [MIGRATION_GUIDE_04_UI_COMPONENTS.md](./MIGRATION_GUIDE_04_UI_COMPONENTS.md)의 "UI 컴포넌트 매핑" 섹션 참조

6. **네비게이션 통합**
   - [MIGRATION_GUIDE_06_NAVIGATION.md](./MIGRATION_GUIDE_06_NAVIGATION.md)의 "Intent 기반 → Navigation Compose 변환" 섹션 참조

---

## 📋 마이그레이션 체크리스트

### Activity → Screen 변환 체크리스트

#### 1. 분석 단계
- [ ] Activity 클래스 위치 확인
- [ ] Layout XML 파일 확인
- [ ] ViewModel 클래스 확인
- [ ] Adapter 확인
- [ ] 네비게이션 대상 확인
- [ ] Intent 파라미터 확인
- [ ] Flavor별 차이 확인

#### 2. Contract 정의
- [ ] State 정의
- [ ] Intent 정의
- [ ] Effect 정의

#### 3. ViewModel 변환
- [ ] BaseViewModel 상속
- [ ] createInitialState() 구현
- [ ] handleIntent() 구현
- [ ] StateFlow로 상태 관리
- [ ] Effect로 Side Effect 처리

#### 4. Compose Screen 생성
- [ ] Screen Composable 생성
- [ ] Content Composable 생성 (Stateless)
- [ ] Preview 추가
- [ ] Effect 처리

#### 5. UI 변환
- [ ] XML Layout → Compose 변환
- [ ] 리소스 참조 변환
- [ ] 이벤트 처리 변환
- [ ] Adapter → LazyColumn 변환

#### 6. 네비게이션 통합
- [ ] Navigation Graph 업데이트
- [ ] Route 정의
- [ ] 파라미터 전달
- [ ] Deep Link 처리 (필요시)

#### 7. 테스트
- [ ] UI 테스트
- [ ] Flavor별 확인
- [ ] 네비게이션 테스트

---

## 🔍 주요 개념 정리

### 아키텍처 패턴

**Old 프로젝트**: MVVM (Model-View-ViewModel)
- View: Activity/Fragment + XML Layout
- ViewModel: LiveData 기반
- Model: Repository + UseCase

**현재 프로젝트**: MVI (Model-View-Intent)
- View: Compose Screen
- ViewModel: StateFlow + Channel 기반
- Intent: 사용자 액션
- Effect: Side Effect (Navigation, Toast 등)

### 상태 관리

**Old**: LiveData + Event Wrapper
```kotlin
private val _state = MutableLiveData<State>()
val state: LiveData<State> = _state

private val _navigate = MutableLiveData<Event<Unit>>()
val navigate: LiveData<Event<Unit>> = _navigate
```

**현재**: StateFlow + Channel
```kotlin
private val _uiState = MutableStateFlow(State())
val uiState: StateFlow<State> = _uiState.asStateFlow()

private val _effect = Channel<Effect>()
val effect = _effect.receiveAsFlow()
```

### 네비게이션

**Old**: Intent 기반
```kotlin
val intent = Intent(this, DetailActivity::class.java)
intent.putExtra("id", id)
startActivity(intent)
```

**현재**: Navigation Compose
```kotlin
navController.navigate("detail/$id")
```

---

## 💡 팁 및 베스트 프랙티스

### 1. 단계별 접근
- 한 번에 하나의 Activity만 변환
- 완전히 변환하고 테스트한 후 다음으로 진행

### 2. 비즈니스 로직 보존
- ViewModel의 비즈니스 로직은 최대한 유지
- UI 레이어만 Compose로 변경

### 3. UI 완전 동일
- 레이아웃, 색상, 스타일 모두 동일하게 구현
- 사용자 경험 변경 최소화

### 4. 코드 리뷰
- 각 Screen 변환 후 코드 리뷰 진행
- 일관성 유지 확인

### 5. 테스트
- 변환 후 반드시 테스트
- Flavor별로 모두 확인

---

## 📞 참고 자료

### 공식 문서
- [Jetpack Compose 공식 문서](https://developer.android.com/jetpack/compose)
- [Navigation Compose 가이드](https://developer.android.com/jetpack/compose/navigation)
- [State and Jetpack Compose](https://developer.android.com/jetpack/compose/state)

### 현재 프로젝트 참고
- 기존 구현된 Screen 참고: `app/src/main/java/net/ib/mn/presentation/`
  - `startup/StartUpScreen.kt`
  - `login/LoginScreen.kt`
  - `signup/SignUpScreen.kt`
  - `main/MainScreen.kt`

---

## 🎯 다음 단계

마이그레이션을 시작하려면:

1. [MIGRATION_GUIDE_01_PROJECT_STRUCTURE.md](./MIGRATION_GUIDE_01_PROJECT_STRUCTURE.md)를 읽고 프로젝트 구조를 이해하세요.

2. 변환할 Activity를 선택하고 [MIGRATION_GUIDE_03_ACTIVITY_TO_COMPOSE.md](./MIGRATION_GUIDE_03_ACTIVITY_TO_COMPOSE.md)의 체크리스트를 따라 진행하세요.

3. 필요시 다른 가이드 문서를 참조하세요.

---

**마지막 업데이트**: 2024년

