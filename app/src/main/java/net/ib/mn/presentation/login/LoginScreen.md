# LoginScreen

> 소셜 로그인 및 이메일 로그인을 제공하는 로그인 화면

---

## 🎯 주요 기능

- 소셜 로그인 지원 (Kakao, Google, Line, Facebook)
- 이메일 로그인 진입점
- 회원 여부 자동 확인 및 분기 처리
- 푸시 알림 권한 요청 및 FCM 토큰 등록
- 로그인 상태 관리 및 인증 정보 저장

---

## 📦 UseCases

* **ValidateUserUseCase**: 이메일로 회원 여부 확인
* **SignInUseCase**: 로그인 API 호출 및 인증 처리

---

## 🌐 호출 API 및 사용 Field

### User API
* **POST /user/validate**
  * type, value, appId
  * 응답: success, domain, message

* **POST /user/signin**
  * domain, email, password, push_key, gmail, device_id, app_id
  * 응답: success, data.userId, data.email, data.username, data.nickname, data.profileImage, data.token, message

---

## 🔄 화면 플로우

### 1️⃣ 진입
* Navigation: StartUpScreen에서 인증 정보가 없을 때 자동 이동
* 파라미터: 없음
* 조건: 로그인 필요 시

### 2️⃣ 사용자 액션

| 액션 | 설명 | 결과 |
|------|------|------|
| 카카오 버튼 클릭 | 카카오톡 또는 카카오 계정으로 로그인 | 로그인 플로우 시작 |
| 라인 버튼 클릭 | Line 계정으로 로그인 | 로그인 플로우 시작 |
| 구글 버튼 클릭 | Google 계정으로 로그인 (권한 요청 포함) | 로그인 플로우 시작 |
| 페이스북 버튼 클릭 | Facebook 계정으로 로그인 | 로그인 플로우 시작 |
| 이메일 로그인 링크 클릭 | 이메일 로그인 화면으로 이동 | 이메일 로그인 화면 |

### 3️⃣ 로그인 플로우
1. 소셜 SDK 로그인 → access token + 사용자 정보 획득
2. validate API 호출 → 회원 여부 확인
3. **기존 회원**: signIn API 호출 → 메인 화면으로 이동
4. **신규 회원**: 회원가입 화면으로 이동 (이메일, 토큰, 도메인 전달)

### 4️⃣ 종료
* ✅ 기존 회원 로그인 성공 시 → StartUpScreen (재초기화) → 메인 화면
* 🆕 신규 회원 확인 시 → 회원가입 화면
* ❌ 로그인 취소 시 → 로그인 화면 유지
* ⚠️ 로그인 실패 시 → 에러 다이얼로그 표시

---

## 💾 데이터 저장

### DataStore (키-값 저장소)

**저장 데이터**
* `accessToken` - 서버 인증 토큰 (소셜 로그인의 경우 access token)
* `loginDomain` - 로그인 도메인 (kakao, google, line, facebook, email)
* `loginEmail` - 로그인 이메일
* `deviceId` - 디바이스 고유 ID (UUID, 아이디 찾기용)
* `fcmToken` - FCM 푸시 토큰

**읽기 데이터**
* `deviceId` - 저장된 디바이스 ID (없으면 새로 생성)
* `fcmToken` - 저장된 FCM 토큰

### AuthInterceptor (메모리)
* `email`, `domain`, `token` - API 호출 시 인증 헤더에 사용

---

## 🎨 UI 구성

| 요소 | 스펙 |
|------|------|
| **배경** | 전체 화면, 스크롤 가능 |
| **메인 로고** | 중앙 상단, 크기 `178dp × 142dp` |
| **앱 로고** | 메인 로고 아래, 높이 `30dp` |
| **시작하기 텍스트** | 크기 `20sp`, 굵게 |
| **SNS 버튼들** | 가로 정렬, 각 `56dp × 56dp`, 간격 `6dp` |
| **이메일 로그인 링크** | 하단, 크기 `13sp`, 회색 텍스트 |
| **로딩 오버레이** | 전체 화면, API 호출 중 표시 |

