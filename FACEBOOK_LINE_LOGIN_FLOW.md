# 페이스북 & 라인 로그인 플로우 분석 문서

## 📋 목차
1. [전체 플로우 비교](#전체-플로우-비교)
2. [페이스북 로그인 플로우](#페이스북-로그인-플로우)
3. [라인 로그인 플로우](#라인-로그인-플로우)
4. [Hashkey 필요 여부](#hashkey-필요-여부)
5. [카카오 vs 페이스북 vs 라인 비교](#카카오-vs-페이스북-vs-라인-비교)

---

## 전체 플로우 비교

```
카카오 로그인:
1. 카카오톡 앱 로그인 시도 → 실패 시 카카오 계정 웹 로그인
2. Access Token 획득
3. me() API로 사용자 정보 조회
4. validate API 호출 (기존 회원 여부 확인)
5. signIn API 호출 (로그인)
6. 카카오 unlink() 호출

페이스북 로그인:
1. LoginManager.getInstance().logInWithReadPermissions() 호출
2. onActivityResult로 로그인 결과 처리
3. AccessToken 획득
4. GraphRequest.newMeRequest()로 사용자 정보 조회
5. validate API 호출 (기존 회원 여부 확인)
6. signIn API 호출 (로그인)

라인 로그인:
1. LineLoginApi.getLoginIntent() 호출
2. startActivityForResult로 로그인 결과 처리
3. LineLoginResult에서 AccessToken 및 사용자 정보 획득
4. validate API 호출 (기존 회원 여부 확인)
5. signIn API 호출 (로그인)
```

---

## 페이스북 로그인 플로우

### 1. 로그인 시작

**위치**: `old/app/src/app/java/net/ib/mn/fragment/SigninFragment.kt:226`

```kotlin
R.id.btn_facebook -> {
    setFirebaseUIAction(GaAction.LOGIN_FACEBOOK)
    
    LoginManager.getInstance().logInWithReadPermissions(
        requireActivity(),
        mutableListOf("email")  // email 권한만 요청
    )
}
```

**특징**:
- Facebook SDK의 `LoginManager` 사용
- `email` 권한만 요청
- `onActivityResult`로 결과 처리

### 2. 로그인 결과 처리 (Callback)

**위치**: `old/app/src/app/java/net/ib/mn/activity/AuthActivity.kt:157`

```kotlin
callbackManager = CallbackManager.Factory.create()
LoginManager.getInstance().registerCallback(callbackManager,
    object : FacebookCallback<LoginResult> {
        override fun onSuccess(loginResult: LoginResult) {
            val loggedIn = AccessToken.getCurrentAccessToken() != null
            if (loggedIn) {
                requestFacebookMe(loginResult)
            }
        }

        override fun onCancel() {
            // 사용자가 취소한 경우
        }

        override fun onError(exception: FacebookException) {
            Toast.makeText(
                this@AuthActivity,
                R.string.line_login_failed,
                Toast.LENGTH_SHORT
            ).show()
        }
    })
```

**onActivityResult 처리**:
```kotlin
override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
    if ((requestCode and 0xffff) == 0xface) {
        callbackManager.onActivityResult(requestCode, resultCode, data)
    }
    // ...
}
```

### 3. 사용자 정보 조회

**위치**: `old/app/src/app/java/net/ib/mn/activity/AuthActivity.kt:640`

```kotlin
fun requestFacebookMe(loginResult: LoginResult) {
    Util.showProgress(this, true)

    val accessToken: AccessToken = loginResult.accessToken
    mAuthToken = accessToken.token
    Util.log("FACEBOOK ACCESS TOKEN=$accessToken")

    val request = GraphRequest.newMeRequest(
        loginResult.accessToken
    ) { jsonObject, response ->
        if (jsonObject == null) {
            Util.closeProgress()
            LoginManager.getInstance().logOut()
            Util.showIdolDialogWithBtn1(
                this@AuthActivity,
                null,
                getString(R.string.facebook_no_email)
            ) { Util.closeIdolDialog() }
            return@newMeRequest
        }

        // Application code
        Util.log("requestFacebookMe $jsonObject")
        mFacebookId = jsonObject.optString("id").toLongOrNull()
        mName = jsonObject.optString("name")
        mEmail = jsonObject.optString("email")

        // 사용자가 email 제공을 거부한 경우
        if (mEmail == null || mEmail!!.isEmpty()) {
            Util.closeProgress()
            LoginManager.getInstance().logOut()
            Util.showIdolDialogWithBtn1(
                this@AuthActivity,
                null,
                getString(R.string.facebook_no_email)
            ) { Util.closeIdolDialog() }
            return@newMeRequest
        }

        ProgressDialogFragment.show(
            this@AuthActivity,
            DOMAIN_FACEBOOK,
            R.string.lable_get_info
        )

        mPasswd = mAuthToken  // Access Token을 비밀번호로 사용

        // 기존에 아이디를 갖고 회원가입을 했었는지 확인한다.
        Util.log("requestFacebookMe  mEmail :$mEmail   mName:$mName   id:$mFacebookId")
        lifecycleScope.launch {
            usersRepository.validate(
                type = "email",
                value = mEmail,
                appId = AppConst.APP_ID,
                listener = { response ->
                    // validate API 응답 처리
                    // ...
                },
                errorListener = {
                    setAgreementFragment(DOMAIN_FACEBOOK, AgreementFragment.LOGIN_FACEBOOK)
                }
            )
        }
    }
    val parameters = Bundle()
    parameters.putString("fields", "id,name,email")  // 필요한 필드만 요청
    request.parameters = parameters
    request.executeAsync()
}
```

**주요 정보**:
- `mEmail`: 페이스북 이메일 (사용자가 제공한 실제 이메일)
- `mPasswd`: Access Token (`FEATURE_AUTH2`인 경우)
- `mName`: 페이스북 프로필 이름
- `mFacebookId`: 페이스북 사용자 ID (Long 타입)

### 4. AndroidManifest 설정

**위치**: `old/app/src/main/AndroidManifest.xml:614`

```xml
<meta-data
    android:name="com.facebook.sdk.ApplicationId"
    android:value="@string/facebook_app_id" />

<meta-data
    android:name="com.facebook.sdk.ClientToken"
    android:value="${FACEBOOK_CLIENT_ID}" />

<activity
    android:name="com.facebook.FacebookActivity"
    android:configChanges="keyboard|keyboardHidden|screenLayout|screenSize|orientation"
    android:label="@string/app_name" />

<activity
    android:name="com.facebook.CustomTabActivity"
    android:exported="true">
    <intent-filter>
        <action android:name="android.intent.action.VIEW" />
        <category android:name="android.intent.category.DEFAULT" />
        <category android:name="android.intent.category.BROWSABLE" />
        <data android:scheme="@string/fb_login_protocol_scheme" />
    </intent-filter>
</activity>
```

**필요한 설정**:
- `facebook_app_id`: strings.xml에 정의
- `FACEBOOK_CLIENT_ID`: build.gradle의 manifestPlaceholders에 정의
- `fb_login_protocol_scheme`: strings.xml에 정의 (예: `fb{facebook_app_id}`)

---

## 라인 로그인 플로우

### 1. 로그인 시작

**위치**: `old/app/src/app/java/net/ib/mn/fragment/SigninFragment.kt:213`

```kotlin
R.id.btn_signin_line -> {
    setFirebaseUIAction(GaAction.LOGIN_LINE)

    val loginIntent = LineLoginApi.getLoginIntent(
        v.context,
        AppConst.CHANNEL_ID,  // LINE Channel ID
        LineAuthenticationParams.Builder()
            .scopes(Arrays.asList(Scope.PROFILE))  // 프로필 정보만 요청
            .build()
    )
    startActivityForResult(loginIntent, Const.LINE_REQUEST_CODE)
}
```

**특징**:
- LINE SDK의 `LineLoginApi` 사용
- `CHANNEL_ID` 필요 (LINE Developers Console에서 발급)
- `Scope.PROFILE`로 프로필 정보만 요청
- `startActivityForResult`로 결과 처리

### 2. 로그인 결과 처리

**위치**: `old/app/src/app/java/net/ib/mn/activity/AuthActivity.kt:300`

```kotlin
fun requestLineSignUp(data: Intent?) {
    if(data == null){
        Toast.makeText(this, R.string.line_login_failed, Toast.LENGTH_SHORT).show()
        return
    }
    if (Const.FEATURE_AUTH2) {
        Util.setPreference(this, KEY_DOMAIN, DOMAIN_LINE)
    }

    val result = LineLoginApi.getLoginResultFromIntent(data)
    when (result.responseCode) {
        LineApiResponseCode.SUCCESS -> requestLineProfile(result)
        LineApiResponseCode.CANCEL -> Util.log("Line: Login cancelled")
        else -> runOnUiThread {
            Util.log(result.toString())
            Toast.makeText(this, R.string.line_login_failed, Toast.LENGTH_SHORT).show()
        }
    }
}
```

**onActivityResult 처리**:
```kotlin
override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
    super.onActivityResult(requestCode, resultCode, data)
    
    val fragment = supportFragmentManager.findFragmentByTag("signin") as? BaseFragment
    if (fragment != null) {
        fragment.onActivityResult(requestCode, resultCode, data)
    }
}
```

### 3. 사용자 정보 조회

**위치**: `old/app/src/app/java/net/ib/mn/activity/AuthActivity.kt:320`

```kotlin
private fun requestLineProfile(result: LineLoginResult) {
    ProgressDialogFragment.show(this, DOMAIN_LINE, R.string.wait_line_signin)

    val mid = result.lineProfile?.userId
    val displayName = result.lineProfile?.displayName

    mEmail = "$mid${Const.POSTFIX_LINE}"  // 예: "U1234567890abcdef@line.com"
    mName = displayName
    mPasswd = "asdfasdf"
    mProfileUrl = null

    if (Const.FEATURE_AUTH2) {
        // ID는 user_id@line.com, 암호는 access token으로 보내자
        mAuthToken = result.lineCredential?.accessToken?.tokenString
        mPasswd = mAuthToken  // Access Token을 비밀번호로 사용
    }

    // 기존에 아이디를 갖고 회원가입을 했었는지 확인한다.
    Util.log("requestLineProfile  mEmail :$mEmail   mName:$mName")
    lifecycleScope.launch {
        usersRepository.validate(
            type = "email",
            value = mEmail,
            appId = AppConst.APP_ID,
            listener = { response ->
                ProgressDialogFragment.hide(this@AuthActivity, DOMAIN_LINE)
                if (response.optBoolean("success")) {
                    setAgreementFragment(DOMAIN_LINE, AgreementFragment.LOGIN_LINE)
                } else {
                    if (Const.FEATURE_AUTH2) {
                        Util.setPreference(this@AuthActivity, KEY_DOMAIN, DOMAIN_LINE)
                    }
                    GcmUtils.registerDevice(this@AuthActivity, wrap)
                }
            },
            errorListener = {
                setAgreementFragment(DOMAIN_LINE, AgreementFragment.LOGIN_LINE)
            }
        )
    }
}
```

**주요 정보**:
- `mEmail`: `{LINE_USER_ID}@line.com` 형식
- `mPasswd`: Access Token (`FEATURE_AUTH2`인 경우)
- `mName`: LINE 프로필 이름
- `mProfileUrl`: LINE 프로필 이미지 URL (null)

### 4. AndroidManifest 설정

**라인 로그인은 AndroidManifest에 특별한 설정이 필요 없습니다.** 
- LINE SDK가 자동으로 처리
- `CHANNEL_ID`만 필요 (코드에서 사용)

---

## Hashkey 필요 여부

### 결론: **페이스북과 라인 모두 Hashkey 불필요** ✅

| 로그인 방식 | Hashkey 필요 여부 | 이유 |
|------------|------------------|------|
| **카카오** | ❌ 불필요 | 카카오 SDK가 자동 처리 |
| **페이스북** | ❌ **불필요** | Facebook SDK가 자동 처리, 단순히 App ID와 Client Token만 필요 |
| **라인** | ❌ **불필요** | LINE SDK가 자동 처리, 단순히 Channel ID만 필요 |
| **구글** | ✅ 필요 | Google Sign-In은 SHA-1 해시를 Google Cloud Console에 등록해야 함 |

### 페이스북 Hashkey 확인

페이스북 로그인은 **Hashkey가 필요 없습니다**:
- Facebook SDK가 자동으로 패키지명과 서명을 확인
- Google Play Console에 앱을 등록하면 Facebook이 자동으로 확인
- 로컬 개발 시에도 별도 Hashkey 등록 불필요

**필요한 것**:
1. Facebook App ID (`@string/facebook_app_id`)
2. Facebook Client Token (`${FACEBOOK_CLIENT_ID}`)
3. AndroidManifest 설정 (이미 완료)

### 라인 Hashkey 확인

라인 로그인도 **Hashkey가 필요 없습니다**:
- LINE SDK가 자동으로 처리
- LINE Developers Console에 앱을 등록하면 자동으로 확인

**필요한 것**:
1. LINE Channel ID (`AppConst.CHANNEL_ID`)
2. LINE SDK 라이브러리 추가

---

## 카카오 vs 페이스북 vs 라인 비교

### 1. OAuth 인증 방식

| 항목 | 카카오 | 페이스북 | 라인 |
|------|--------|----------|------|
| **SDK** | Kakao SDK | Facebook SDK | LINE SDK |
| **로그인 방식** | 카카오톡 앱 → 웹뷰 fallback | 웹뷰 또는 Facebook 앱 | 웹뷰 |
| **Access Token 획득** | SDK 자동 | SDK 자동 | SDK 자동 |
| **사용자 정보 조회** | `UserApiClient.me()` | `GraphRequest.newMeRequest()` | `LineLoginResult`에서 직접 |
| **Hashkey 필요** | ❌ | ❌ | ❌ |

### 2. 사용자 정보 처리

| 항목 | 카카오 | 페이스북 | 라인 |
|------|--------|----------|------|
| **이메일 형식** | `{userId}@kakao.com` | 실제 이메일 주소 | `{userId}@line.com` |
| **사용자 ID** | Long 타입 | Long 타입 (facebookId) | String 타입 (userId) |
| **비밀번호** | Access Token | Access Token | Access Token |
| **프로필 이미지** | `thumbnailImageUrl` | Graph API에서 별도 요청 | `profilePictureUrl` (null일 수 있음) |

### 3. 에러 처리

**카카오**:
- 취소: `ClientErrorCause.Cancelled` → 에러 처리
- 실패: 자동으로 카카오 계정 웹 로그인으로 fallback

**페이스북**:
- 이메일 없음: `FacebookException` → 에러 다이얼로그 표시
- 취소: `onCancel()` 콜백

**라인**:
- 취소: `LineApiResponseCode.CANCEL`
- 실패: `LineApiResponseCode` 기타 코드 → 에러 메시지 표시

### 4. 서버 API 호출

모두 동일한 플로우:
1. `validate` API 호출 (기존 회원 여부 확인)
2. 기존 회원 → `signIn` API 호출
3. 신규 회원 → 회원가입 플로우

**차이점**:
- 페이스북: `facebookId` (Long)를 회원가입 시 전달 (`SignUpDTO.facebookId`)
- 카카오/라인: `facebookId` 없음

---

## 구현 시 주의사항

### 페이스북 로그인

1. **이메일 필수 확인**
   ```kotlin
   if (mEmail == null || mEmail!!.isEmpty()) {
       // 에러 처리: 이메일 없음
       LoginManager.getInstance().logOut()
       return
   }
   ```

2. **FacebookCallback 등록**
   ```kotlin
   LoginManager.getInstance().registerCallback(callbackManager, facebookCallback)
   ```

3. **onActivityResult 처리**
   ```kotlin
   if ((requestCode and 0xffff) == 0xface) {
       callbackManager.onActivityResult(requestCode, resultCode, data)
   }
   ```

### 라인 로그인

1. **CHANNEL_ID 확인**
   ```kotlin
   // AppConst.CHANNEL_ID 또는 strings.xml에서 가져오기
   val channelId = AppConst.CHANNEL_ID
   ```

2. **Scope 설정**
   ```kotlin
   LineAuthenticationParams.Builder()
       .scopes(Arrays.asList(Scope.PROFILE))  // 프로필 정보만
       .build()
   ```

3. **onActivityResult 처리**
   ```kotlin
   val result = LineLoginApi.getLoginResultFromIntent(data)
   when (result.responseCode) {
       LineApiResponseCode.SUCCESS -> // 처리
       // ...
   }
   ```

---

## 요약

### Hashkey 없이 바로 적용 가능한 로그인

✅ **페이스북**: Hashkey 불필요, Facebook SDK만 있으면 바로 사용 가능
✅ **라인**: Hashkey 불필요, LINE SDK와 Channel ID만 있으면 바로 사용 가능
✅ **카카오**: Hashkey 불필요, Kakao SDK만 있으면 바로 사용 가능

### 비교

| 로그인 방식 | Hashkey 필요 | 구현 난이도 | 추가 설정 |
|------------|------------|-----------|----------|
| 페이스북 | ❌ | ⭐⭐ | Facebook App ID, Client Token |
| 라인 | ❌ | ⭐⭐ | LINE Channel ID |
| 카카오 | ❌ | ⭐⭐⭐ | Kakao App Key |
| 구글 | ✅ | ⭐⭐⭐⭐ | SHA-1 Hashkey 등록 필수 |

**결론**: 페이스북과 라인 모두 Hashkey 없이 바로 적용 가능하며, 카카오보다 구현이 더 간단합니다.

