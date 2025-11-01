# 카카오 로그인 구현 검증 보고서

## 📋 검증 개요

현재 프로젝트의 카카오 로그인 OAuth 처리 과정과 API 인증 과정을 Old 프로젝트의 메뉴얼(`KAKAO_LOGIN_FLOW.md`)과 비교하여 잘못 구현된 부분을 확인했습니다.

---

## ✅ 정상적으로 구현된 부분

### 1. 카카오 OAuth 인증 과정
- ✅ 카카오톡 앱 설치 여부 확인 (`isKakaoTalkLoginAvailable`)
- ✅ 카카오톡 로그인 시도 → 실패 시 카카오 계정 로그인으로 fallback
- ✅ 사용자 정보 조회 (`me()` API)
- ✅ 이메일 형식 생성: `{userId}@kakao.com`
- ✅ Access Token을 비밀번호로 사용 (`FEATURE_AUTH2` 로직)
- ✅ 로그인 성공 후 카카오 연결 해제 (`unlink()`)

### 2. 서버 인증 플로우
- ✅ validate API 호출 (기존 회원 여부 확인)
- ✅ signIn API 호출 (로그인)
- ✅ 계정 정보 저장 (`AuthInterceptor.setAuthCredentials()`)

### 3. Authorization 헤더 형식
- ✅ `Basic {Base64(email:domain:token)}` 형식 정확히 구현
- ✅ Old 프로젝트와 동일한 형식 사용

---

## ❌ 잘못 구현된 부분

### 1. **X-Nonce 헤더 누락** ⚠️ **중요**

**문제점**:
- Old 프로젝트에서는 POST/DELETE 요청 시 `X-Nonce` 헤더를 추가하지만, 현재 프로젝트에서는 누락되어 있습니다.

**Old 프로젝트 코드** (`old/core/data/src/main/java/net/ib/mn/core/data/di/ApiModule.kt:153`):
```kotlin
if (method == "POST" || method == "DELETE") {
    requestBuilder.addHeader("X-Nonce", System.nanoTime().toString() + "")
}
```

**현재 프로젝트** (`app/src/main/java/net/ib/mn/data/remote/interceptor/AuthInterceptor.kt:58`):
```kotlin
override fun intercept(chain: Interceptor.Chain): Response {
    val originalRequest = chain.request()
    val url = originalRequest.url.toString()

    // ... 헤더 설정 ...
    
    val requestBuilder = originalRequest.newBuilder()
        .header("User-Agent", userAgent)
        .header("X-HTTP-APPID", Constants.APP_ID)
        .header("X-HTTP-VERSION", appVersion)
        .header("X-HTTP-NATION", systemLanguage)
    
    // ❌ X-Nonce 헤더가 누락됨
    
    // ... Authorization 헤더 설정 ...
}
```

**영향**:
- 서버가 POST/DELETE 요청에 대해 `X-Nonce` 헤더를 검증하는 경우 인증 실패 가능성이 있습니다.
- 보안을 위한 nonce 검증이 누락되어 있습니다.

**수정 방법**:
```kotlin
override fun intercept(chain: Interceptor.Chain): Response {
    val originalRequest = chain.request()
    val url = originalRequest.url.toString()
    val method = originalRequest.method  // ✅ 추가

    // ... 기존 헤더 설정 ...

    val requestBuilder = originalRequest.newBuilder()
        .header("User-Agent", userAgent)
        .header("X-HTTP-APPID", Constants.APP_ID)
        .header("X-HTTP-VERSION", appVersion)
        .header("X-HTTP-NATION", systemLanguage)

    // ✅ POST/DELETE 요청 시 X-Nonce 헤더 추가
    if (method == "POST" || method == "DELETE") {
        requestBuilder.addHeader("X-Nonce", System.nanoTime().toString())
    }

    // ... Authorization 헤더 설정 ...
}
```

---

### 2. **언어 헤더 차이** ⚠️

**문제점**:
- Old 프로젝트는 `languagePreferenceRepository.getSystemLanguage()`를 사용하지만, 현재 프로젝트는 `Locale.getDefault().language`를 사용합니다.

**Old 프로젝트**:
```kotlin
.header("X-HTTP-NATION", languagePreferenceRepository.getSystemLanguage())
```

**현재 프로젝트** (`app/src/main/java/net/ib/mn/data/remote/interceptor/AuthInterceptor.kt:68`):
```kotlin
val systemLanguage = Locale.getDefault().language
// ...
.header("X-HTTP-NATION", systemLanguage)
```

**차이점**:
- `Locale.getDefault().language`: 시스템의 기본 언어만 반환 (예: "ko", "en")
- `languagePreferenceRepository.getSystemLanguage()`: 앱 내에서 설정한 언어 설정을 반환 (예: "ko_KR", "en_US")

**영향**:
- 서버가 언어 코드 형식을 엄격하게 검증하는 경우 차이가 있을 수 있습니다.
- Old 프로젝트와 동일한 언어 코드 형식을 사용하는 것이 안전합니다.

**수정 방법**:
- `PreferencesManager`나 별도 Repository에서 언어 설정을 관리하도록 수정
- 또는 Old 프로젝트와 동일한 형식으로 변환 (예: `Locale.getDefault().toLanguageTag()`)

---

