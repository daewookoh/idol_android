# UnifiedRankingSubPage

> Global/Group/Solo 세 가지 랭킹 타입을 하나로 통합한 범용 랭킹 SubPage 화면

---

## 🎯 주요 기능

- 세 가지 랭킹 타입 지원 (Global, Group, Solo)
- 성별 카테고리 변경 지원 (Group, Solo만 해당)
- Room DB 기반 실시간 데이터 구독 및 UI 반영
- 캐시 데이터 즉시 표시 (빠른 로딩)
- 투표 후 실시간 랭킹 재정렬
- MyFavorite 화면 지원 (ExoTop3 숨김 모드)

---

## 📦 UseCases

이 화면은 직접 UseCase를 호출하지 않습니다.
- **ChartRankingRepository**를 통해 Room DB에서 캐시된 데이터를 구독
- API 호출 및 초기 데이터 로딩은 **StartUpViewModel**에서 처리

---

## 🌐 호출 API 및 사용 Field

### 데이터 흐름
1. **StartUpViewModel**: 앱 시작 시 5개 차트 데이터를 API로 가져와 Room DB에 저장
2. **UnifiedRankingSubPageViewModel**: Room DB를 구독하여 실시간으로 UI 업데이트
3. **백그라운드 갱신**: 화면 가시성 변경 시 `refreshChart()` 호출하여 DB 갱신

### 차트 새로고침 API
* **POST** `/api/chart/refresh`
  * 파라미터: `chartCode` (PR_S_M, PR_S_F, PR_G_M, PR_G_F, GLOBALS)
  * 사용: 화면이 보일 때 백그라운드에서 호출하여 DB 갱신

---

## 🔄 화면 플로우

### 1️⃣ 진입
* **Navigation**: RankingPage의 탭 전환을 통해 진입
* **파라미터**:
  * `chartCode`: 차트 코드 (예: PR_S_M, PR_G_F, GLOBALS)
  * `dataSource`: RankingDataSource (Global/Group/Solo 타입)
  * `isVisible`: 화면 가시성 (UDP 리스닝 제어)
  * `listState`: LazyList 스크롤 상태
  * `isForFavorite`: MyFavorite용 여부 (true면 ExoTop3 숨김)

### 2️⃣ 초기 로딩
1. ViewModel 생성 (타입별 고유 key 사용)
2. Room DB 구독 시작 (flatMapLatest 사용)
3. 캐시 데이터 즉시 로드 및 표시
4. 화면이 보이면 백그라운드에서 API 호출하여 DB 갱신

### 3️⃣ 사용자 액션

| 액션 | 설명 | 결과 |
|------|------|------|
| 성별 토글 | 남자/여자 카테고리 전환 (Group/Solo) | 새로운 차트 코드로 재구독 및 DB 갱신 |
| 아이템 클릭 | 랭킹 아이템 클릭 | 로그 출력 (현재 미구현) |
| 투표 성공 | 하트 투표 완료 | DB 업데이트 및 실시간 랭킹 재정렬 |
| 스크롤 | 리스트 스크롤 | 페이지네이션 (구현 필요) |

### 4️⃣ 화면 가시성 관리
* **화면이 보일 때** (`isVisible = true`):
  * `onScreenVisible()` 호출
  * 백그라운드에서 `refreshChart()` 실행하여 DB 갱신
* **화면이 숨겨질 때** (`isVisible = false`):
  * `onScreenHidden()` 호출
  * Flow 구독은 viewModelScope에 의해 자동 관리

### 5️⃣ 종료
* ViewModel은 화면 종료 시 자동 제거
* Flow 구독은 viewModelScope에 의해 자동 정리

---

## 💾 데이터 저장

### SharedPreferences (DataStore)

**읽기 데이터**
* `chartRanking_[chartCode]` - 차트별 랭킹 데이터 (JSON 형태)
  * 예: `chartRanking_PR_S_M`, `chartRanking_GLOBALS`

**저장 데이터**
* 투표 후 `updateVoteAndRerank()` 호출 시 업데이트

### Room Database

**idol 테이블**
* 목적: 아이돌 기본 정보 및 투표 수 저장
* 주요 필드: `id`, `name`, `profileImage`, `hearts`, `level`, `groupName` 등

---

## 🎨 UI 구성

