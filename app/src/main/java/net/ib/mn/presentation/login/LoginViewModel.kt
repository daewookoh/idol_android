package net.ib.mn.presentation.login

import android.content.Context
import androidx.lifecycle.viewModelScope
import net.ib.mn.base.BaseViewModel
import net.ib.mn.data.local.PreferencesManager
import net.ib.mn.data.remote.interceptor.AuthInterceptor
import net.ib.mn.domain.model.ApiResult
import net.ib.mn.domain.usecase.SignInUseCase
import net.ib.mn.domain.usecase.ValidateUserUseCase
import net.ib.mn.util.Constants
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

/**
 * Login 화면의 ViewModel.
 * old 프로젝트의 AuthActivity + SigninFragment 비즈니스 로직을 MVI 패턴으로 구현.
 *
 * 주요 기능:
 * 1. 소셜 로그인 (Kakao, Google, Line, Facebook)
 * 2. Email 로그인
 * 3. 로그인 상태 관리
 * 4. 에러 처리
 *
 * 로그인 플로우 (old 프로젝트 참고):
 * 1. 소셜 SDK로 로그인 -> access token + 사용자 정보 받음
 * 2. validate API로 회원 여부 확인
 * 3. 회원이면 signIn API 호출
 * 4. 비회원이면 회원가입 프로세스로 이동 (TODO)
 * 5. 로그인 성공 시 토큰 저장 및 메인으로 이동
 */
