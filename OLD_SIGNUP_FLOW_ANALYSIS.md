# Old 프로젝트 가입 플로우 분석

## 개요
Old 프로젝트의 SNS 로그인 후 가입 플로우를 분석하여 신규 프로젝트에 적용합니다.

## 카카오 로그인 플로우

### 1. 카카오 로그인 성공 후 (`getSignupValidate()`)

```kotlin
usersRepository.validate(
    type = "email",
    value = mEmail,
    appId = AppConst.APP_ID,
    listener = { response ->
        if (response.optBoolean("success")) {
            // 이미 가입된 이메일 → 약관 동의 화면으로 이동
            setAgreementFragment(DOMAIN_KAKAO, AgreementFragment.LOGIN_KAKAO)
        } else {
            // 신규 회원 → 푸시 키 등록 후 trySignin() 호출 (버그로 보임)
            GcmUtils.registerDevice(this@AuthActivity, wrap)
        }
    }
)
```

**주석**: "기존에 아이디를 갖고 회원가입을 했었는지 확인한다"

### 2. validate API 응답 의미

- **`success == true`**: 이미 가입된 이메일 (기존 회원)
  - → `setAgreementFragment()` 호출
  - → 약관 동의 화면 (`AgreementFragment`)
  - → 약관 동의 후 `KakaoMoreFragment`로 이동
  - → 회원가입 API 호출 (`processSignup()`)
  - → 회원가입 성공 후 `trySignin()` 호출

- **`success == false`**: 신규 회원 (이메일이 존재하지 않음)
  - → `GcmUtils.registerDevice()` 호출
  - → 푸시 키 등록 후 `wrap` 콜백 실행
  - → `wrap` 콜백에서 `trySignin()` 호출
  - **⚠️ 버그**: 신규 회원인데 `trySignin()`을 호출하면 실패할 수 있음

### 3. wrap 콜백

```kotlin
val wrap = OnRegistered { id -> // preference에 저장
    Util.setPreference(this@AuthActivity, Const.PREF_GCM_PUSH_KEY, id)
    // 로그인
    // 다른 쓰레드로 불려 mDomain 값이 카카오 로그인을 해도 null로 가는 현상이 있어 preference로 저장
    val domain = Util.getPreference(this@AuthActivity, KEY_DOMAIN)
    trySignin(mEmail ?: return@OnRegistered, mPasswd ?: return@OnRegistered, id, domain)
}
```

### 4. AgreementFragment → KakaoMoreFragment 플로우

1. `AgreementFragment`에서 약관 동의 체크
2. "다음" 버튼 클릭 시:
   ```kotlin
   if (email != null) {
       if (loginType == LOGIN_GOOGLE) {
           val mGoogleMoreFragment = GoogleMoreFragment.newInstance(email, displayName, password, "")
           fm.beginTransaction()
               .replace(R.id.fragment_container, mGoogleMoreFragment)
               .addToBackStack(null).commit()
       } else {
           val mKakaoMoreFragment = KakaoMoreFragment.newInstance(email, displayName, password, domain, "", facebookId)
           fm.beginTransaction()
               .replace(R.id.fragment_container, mKakaoMoreFragment)
               .addToBackStack(null).commit()
       }
   }
   ```

### 5. KakaoMoreFragment → 회원가입 API 호출 (`processSignup()`)

```kotlin
usersRepository.signUp(
    domain = mDomain,
    email = mEmail!!,
    passwd = mPasswd!!,
    name = binding.etName.text.toString(),
    referralCode = recommender,
    deviceKey = deviceKey!!,
    version = getString(R.string.app_version),
    googleAccount = Const.PARMA_N,
    time = date?.time ?: 0,
    appId = AppConst.APP_ID,
    deviceId = Util.getDeviceUUID(context),
    gmail = gmail,
    facebookId = facebookId,
    listener = listener,
    errorListener = errorListener
)
```

