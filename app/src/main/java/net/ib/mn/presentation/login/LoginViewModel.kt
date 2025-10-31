package net.ib.mn.presentation.login

import android.content.Context
import androidx.lifecycle.viewModelScope
import com.kakao.sdk.user.UserApiClient
import net.ib.mn.base.BaseViewModel
import net.ib.mn.data.local.PreferencesManager
import net.ib.mn.data.remote.interceptor.AuthInterceptor
import net.ib.mn.domain.model.ApiResult
import net.ib.mn.domain.usecase.SignInUseCase
import net.ib.mn.domain.usecase.ValidateUserUseCase
import net.ib.mn.util.Constants
import net.ib.mn.R
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
        private const val KAKAO_LOGIN_TAG = "KAKAO_LOGIN"
        private const val GOOGLE_LOGIN_TAG = "GOOGLE_LOGIN"
    }

    // 소셜 로그인 임시 데이터 저장
    private var tempEmail: String? = null
    private var tempPassword: String? = null
    private var tempDomain: String? = null
    private var tempDisplayName: String? = null
    private var tempProfileImageUrl: String? = null

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
        android.util.Log.d(KAKAO_LOGIN_TAG, "========================================")
        android.util.Log.d(KAKAO_LOGIN_TAG, "loginWithKakao() called")
        android.util.Log.d(KAKAO_LOGIN_TAG, "========================================")
        
        viewModelScope.launch {
            try {
                setState { copy(isLoading = true, loginType = LoginContract.LoginType.KAKAO) }
                android.util.Log.d(TAG, "Kakao login started")
                android.util.Log.d(KAKAO_LOGIN_TAG, "State updated: isLoading=true, loginType=KAKAO")

                // Kakao SDK 호출은 LoginScreen에서 처리
                setEffect { LoginContract.Effect.StartSocialLogin(LoginContract.LoginType.KAKAO) }
                android.util.Log.d(KAKAO_LOGIN_TAG, "StartSocialLogin effect sent")

            } catch (e: Exception) {
                android.util.Log.e(KAKAO_LOGIN_TAG, "========================================")
                android.util.Log.e(KAKAO_LOGIN_TAG, "loginWithKakao() EXCEPTION")
                android.util.Log.e(KAKAO_LOGIN_TAG, "  error: ${e.message}")
                android.util.Log.e(KAKAO_LOGIN_TAG, "  stackTrace:", e)
                android.util.Log.e(KAKAO_LOGIN_TAG, "========================================")
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
        android.util.Log.d(KAKAO_LOGIN_TAG, "========================================")
        android.util.Log.d(KAKAO_LOGIN_TAG, "handleKakaoLoginResult() called")
        android.util.Log.d(KAKAO_LOGIN_TAG, "  userId: $userId")
        android.util.Log.d(KAKAO_LOGIN_TAG, "  nickname: $nickname")
        android.util.Log.d(KAKAO_LOGIN_TAG, "  profileImageUrl: $profileImageUrl")
        android.util.Log.d(KAKAO_LOGIN_TAG, "  accessToken: ${accessToken.take(20)}...")
        android.util.Log.d(KAKAO_LOGIN_TAG, "========================================")
        
        viewModelScope.launch {
            try {
                // old 코드: mEmail = "$id${Const.POSTFIX_KAKAO}"
                val email = "$userId${Constants.POSTFIX_KAKAO}"
                tempEmail = email
                tempPassword = accessToken // old 코드: mPasswd = mAuthToken
                tempDomain = Constants.DOMAIN_KAKAO
                tempDisplayName = nickname
                tempProfileImageUrl = profileImageUrl

                android.util.Log.d(KAKAO_LOGIN_TAG, "========================================")
                android.util.Log.d(KAKAO_LOGIN_TAG, "Temp data set")
                android.util.Log.d(KAKAO_LOGIN_TAG, "  tempEmail: $email")
                android.util.Log.d(KAKAO_LOGIN_TAG, "  tempPassword: ${accessToken.take(20)}...")
                android.util.Log.d(KAKAO_LOGIN_TAG, "  tempDomain: ${Constants.DOMAIN_KAKAO}")
                android.util.Log.d(KAKAO_LOGIN_TAG, "  tempDisplayName: $nickname")
                android.util.Log.d(KAKAO_LOGIN_TAG, "  tempProfileImageUrl: $profileImageUrl")
                android.util.Log.d(KAKAO_LOGIN_TAG, "========================================")

                android.util.Log.d(TAG, "Kakao login success - userId: $userId, email: $email, nickname: $nickname")
                android.util.Log.d(KAKAO_LOGIN_TAG, "Calling validateAndSignIn()")
                
                // validate API 호출하여 회원 여부 확인
                validateAndSignIn(email, Constants.DOMAIN_KAKAO)

            } catch (e: Exception) {
                android.util.Log.e(KAKAO_LOGIN_TAG, "========================================")
                android.util.Log.e(KAKAO_LOGIN_TAG, "handleKakaoLoginResult() EXCEPTION")
                android.util.Log.e(KAKAO_LOGIN_TAG, "  error: ${e.message}")
                android.util.Log.e(KAKAO_LOGIN_TAG, "  stackTrace:", e)
                android.util.Log.e(KAKAO_LOGIN_TAG, "========================================")
                handleError(e)
            }
        }
    }

    /**
     * 회원 여부 확인 및 로그인.
     */
    private suspend fun validateAndSignIn(email: String, domain: String) {
        // domain에 따라 적절한 태그 선택
        val loginTag = when (domain) {
            Constants.DOMAIN_KAKAO -> KAKAO_LOGIN_TAG
            Constants.DOMAIN_GOOGLE -> GOOGLE_LOGIN_TAG
            else -> TAG
        }
        
        android.util.Log.d(loginTag, "========================================")
        android.util.Log.d(loginTag, "validateAndSignIn() called")
        android.util.Log.d(loginTag, "  email: $email")
        android.util.Log.d(loginTag, "  domain: $domain")
        android.util.Log.d(loginTag, "  appId: ${Constants.APP_ID}")
        android.util.Log.d(loginTag, "========================================")
        
        validateUserUseCase(
            type = "email",
            value = email,
            appId = Constants.APP_ID
        ).collect { result ->
            android.util.Log.d(loginTag, "========================================")
            android.util.Log.d(loginTag, "validateUserUseCase result received")
            android.util.Log.d(loginTag, "  result type: ${result.javaClass.simpleName}")
            android.util.Log.d(loginTag, "========================================")
            
            when (result) {
                is ApiResult.Loading -> {
                    android.util.Log.d(loginTag, "Validate API: Loading...")
                }
                is ApiResult.Success -> {
                    val response = result.data
                    android.util.Log.d(loginTag, "========================================")
                    android.util.Log.d(loginTag, "Validate API: Success")
                    android.util.Log.d(loginTag, "  response.success: ${response.success}")
                    android.util.Log.d(loginTag, "  response.domain: ${response.domain}")
                    android.util.Log.d(loginTag, "  response.message: ${response.message}")
                    android.util.Log.d(loginTag, "========================================")

                    if (response.success) {
                        // Old 프로젝트: success == true일 때도 약관 동의 화면으로 이동
                        // 이유: 약관 동의를 다시 받기 위함 (161011 약관동의 처리)
                        // → AgreementFragment → KakaoMoreFragment → 회원가입 API → trySignin()
                        // 
                        // 하지만 사용자 요구사항: 이미 있는 회원은 바로 로그인해야 함
                        // 따라서 기존 회원은 바로 로그인 API 호출
                        android.util.Log.d(TAG, "User exists - calling signIn API directly")
                        android.util.Log.d(loginTag, "========================================")
                        android.util.Log.d(loginTag, "User EXISTS - Calling performSignIn()")
                        android.util.Log.d(loginTag, "========================================")
                        
                        // 기존 회원: 바로 로그인 API 호출
                        performSignIn()
                        
                    } else {
                        // 회원이 존재하지 않음 -> domain 확인 (old 프로젝트 로직)
                        val registeredDomain = response.domain
                        android.util.Log.d(TAG, "User not found - checking if registered with different method")
                        android.util.Log.d(TAG, "Registered domain: $registeredDomain, trying domain: $domain")
                        android.util.Log.d(loginTag, "========================================")
                        android.util.Log.d(loginTag, "User NOT FOUND")
                        android.util.Log.d(loginTag, "  registeredDomain: $registeredDomain")
                        android.util.Log.d(loginTag, "  trying domain: $domain")
                        android.util.Log.d(loginTag, "========================================")

                        // Old 프로젝트: registeredDomain이 null이 아니고 domain과 다르면 다이얼로그 표시
                        // 하지만 사용자 요구사항: 계정이 없으면 회원가입으로 이동해야 함
                        // 따라서 신규 회원으로 간주하고 회원가입 플로우로 이동
                        android.util.Log.d(TAG, "New user - sign up required")
                        android.util.Log.d(loginTag, "========================================")
                        android.util.Log.d(loginTag, "NEW USER - Checking temp data")
                        android.util.Log.d(loginTag, "  tempEmail: $tempEmail")
                        android.util.Log.d(loginTag, "  tempPassword: ${tempPassword?.take(20)}...")
                        android.util.Log.d(loginTag, "  tempDomain: $tempDomain")
                        android.util.Log.d(loginTag, "  tempDisplayName: $tempDisplayName")
                        android.util.Log.d(loginTag, "  tempProfileImageUrl: $tempProfileImageUrl")
                        android.util.Log.d(loginTag, "========================================")
                        
                        val email = tempEmail ?: run {
                            android.util.Log.e(loginTag, "========================================")
                            android.util.Log.e(loginTag, "ERROR: tempEmail is null")
                            android.util.Log.e(loginTag, "========================================")
                            setState { copy(isLoading = false) }
                            setEffect {
                                LoginContract.Effect.ShowError(context.getString(R.string.error_abnormal_exception))
                            }
                            return@collect
                        }
                        val password = tempPassword ?: run {
                            android.util.Log.e(loginTag, "========================================")
                            android.util.Log.e(loginTag, "ERROR: tempPassword is null")
                            android.util.Log.e(loginTag, "========================================")
                            setState { copy(isLoading = false) }
                            setEffect {
                                LoginContract.Effect.ShowError(context.getString(R.string.error_abnormal_exception))
                            }
                            return@collect
                        }
                        val currentDomain = tempDomain ?: run {
                            android.util.Log.e(loginTag, "========================================")
                            android.util.Log.e(loginTag, "ERROR: tempDomain is null")
                            android.util.Log.e(loginTag, "========================================")
                            setState { copy(isLoading = false) }
                            setEffect {
                                LoginContract.Effect.ShowError(context.getString(R.string.error_abnormal_exception))
                            }
                            return@collect
                        }
                        
                        // 다른 소셜 로그인으로 가입된 경우 에러 표시 (Old 프로젝트 로직)
                        if (registeredDomain != null && !registeredDomain.equals(currentDomain, ignoreCase = true)) {
                            android.util.Log.w(TAG, "User registered with different method: $registeredDomain")
                            android.util.Log.w(loginTag, "User registered with DIFFERENT method: $registeredDomain")
                            setState { copy(isLoading = false) }
                            setEffect {
                                LoginContract.Effect.ShowError(
                                    context.getString(R.string.error_1031)
                                )
                            }
                            return@collect
                        }
                        
                        // SNS 로그인(카카오, 구글 등) 시 nickname(displayName)을 전달하지 않음
                        // 사용자가 직접 닉네임을 입력해야 함
                        val displayName = if (currentDomain == Constants.DOMAIN_EMAIL) tempDisplayName else null
                        val profileImageUrl = tempProfileImageUrl
                        
                        android.util.Log.d(loginTag, "========================================")
                        android.util.Log.d(loginTag, "NEW USER - NavigateToSignUp")
                        android.util.Log.d(loginTag, "  email: $email")
                        android.util.Log.d(loginTag, "  password: ${password.take(20)}...")
                        android.util.Log.d(loginTag, "  domain: $currentDomain")
                        android.util.Log.d(loginTag, "  displayName: $displayName (SNS 로그인은 null로 전달)")
                        android.util.Log.d(loginTag, "  profileImageUrl: $profileImageUrl")
                        android.util.Log.d(loginTag, "========================================")
                        
                        setState { copy(isLoading = false) }
                        setEffect {
                            LoginContract.Effect.NavigateToSignUp(
                                email = email,
                                password = password,
                                displayName = displayName,
                                domain = currentDomain,
                                profileImageUrl = profileImageUrl
                            )
                        }
                    }
                }
                is ApiResult.Error -> {
                    android.util.Log.e(loginTag, "========================================")
                    android.util.Log.e(loginTag, "Validate API: ERROR")
                    android.util.Log.e(loginTag, "  error message: ${result.message}")
                    android.util.Log.e(loginTag, "  error exception: ${result.exception?.message}")
                    android.util.Log.e(loginTag, "========================================")
                    setState { copy(isLoading = false) }
                    setEffect {
                        LoginContract.Effect.ShowError(result.message ?: context.getString(R.string.error_1031))
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

        // domain에 따라 적절한 태그 선택
        val loginTag = when (domain) {
            Constants.DOMAIN_KAKAO -> KAKAO_LOGIN_TAG
            Constants.DOMAIN_GOOGLE -> GOOGLE_LOGIN_TAG
            else -> TAG
        }

        android.util.Log.d(loginTag, "========================================")
        android.util.Log.d(loginTag, "performSignIn() called")
        android.util.Log.d(loginTag, "  email: $email")
        android.util.Log.d(loginTag, "  domain: $domain")
        android.util.Log.d(loginTag, "  password: ${password.take(20)}...")
        android.util.Log.d(loginTag, "========================================")

        // Device info
        val deviceId = getDeviceId()
        val gmail = getGmail()
        val deviceKey = getFcmToken() // FCM token from DataStore

        // SignIn API 파라미터 로그 출력
        android.util.Log.d(loginTag, "========================================")
        android.util.Log.d(loginTag, "SignIn API Parameters:")
        android.util.Log.d(loginTag, "  domain: $domain")
        android.util.Log.d(loginTag, "  email: $email")
        android.util.Log.d(loginTag, "  passwd: ${password.take(20)}...")
        android.util.Log.d(loginTag, "  push_key: $deviceKey")
        android.util.Log.d(loginTag, "  gmail: $gmail")
        android.util.Log.d(loginTag, "  device_id: $deviceId")
        android.util.Log.d(loginTag, "  app_id: ${Constants.APP_ID}")
        android.util.Log.d(loginTag, "========================================")

        signInUseCase(
            domain = domain,
            email = email,
            password = password,
            deviceKey = deviceKey,
            gmail = gmail,
            deviceId = deviceId,
            appId = Constants.APP_ID
        ).collect { result ->
            android.util.Log.d(loginTag, "========================================")
            android.util.Log.d(loginTag, "signInUseCase result received")
            android.util.Log.d(loginTag, "  result type: ${result.javaClass.simpleName}")
            android.util.Log.d(loginTag, "========================================")
            
            when (result) {
                is ApiResult.Loading -> {
                    android.util.Log.d(loginTag, "SignIn API: Loading...")
                }
                is ApiResult.Success -> {
                    val response = result.data
                    android.util.Log.d(loginTag, "========================================")
                    android.util.Log.d(loginTag, "SignIn API: Success")
                    android.util.Log.d(loginTag, "  response.success: ${response.success}")
                    android.util.Log.d(loginTag, "  response.data: ${response.data != null}")
                    android.util.Log.d(loginTag, "  response.message: ${response.message}")
                    android.util.Log.d(loginTag, "========================================")

                    // Old 프로젝트: response.optBoolean("success")만 체크
                    // response.data가 null이어도 성공으로 처리 (사용자 정보는 이후에 별도로 가져옴)
                    if (response.success) {
                        android.util.Log.d(loginTag, "========================================")
                        android.util.Log.d(loginTag, "Login SUCCESS")
                        android.util.Log.d(loginTag, "  Note: response.data may be null, user info will be fetched separately")
                        android.util.Log.d(loginTag, "========================================")

                        // Old 프로젝트: 카카오 로그인 성공 후 unlink 호출
                        if (domain == Constants.DOMAIN_KAKAO) {
                            android.util.Log.d(loginTag, "Calling requestKakaoUnlink()")
                            requestKakaoUnlink()
                        }

                        // response.data가 있는 경우에만 저장 (Old 프로젝트는 afterSignin에서 별도로 처리)
                        if (response.data != null) {
                            val userData = response.data
                            android.util.Log.d(loginTag, "========================================")
                            android.util.Log.d(loginTag, "User data available in response")
                            android.util.Log.d(loginTag, "  userId: ${userData.userId}")
                            android.util.Log.d(loginTag, "  email: ${userData.email}")
                            android.util.Log.d(loginTag, "  username: ${userData.username}")
                            android.util.Log.d(loginTag, "  token: ${userData.token.take(20)}...")
                            android.util.Log.d(loginTag, "========================================")

                            // 1. 인증 정보 저장 (AuthInterceptor에 설정)
                            authInterceptor.setAuthCredentials(
                                email = userData.email,
                                domain = domain,
                                token = userData.token
                            )
                            android.util.Log.d(loginTag, "Auth credentials saved")

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
                            android.util.Log.d(loginTag, "User info saved")
                        } else {
                            // response.data가 null인 경우 (Old 프로젝트와 동일)
                            // 사용자 정보는 이후에 별도로 가져옴 (StartUpScreen 또는 getUserSelf에서)
                            android.util.Log.d(loginTag, "========================================")
                            android.util.Log.d(loginTag, "User data is null - will be fetched separately")
                            android.util.Log.d(loginTag, "  email: $email")
                            android.util.Log.d(loginTag, "  domain: $domain")
                            android.util.Log.d(loginTag, "========================================")

                            // Old 프로젝트의 afterSignin 로직:
                            // - hashToken 계산 (domain에 따라)
                            // - IdolAccount.createAccount 호출 (이 함수가 사용자 정보를 가져옴)
                            // - 현재는 NavigateToMain으로 이동하고, StartUpScreen에서 사용자 정보를 가져옴
                            
                            // 기본 인증 정보만 저장 (이메일과 도메인)
                            // 토큰은 tempPassword에 저장된 access token 사용
                            val hashToken = if (domain == Constants.DOMAIN_EMAIL) {
                                // Email 로그인의 경우 md5salt (현재는 구현하지 않음)
                                password
                            } else {
                                // SNS 로그인의 경우 access token 그대로 사용
                                password
                            }
                            
                            // 1. AuthInterceptor에 기본 정보 저장 (토큰은 나중에 업데이트될 수 있음)
                            authInterceptor.setAuthCredentials(
                                email = email,
                                domain = domain,
                                token = hashToken
                            )
                            
                            // 2. DataStore에 로그인 정보 저장 (StartUpScreen에서 확인용)
                            preferencesManager.setAccessToken(hashToken)
                            preferencesManager.setLoginDomain(domain)
                            
                            // 3. 최소한의 사용자 정보 저장 (id는 임시로 0 사용, StartUpScreen에서 업데이트됨)
                            // Old 프로젝트: createAccount에서 사용자 정보를 가져와서 저장
                            // 현재 프로젝트: StartUpScreen의 loadUserSelf에서 사용자 정보를 가져와서 저장
                            preferencesManager.setUserInfo(
                                id = 0, // 임시 ID, StartUpScreen에서 실제 ID로 업데이트됨
                                email = email,
                                username = "", // StartUpScreen에서 업데이트됨
                                nickname = null,
                                profileImage = null,
                                hearts = null,
                                domain = domain
                            )
                            
                            android.util.Log.d(loginTag, "Basic auth credentials saved (token will be updated later)")
                            android.util.Log.d(loginTag, "  - Email: $email")
                            android.util.Log.d(loginTag, "  - Domain: $domain")
                            android.util.Log.d(loginTag, "  - Token: ${hashToken.take(20)}...")
                            android.util.Log.d(loginTag, "  - Note: User info will be fetched in StartUpScreen")
                        }

                        android.util.Log.d(TAG, "✓ Login successful")
                        android.util.Log.d(loginTag, "========================================")
                        android.util.Log.d(loginTag, "NavigateToMain effect sent")
                        android.util.Log.d(loginTag, "========================================")

                        setState { copy(isLoading = false, loginType = null) }
                        setEffect { LoginContract.Effect.NavigateToMain }
                    } else {
                        // Old 프로젝트: response.optBoolean("success")가 false인 경우
                        // "이메일이 잘못되었습니다" 메시지가 포함된 경우 신규 회원으로 간주하고 회원가입 플로우로 이동
                        val errorMessage = response.message ?: ""
                        val isEmailError = errorMessage.contains("이메일이 잘못되었습니다", ignoreCase = true) ||
                                errorMessage.contains("email", ignoreCase = true) ||
                                errorMessage.contains("잘못", ignoreCase = true)
                        
                        android.util.Log.e(loginTag, "========================================")
                        android.util.Log.e(loginTag, "SignIn API: FAILED")
                        android.util.Log.e(loginTag, "  response.success: ${response.success}")
                        android.util.Log.e(loginTag, "  response.message: $errorMessage")
                        android.util.Log.e(loginTag, "  isEmailError: $isEmailError")
                        android.util.Log.e(loginTag, "========================================")
                        
                        if (isEmailError) {
                            // 이메일 오류인 경우 신규 회원으로 간주하고 회원가입 플로우로 이동
                            android.util.Log.d(loginTag, "========================================")
                            android.util.Log.d(loginTag, "Email error detected - NavigateToSignUp")
                            android.util.Log.d(loginTag, "  Checking temp data...")
                            android.util.Log.d(loginTag, "  tempEmail: $tempEmail")
                            android.util.Log.d(loginTag, "  tempPassword: ${tempPassword?.take(20)}...")
                            android.util.Log.d(loginTag, "  tempDomain: $tempDomain")
                            android.util.Log.d(loginTag, "  tempDisplayName: $tempDisplayName")
                            android.util.Log.d(loginTag, "  tempProfileImageUrl: $tempProfileImageUrl")
                            android.util.Log.d(loginTag, "========================================")
                            
                            val email = tempEmail ?: run {
                                android.util.Log.e(loginTag, "========================================")
                                android.util.Log.e(loginTag, "ERROR: tempEmail is null")
                                android.util.Log.e(loginTag, "========================================")
                                setState { copy(isLoading = false) }
                                setEffect {
                                    LoginContract.Effect.ShowError(context.getString(R.string.error_abnormal_exception))
                                }
                                return@collect
                            }
                            val password = tempPassword ?: run {
                                android.util.Log.e(loginTag, "========================================")
                                android.util.Log.e(loginTag, "ERROR: tempPassword is null")
                                android.util.Log.e(loginTag, "========================================")
                                setState { copy(isLoading = false) }
                                setEffect {
                                    LoginContract.Effect.ShowError(context.getString(R.string.error_abnormal_exception))
                                }
                                return@collect
                            }
                            val currentDomain = tempDomain ?: run {
                                android.util.Log.e(loginTag, "========================================")
                                android.util.Log.e(loginTag, "ERROR: tempDomain is null")
                                android.util.Log.e(loginTag, "========================================")
                                setState { copy(isLoading = false) }
                                setEffect {
                                    LoginContract.Effect.ShowError(context.getString(R.string.error_abnormal_exception))
                                }
                                return@collect
                            }
                            
                            // SNS 로그인(카카오, 구글 등) 시 nickname(displayName)을 전달하지 않음
                            val displayName = if (currentDomain == Constants.DOMAIN_EMAIL) tempDisplayName else null
                            val profileImageUrl = tempProfileImageUrl
                            
                            android.util.Log.d(loginTag, "========================================")
                            android.util.Log.d(loginTag, "NEW USER - NavigateToSignUp (from SignIn API email error)")
                            android.util.Log.d(loginTag, "  email: $email")
                            android.util.Log.d(loginTag, "  password: ${password.take(20)}...")
                            android.util.Log.d(loginTag, "  domain: $currentDomain")
                            android.util.Log.d(loginTag, "  displayName: $displayName")
                            android.util.Log.d(loginTag, "  profileImageUrl: $profileImageUrl")
                            android.util.Log.d(loginTag, "========================================")
                            
                            setState { copy(isLoading = false) }
                            setEffect {
                                LoginContract.Effect.NavigateToSignUp(
                                    email = email,
                                    password = password,
                                    displayName = displayName,
                                    domain = currentDomain,
                                    profileImageUrl = profileImageUrl
                                )
                            }
                        } else {
                            // 그 외의 경우는 에러만 표시
                            android.util.Log.e(loginTag, "Non-email error - showing error dialog")
                            setState { copy(isLoading = false) }
                            setEffect {
                                LoginContract.Effect.ShowError(
                                    errorMessage.ifEmpty { context.getString(R.string.error_1031) }
                                )
                            }
                        }
                    }
                }
                is ApiResult.Error -> {
                    android.util.Log.e(loginTag, "========================================")
                    android.util.Log.e(loginTag, "SignIn API: ERROR")
                    android.util.Log.e(loginTag, "  error message: ${result.message}")
                    android.util.Log.e(loginTag, "  error exception: ${result.exception?.message}")
                    android.util.Log.e(loginTag, "========================================")
                    
                    setState { copy(isLoading = false) }
                        setEffect {
                            LoginContract.Effect.ShowError(result.message ?: context.getString(R.string.error_1031))
                        }
                }
            }
        }
    }

    /**
     * Google 로그인.
     */
    private fun loginWithGoogle() {
        android.util.Log.d(GOOGLE_LOGIN_TAG, "========================================")
        android.util.Log.d(GOOGLE_LOGIN_TAG, "loginWithGoogle() called")
        android.util.Log.d(GOOGLE_LOGIN_TAG, "========================================")
        
        viewModelScope.launch {
            try {
                setState { copy(isLoading = true, loginType = LoginContract.LoginType.GOOGLE) }
                android.util.Log.d(GOOGLE_LOGIN_TAG, "Google login started")
                android.util.Log.d(GOOGLE_LOGIN_TAG, "State updated: isLoading=true, loginType=GOOGLE")

                // Google SDK 호출은 LoginScreen에서 처리
                setEffect { LoginContract.Effect.StartSocialLogin(LoginContract.LoginType.GOOGLE) }
                android.util.Log.d(GOOGLE_LOGIN_TAG, "StartSocialLogin effect sent")

            } catch (e: Exception) {
                android.util.Log.e(GOOGLE_LOGIN_TAG, "========================================")
                android.util.Log.e(GOOGLE_LOGIN_TAG, "loginWithGoogle() EXCEPTION")
                android.util.Log.e(GOOGLE_LOGIN_TAG, "  error: ${e.message}")
                android.util.Log.e(GOOGLE_LOGIN_TAG, "  stackTrace:", e)
                android.util.Log.e(GOOGLE_LOGIN_TAG, "========================================")
                handleError(e)
            }
        }
    }

    /**
     * Google 로그인 결과 처리.
     *
     * @param email Google 계정 이메일
     * @param displayName 사용자 이름
     * @param accessToken Google OAuth access token (Old 프로젝트의 mAuthToken과 동일)
     */
    fun handleGoogleLoginResult(
        email: String,
        displayName: String?,
        accessToken: String
    ) {
        android.util.Log.d(GOOGLE_LOGIN_TAG, "========================================")
        android.util.Log.d(GOOGLE_LOGIN_TAG, "handleGoogleLoginResult() called")
        android.util.Log.d(GOOGLE_LOGIN_TAG, "  email: $email")
        android.util.Log.d(GOOGLE_LOGIN_TAG, "  displayName: $displayName")
        android.util.Log.d(GOOGLE_LOGIN_TAG, "  accessToken: ${accessToken.take(20)}...")
        android.util.Log.d(GOOGLE_LOGIN_TAG, "========================================")
        
        viewModelScope.launch {
            try {
                // Old 프로젝트와 동일: email이 필수, 없으면 에러 처리
                if (email.isEmpty()) {
                    android.util.Log.e(GOOGLE_LOGIN_TAG, "========================================")
                    android.util.Log.e(GOOGLE_LOGIN_TAG, "Google login error: email is empty")
                    android.util.Log.e(GOOGLE_LOGIN_TAG, "========================================")
                    handleSnsLoginError(context.getString(R.string.facebook_no_email))
                    return@launch
                }

                // Old 프로젝트: access token이 필수 (mPasswd = mAuthToken)
                if (accessToken.isEmpty()) {
                    android.util.Log.e(GOOGLE_LOGIN_TAG, "========================================")
                    android.util.Log.e(GOOGLE_LOGIN_TAG, "Google login error: accessToken is empty")
                    android.util.Log.e(GOOGLE_LOGIN_TAG, "========================================")
                    handleSnsLoginError(context.getString(R.string.error_abnormal_exception))
                    return@launch
                }

                tempEmail = email
                tempPassword = accessToken // Old 프로젝트: mPasswd = mAuthToken (access token)
                tempDomain = Constants.DOMAIN_GOOGLE
                tempDisplayName = displayName
                tempProfileImageUrl = null // Google은 프로필 이미지 URL을 별도로 받지 않음

                android.util.Log.d(GOOGLE_LOGIN_TAG, "========================================")
                android.util.Log.d(GOOGLE_LOGIN_TAG, "Temp data set")
                android.util.Log.d(GOOGLE_LOGIN_TAG, "  tempEmail: $email")
                android.util.Log.d(GOOGLE_LOGIN_TAG, "  tempPassword: ${accessToken.take(20)}...")
                android.util.Log.d(GOOGLE_LOGIN_TAG, "  tempDomain: ${Constants.DOMAIN_GOOGLE}")
                android.util.Log.d(GOOGLE_LOGIN_TAG, "  tempDisplayName: $displayName")
                android.util.Log.d(GOOGLE_LOGIN_TAG, "========================================")
                android.util.Log.d(GOOGLE_LOGIN_TAG, "Calling validateAndSignIn()")

                // validate API 호출하여 회원 여부 확인
                validateAndSignIn(email, Constants.DOMAIN_GOOGLE)

            } catch (e: Exception) {
                android.util.Log.e(GOOGLE_LOGIN_TAG, "========================================")
                android.util.Log.e(GOOGLE_LOGIN_TAG, "handleGoogleLoginResult() EXCEPTION")
                android.util.Log.e(GOOGLE_LOGIN_TAG, "  error: ${e.message}")
                android.util.Log.e(GOOGLE_LOGIN_TAG, "  stackTrace:", e)
                android.util.Log.e(GOOGLE_LOGIN_TAG, "========================================")
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
                tempDisplayName = displayName
                tempProfileImageUrl = null // Line은 프로필 이미지 URL을 별도로 받지 않음

                android.util.Log.d(TAG, "Line login success - userId: $userId, email: $email, displayName: $displayName")

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
                android.util.Log.d(TAG, "Facebook login success - email: $email, name: $name")

                tempEmail = email
                tempPassword = accessToken // old 코드: mPasswd = mAuthToken
                tempDomain = Constants.DOMAIN_FACEBOOK
                tempDisplayName = name
                tempProfileImageUrl = null // Facebook은 프로필 이미지 URL을 별도로 받지 않음

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
        android.util.Log.e(KAKAO_LOGIN_TAG, "========================================")
        android.util.Log.e(KAKAO_LOGIN_TAG, "handleSnsLoginError() called")
        android.util.Log.e(KAKAO_LOGIN_TAG, "  errorMessage: $errorMessage")
        android.util.Log.e(KAKAO_LOGIN_TAG, "========================================")
        
        setState {
            copy(
                isLoading = false,
                error = errorMessage,
                loginType = null
            )
        }
        android.util.Log.e(TAG, "SNS login error: $errorMessage")
        android.util.Log.d(KAKAO_LOGIN_TAG, "State updated: isLoading=false, loginType=null")

        setEffect { LoginContract.Effect.ShowError(errorMessage ?: context.getString(R.string.error_1031)) }
        android.util.Log.d(KAKAO_LOGIN_TAG, "ShowError effect sent")
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
     * FCM Token을 DataStore에 저장 (Old 프로젝트의 GcmUtils.registerDevice와 동일).
     * Old 프로젝트: OnRegistered 콜백에서 Util.setPreference(this@AuthActivity, Const.PREF_GCM_PUSH_KEY, id) 호출
     */
    fun registerFcmToken(token: String) {
        android.util.Log.d(TAG, "========================================")
        android.util.Log.d(TAG, "registerFcmToken() called")
        android.util.Log.d(TAG, "  token: ${token.take(20)}...")
        android.util.Log.d(TAG, "========================================")
        
        viewModelScope.launch {
            try {
                preferencesManager.setFcmToken(token)
                android.util.Log.d(TAG, "FCM token saved to DataStore")
            } catch (e: Exception) {
                android.util.Log.e(TAG, "Failed to save FCM token to DataStore", e)
            }
        }
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

    /**
     * 카카오 unlink 호출 (Old 프로젝트의 requestKakaoUnlink와 동일).
     * 카카오 로그인 성공 후 연결 해제를 호출합니다.
     * 
     * Old 프로젝트 로직:
     * - 로그인 성공 후 unlink 호출
     * - 에러가 발생해도 무시하고 계속 진행
     */
    private fun requestKakaoUnlink() {
        android.util.Log.d(KAKAO_LOGIN_TAG, "========================================")
        android.util.Log.d(KAKAO_LOGIN_TAG, "requestKakaoUnlink() called")
        android.util.Log.d(KAKAO_LOGIN_TAG, "========================================")
        
        viewModelScope.launch {
            try {
                android.util.Log.d(TAG, "Kakao unlink called")
                UserApiClient.instance.unlink { throwable ->
                    if (throwable != null) {
                        android.util.Log.e(TAG, "Kakao unlink error: ${throwable.message}", throwable)
                        android.util.Log.e(KAKAO_LOGIN_TAG, "========================================")
                        android.util.Log.e(KAKAO_LOGIN_TAG, "Kakao unlink ERROR")
                        android.util.Log.e(KAKAO_LOGIN_TAG, "  error: ${throwable.message}")
                        android.util.Log.e(KAKAO_LOGIN_TAG, "========================================")
                    } else {
                        android.util.Log.d(TAG, "Kakao unlink success")
                        android.util.Log.d(KAKAO_LOGIN_TAG, "========================================")
                        android.util.Log.d(KAKAO_LOGIN_TAG, "Kakao unlink SUCCESS")
                        android.util.Log.d(KAKAO_LOGIN_TAG, "========================================")
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e(TAG, "Kakao unlink exception: ${e.message}", e)
                android.util.Log.e(KAKAO_LOGIN_TAG, "========================================")
                android.util.Log.e(KAKAO_LOGIN_TAG, "Kakao unlink EXCEPTION")
                android.util.Log.e(KAKAO_LOGIN_TAG, "  error: ${e.message}")
                android.util.Log.e(KAKAO_LOGIN_TAG, "========================================")
            }
        }
    }
}