@HiltViewModel
class LoginViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val validateUserUseCase: ValidateUserUseCase,
    private val signInUseCase: SignInUseCase,
    private val preferencesManager: PreferencesManager,
    private val authInterceptor: AuthInterceptor
) : BaseViewModel<LoginContract.State, LoginContract.Intent, LoginContract.Effect>() {

    companion object {
        private const val TAG = "LoginViewModel"
    }

    // 소셜 로그인 임시 데이터 저장
    private var tempEmail: String? = null
    private var tempPassword: String? = null
    private var tempDomain: String? = null

    override fun createInitialState(): LoginContract.State {
        return LoginContract.State()
    }

    override fun handleIntent(intent: LoginContract.Intent) {
        when (intent) {
            is LoginContract.Intent.LoginWithKakao -> loginWithKakao()
            is LoginContract.Intent.LoginWithGoogle -> loginWithGoogle()
            is LoginContract.Intent.LoginWithLine -> loginWithLine()
            is LoginContract.Intent.LoginWithFacebook -> loginWithFacebook()
            is LoginContract.Intent.NavigateToEmailLogin -> navigateToEmailLogin()
        }
    }

    /**
     * Kakao 로그인.
     * LoginScreen에서 Kakao SDK로 받은 정보를 처리합니다.
     */
    private fun loginWithKakao() {
        viewModelScope.launch {
            try {
                setState { copy(isLoading = true, loginType = LoginContract.LoginType.KAKAO) }
                android.util.Log.d(TAG, "Kakao login started")

                // Kakao SDK 호출은 LoginScreen에서 처리
                setEffect { LoginContract.Effect.StartSocialLogin(LoginContract.LoginType.KAKAO) }

            } catch (e: Exception) {
                handleError(e)
            }
        }
    }

    /**
     * Kakao 로그인 결과 처리.
     * LoginScreen에서 Kakao SDK로부터 받은 정보를 처리합니다.
     *
     * @param userId Kakao user ID
     * @param nickname 사용자 닉네임
     * @param profileImageUrl 프로필 이미지 URL
     * @param accessToken Kakao access token
     */
    fun handleKakaoLoginResult(
        userId: Long,
        nickname: String?,
        profileImageUrl: String?,
        accessToken: String
    ) {
        viewModelScope.launch {
            try {
                // old 코드: mEmail = "$id${Const.POSTFIX_KAKAO}"
                val email = "$userId${Constants.POSTFIX_KAKAO}"
                tempEmail = email
                tempPassword = accessToken // old 코드: mPasswd = mAuthToken
                tempDomain = Constants.DOMAIN_KAKAO

                android.util.Log.d(TAG, "Kakao login success - userId: $userId, email: $email")

                // validate API 호출하여 회원 여부 확인
                validateAndSignIn(email, Constants.DOMAIN_KAKAO)

            } catch (e: Exception) {
                handleError(e)
            }
        }
    }

    /**
     * 회원 여부 확인 및 로그인.
     */
    private suspend fun validateAndSignIn(email: String, domain: String) {
        validateUserUseCase(
            type = "email",
            value = email,
            appId = Constants.APP_ID
        ).collect { result ->
            when (result) {
                is ApiResult.Loading -> {
                    // 로딩 중
                }
                is ApiResult.Success -> {
                    val response = result.data

                    if (response.success) {
                        // 회원이 존재함 -> 바로 로그인 진행 (old 프로젝트와 동일)
                        android.util.Log.d(TAG, "User exists - proceeding to sign in")
                        performSignIn()
                    } else {
                        // 회원이 존재하지 않음 -> domain 확인 (old 프로젝트 로직)
                        val registeredDomain = response.domain
                        android.util.Log.d(TAG, "User not found - checking if registered with different method")
                        android.util.Log.d(TAG, "Registered domain: $registeredDomain, trying domain: $domain")

                        if (registeredDomain != null && !registeredDomain.equals(domain, ignoreCase = true)) {
                            // 다른 소셜 로그인으로 가입됨
                            android.util.Log.w(TAG, "User registered with different method: $registeredDomain")
                            setState { copy(isLoading = false) }
                            setEffect {
                                LoginContract.Effect.ShowError("이미 다른 방법(${registeredDomain})으로 가입된 계정입니다.")
                            }
                        } else {
                            // 신규 회원가입 필요
                            android.util.Log.d(TAG, "New user - sign up required")
                            setState { copy(isLoading = false) }
                            setEffect {
                                LoginContract.Effect.ShowToast("회원가입이 필요합니다. (회원가입 화면 미구현)")
                            }
                            // NOTE: 회원가입 화면 구현 시 NavigateToSignUp Effect 추가
                        }
                    }
                }
                is ApiResult.Error -> {
                    setState { copy(isLoading = false) }
                    setEffect {
                        LoginContract.Effect.ShowError(result.message ?: "Validation failed")
                    }
                }
            }
        }
    }

    /**
     * 실제 로그인 API 호출.
     *
     * NOTE: old 프로젝트 참고 - 서버는 로그인 성공 여부만 반환하고 토큰을 반환하지 않습니다.
     * 클라이언트가 소셜 플랫폼의 access token을 직접 저장하여 사용합니다.
     */
    private suspend fun performSignIn() {
        val email = tempEmail ?: return
        val password = tempPassword ?: return
        val domain = tempDomain ?: return

        // Device info
        val deviceId = getDeviceId()
        val gmail = getGmail()
        val deviceKey = getFcmToken() // FCM token from DataStore

        // GOOGLE LOGIN: 파라미터 로그 출력
        android.util.Log.d("GOOGLE_LOGIN", "========================================")
        android.util.Log.d("GOOGLE_LOGIN", "SignIn API Parameters:")
        android.util.Log.d("GOOGLE_LOGIN", "  domain: $domain")
        android.util.Log.d("GOOGLE_LOGIN", "  email: $email")
        android.util.Log.d("GOOGLE_LOGIN", "  passwd: $password")
        android.util.Log.d("GOOGLE_LOGIN", "  push_key: $deviceKey")
        android.util.Log.d("GOOGLE_LOGIN", "  gmail: $gmail")
        android.util.Log.d("GOOGLE_LOGIN", "  device_id: $deviceId")
        android.util.Log.d("GOOGLE_LOGIN", "  app_id: ${Constants.APP_ID}")
        android.util.Log.d("GOOGLE_LOGIN", "========================================")

        signInUseCase(
            domain = domain,
            email = email,
            password = password,
            deviceKey = deviceKey,
            gmail = gmail,
            deviceId = deviceId,
            appId = Constants.APP_ID
        ).collect { result ->
            when (result) {
                is ApiResult.Loading -> {
                    // 로딩 중
                }
                is ApiResult.Success -> {
                    val response = result.data

                    if (response.success && response.data != null) {
                        val userData = response.data

                        // 서버에서 받은 토큰과 사용자 정보 저장
                        android.util.Log.d(TAG, "========================================")
                        android.util.Log.d(TAG, "Login success - saving user info")
                        android.util.Log.d(TAG, "  userId: ${userData.userId}")
                        android.util.Log.d(TAG, "  email: ${userData.email}")
                        android.util.Log.d(TAG, "  username: ${userData.username}")
                        android.util.Log.d(TAG, "  domain: $domain")
                        android.util.Log.d(TAG, "========================================")

                        // 1. 인증 정보 저장 (AuthInterceptor에 설정)
                        authInterceptor.setAuthCredentials(
                            email = userData.email,
                            domain = domain,
                            token = userData.token
                        )

                        // 2. 기본 사용자 정보 저장 (나머지는 getUserSelf에서 받아서 저장)
                        preferencesManager.setUserInfo(
                            id = userData.userId,
                            email = userData.email,
                            username = userData.username,
                            nickname = null, // getUserSelf에서 받음
                            profileImage = null,
                            hearts = null,
                            domain = domain  // 로그인 타입 저장
                        )

                        android.util.Log.d(TAG, "✓ User info and credentials saved")

                        setState { copy(isLoading = false, loginType = null) }
                        setEffect { LoginContract.Effect.NavigateToMain }
                    } else {
                        setState { copy(isLoading = false) }
                        setEffect {
                            LoginContract.Effect.ShowError(
                                response.message ?: "Login failed"
                            )
                        }
                    }
                }
                is ApiResult.Error -> {
                    setState { copy(isLoading = false) }
                    setEffect {
                        LoginContract.Effect.ShowError(result.message ?: "Login failed")
                    }
                }
            }
        }
    }

    /**
     * Google 로그인.
     */
    private fun loginWithGoogle() {
        viewModelScope.launch {
            try {
                setState { copy(isLoading = true, loginType = LoginContract.LoginType.GOOGLE) }
                android.util.Log.d(TAG, "Google login started")
                setEffect { LoginContract.Effect.StartSocialLogin(LoginContract.LoginType.GOOGLE) }
            } catch (e: Exception) {
                handleError(e)
            }
        }
    }

    /**
     * Google 로그인 결과 처리.
     *
     * @param email Google 계정 이메일
     * @param displayName 사용자 이름
     * @param idToken Google ID 토큰
     */
    fun handleGoogleLoginResult(
        email: String,
        displayName: String?,
        idToken: String?
    ) {
        viewModelScope.launch {
            try {
                android.util.Log.d(TAG, "Google login success - email: $email")

                tempEmail = email
                tempPassword = idToken ?: "qazqazqaz" // old 코드: mPasswd = mAuthToken or "qazqazqaz"
                tempDomain = Constants.DOMAIN_GOOGLE

                // validate API 호출하여 회원 여부 확인
                validateAndSignIn(email, Constants.DOMAIN_GOOGLE)

            } catch (e: Exception) {
                handleError(e)
            }
        }
    }

    /**
     * Line 로그인.
     */
    private fun loginWithLine() {
        viewModelScope.launch {
            try {
                setState { copy(isLoading = true, loginType = LoginContract.LoginType.LINE) }
                android.util.Log.d(TAG, "Line login started")
                setEffect { LoginContract.Effect.StartSocialLogin(LoginContract.LoginType.LINE) }
            } catch (e: Exception) {
                handleError(e)
            }
        }
    }

    /**
     * Line 로그인 결과 처리.
     *
     * @param userId Line user ID
     * @param displayName 사용자 닉네임
     * @param accessToken Line access token
     */
    fun handleLineLoginResult(
        userId: String,
        displayName: String?,
        accessToken: String
    ) {
        viewModelScope.launch {
            try {
                // old 코드: mEmail = "$mid${Const.POSTFIX_LINE}"
                val email = "$userId${Constants.POSTFIX_LINE}"
                tempEmail = email
                tempPassword = accessToken // old 코드: mPasswd = mAuthToken
                tempDomain = Constants.DOMAIN_LINE

                android.util.Log.d(TAG, "Line login success - userId: $userId, email: $email")

                // validate API 호출하여 회원 여부 확인
                validateAndSignIn(email, Constants.DOMAIN_LINE)

            } catch (e: Exception) {
                handleError(e)
            }
        }
    }

    /**
     * Facebook 로그인.
     */
    private fun loginWithFacebook() {
        viewModelScope.launch {
            try {
                setState { copy(isLoading = true, loginType = LoginContract.LoginType.FACEBOOK) }
                android.util.Log.d(TAG, "Facebook login started")
                setEffect { LoginContract.Effect.StartSocialLogin(LoginContract.LoginType.FACEBOOK) }
            } catch (e: Exception) {
                handleError(e)
            }
        }
    }

    /**
     * Facebook 로그인 결과 처리.
     *
     * @param email Facebook 계정 이메일
     * @param name 사용자 이름
     * @param accessToken Facebook access token
     */
    fun handleFacebookLoginResult(
        email: String,
        name: String?,
        accessToken: String
    ) {
        viewModelScope.launch {
            try {
                android.util.Log.d(TAG, "Facebook login success - email: $email")

                tempEmail = email
                tempPassword = accessToken // old 코드: mPasswd = mAuthToken
                tempDomain = Constants.DOMAIN_FACEBOOK

                // validate API 호출하여 회원 여부 확인
                validateAndSignIn(email, Constants.DOMAIN_FACEBOOK)

            } catch (e: Exception) {
                handleError(e)
            }
        }
    }

    /**
     * Email 로그인 화면으로 이동.
     */
    private fun navigateToEmailLogin() {
        setEffect { LoginContract.Effect.NavigateToEmailLogin }
    }

    /**
     * 에러 처리.
     */
    private fun handleError(exception: Exception) {
        val errorMessage = exception.message ?: "Login failed"

        setState {
            copy(
                isLoading = false,
                error = errorMessage,
                loginType = null
            )
        }

        setEffect { LoginContract.Effect.ShowError(errorMessage) }
        android.util.Log.e(TAG, "Login error: $errorMessage", exception)
    }

    /**
     * SNS SDK 에러 처리 (LoginScreen에서 SDK 에러 발생 시 호출).
     * 로딩 상태를 해제하고 에러 메시지를 표시합니다.
     */
    fun handleSnsLoginError(errorMessage: String? = null) {
        setState {
            copy(
                isLoading = false,
                error = errorMessage,
                loginType = null
            )
        }
        android.util.Log.e(TAG, "SNS login error: $errorMessage")
    }

    /**
     * 디바이스 ID 가져오기 (UUID).
     * DataStore에 저장된 UUID를 가져오거나, 없으면 새로 생성하여 저장.
     */
    private suspend fun getDeviceId(): String {
        val savedDeviceId = preferencesManager.deviceId.first()
        return if (savedDeviceId != null) {
            savedDeviceId
        } else {
            val newDeviceId = UUID.randomUUID().toString()
            preferencesManager.setDeviceId(newDeviceId)
            newDeviceId
        }
    }

    /**
     * FCM Token 가져오기.
     * DataStore에 저장된 FCM Token을 반환, 없으면 빈 문자열.
     */
    private suspend fun getFcmToken(): String {
        return preferencesManager.fcmToken.first() ?: ""
    }

    /**
     * Gmail 계정 가져오기.
     * AccountManager에서 Gmail 계정을 가져오거나, 없으면 device UUID 사용.
     *
     * NOTE: AccountManager 사용을 위한 구현 가이드:
     * 1. AndroidManifest.xml에 권한 추가:
     *    <uses-permission android:name="android.permission.GET_ACCOUNTS" />
     * 2. AccountManager를 통해 계정 조회:
     *    val accountManager = AccountManager.get(context)
     *    val accounts = accountManager.getAccountsByType("com.google")
     *    val gmail = accounts.firstOrNull()?.name
     * 3. Android 6.0+ (API 23+)에서는 런타임 권한 요청 필요
     * 4. Android 8.0+ (API 26+)에서는 GET_ACCOUNTS 권한이 제한됨
     *    대신 GoogleSignIn API 사용을 권장
     */
    private suspend fun getGmail(): String {
        // 현재는 deviceId를 반환 (AccountManager 구현 필요 시 위 가이드 참조)
        return getDeviceId()
    }
}