**회원가입 API 응답 처리**:
```kotlin
if (response.optBoolean("success")) {
    // 회원가입 성공 → trySignin() 호출
    trySignin(mEmail, mPasswd, deviceKey, callback)
} else {
    // 회원가입 실패 → 에러 표시
    val gcode = response.optInt("gcode")
    val responseMsg = ErrorControl.parseError(activity, response)
    // 에러 처리...
}
```

## 문제점 분석

### Old 프로젝트의 버그
- `validate` API에서 `success == false`일 때 (신규 회원) `trySignin()`을 호출하는 것은 버그로 보입니다.
- 신규 회원인 경우 회원가입이 필요하므로 `trySignin()`을 호출하면 실패할 수 있습니다.

### 실제 의도된 플로우
1. **기존 회원** (`success == true`):
   - 약관 동의 화면 → `KakaoMoreFragment` → 회원가입 API 호출 → `trySignin()` 호출
   - ⚠️ 이건 이상한데... 기존 회원인데 왜 회원가입 API를 호출하는가?
   - 아마도 약관 동의를 다시 받기 위한 플로우인 것 같습니다.

2. **신규 회원** (`success == false`):
   - 약관 동의 화면으로 이동해야 하는데, Old 프로젝트는 `trySignin()`을 호출합니다.
   - 이는 버그로 보이며, 실제로는 약관 동의 화면으로 이동해야 합니다.

## 신규 프로젝트 적용 방안

### 현재 코드의 로직
```kotlin
if (response.success) {
    // 회원이 존재함 → 바로 로그인 진행
    performSignIn()
} else {
    // 회원이 존재하지 않음 → 회원가입 플로우로 이동
    setEffect {
        LoginContract.Effect.NavigateToSignUp(...)
    }
}
```

### Old 프로젝트의 실제 플로우를 반영하면
```kotlin
if (response.success) {
    // 기존 회원 → 약관 동의 화면으로 이동 (약관 동의를 다시 받기 위함)
    setEffect {
        LoginContract.Effect.NavigateToSignUp(...)
    }
} else {
    // 신규 회원 → 약관 동의 화면으로 이동
    setEffect {
        LoginContract.Effect.NavigateToSignUp(...)
    }
}
```

**결론**: Old 프로젝트의 로직이 일관성이 없어 보입니다. `success == true`일 때도 약관 동의 화면으로 이동하는 것을 보면, 실제로는:
- **`success == true`**: 약관 동의 화면 → 회원가입 API 호출 → `trySignin()` 호출
- **`success == false`**: 약관 동의 화면 → 회원가입 API 호출 → `trySignin()` 호출

즉, **두 경우 모두 약관 동의 화면으로 이동**하는 것이 맞는 것 같습니다.

하지만 주석에는 "기존에 아이디를 갖고 회원가입을 했었는지 확인한다"고 되어 있으므로, `success == true`는 "이미 가입된 이메일"을 의미하는 것이 맞습니다.

그렇다면 Old 프로젝트의 실제 의도는:
- **`success == true`**: 이미 가입된 회원 → 약관 동의를 다시 받기 위해 약관 동의 화면으로 이동 → 회원가입 API 호출 (이미 가입된 회원이지만 약관 동의를 다시 받기 위함) → `trySignin()` 호출
- **`success == false`**: 신규 회원 → 약관 동의 화면으로 이동 → 회원가입 API 호출 → `trySignin()` 호출

## 최종 결론

Old 프로젝트의 로직이 복잡하고 일관성이 없어 보입니다. 

**신규 프로젝트의 현재 로직이 더 합리적입니다**:
- `success == true`: 기존 회원 → 바로 로그인
- `success == false`: 신규 회원 → 회원가입 플로우로 이동

하지만 사용자가 "이메일이 잘못되었습니다" 에러를 받고 있다는 것은, 아마도 `validate` API에서 `success == true`를 받았는데 실제로는 신규 회원인 경우일 수 있습니다. 또는 `signIn` API 호출 시 파라미터가 잘못되었을 수 있습니다.