### 로딩 상태 (UiState.Loading)
| 요소 | 스펙 |
|------|------|
| **레이아웃** | Box(fillMaxSize, Center) |
| **인디케이터** | CircularProgressIndicator |
| **색상** | ColorPalette.main |

### 에러 상태 (UiState.Error)
| 요소 | 스펙 |
|------|------|
| **레이아웃** | Box(fillMaxSize, Center) |
| **텍스트** | "오류: {message}" |
| **폰트 크기** | 16.sp |
| **색상** | ColorPalette.main |
| **패딩** | 16.dp |

### 빈 데이터 상태 (Success with empty list)
| 요소 | 스펙 |
|------|------|
| **레이아웃** | Box(fillMaxSize, Center) |
| **텍스트** | "랭킹 데이터가 없습니다." |
| **폰트 크기** | 16.sp |
| **색상** | ColorPalette.textDimmed |
| **패딩** | 16.dp |

### 성공 상태 (UiState.Success)
| 요소 | 스펙 |
|------|------|
| **컴포넌트** | ExoRankingList |
| **ExoTop3 표시** | isForFavorite = false일 때만 표시 |
| **애니메이션** | disableAnimation = true (딜레이 제거) |
| **스크롤 상태** | isForFavorite = true면 독립적인 스크롤 |

---

## 🔄 데이터 구독 메커니즘

### flatMapLatest 패턴
```
currentChartCode (StateFlow)
  ↓ flatMapLatest
observeChartData(chartCode) (Flow from DB)
  ↓ collect
UiState 업데이트
```

**동작 방식:**
1. `currentChartCode`가 변경되면 이전 Flow 구독을 취소
2. 새로운 `chartCode`로 DB를 구독
3. DB 변경사항이 자동으로 UI에 반영

### 차트 코드 변경 지원

| 타입 | 남녀 변경 지원 | 차트 코드 예시 |
|------|---------------|---------------|
| **Global** | ❌ | GLOBALS (고정) |
| **Group** | ✅ | PR_G_M ↔ PR_G_F |
| **Solo** | ✅ | PR_S_M ↔ PR_S_F |

---

## ⚡ 성능 최적화

* **즉시 캐시 로드**: Room DB에서 캐시 데이터를 먼저 표시하여 빠른 초기 렌더링
* **백그라운드 갱신**: API 호출은 백그라운드에서 진행하여 UI 블로킹 없음
* **Flow 구독**: DB 변경 시 자동으로 UI 업데이트 (수동 갱신 불필요)
* **flatMapLatest**: 차트 변경 시 이전 구독 자동 취소하여 메모리 효율적
* **애니메이션 비활성화**: `disableAnimation = true`로 렌더링 딜레이 제거
* **독립적인 ViewModel**: 각 타입/차트별로 고유 key를 사용하여 캐시 충돌 방지

---

## 📝 주의사항

* ⚠️ **ViewModel 키**: 각 타입과 차트 코드별로 고유한 ViewModel 인스턴스 생성 (`"unified_ranking_{type}_{chartCode}"`)
* ⚠️ **성별 변경**: Global 랭킹은 성별 변경을 지원하지 않음 (`supportGenderChange() = false`)
* ⚠️ **MyFavorite 모드**: `isForFavorite = true`일 때 ExoTop3 숨김 및 독립적인 스크롤 상태 사용
* 🔄 **실시간 업데이트**: DB 구독 방식이므로 다른 화면에서 데이터를 변경해도 자동으로 UI에 반영됨
* 📊 **투표 반영**: `updateVoteAndRerank()` 호출 시 DB에서 자동으로 랭킹 재정렬 및 UI 업데이트
* 🏭 **AssistedInject**: Hilt의 AssistedFactory를 사용하여 런타임에 파라미터 주입

---

## 🔗 관련 컴포넌트

* **UnifiedRankingSubPageViewModel**: 비즈니스 로직 및 데이터 구독
* **RankingDataSource**: Strategy Pattern으로 타입별 데이터 소스 제공
* **ChartRankingRepository**: Room DB 및 SharedPreferences 관리
* **ExoRankingList**: 랭킹 리스트 UI 컴포넌트
* **StartUpViewModel**: 초기 차트 데이터 로딩

---

**문서 버전**: 1.0.0
**최종 수정일**: 2025-11-14
**작성**: UnifiedRankingSubPage 화면 분석 및 정리