### 3. **validate API 호출 시 Authorization 헤더 처리** ✅ (정상)

**확인 결과**:
- validate API는 로그인 전에 호출되므로 Authorization 헤더가 없어야 합니다.
- 현재 구현은 `if (email != null && domain != null && token != null)` 조건으로 헤더를 추가하므로, 로그인 전에는 credentials가 없어 헤더가 추가되지 않습니다.
- **정상적으로 구현되어 있습니다.**

**현재 구현** (`app/src/main/java/net/ib/mn/data/remote/interceptor/AuthInterceptor.kt:76`):
```kotlin
if (originalRequest.header("Authorization") == null) {
    if (email != null && domain != null && token != null) {
        // Authorization 헤더 추가
    } else {
        // 헤더 없이 요청 (validate API 호출 시 이 경우)
    }
}
```

---

## 📊 비교표

| 항목 | Old 프로젝트 | 현재 프로젝트 | 상태 |
|------|-------------|-------------|------|
| 카카오 OAuth 플로우 | ✅ | ✅ | 정상 |
| Authorization 헤더 형식 | `Basic {Base64(email:domain:token)}` | `Basic {Base64(email:domain:token)}` | 정상 |
| User-Agent 헤더 | ✅ | ✅ | 정상 |
| X-HTTP-APPID 헤더 | ✅ | ✅ | 정상 |
| X-HTTP-VERSION 헤더 | ✅ | ✅ | 정상 |
| X-HTTP-NATION 헤더 | `languagePreferenceRepository.getSystemLanguage()` | `Locale.getDefault().language` | ⚠️ 차이 |
| X-Nonce 헤더 | POST/DELETE 시 추가 | ❌ 누락 | ❌ 누락 |
| validate API 헤더 | 로그인 전이므로 없음 | 로그인 전이므로 없음 | 정상 |

---

## 🔧 수정 권장사항

### 1. X-Nonce 헤더 추가 (필수)

**파일**: `app/src/main/java/net/ib/mn/data/remote/interceptor/AuthInterceptor.kt`

**수정 내용**:
```kotlin
override fun intercept(chain: Interceptor.Chain): Response {
    val originalRequest = chain.request()
    val url = originalRequest.url.toString()
    val method = originalRequest.method  // ✅ 추가

    // User-Agent 헤더 구성
    val systemUserAgent = System.getProperty("http.agent") ?: ""
    val packageName = context.packageName
    val appVersion = "10.10.0"
    val versionCode = BuildConfig.VERSION_CODE
    val userAgent = "$systemUserAgent ($packageName/$appVersion/$versionCode)"
    val systemLanguage = Locale.getDefault().language

    val requestBuilder = originalRequest.newBuilder()
        .header("User-Agent", userAgent)
        .header("X-HTTP-APPID", Constants.APP_ID)
        .header("X-HTTP-VERSION", appVersion)
        .header("X-HTTP-NATION", systemLanguage)

    // ✅ POST/DELETE 요청 시 X-Nonce 헤더 추가
    if (method == "POST" || method == "DELETE") {
        requestBuilder.addHeader("X-Nonce", System.nanoTime().toString())
    }

    // ... 나머지 코드 ...
}
```

### 2. 언어 헤더 형식 통일 (선택)

**옵션 1**: Old 프로젝트와 동일한 형식 사용
```kotlin
// 언어 설정을 PreferencesManager에 추가
val systemLanguage = preferencesManager.getSystemLanguage() // "ko_KR" 형식
```

**옵션 2**: 현재 형식 유지 (시스템 언어만 사용)
```kotlin
// 현재 구현 유지: Locale.getDefault().language
// 단, 서버가 형식을 엄격하게 검증하지 않는다면 문제 없음
```

---

## 📝 테스트 체크리스트

다음 항목들을 테스트하여 수정이 올바르게 적용되었는지 확인하세요:

- [ ] 카카오 로그인 플로우 (신규 회원)
- [ ] 카카오 로그인 플로우 (기존 회원)
- [ ] POST 요청 시 `X-Nonce` 헤더 포함 확인
- [ ] DELETE 요청 시 `X-Nonce` 헤더 포함 확인
- [ ] validate API 호출 시 Authorization 헤더 없음 확인
- [ ] signIn API 호출 시 Authorization 헤더 포함 확인
- [ ] 로그인 후 후속 API 호출 시 Authorization 헤더 포함 확인

---

## 📚 참고 파일

- Old 프로젝트 Interceptor: `old/core/data/src/main/java/net/ib/mn/core/data/di/ApiModule.kt:124`
- 현재 프로젝트 Interceptor: `app/src/main/java/net/ib/mn/data/remote/interceptor/AuthInterceptor.kt`
- 카카오 로그인 처리: `app/src/main/java/net/ib/mn/presentation/login/LoginScreen.kt:694`
- 카카오 로그인 ViewModel: `app/src/main/java/net/ib/mn/presentation/login/LoginViewModel.kt:113`

---

## 결론

현재 프로젝트의 카카오 로그인 구현은 대부분 올바르게 되어 있으나, **X-Nonce 헤더 누락**이 가장 중요한 문제입니다. 이는 POST/DELETE 요청 시 서버 인증 실패를 일으킬 수 있으므로 즉시 수정이 필요합니다.

