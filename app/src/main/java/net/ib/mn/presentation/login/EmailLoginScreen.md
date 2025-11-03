# EmailLoginScreen

> 이메일과 비밀번호를 입력하여 로그인하는 화면

---

## 🎯 주요 기능

- 이메일/비밀번호 입력 및 로그인
- 회원가입 화면 진입
- 아이디 찾기 (디바이스 ID 기반)
- 비밀번호 찾기 화면 진입
- 입력 필드 자동완성 지원 (Autofill)

---

## 📦 UseCases

* **SignInUseCase**: 이메일 로그인 처리
* **UserRepository.findId**: 디바이스 ID로 아이디 찾기

---

## 🌐 호출 API 및 사용 Field

### User API
* **POST /user/signin**
  * domain, email, password, push_key, gmail, device_id, app_id
  * 응답: success, gcode, mcode, message, data.userId, data.email, data.username, data.nickname, data.profileImage, data.token

* **POST /user/findid**
  * device_id
  * 응답: email (문자열)

---

## 🔄 화면 플로우

### 1️⃣ 진입
* Navigation: LoginScreen에서 "이메일 로그인" 링크 클릭
* 파라미터: 없음

### 2️⃣ 사용자 액션

| 액션 | 설명 | 결과 |
|------|------|------|
| 이메일 입력 | 이메일 주소 입력 (자동완성 지원) | 실시간 검증 |
| 비밀번호 입력 | 비밀번호 입력 (표시/숨김 토글) | 실시간 입력 |
| 로그인 버튼 클릭 | 입력한 정보로 로그인 시도 | 로그인 처리 |
| 회원가입 버튼 클릭 | 회원가입 화면으로 이동 | 회원가입 화면 |
| 아이디 찾기 클릭 | 디바이스 ID로 아이디 조회 | 아이디 찾기 다이얼로그 |
| 비밀번호 찾기 클릭 | 비밀번호 찾기 화면으로 이동 | 비밀번호 찾기 화면 |
| 뒤로가기 | 이전 화면으로 이동 | LoginScreen |

### 3️⃣ 로그인 플로우
1. 이메일 형식 검증
2. 비밀번호 해시화 (MD5 salt)
3. signIn API 호출
4. **성공 시**: 인증 정보 저장 → StartUpScreen (재초기화)
5. **실패 시**: 에러 메시지 표시

### 4️⃣ 종료
* ✅ 로그인 성공 시 → StartUpScreen → 메인 화면
* 📝 회원가입 시 → 회원가입 화면
* ❌ 취소 시 → LoginScreen

---

## 💾 데이터 저장

### DataStore (키-값 저장소)

**저장 데이터**
* `accessToken` - 서버 토큰 또는 해시된 비밀번호 (서버 응답 우선, 없으면 MD5 salt)
* `loginDomain` - 로그인 도메인 (`email`)
* `loginEmail` - 로그인 이메일
* `loginTimestamp` - 로그인 시각 (밀리초)
* `deviceId` - 디바이스 고유 ID (UUID, 아이디 찾기용)

**읽기 데이터**
* `deviceId` - 저장된 디바이스 ID (아이디 찾기 시 우선 사용)
* `fcmToken` - FCM 푸시 토큰

### AuthInterceptor (메모리)
* `email`, `domain`, `token` - API 호출 시 인증 헤더에 사용

---

## 🎨 UI 구성

| 요소 | 스펙 |
|------|------|
| **상단바** | 높이 `56dp`, 닫기 버튼, 중앙 타이틀 "이메일 로그인" |
| **이메일 입력 필드** | 아이콘 + 입력창 + 하단 밑줄, 포커스 시 색상 변경 |
| **비밀번호 입력 필드** | 아이콘 + 입력창 + 표시/숨김 버튼 + 하단 밑줄 |
| **로그인 버튼** | 전체 너비, 높이 `36dp`, 둥근 모서리 `18dp`, 메인 색상 |
| **회원가입 버튼** | 전체 너비, 높이 `36dp`, 테두리만, 메인 색상 텍스트 |
| **아이디/비밀번호 찾기** | 하단, 크기 `12sp`, 가운데 구분선 |
| **로딩 오버레이** | 전체 화면, API 호출 중 표시 |

### 입력 필드 스타일
* 배경: 투명
* 밑줄: 일반 `text_dimmed`, 포커스 `main`
* 아이콘: 좌측 `24dp`, 여백 `5dp`
* 텍스트: 크기 `14sp`, 색상 `text_default`
* 힌트: 색상 `text_dimmed`

---

## ⚠️ 에러 처리

| 상황 | 처리 방법 |
|------|----------|
| 이메일 미입력 | "Required field" 에러 |
| 이메일 형식 오류 | "Invalid email format" 에러 |
| 비밀번호 미입력 | "Required field" 에러 |
| 로그인 실패 (gcode=1031) | "비밀번호를 확인해 주세요" |
| 로그인 실패 (gcode=1002) | "이메일을 확인해 주세요" |
| 로그인 실패 (gcode=1030) | "닉네임이 잘못되었습니다" |
| 점검 중 (gcode=88888, mcode=1) | 서버 메시지 표시 |
| 기타 에러 | 서버 메시지 또는 기본 에러 메시지 |
| 아이디 찾기 실패 | "아이디를 찾을 수 없습니다" 다이얼로그 |

---

## 🔐 보안 처리

### 비밀번호 해시화
* 로그인 시: MD5 salt 해시 적용 후 서버 전송
* 저장 시: 서버 토큰 우선, 없으면 해시된 비밀번호 저장
* 목적: 평문 비밀번호 저장 방지

### 자동완성 지원
* 이메일: AUTOFILL_HINT_EMAIL_ADDRESS
* 비밀번호: AUTOFILL_HINT_PASSWORD
* Android 8.0+ (API 26+) 지원

---

## 📝 주의사항

* 📧 **이메일 형식**: 표준 이메일 형식 검증 (Patterns.EMAIL_ADDRESS)
* 🔑 **비밀번호 해시**: MD5 salt로 해시 후 전송 및 저장
* 💾 **최소 정보 저장**: 로그인 시 email, domain, token만 저장 (전체 정보는 StartUpScreen에서 로드)
* 🔄 **재초기화**: 로그인 성공 시 StartUpScreen으로 이동하여 앱 데이터 재로드
* 📱 **아이디 찾기**: 디바이스 ID 기반, 저장된 deviceId 우선 사용 (앱 재설치 고려)
* ⏱️ **로그인 타임스탬프**: 로그인 시각 저장 (분석 및 세션 관리용)
* 🔐 **Autofill 지원**: AndroidView + EditText 사용으로 안정적인 자동완성 구현
* 👁️ **비밀번호 표시/숨김**: 사용자가 토글 가능

---

**문서 버전**: 1.0.0
**최종 수정일**: 2025-11-03
**작성**: 화면 분석 및 정리
