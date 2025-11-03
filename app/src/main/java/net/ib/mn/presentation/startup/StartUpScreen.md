# StartUpScreen

> 앱 시작 시 필요한 설정과 데이터를 서버에서 로드하는 초기화 화면

---

## 🎯 주요 기능

- 앱 전역 설정 및 사용자 설정 로드
- 사용자 프로필 정보 동기화
- 아이돌 데이터 및 업데이트 정보 로드
- 광고 타입 및 쿠폰 메시지 로드
- 프로그레스바를 통한 초기화 진행 상황 표시

---

## 📦 UseCases

* GetConfigStartupUseCase - 앱 전역 설정 조회 (욕설 필터, 공지사항, 이벤트 등)
* GetConfigSelfUseCase - 사용자 앱 설정 조회 (언어, 테마, 푸시 알림)
* GetUpdateInfoUseCase - 아이돌 업데이트 플래그 조회 (전체/일일/SNS)
* GetUserSelfUseCase - 사용자 프로필 정보 조회 (ETag 캐싱 지원)
* GetUserStatusUseCase - 사용자 상태 조회 (튜토리얼, 첫 로그인)
* GetAdTypeListUseCase - 광고 타입 목록 조회
* GetMessageCouponUseCase - 쿠폰 메시지 목록 조회
* UpdateTimezoneUseCase - 타임존 업데이트
* GetIdolsUseCase - 전체 아이돌 목록 조회 및 로컬 DB 저장

---

## 🌐 호출 API 및 사용 Field

### Config API
* GET /config/startup - badWords, boardTags, noticeList, eventList, snsChannels, uploadVideoSpec, familyAppList, endPopup, newPicks, helpInfos
* GET /config/self - language, theme, pushEnabled

### Update API
* GET /update/info - allIdolUpdate, dailyIdolUpdate, snsChannelUpdate

### User API
* GET /user/self - id, username, email, nickname, profileImage, hearts, diamond, strongHeart, weakHeart, level, levelHeart, power, resourceUri, pushKey, createdAt, pushFilter, statusMessage, ts, itemNo, domain, giveHeart
* GET /user/status - tutorialCompleted, firstLogin
* PUT /user/timezone - timezone

### Ad & Message API
* GET /ad/types - id, type, reward
* GET /message/coupon - id, message, couponCode

### Idol API
* GET /idols - id, name, group, imageUrl, type, debutDate

---

## 🔄 화면 플로우

### 1️⃣ 진입
* Navigation: 앱 시작 시 자동 진입 (첫 화면)
* 파라미터: 없음
* 조건: 앱 실행 시 항상 표시

### 2️⃣ 초기화 프로세스

| 단계 | 설명 | 처리 |
|------|------|------|
| 인증 확인 | DataStore에서 저장된 인증 정보 로드 | email, domain, token 확인 |
| Phase 1 | ConfigSelf API 호출 | 사용자 설정 로드 |
| Phase 2 | ConfigStartup API 호출 | 앱 전역 설정 로드 (실패 시 중단) |
| Phase 3 | 나머지 API 병렬 호출 | UpdateInfo, UserSelf, UserStatus, AdTypes 등 |
| 진행 표시 | 프로그레스바 업데이트 | 0.0 → 1.0 진행률 표시 |

### 3️⃣ 종료
* ✅ 초기화 성공 시 → 메인 화면으로 이동
* ❌ 인증 정보 없을 시 → 로그인 화면으로 이동
* ⚠️ ConfigStartup API 실패 시 → Toast 에러 메시지 표시 후 대기

---

## 💾 데이터 저장

### DataStore (키-값 저장소)

**Config 데이터**
* `badWords` - 욕설 필터 목록
* `boardTags` - 게시판 태그 목록
* `noticeList` - 공지사항 목록 (JSON)
* `eventList` - 이벤트 목록 (JSON)
* `language` - 사용자 언어 설정
* `theme` - 테마 설정
* `pushEnabled` - 푸시 알림 활성화 여부

**Update 플래그**
* `allIdolUpdate` - 전체 아이돌 업데이트 플래그
* `dailyIdolUpdate` - 일일 아이돌 업데이트 플래그
* `snsChannelUpdate` - SNS 채널 업데이트 플래그

**사용자 정보**
* `id`, `email`, `username`, `nickname`, `profileImage`
* `hearts`, `diamond`, `strongHeart`, `weakHeart`
* `level`, `levelHeart`, `power`
* `resourceUri`, `pushKey`, `createdAt`
* `pushFilter`, `statusMessage`, `ts`, `itemNo`, `domain`, `giveHeart`
* `userSelfETag` - ETag 캐싱용

**사용자 상태**
* `tutorialCompleted` - 튜토리얼 완료 여부
* `firstLogin` - 첫 로그인 여부

**인증 정보 (읽기 전용)**
* `loginEmail`, `loginDomain`, `accessToken`

### Room Database (로컬 DB)

**Idol 테이블**
* 목적: 전체 아이돌 목록 저장
* 필드: `id`, `name`, `group`, `imageUrl`, `type`, `debutDate`

---

## 🎨 UI 구성

| 요소 | 스펙 |
|------|------|
| **배경** | Light `#ffffff` / Dark `#121212` |
| **로고** | 중앙 배치, 크기 `130dp × 110dp` |
| **프로그레스바** | 하단 중앙, 너비 `160dp`, 높이 `1dp`, 하단 여백 `60dp` |
| **프로그레스 색상** | Light `#ff4444` / Dark `#E24848` |
| **트랙 색상** | Light `#dddddd` / Dark `#404040` |

---

## ⚠️ 에러 처리

| 상황 | 처리 방법 |
|------|----------|
| 인증 정보 없음 | 로그인 화면으로 이동 |
| ConfigStartup API 실패 | Toast 에러 메시지 표시 후 초기화 중단 |
| 기타 API 실패 | 로그만 출력하고 계속 진행 (비필수 데이터) |
| ETag 304 응답 | 캐시된 데이터 사용 (정상 동작) |

---

## ⚡ 성능 최적화

* **API 병렬 호출**: Phase 3에서 여러 API를 동시에 호출하여 초기화 시간 단축
* **ETag 캐싱**: UserSelf API는 ETag를 사용하여 불필요한 데이터 전송 방지
* **순차 처리**: ConfigSelf → ConfigStartup 순서로 필수 API를 먼저 호출
* **프로그레스바**: 사용자에게 진행 상황을 시각적으로 표시하여 체감 속도 향상

---

## 📝 주의사항

* ⚠️ **ConfigStartup 필수**: 이 API가 실패하면 전체 초기화가 중단됨 (서버 필수)
* 🔐 **인증 정보**: DataStore에 저장된 email, domain, token을 AuthInterceptor에 설정
* ⏱️ **초기화 시간**: 평균 1~2초 소요 (네트워크 상태에 따라 다름)
* 📡 **네트워크**: 모든 데이터는 서버에서 가져오므로 네트워크 연결 필수
* 🔄 **업데이트 플래그**: 변경 감지 시 로그 출력하여 동기화 필요 여부 알림
* 💾 **데이터 저장**: DataStore (설정 및 사용자 정보) + Room DB (아이돌 목록)

---

**문서 버전**: 1.0.0
**최종 수정일**: 2025-11-03
**작성**: 화면 분석 및 정리
