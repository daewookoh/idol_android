package net.ib.mn.presentation.login

import android.content.Context
import android.util.Patterns
import androidx.lifecycle.viewModelScope
import net.ib.mn.base.BaseViewModel
import net.ib.mn.data.local.PreferencesManager
import net.ib.mn.data.repository.AuthRepository
import net.ib.mn.domain.model.ApiResult
import net.ib.mn.domain.repository.UserRepository
import net.ib.mn.domain.usecase.SignInUseCase
import net.ib.mn.util.Constants
import net.ib.mn.util.CryptoUtil
import net.ib.mn.util.DeviceUtil
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * EmailLoginViewModel - 이메일 로그인 화면 ViewModel.
 */
@HiltViewModel
class EmailLoginViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val signInUseCase: SignInUseCase,
    private val preferencesManager: PreferencesManager,
    private val authRepository: AuthRepository,
    private val deviceUtil: DeviceUtil,
    private val userRepository: UserRepository
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
                handleFindId()
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

        // old 프로젝트와 동일: email, domain, token만 저장 (IdolAccount.createAccount)
        // old: IdolAccount.createAccount(context, email, token, domain)
        // old 프로젝트는 afterSignin에서 createAccount 호출 후 StartupActivity로 이동
        // StartupActivity에서 getUserSelf API를 호출하여 전체 사용자 정보를 가져옴
        authRepository.login(
            email = email,
            domain = Constants.DOMAIN_EMAIL,
            token = tokenToSave
        )
        
        // old 프로젝트와 동일: email을 별도로 저장 (StartUpViewModel에서 loginEmail로 확인)
        // setUserInfo를 최소한의 정보로 호출하여 email 저장 (id는 0으로 설정, 나머지는 StartUpScreen에서 업데이트)
        preferencesManager.setUserInfo(
            id = 0, // StartUpScreen에서 getUserSelf API로 실제 ID로 업데이트됨
            email = email,
            username = "",
            nickname = null,
            profileImage = null,
            heart = null,
            domain = Constants.DOMAIN_EMAIL
        )
        
        android.util.Log.d("USER_INFO", "[EmailLoginViewModel] ✓ Auth credentials saved (email, domain, token)")
        android.util.Log.d("USER_INFO", "[EmailLoginViewModel]   - Email: $email")
        android.util.Log.d("USER_INFO", "[EmailLoginViewModel]   - Domain: ${Constants.DOMAIN_EMAIL}")
        android.util.Log.d("USER_INFO", "[EmailLoginViewModel]   - Token: ${tokenToSave.take(20)}...")
        android.util.Log.d("USER_INFO", "[EmailLoginViewModel]   - Note: Full user info will be fetched in StartUpScreen")

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

        // 5. 디바이스 ID 저장 (아이디 찾기용)
        // old 프로젝트: 로그인 시 서버에 deviceId가 전달되지만, 로컬에도 저장하여 재설치 시에도 사용 가능하도록 함
        val deviceId = deviceUtil.getDeviceUUID()
        preferencesManager.setDeviceId(deviceId)
        android.util.Log.d(TAG, "✓ Device ID saved: $deviceId")

        // 6. AuthRepository는 이미 위에서 login() 호출 완료 (메모리 캐시 + DataStore 저장)
        // old: IdolAccount.createAccount()는 sAccount 메모리 객체도 생성
        android.util.Log.d("USER_INFO", "[EmailLoginViewModel] ✓ AuthRepository credentials already set (memory + DataStore)")
        android.util.Log.d(TAG, "✓ Auth credentials saved via AuthRepository")

        android.util.Log.d(TAG, "========================================")
        android.util.Log.d(TAG, "All login data saved successfully!")
        android.util.Log.d(TAG, "Navigating to StartUp screen...")
        android.util.Log.d(TAG, "========================================")

        // 6. old 프로젝트와 동일: StartupActivity로 이동
        // old: startActivity(StartupActivity.createIntent(this))
        // StartupActivity에서 getUserSelf API를 호출하여 전체 사용자 정보를 가져옴
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
        setEffect { EmailLoginContract.Effect.NavigateToStartUp }
    }

    /**
     * 로그인 실패 처리
     *
     * old 프로젝트의 trySignin() 에러 처리 로직:
     * - gcode=88888, mcode=1: 점검 중 메시지 표시
     * - gcode=1031: 비밀번호 확인 메시지 (ErrorControl.parseError 사용)
     * - 그 외: ErrorControl.parseError로 gcode에 따른 메시지 표시
     */
    private fun handleSignInFailure(response: net.ib.mn.data.remote.dto.SignInResponse) {
        setState { copy(isLoading = false) }

        android.util.Log.e(TAG, "========================================")
        android.util.Log.e(TAG, "Login FAILURE")
        android.util.Log.e(TAG, "  gcode: ${response.gcode}")
        android.util.Log.e(TAG, "  mcode: ${response.mcode}")
        android.util.Log.e(TAG, "  message: ${response.message}")
        android.util.Log.e(TAG, "========================================")

        // 점검 중인지 확인
        if (response.gcode == 88888 && response.mcode == 1) {
            // 점검 중 메시지 표시
            val errorMessage = response.message ?: "서비스 점검 중입니다."
            setEffect {
                EmailLoginContract.Effect.ShowError(errorMessage)
            }
        } else {
            // old 프로젝트: ErrorControl.parseError 사용하여 gcode에 따른 메시지 표시
            val errorMessage = when (response.gcode) {
                1031 -> context.getString(net.ib.mn.R.string.error_1031) // "비밀번호를 확인해 주세요"
                1030 -> context.getString(net.ib.mn.R.string.error_1030) // "닉네임이 잘못되었습니다"
                1002 -> context.getString(net.ib.mn.R.string.error_1002) // "이메일을 확인해 주세요"
                88888 -> response.message ?: context.getString(net.ib.mn.R.string.error_abnormal_default)
                else -> {
                    // message가 있으면 message 사용, 없으면 기본 메시지
                    response.message ?: context.getString(net.ib.mn.R.string.error_abnormal_default)
                }
            }
            setEffect {
                EmailLoginContract.Effect.ShowError(errorMessage)
            }
        }
    }

    /**
     * 아이디 찾기 처리
     * old 프로젝트의 findId() 로직 참고
     * 
     * 앱 재설치 시에도 아이디를 찾을 수 있도록:
     * 1. 저장된 deviceId 사용 (DataStore에 저장된 마지막 로그인 시 deviceId)
     * 2. 저장된 deviceId가 없으면 현재 deviceId 사용
     */
    private fun handleFindId() {
        viewModelScope.launch {
            try {
                // 저장된 deviceId 우선 사용 (마지막 로그인 시 저장된 deviceId)
                val savedDeviceId = preferencesManager.deviceId.first()
                val deviceId = savedDeviceId ?: deviceUtil.getDeviceUUID()
                
                android.util.Log.d(TAG, "Finding ID with deviceId: $deviceId (saved: ${savedDeviceId != null})")
                
                userRepository.findId(deviceId).collect { result ->
                    when (result) {
                        is ApiResult.Success -> {
                            val email = result.data
                            // 빈 문자열이면 아이디를 찾을 수 없음
                            setEffect {
                                EmailLoginContract.Effect.ShowFindIdDialog(
                                    if (email.isNullOrEmpty()) null else email
                                )
                            }
                        }
                        is ApiResult.Error -> {
                            android.util.Log.e(TAG, "Find ID error: ${result.message}")
                            setEffect {
                                EmailLoginContract.Effect.ShowFindIdDialog(null)
                            }
                        }
                        is ApiResult.Loading -> {
                            // 로딩 상태는 필요 없음 (다이얼로그만 표시)
                        }
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e(TAG, "Find ID exception", e)
                setEffect {
                    EmailLoginContract.Effect.ShowFindIdDialog(null)
                }
            }
        }
    }
}
