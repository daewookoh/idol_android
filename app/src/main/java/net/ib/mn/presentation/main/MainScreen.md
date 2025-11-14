# MainScreen

> 앱의 메인 화면으로 하단 네비게이션과 상단 앱바를 포함한 컨테이너 역할을 수행

---

## 🎯 주요 기능

- 5개의 탭(랭킹, 나의최애, 프로필, 자유게시판, 메뉴) 간 전환
- 성별 카테고리 토글 버튼 제공 (남자/여자)
- 실시간 타이머 표시 (상단바)
- 앱 생명주기 관리 (백그라운드 복귀 시 캐시 갱신)
- 로그아웃 처리

---

## 📦 UseCases

해당 화면은 컨테이너 역할만 수행하며 직접 UseCase를 호출하지 않습니다.
각 탭(RankingPage, MyFavoritePage, ProfilePage, FreeBoardPage, MenuPage)에서 개별 UseCase를 사용합니다.

---

## 🌐 호출 API 및 사용 Field

해당 화면은 직접 API를 호출하지 않습니다.
각 탭 페이지에서 개별 API를 호출합니다.

---

## 🔄 화면 플로우

### 1️⃣ 진입
* **Navigation**: 로그인 성공 후 자동 진입
* **조건**: 사용자 인증이 완료된 상태

### 2️⃣ 사용자 액션

| 액션 | 설명 | 결과 |
|------|------|------|
| 하단 탭 선택 | 5개 탭 중 하나 선택 | 해당 페이지로 전환 (페이드 애니메이션) |
| 성별 토글 | 남자/여자 카테고리 전환 | 랭킹 탭에서 성별에 따른 데이터 필터링 |
| 검색 버튼 | 상단 검색 아이콘 클릭 | (현재 미구현) |
| 친구 버튼 | 상단 친구 아이콘 클릭 | (현재 미구현) |
| 출석 버튼 | 상단 출석 아이콘 클릭 | (현재 미구현) |
| 알림 버튼 | 상단 알림 아이콘 클릭 | (현재 미구현) |
| 설정 버튼 | 상단 설정 아이콘 클릭 | (현재 미구현) |

### 3️⃣ 종료
* ✅ 로그아웃 → 로그인 화면으로 이동
* ✅ 앱 종료 → 앱 종료

---

## 💾 데이터 저장

### DataStore (키-값 저장소)

**읽기 데이터**
* `userInfo` - 사용자 정보 (ID, 이메일, 닉네임, 프로필 이미지 등)
* `defaultCategory` - 선택된 성별 카테고리 (TYPE_MALE / TYPE_FEMALE)

**저장 데이터**
* `defaultCategory` - 성별 카테고리 변경 시 저장

### Room Database (로컬 DB)

**로그아웃 시 처리**
* 로그아웃 시 모든 차트 랭킹 데이터 삭제 (`ChartRankingRepository.clearAll()`)

---

## 🎨 UI 구성

### 상단 앱바 (MainTopBar)
| 요소 | 스펙 |
|------|------|
| **높이** | 56dp (상태바 포함) |
| **중앙** | 실시간 타이머 텍스트 (16sp, Bold) |
| **좌측** | 성별 토글 버튼 (랭킹 탭에만 표시, width: 130dp) |
| **우측** | 아이콘 버튼 (검색, 친구 / 출석, 알림, 설정) |
| **배경** | `R.color.navigation_bar` |

### 하단 네비게이션 (MainBottomNavigation)
| 요소 | 스펙 |
|------|------|
| **탭 개수** | 5개 (랭킹, 나의최애, 프로필, 자유게시판, 메뉴) |
| **아이콘** | 선택/미선택 상태별 아이콘 |
| **배경** | `R.color.background_200` |
| **경계선** | `R.color.gray150` |
| **텍스트** | `R.color.text_default` |

### 탭별 상단바 구성
| 탭 인덱스 | 탭 이름 | 토글 버튼 | 메인 메뉴 (검색/친구) | 나의정보 메뉴 (출석/알림/설정) |
|----------|---------|-----------|---------------------|---------------------------|
| 0 | 랭킹 | ✅ | ✅ | ❌ |
| 1 | 나의최애 | ❌ | ✅ | ❌ |
| 2 | 프로필 | ❌ | ✅ | ❌ |
| 3 | 자유게시판 | ❌ | ✅ | ❌ |
| 4 | 메뉴 | ❌ | ❌ | ✅ |

### 콘텐츠 영역
| 요소 | 스펙 |
|------|------|
| **전환 애니메이션** | Fade In/Out (300ms) |
| **레이아웃** | Box(Modifier.fillMaxSize()) |

---

## 🔄 생명주기 관리

### ON_RESUME (앱 복귀)
* `MainViewModel.onAppResume()` 호출
* 로그 출력: "📱 MainScreen lifecycle: ON_RESUME"

### ON_PAUSE (앱 백그라운드)
* `MainViewModel.onAppPause()` 호출
* 로그 출력: "📱 MainScreen lifecycle: ON_PAUSE"

### DisposableEffect
* 생명주기 옵저버 등록/해제
* 화면 종료 시 옵저버 자동 제거

---

## 🌍 다국어 지원

### 성별 토글 버튼 텍스트

| Locale | 남자 | 여자 |
|--------|------|------|
| **ko_KR** | R.string.male | R.string.female |
| **ja_JP** | R.string.male | R.string.female |
| **zh_CN** | R.string.male | R.string.female |
| **zh_TW** | R.string.male | R.string.female |
| **기타** | "TYPE_MALE" | "TYPE_FEMALE" |

### 하단 탭 메뉴 텍스트

* `R.string.hometab_title_rank` - 랭킹
* `R.string.hometab_title_myidol` - 나의최애
* `R.string.hometab_title_profile` - 프로필
* `R.string.hometab_title_freeboard` - 자유게시판
* `R.string.hometab_title_menu` - 메뉴

---

## ⚡ 성능 최적화

* **즉시 반응하는 카테고리 상태**: UI가 먼저 업데이트되고 백그라운드에서 DataStore에 저장하여 반응성 개선
* **페이드 애니메이션**: 탭 전환 시 부드러운 사용자 경험 제공 (300ms)
* **생명주기 옵저버**: 앱 복귀 시 자동으로 캐시 갱신 트리거

---

## 📝 주의사항

* ⚠️ **상단바 버튼**: 현재 검색, 친구, 출석, 알림, 설정 버튼은 클릭 이벤트만 정의되어 있고 실제 기능은 미구현 상태
* ⚠️ **성별 토글**: 랭킹 탭(인덱스 0)에만 표시되며, 다른 탭에서는 투명하게 처리 (클릭 불가)
* ⚠️ **로그아웃**: 로그아웃 시 DataStore의 모든 데이터와 Room DB의 차트 데이터가 삭제됨
* 🔒 **타이머**: 화면 진입 시 자동으로 시작되며, MainTopBarViewModel에서 관리
* 📱 **생명주기**: 앱이 백그라운드에서 복귀할 때마다 `onAppResume()`이 호출됨

---

**문서 버전**: 1.0.0
**최종 수정일**: 2025-11-14
**작성**: MainScreen 화면 분석 및 정리
