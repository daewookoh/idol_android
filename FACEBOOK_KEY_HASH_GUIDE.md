# 페이스북 Key Hash 오류 해결 가이드

## 문제 상황
페이스북 로그인 시 "invalid key hash" 오류가 발생합니다.

## 원인
페이스북은 **개발 단계**에서 Key Hash (SHA-1)를 Facebook Developer Console에 등록해야 합니다.
- Google Play Store에 배포하면 자동으로 확인되지만, 로컬 개발/디버깅 시에는 수동 등록이 필요합니다.

## 해결 방법

### 1. Key Hash 확인하기

#### 방법 1: keytool 명령어 사용 (Debug Key Store)

```bash
# macOS/Linux
keytool -list -v -keystore ~/.android/debug.keystore -alias androiddebugkey -storepass android -keypass android | grep SHA1

# Windows
keytool -list -v -keystore "%USERPROFILE%\.android\debug.keystore" -alias androiddebugkey -storepass android -keypass android | findstr SHA1
```

출력 예시:
```
SHA1: AA:BB:CC:DD:EE:FF:11:22:33:44:55:66:77:88:99:AA:BB:CC:DD:EE
```

**중요**: 콜론(`:`)을 제거하고 등록해야 합니다.
예: `AABBCCDDEEFF112233445566778899AABBCCDDEE`

#### 방법 2: Release Key Store 사용 시

만약 release keystore를 사용한다면:

```bash
keytool -list -v -keystore [keystore 파일 경로] -alias [alias 이름]
```

비밀번호 입력 후 SHA1 값을 확인합니다.

#### 방법 3: 코드로 확인 (임시)

앱 실행 시 로그로 Key Hash를 확인할 수 있습니다:

```kotlin
// 임시로 IdolApplication.kt에 추가
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.content.pm.Signature
import android.util.Base64
import java.security.MessageDigest

// onCreate()에 추가
try {
    val info: PackageInfo = packageManager.getPackageInfo(
        packageName,
        PackageManager.GET_SIGNATURES
    )
    for (signature in info.signatures) {
        val md = MessageDigest.getInstance("SHA")
        md.update(signature.toByteArray())
        val hashKey = Base64.encodeToString(md.digest(), Base64.DEFAULT)
        android.util.Log.d("FACEBOOK_KEY_HASH", "Key Hash: $hashKey")
        android.util.Log.d("FACEBOOK_KEY_HASH", "Key Hash (no newline): ${hashKey.trim()}")
    }
} catch (e: Exception) {
    android.util.Log.e("FACEBOOK_KEY_HASH", "Error getting hash", e)
}
```

### 2. Facebook Developer Console에 등록하기

1. [Facebook Developers](https://developers.facebook.com/) 접속
2. 해당 앱 선택
3. **Settings** → **Basic** 메뉴로 이동
4. **Key Hashes** 섹션 찾기
5. 확인한 Key Hash를 추가
   - 여러 개 추가 가능 (개발용, 릴리즈용 각각 등록)
   - 콜론(`:`) 없이 입력

### 3. 패키지명 확인

Facebook Developer Console에서 **Package Name**도 확인해야 합니다:
- `net.ib.mn` (app flavor)
- `com.exodus.myloveidol.twostore` (onestore flavor)
- `com.exodus.myloveidol.china` (china flavor)
- `com.exodus.myloveactor` (celeb flavor)

각 flavor별로 다른 Facebook App ID를 사용하므로, 각각의 Facebook App에 등록해야 합니다.

### 4. 현재 프로젝트의 Facebook App ID

각 flavor별 Facebook App ID:
- **app**: `234065935136588` (strings.xml의 `facebook_app_id`)
- **onestore**: 확인 필요
- **china**: 확인 필요
- **celeb**: `751301805589552` (strings.xml의 `actor_facebook_app_id`)

### 5. 확인 후 테스트

1. Key Hash 등록 후 몇 분 정도 기다림 (Facebook 서버 반영 시간)
2. 앱 재시작
3. 페이스북 로그인 테스트

## 참고사항

- **Debug 모드**: Android 기본 debug keystore 사용 (`~/.android/debug.keystore`)
- **Release 모드**: 프로젝트의 release keystore 사용
- 여러 개발자가 작업하는 경우, 각자의 debug keystore의 Key Hash를 모두 등록해야 합니다.
- Google Play Store에 배포하면 자동으로 확인되므로, 개발 단계에서만 필요합니다.

## 빠른 해결 방법

가장 빠른 방법은 **방법 3 (코드로 확인)**을 사용하여 앱 실행 시 로그를 확인하고, 해당 값을 Facebook Developer Console에 등록하는 것입니다.

