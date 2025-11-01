# 카카오 로그인 플로우 분석 문서

## 📋 목차
1. [전체 플로우 개요](#전체-플로우-개요)
2. [카카오 OAuth 인증 과정](#카카오-oauth-인증-과정)
3. [서버 API 인증 헤더](#서버-api-인증-헤더)
4. [서버 인증 플로우](#서버-인증-플로우)
5. [코드 참조](#코드-참조)

---

## 전체 플로우 개요

```
1. 사용자: 카카오 로그인 버튼 클릭
   ↓
2. 앱: 카카오톡 앱 로그인 시도
   ├─ 성공 → Access Token 획득
   └─ 실패 → 카카오 계정 로그인 시도
   ↓
3. 앱: 카카오 사용자 정보 조회 (me API)
   ↓
4. 앱: 서버 validate API 호출 (기존 회원 여부 확인)
   ├─ 기존 회원 → 로그인 플로우
   └─ 신규 회원 → 회원가입 플로우
   ↓
5. 앱: 서버 signIn API 호출 (로그인 또는 회원가입 후 자동 로그인)
   ↓
6. 앱: 계정 정보 저장 및 메인 화면 이동
```

---

## 카카오 OAuth 인증 과정

### 1. 로그인 시작

**위치**: `old/app/src/app/java/net/ib/mn/activity/AuthActivity.kt:362`

```kotlin
fun requestKakaoLogin() {
    Util.showProgress(this, true)

    if (UserApiClient.instance.isKakaoTalkLoginAvailable(this)) {
        // 카카오톡 앱이 설치되어 있는 경우
        UserApiClient.rx.loginWithKakaoTalk(this)
            .observeOn(AndroidSchedulers.mainThread())
            .onErrorResumeNext { error ->
                if (error is ClientError && error.reason == ClientErrorCause.Cancelled) {
                    // 사용자가 취소한 경우
                    Single.error(error)
                } else {
                    // 카카오톡에 연결된 카카오계정이 없는 경우
                    // 카카오 계정으로 로그인 시도
                    UserApiClient.rx.loginWithKakaoAccount(this)
                }
            }
            .subscribe({ token ->
                Logger.v("KakaoLogin::로그인 성공.")
                requestKakaoMe(token.accessToken)
            }, { error ->
                Logger.v("KakaoLogin:: ${error.message}")
            }).addTo(disposables)
    } else {
        // 카카오톡 앱이 설치되어 있지 않은 경우
        UserApiClient.rx.loginWithKakaoAccount(this)
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({ token ->
                requestKakaoMe(token.accessToken)
            }, {
                Logger.v("KakaoLogin:: 카카오 체크 앱 안깔려져있음.")
            })
            .addTo(disposables)
    }
}
```

**플로우**:
1. 카카오톡 앱 설치 여부 확인
2. 설치되어 있으면 → `loginWithKakaoTalk()` 호출
3. 실패 시 → `loginWithKakaoAccount()` 호출 (웹뷰 로그인)
4. 성공 시 Access Token 획득

### 2. 사용자 정보 조회

**위치**: `old/app/src/app/java/net/ib/mn/activity/AuthActivity.kt:401`

```kotlin
private fun requestKakaoMe(accessToken: String) {
    UserApiClient.rx.me()
        .subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe({ user ->
            ProgressDialogFragment.show(
                this@AuthActivity, DOMAIN_KAKAO,
                R.string.wait_kakao_signin
            )
            val id = user.id
            mEmail = "$id${Const.POSTFIX_KAKAO}"  // 예: "123456789@kakao.com"
            mName = user.kakaoAccount?.profile?.nickname
            mPasswd = "asdfasdf"
            mProfileUrl = user.kakaoAccount?.profile?.thumbnailImageUrl

            if (Const.FEATURE_AUTH2) {
                // ID는 user_id@kakao.com, 암호는 access token으로 보내자
                mAuthToken = accessToken
                mPasswd = mAuthToken  // 비밀번호를 Access Token으로 설정
            }

            getSignupValidate()
        }, { error ->
            Logger.v("KakaoLogin:: Kakao me error ${error.message}")
        }).addTo(disposables)
}
```

**주요 정보**:
- `mEmail`: `{카카오_사용자_ID}@kakao.com` 형식
- `mPasswd`: `Const.FEATURE_AUTH2 = true`인 경우 Access Token 사용
- `mName`: 카카오 프로필 닉네임
- `mProfileUrl`: 카카오 프로필 이미지 URL

### 3. AndroidManifest 설정

**위치**: `old/app/src/main/AndroidManifest.xml:636`

```xml
<activity
    android:name="com.kakao.sdk.auth.AuthCodeHandlerActivity"
    android:exported="true">
    <intent-filter>
        <action android:name="android.intent.action.VIEW" />
        <category android:name="android.intent.category.DEFAULT" />
        <category android:name="android.intent.category.BROWSABLE" />
        <!-- Redirect URI: "kakao${NATIVE_APP_KEY}://oauth" -->
        <data
            android:host="oauth"
            android:scheme="kakao${KAKAO_APP_KEY_FOR_MANIFEST}" />
    </intent-filter>
</activity>
```

---

## 서버 API 인증 헤더

### 기본 헤더 구조

**위치**: `old/core/data/src/main/java/net/ib/mn/core/data/di/ApiModule.kt:124`

```kotlin
fun provideInterceptor(
    accountPreferencesRepository: AccountPreferencesRepository,
    languagePreferenceRepository: LanguagePreferenceRepository,
    @ApplicationContext context: Context,
): Interceptor {
    return Interceptor { chain ->
        val account = accountPreferencesRepository.getAccount()
        
        // Authorization 헤더 생성
        // 형식: "email:domain:token"을 Base64 인코딩
        val credential = "${account?.email}:${account?.domain}:${account?.token}"
        val authHeader =
            "Basic ${Base64.encodeToString(credential.toByteArray(), Base64.NO_WRAP)}"

        val originalRequest = chain.request()
        val method = originalRequest.method

        val requestBuilder = originalRequest.newBuilder()
            .header("Authorization", authHeader)  // ⭐ 가장 중요
            .header(
                "User-Agent",
                "${(System.getProperty("http.agent") ?: "")} (${context.applicationInfo.packageName}/${context.getString(R.string.app_version)}/${BuildConfig.VERSION_CODE})"
            )
            .header("X-HTTP-APPID", AppConst.APP_ID)
            .header(
                "X-HTTP-VERSION", context.getString(R.string.app_version)
            )
            .header("X-HTTP-NATION", languagePreferenceRepository.getSystemLanguage())

        // POST, DELETE 메서드에만 Nonce 추가
        if (method == "POST" || method == "DELETE") {
            requestBuilder.addHeader("X-Nonce", System.nanoTime().toString() + "")
        }

        val request = requestBuilder.build()
        val response = chain.proceed(request)
        
        return@Interceptor response
    }
}
```

### 헤더 상세 설명

| 헤더 이름 | 값 형식 | 설명 | 예시 |
|----------|---------|------|------|
| `Authorization` | `Basic {Base64(email:domain:token)}` | **가장 중요**: 로그인 인증 정보 | `Basic MTIzNDU2Nzg5QGtha2FvLmNvbTprYWthbzphY2Nlc3NfdG9rZW4=` |
| `User-Agent` | `{system_agent} ({package}/{version}/{version_code})` | 앱 식별 정보 | `Dalvik/2.1.0 (Linux; U; Android 13) (com.example.app/1.0.0/1)` |
| `X-HTTP-APPID` | `{APP_ID}` | 앱 ID | `your_app_id` |
| `X-HTTP-VERSION` | `{app_version}` | 앱 버전 | `1.0.0` |
| `X-HTTP-NATION` | `{language_code}` | 언어 코드 | `ko_KR` |
| `X-Nonce` | `{nanotime}` | POST/DELETE 요청 시만 추가 | `1234567890123456789` |

### Authorization 헤더 생성 예시

```kotlin
// 카카오 로그인 후 계정 정보
val email = "123456789@kakao.com"
val domain = "kakao"
val token = "kakao_access_token_here"

// Credential 생성
val credential = "$email:$domain:$token"
// 결과: "123456789@kakao.com:kakao:kakao_access_token_here"

// Base64 인코딩
val authHeader = "Basic ${Base64.encodeToString(credential.toByteArray(), Base64.NO_WRAP)}"
// 결과: "Basic MTIzNDU2Nzg5QGtha2FvLmNvbTprYWthbzprYWthb19hY2Nlc3NfdG9rZW5faGVyZQ=="
```

---

## 서버 인증 플로우

### 1. 기존 회원 여부 확인 (validate API)

**위치**: `old/app/src/app/java/net/ib/mn/activity/AuthActivity.kt:430`

```kotlin
private fun getSignupValidate() {
    lifecycleScope.launch {
        usersRepository.validate(
            type = "email",
            value = mEmail,  // "123456789@kakao.com"
            appId = AppConst.APP_ID,
            listener = { response ->
                ProgressDialogFragment.hide(this@AuthActivity, DOMAIN_KAKAO)
                if (response.optBoolean("success")) {
                    // 기존 회원 → 로그인 플로우
                    Util.closeProgress()
                    setAgreementFragment(DOMAIN_KAKAO, AgreementFragment.LOGIN_KAKAO)
                } else {
                    // 신규 회원 → 회원가입 플로우
                    if (Const.FEATURE_AUTH2) {
                        Util.setPreference(this@AuthActivity, KEY_DOMAIN, DOMAIN_KAKAO)
                    }
                    GcmUtils.registerDevice(this@AuthActivity, wrap)
                }
            },
            errorListener = {
                setAgreementFragment(DOMAIN_KAKAO, AgreementFragment.LOGIN_KAKAO)
            }
        )
    }
}
```

**API 엔드포인트**: `POST /api/v1/users/validate/`

**요청 Body**:
```json
{
  "type": "email",
  "value": "123456789@kakao.com"
}
```

**응답 예시**:
```json
{
  "success": true,  // true: 기존 회원, false: 신규 회원
  "gcode": 0,
  "msg": "..."
}
```

### 2. 로그인 API 호출 (signIn)

**위치**: `old/app/src/app/java/net/ib/mn/activity/AuthActivity.kt:238`

```kotlin
fun trySignin(email: String, passwd: String, deviceKey: String?, domain: String) {
    ProgressDialogFragment.show(this, "signin", R.string.wait_signin)
    var gmail = Util.getGmail(this@AuthActivity)
    if(gmail.isEmpty()) {
        gmail = Util.getDeviceUUID(this@AuthActivity)
    }
    MainScope().launch {
        usersRepository.signIn(
            domain,      // "kakao"
            email,       // "123456789@kakao.com"
            passwd,      // Access Token (FEATURE_AUTH2인 경우)
            deviceKey ?: "",
            gmail,
            Util.getDeviceUUID(this@AuthActivity),
            AppConst.APP_ID,
            { response ->
                Util.closeProgress()
                if (response.optBoolean("success")) {
                    if(domain == DOMAIN_KAKAO) {
                        requestKakaoUnlink()  // 카카오 연결 해제
                    }
                    afterSignin(email, passwd, domain)
                } else {
                    // 에러 처리
                }
            },
            { throwable ->
                Util.closeProgress()
                // 에러 처리
            }
        )
    }
}
```

**API 엔드포인트**: `POST /api/v1/users/email_signin/`

**요청 Body** (`SignInDTO`):
```kotlin
data class SignInDTO(
    @SerialName("domain") val domain: String? = null,      // "kakao"
    @SerialName("email") val email: String,                 // "123456789@kakao.com"
    @SerialName("passwd") val passwd: String,               // Access Token
    @SerialName("push_key") val deviceKey: String,          // FCM 토큰
    @SerialName("gmail") val gmail: String,                 // Gmail 주소 또는 Device UUID
    @SerialName("device_id") val deviceId: String,          // Device UUID
    @SerialName("app_id") val appId: String,               // 앱 ID
)
```

**요청 헤더**:
```
Authorization: Basic {Base64(email:domain:token)}
User-Agent: {system_agent} ({package}/{version}/{version_code})
X-HTTP-APPID: {app_id}
X-HTTP-VERSION: {version}
X-HTTP-NATION: {language}
X-Nonce: {nanotime}
```

**응답 예시**:
```json
{
  "success": true,
  "gcode": 0,
  "token": "server_auth_token",
  "resource_uri": "/api/v1/users/123/",
  "msg": "로그인 성공"
}
```

### 3. 계정 정보 저장

**위치**: `old/app/src/app/java/net/ib/mn/activity/AuthActivity.kt:459`

```kotlin
fun afterSignin(email: String, token: String, domain: String) {
    ProgressDialogFragment.show(this, "userinfo", R.string.wait_userinfo)
    
    val hashToken =
        if (domain == null || domain.equals(Const.DOMAIN_EMAIL, ignoreCase = true)) {
            Util.md5salt(token)
        } else {
            mAuthToken  // 카카오의 경우 Access Token 사용
        }

    // AppsFlyer 이벤트 로깅
    val appsFlyerInstance = AppsFlyerLib.getInstance()
    val eventValues = mutableMapOf<String, Any>()
    eventValues["user_id"] = appsFlyerInstance.getAppsFlyerUID(applicationContext) ?: ""
    eventValues[AFInAppEventParameterName.REGISTRATION_METHOD] = domain ?: ""
    appsFlyerInstance.logEvent(
        applicationContext,
        AFInAppEventType.COMPLETE_REGISTRATION,
        eventValues
    )

    // 계정 정보 저장
    IdolAccount.createAccount(this, email, hashToken, domain)

    // 로그인 시간 저장
    Util.setPreference(this, "user_login_ts", System.currentTimeMillis())

    setResult(RESULT_OK)
    val startIntent = StartupActivity.createIntent(this)
    startActivity(startIntent)
    finish()
}
```

**계정 저장 형식** (`IdolAccount.createAccount`):
```kotlin
// SharedPreferences에 저장
PREFS_ACCOUNT {
    "email": "123456789@kakao.com",
    "token": "kakao_access_token",
    "domain": "kakao"
}
```

### 4. 카카오 연결 해제 (Unlink)

**위치**: `old/app/src/app/java/net/ib/mn/activity/AuthActivity.kt:756`

```kotlin
private fun requestKakaoUnlink() {
    UserApiClient.rx.unlink()
        .subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe({
            Logger.v("KakaoLogin::unlink success")
        }, { error ->
            Logger.v("KakaoLogin::unlink error ${error.message}")
        }).addTo(disposables)
}
```

**참고**: 로그인 성공 후 카카오 연결을 해제하는 이유는 서버에서 계정을 관리하기 때문입니다.

---

## 코드 참조

### 주요 파일 위치

| 파일 | 경로 | 설명 |
|------|------|------|
| `AuthActivity.kt` | `old/app/src/app/java/net/ib/mn/activity/AuthActivity.kt` | 카카오 로그인 메인 로직 |
| `ApiModule.kt` | `old/core/data/src/main/java/net/ib/mn/core/data/di/ApiModule.kt` | API 헤더 설정 |
| `UsersApi.kt` | `old/core/data/src/main/java/net/ib/mn/core/data/api/UsersApi.kt` | 서버 API 인터페이스 |
| `UsersRepositoryImpl.kt` | `old/core/data/src/main/java/net/ib/mn/core/data/repository/UsersRepositoryImpl.kt` | 서버 API 호출 구현 |
| `IdolAccount.kt` | `old/app/src/main/java/net/ib/mn/account/IdolAccount.kt` | 계정 정보 관리 |
| `Const.kt` | `old/app/src/main/java/net/ib/mn/utils/Const.kt` | 상수 정의 |

### 주요 상수

```kotlin
// Const.kt
const val FEATURE_AUTH2: Boolean = true
const val DOMAIN_KAKAO: String = "kakao"
const val POSTFIX_KAKAO: String = "@kakao.com"
```

### 카카오 SDK 의존성

```kotlin
// gradle/libs.versions.toml
v2-user-rx = { module = "com.kakao.sdk:v2-user-rx", version.ref = "v2UserRx" }

// gradle.properties
KAKAO_SDK_VERSION=1.30.7
KAKAO_SDK_GROUP=com.kakao.sdk
```

---

## 요약

### 카카오 OAuth 과정
1. 카카오톡 앱 로그인 시도 (`loginWithKakaoTalk`)
2. 실패 시 카카오 계정 웹 로그인 (`loginWithKakaoAccount`)
3. Access Token 획득
4. 사용자 정보 조회 (`me()` API)

### API 헤더 설정
- **Authorization**: `Basic {Base64(email:domain:token)}`
  - `email`: `{카카오_사용자_ID}@kakao.com`
  - `domain`: `kakao`
  - `token`: 카카오 Access Token
- 기타 헤더: `User-Agent`, `X-HTTP-APPID`, `X-HTTP-VERSION`, `X-HTTP-NATION`, `X-Nonce`

### 서버 인증 플로우
1. `validate` API로 기존 회원 여부 확인
2. 기존 회원 → `signIn` API 호출
3. 신규 회원 → 회원가입 후 `signIn` API 호출
4. 계정 정보 저장 (`IdolAccount.createAccount`)
5. 카카오 연결 해제 (`unlink`)

---

## 주의사항

1. **Access Token 사용**: `FEATURE_AUTH2 = true`인 경우 비밀번호로 Access Token을 사용합니다.
2. **이메일 형식**: 카카오 사용자 ID에 `@kakao.com`을 붙여 이메일 형식으로 변환합니다.
3. **카카오 연결 해제**: 로그인 성공 후 `unlink()`를 호출하여 카카오 연결을 해제합니다.
4. **도메인 저장**: SharedPreferences에 `domain = "kakao"`를 저장하여 이후 API 호출 시 사용합니다.