### SNS 버튼
* pressed 상태 지원 (누르는 동안 다른 이미지 표시)
* 순서: Kakao → Line → Google → Facebook

---

## ⚠️ 에러 처리

| 상황 | 처리 방법 |
|------|----------|
| SNS 로그인 취소 | 로딩만 해제, 에러 메시지 없음 |
| SNS 로그인 실패 | 에러 다이얼로그 표시 |
| 이메일 누락 (Facebook) | "이메일 정보 없음" 에러 |
| validate API 실패 | 에러 다이얼로그 표시 |
| signIn API 실패 (기존 회원) | 로그인 처리 계속 진행 |
| signIn API 실패 (신규 회원) | 회원가입 화면으로 이동 |
| 다른 도메인으로 가입됨 | "다른 방법으로 가입됨" 에러 |
| 권한 거부 (Google) | 권한 없이도 로그인 계속 진행 |

---

## 🔐 권한 처리

### 푸시 알림 권한 (Android 13+)
* 화면 진입 시 자동 요청
* 허용 시: FCM 토큰 등록
* 거부 시: 푸시 알림 없이 계속 진행

### GET_ACCOUNTS 권한 (Google 로그인, Android 6.0+)
* Google 로그인 시작 전 확인
* 첫 요청 또는 거부 시: 설명 다이얼로그 표시
* "다시 묻지 않음" 선택 시: 다이얼로그 없이 권한 요청
* 거부되어도 로그인 계속 진행

---

## 🔄 소셜 로그인 세부 플로우

### Kakao
1. 카카오톡 설치 확인
2. 설치됨: 카카오톡으로 로그인 시도
   * 취소 시: 로그인 중단
   * 실패 시: 카카오 계정으로 재시도
3. 미설치: 카카오 계정으로 로그인
4. 성공 시: 사용자 정보 조회 → validate/signIn
5. 로그인 성공 후: unlink 호출 (연결 해제)

### Google
1. GET_ACCOUNTS 권한 확인
2. 권한 설명 다이얼로그 (필요시)
3. Google Sign-In 화면 표시
4. 계정 선택 및 인증
5. access token 획득 (fallback: idToken)
6. validate/signIn

### Line
1. Line 로그인 화면 표시 (Line SDK)
2. 인증 완료 후 결과 수신
3. userId와 accessToken 획득
4. validate/signIn

### Facebook
1. Facebook 로그인 화면 표시 (Facebook SDK)
2. 로그인 성공 후 Graph API 호출
3. 이메일, 이름, ID 획득
4. 이메일 필수 확인
5. validate/signIn

---

## 📝 주의사항

* 🔑 **소셜 SDK 초기화**: 앱 시작 시 각 SNS SDK 초기화 필요 (Application 클래스)
* 📱 **권한 요청**: 푸시 알림 및 Google 계정 접근 권한 처리
* 🔐 **토큰 관리**: 소셜 access token을 비밀번호로 사용하여 서버 인증
* 🆔 **이메일 형식**: 소셜 로그인 시 이메일은 "{id}@{domain}.com" 형식 (Kakao, Line)
* ⚡ **자동 분기**: validate API 응답에 따라 자동으로 로그인/회원가입 분기
* 💾 **최소 정보 저장**: 로그인 시 email, domain, token만 저장 (전체 정보는 StartUpScreen에서 로드)
* 🔄 **재초기화**: 로그인 성공 시 StartUpScreen으로 이동하여 앱 데이터 재로드
* 📡 **FCM 토큰**: 로그인 전에 FCM 토큰을 미리 등록하여 signIn API에 전달

---

**문서 버전**: 1.0.0
**최종 수정일**: 2025-11-03
**작성**: 화면 분석 및 정리
