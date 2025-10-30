package net.ib.mn.presentation.login

import android.util.Patterns
import androidx.lifecycle.viewModelScope
import net.ib.mn.base.BaseViewModel
import net.ib.mn.data.local.PreferencesManager
import net.ib.mn.data.remote.interceptor.AuthInterceptor
import net.ib.mn.domain.model.ApiResult
import net.ib.mn.domain.usecase.SignInUseCase
import net.ib.mn.util.Constants
import net.ib.mn.util.CryptoUtil
import net.ib.mn.util.DeviceUtil
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * EmailLoginViewModel - 이메일 로그인 화면 ViewModel.
 */
@HiltViewModel
class EmailLoginViewModel @Inject constructor(
    private val signInUseCase: SignInUseCase,
    private val preferencesManager: PreferencesManager,
    private val authInterceptor: AuthInterceptor,
    private val deviceUtil: DeviceUtil
) : BaseViewModel<EmailLoginContract.State, EmailLoginContract.Intent, EmailLoginContract.Effect>() {

    companion object {
        private const val TAG = "EmailLoginViewModel"
    }

    override fun createInitialState(): EmailLoginContract.State {
        return EmailLoginContract.State()
    }

    override fun handleIntent(intent: EmailLoginContract.Intent) {
        when (intent) {
            is EmailLoginContract.Intent.EmailChanged -> {
                setState { copy(email = intent.email, emailError = null) }
            }
            is EmailLoginContract.Intent.PasswordChanged -> {
                setState { copy(password = intent.password, passwordError = null) }
            }
            is EmailLoginContract.Intent.EmailFocusChanged -> {
                setState { copy(isEmailFocused = intent.isFocused) }
            }
            is EmailLoginContract.Intent.PasswordFocusChanged -> {
                setState { copy(isPasswordFocused = intent.isFocused) }
            }
            is EmailLoginContract.Intent.TogglePasswordVisibility -> {
                setState { copy(isPasswordVisible = !isPasswordVisible) }
            }
            is EmailLoginContract.Intent.SignIn -> handleSignIn()
            is EmailLoginContract.Intent.SignUp -> {
                setEffect { EmailLoginContract.Effect.NavigateToSignUp }
            }
            is EmailLoginContract.Intent.ForgotId -> {
                setEffect { EmailLoginContract.Effect.NavigateToForgotId }
            }
            is EmailLoginContract.Intent.ForgotPassword -> {
                setEffect { EmailLoginContract.Effect.NavigateToForgotPassword }
            }
            is EmailLoginContract.Intent.NavigateBack -> {
                setEffect { EmailLoginContract.Effect.NavigateBack }
            }
        }
    }

    /**
     * 로그인 처리.
     *
     * old 프로젝트의 trySignin(), afterSignin() 로직 참고:
     * - 로그인 성공 시: 토큰 해시화(MD5 salt), 사용자 정보 저장, 타임스탬프 저장
     * - 로그인 실패 시: gcode, mcode 체크하여 점검 여부 확인
     */
    private fun handleSignIn() {
        val currentState = uiState.value
        val email = currentState.email.trim()
        val password = currentState.password

        // Validation
        if (email.isEmpty()) {
            setState { copy(emailError = "Required field") }
            return
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            setState { copy(emailError = "Invalid email format") }
            return
        }

        if (password.isEmpty()) {
            setState { copy(passwordError = "Required field") }
            return
        }

        // Start sign in process
        setState { copy(isLoading = true) }

        viewModelScope.launch {
            try {
                val deviceKey = deviceUtil.getFcmToken()
                val gmail = deviceUtil.getGmail()
                val deviceId = deviceUtil.getDeviceUUID()

                // old 프로젝트와 동일: 이메일 도메인 로그인 시 비밀번호를 md5salt로 해시하여 전송
                val hashedPassword = CryptoUtil.md5salt(password) ?: password
                android.util.Log.d(TAG, "Sending login request with email: $email, hashedPassword length: ${hashedPassword.length}")

                signInUseCase(
                    domain = Constants.DOMAIN_EMAIL,
                    email = email,
                    password = hashedPassword,  // 해시된 비밀번호 전송
                    deviceKey = deviceKey,
                    gmail = gmail,
                    deviceId = deviceId,
                    appId = Constants.APP_ID
                ).collect { result ->
                    android.util.Log.d(TAG, "SignIn Result: $result")
                    when (result) {
                        is ApiResult.Success<*> -> {
                            val response = result.data as net.ib.mn.data.remote.dto.SignInResponse
                            android.util.Log.d(TAG, "SignIn Response - success: ${response.success}, message: ${response.message}, gcode: ${response.gcode}, mcode: ${response.mcode}")
                            if (response.success) {
                                // 로그인 성공 - afterSignin() 로직 구현
                                android.util.Log.d(TAG, "Login SUCCESS - navigating to success handler")
                                handleSignInSuccess(email, password, response)
                            } else {
                                // 서버에서 실패 응답 - gcode, mcode 체크
                                android.util.Log.d(TAG, "Login FAILURE - handling failure")
                                handleSignInFailure(response)
                            }
                        }
                        is ApiResult.Error -> {
                            android.util.Log.e(TAG, "SignIn Error: ${result.message}")
                            setState { copy(isLoading = false) }
                            setEffect { EmailLoginContract.Effect.ShowError(result.message ?: "Error occurred") }
                        }
                        is ApiResult.Loading -> {
                            android.util.Log.d(TAG, "SignIn Loading...")
                            // Already set loading state above
                        }
                    }
                }
            } catch (e: Exception) {
                setState { copy(isLoading = false) }
                setEffect {
                    EmailLoginContract.Effect.ShowError(
                        e.message ?: "Unknown error occurred"
                    )
                }
            }
        }
    }

    /**
     * 로그인 성공 처리
     *
     * old 프로젝트의 afterSignin() 로직:
     * 1. 이메일 도메인인 경우 비밀번호를 MD5 salt로 해시화
     * 2. 토큰, 이메일, 도메인 저장 (IdolAccount.createAccount 동일)
     * 3. 사용자 정보 저장 (userId, email, username, nickname, profileImage)
     * 4. 로그인 타임스탬프 저장
     * 5. AuthInterceptor에 토큰 설정
     * 6. StartupActivity로 이동
     */
    private suspend fun handleSignInSuccess(
        email: String,
        password: String,
        response: net.ib.mn.data.remote.dto.SignInResponse
    ) {
        android.util.Log.d("USER_INFO", "========================================")
        android.util.Log.d("USER_INFO", "[EmailLoginViewModel] Login Success - Analyzing server response")
        android.util.Log.d("USER_INFO", "========================================")
        android.util.Log.d("USER_INFO", "[EmailLoginViewModel] response.success: ${response.success}")
        android.util.Log.d("USER_INFO", "[EmailLoginViewModel] response.data: ${response.data}")
        android.util.Log.d("USER_INFO", "[EmailLoginViewModel] response.data?.token: ${response.data?.token}")
        android.util.Log.d("USER_INFO", "[EmailLoginViewModel] response.data?.userId: ${response.data?.userId}")
        android.util.Log.d("USER_INFO", "[EmailLoginViewModel] response.data?.email: ${response.data?.email}")
        android.util.Log.d("USER_INFO", "[EmailLoginViewModel] response.data?.username: ${response.data?.username}")
        android.util.Log.d("USER_INFO", "========================================")

        android.util.Log.d(TAG, "========================================")
        android.util.Log.d(TAG, "Login Success - Saving user data...")
        android.util.Log.d(TAG, "========================================")
        android.util.Log.d(TAG, "Response data: ${response.data}")
        android.util.Log.d(TAG, "Response data.token: ${response.data?.token}")
        android.util.Log.d(TAG, "Response data.userId: ${response.data?.userId}")
        android.util.Log.d(TAG, "Response data.email: ${response.data?.email}")

        // 1. 이메일 도메인인 경우 비밀번호를 MD5 salt로 해시화하여 저장
        // old: val hashToken = Util.md5salt(token)
        val hashedToken = CryptoUtil.md5salt(password) ?: password
        android.util.Log.d(TAG, "✓ Password hashed for local storage")

        // 2. old 프로젝트의 IdolAccount.createAccount와 동일하게 저장
        // old: editor.putString(PREF_KEY__EMAIL, email)
        //      editor.putString(PREF_KEY__TOKEN, token)  <- hashToken (해시된 비밀번호)
        //      editor.putString(PREF_KEY__DOMAIN, domain)
        //      editor.commit()

        // old 프로젝트는 서버 응답의 token을 사용하지 않고, 입력한 비밀번호를 해시하여 저장
        // trySignin에서 afterSignin(email, passwd, domain) 호출
        // afterSignin에서 md5salt(passwd)를 token으로 저장

        // Access token 저장 (StartUpViewModel에서 체크하는 값)
        val tokenToSave = response.data?.token ?: hashedToken

        android.util.Log.d("USER_INFO", "========================================")
        android.util.Log.d("USER_INFO", "[EmailLoginViewModel] Saving auth credentials to DataStore")
        android.util.Log.d("USER_INFO", "[EmailLoginViewModel]   - Email: $email")
        android.util.Log.d("USER_INFO", "[EmailLoginViewModel]   - Domain: ${Constants.DOMAIN_EMAIL}")
        android.util.Log.d("USER_INFO", "[EmailLoginViewModel]   - Token source: ${if (response.data?.token != null) "server response" else "hashed password"}")
        android.util.Log.d("USER_INFO", "[EmailLoginViewModel]   - Token preview: ${tokenToSave.take(20)}...")

        // old 프로젝트와 동일: email, domain, token을 모두 저장
        // old에서는 IdolAccount.createAccount(context, email, token, domain) 호출
        preferencesManager.setAccessToken(tokenToSave)
        preferencesManager.setLoginDomain(Constants.DOMAIN_EMAIL)

        // IMPORTANT: Email도 명시적으로 저장 (StartUpViewModel에서 필요)
        // setUserInfo에서도 email을 저장하지만, response.data가 null일 수 있으므로
        // 여기서 미리 저장하여 StartUpViewModel이 즉시 사용할 수 있도록 함
        if (response.data != null) {
            // 서버에서 받은 전체 정보 저장
            preferencesManager.setUserInfo(
                id = response.data.userId,
                email = response.data.email,
                username = response.data.username,
                nickname = response.data.nickname,
                profileImage = response.data.profileImage,
                hearts = null
            )
            android.util.Log.d("USER_INFO", "[EmailLoginViewModel] ✓ User info saved (from server response)")
        } else {
            // 서버가 user data를 보내지 않은 경우, email만 최소한 저장
            // (id=0은 임시값, StartUpViewModel에서 /users/self/로 정확한 정보 가져옴)
            preferencesManager.setUserInfo(
                id = 0,  // 임시값
                email = email,
                username = email.substringBefore("@"),  // 임시값
                nickname = null,
                profileImage = null,
                hearts = null
            )
            android.util.Log.d("USER_INFO", "[EmailLoginViewModel] ✓ Minimal user info saved (email only)")
            android.util.Log.d("USER_INFO", "[EmailLoginViewModel]   - Full user info will be loaded from /users/self/")
        }

        android.util.Log.d("USER_INFO", "[EmailLoginViewModel] ✓ Auth credentials saved to DataStore")
        android.util.Log.d("USER_INFO", "========================================")

        android.util.Log.d(TAG, "✓ Access token saved: ${tokenToSave.take(20)}...")
        android.util.Log.d(TAG, "  (Source: ${if (response.data?.token != null) "server response" else "hashed password"})")
        android.util.Log.d(TAG, "✓ Login domain saved: ${Constants.DOMAIN_EMAIL}")
        android.util.Log.d(TAG, "✓ User email saved: $email")

        // 4. 로그인 타임스탬프 저장
        // old: Util.setPreference(this, "user_login_ts", System.currentTimeMillis())
        val loginTimestamp = System.currentTimeMillis()
        preferencesManager.setLoginTimestamp(loginTimestamp)
        android.util.Log.d(TAG, "✓ Login timestamp saved: $loginTimestamp")

        // 5. AuthInterceptor에 인증 정보 설정 (API 호출용)
        // old: IdolAccount.createAccount()는 sAccount 메모리 객체도 생성
        android.util.Log.d("USER_INFO", "[EmailLoginViewModel] Setting auth credentials in AuthInterceptor...")
        authInterceptor.setAuthCredentials(email, Constants.DOMAIN_EMAIL, tokenToSave)
        android.util.Log.d("USER_INFO", "[EmailLoginViewModel] ✓ AuthInterceptor credentials set for immediate API calls")
        android.util.Log.d(TAG, "✓ AuthInterceptor credentials set")

        android.util.Log.d(TAG, "========================================")
        android.util.Log.d(TAG, "All login data saved successfully!")
        android.util.Log.d(TAG, "Navigating to StartUp screen...")
        android.util.Log.d(TAG, "========================================")

        // NOTE: AppsFlyer 이벤트 로깅 구현 가이드 (필요 시 추가):
        // 1. build.gradle에 추가: implementation("com.appsflyer:af-android-sdk:6.x.x")
        // 2. Application 클래스에서 AppsFlyer 초기화
        // 3. 로그인 완료 후 이벤트 전송:
        // val appsFlyerInstance = AppsFlyerLib.getInstance()
        // val eventValues = mapOf(
        //     "user_id" to appsFlyerInstance.getAppsFlyerUID(context),
        //     AFInAppEventParameterName.REGISTRATION_METHOD to Constants.DOMAIN_EMAIL
        // )
        // appsFlyerInstance.logEvent(context, AFInAppEventType.COMPLETE_REGISTRATION, eventValues)

        // 6. StartupActivity로 이동 (old: startActivity(StartupActivity.createIntent(this)))
        // 모든 데이터 저장 완료 후 navigate
        setState { copy(isLoading = false) }
        setEffect { EmailLoginContract.Effect.NavigateToMain }
    }

    /**
     * 로그인 실패 처리
     *
     * old 프로젝트의 trySignin() 에러 처리 로직:
     * - gcode=88888, mcode=1: 점검 중 메시지 표시
     * - 그 외: 일반 에러 메시지 표시
     */
    private fun handleSignInFailure(response: net.ib.mn.data.remote.dto.SignInResponse) {
        setState { copy(isLoading = false) }

        // 점검 중인지 확인
        if (response.gcode == 88888 && response.mcode == 1) {
            // 점검 중 메시지 표시
            setEffect {
                EmailLoginContract.Effect.ShowError(
                    response.message ?: "서비스 점검 중입니다."
                )
            }
        } else {
            // 일반 에러 메시지 표시
            setEffect {
                EmailLoginContract.Effect.ShowError(
                    response.message ?: "로그인에 실패했습니다."
                )
            }
        }
    }
}
