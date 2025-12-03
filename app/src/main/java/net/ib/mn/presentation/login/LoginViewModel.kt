package net.ib.mn.presentation.login

import net.ib.mn.util.logW
import android.content.Context
import androidx.lifecycle.viewModelScope
import com.kakao.sdk.user.UserApiClient
import net.ib.mn.base.BaseViewModel
import net.ib.mn.data.local.PreferencesManager
import net.ib.mn.data.repository.AuthRepository
import net.ib.mn.domain.model.ApiResult
import net.ib.mn.domain.usecase.SignInUseCase
import net.ib.mn.domain.usecase.ValidateUserUseCase
import net.ib.mn.util.Constants
import net.ib.mn.util.DeviceUtil
import net.ib.mn.R
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
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
    private val authRepository: AuthRepository,
    private val deviceUtil: DeviceUtil
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
    private var tempFacebookId: Long? = null  // 페이스북 사용자 ID (회원가입 시 필요)

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
                tempDisplayName = nickname
                tempProfileImageUrl = profileImageUrl


                
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
        // domain에 따라 적절한 태그 선택
        val loginTag = when (domain) {
            Constants.DOMAIN_KAKAO -> KAKAO_LOGIN_TAG
            Constants.DOMAIN_GOOGLE -> GOOGLE_LOGIN_TAG
            else -> TAG
        }
        
        
        validateUserUseCase(
            type = "email",
            value = email,
            appId = Constants.APP_ID
        ).collect { result ->
            
            when (result) {
                is ApiResult.Loading -> {
                }
                is ApiResult.Success -> {
                    val response = result.data
                    val registeredDomain = response.domain

                    // **핵심 수정**: registeredDomain이 있고 현재 domain과 일치하면 기존 회원으로 처리
                    // success 값에 관계없이 도메인이 일치하면 이미 가입된 회원입니다.
                    // signIn API를 호출하여 서버 토큰을 받아옵니다.
                    if (registeredDomain != null && registeredDomain.equals(domain, ignoreCase = true)) {

                        // Old 프로젝트와 동일: 기존 회원인 경우에도 signIn API를 호출하여 서버 토큰을 받아옴
                        // validate API는 회원 여부만 확인하고, 실제 로그인(서버 토큰 발급)은 signIn API에서 처리
                        performSignIn(isExistingUser = true)
                        return@collect
                    }

                    // registeredDomain이 null이거나 현재 domain과 다른 경우 기존 로직 처리
                    if (response.success) {
                        // success == true이지만 domain이 null이면 미가입자로 처리
                        if (registeredDomain == null) {
                            
                            // 신규 회원: 회원가입 화면으로 이동
                            val password = tempPassword ?: run {
                                setState { copy(isLoading = false) }
                                setEffect {
                                    LoginContract.Effect.ShowError(context.getString(R.string.error_abnormal_exception))
                                }
                                return@collect
                            }
                            val currentDomain = tempDomain ?: run {
                                setState { copy(isLoading = false) }
                                setEffect {
                                    LoginContract.Effect.ShowError(context.getString(R.string.error_abnormal_exception))
                                }
                                return@collect
                            }
                            
                            // SNS 로그인(카카오, 구글 등) 시 nickname(displayName)을 전달하지 않음
                            val displayName = if (currentDomain == Constants.DOMAIN_EMAIL) tempDisplayName else null
                            val profileImageUrl = tempProfileImageUrl
                            
                            
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
                            return@collect
                        }

                        // success==true이고 registeredDomain이 null이 아닌 경우
                        // 이미 line 210에서 domain이 일치하는 경우를 처리했으므로
                        // 여기 도달했다면 다른 도메인으로 가입된 경우입니다.
                        logW(loginTag, "========================================")
                        logW(loginTag, "User registered with different domain")
                        logW(loginTag, "  email: $email")
                        logW(loginTag, "  current domain: $domain")
                        logW(loginTag, "  registeredDomain: $registeredDomain")
                        logW(loginTag, "========================================")
                        setState { copy(isLoading = false) }
                        setEffect {
                            LoginContract.Effect.ShowError(
                                context.getString(R.string.error_1031)
                            )
                        }

                    } else {
                        // 새로운 로직: validate API에서 success == false면 신규 회원 또는 email이 없음
                        // email이 없거나 이메일이 잘못되었다고 나올 경우 회원가입화면으로 이동
                        val errorMessage = response.message ?: ""
                        val isEmailError = errorMessage.contains("이메일이 잘못되었습니다", ignoreCase = true) ||
                                errorMessage.contains("email", ignoreCase = true) ||
                                errorMessage.contains("잘못", ignoreCase = true)
                        val isDuplicateEmail = errorMessage.contains("중복되는 이메일", ignoreCase = true) ||
                                errorMessage.contains("중복", ignoreCase = true)
                        

                        // 다른 소셜 로그인으로 가입된 경우 에러 표시
                        // 이미 line 210에서 domain이 일치하는 경우를 처리했으므로,
                        // 여기서는 다른 도메인으로 가입된 경우만 체크
                        if (registeredDomain != null && !registeredDomain.equals(domain, ignoreCase = true)) {
                            logW(loginTag, "User registered with different method: $registeredDomain")
                            setState { copy(isLoading = false) }
                            setEffect {
                                LoginContract.Effect.ShowError(
                                    context.getString(R.string.error_1031)
                                )
                            }
                            return@collect
                        }
                        
                        
                        // 신규 회원: 회원가입 화면으로 이동
                        val password = tempPassword ?: run {
                            setState { copy(isLoading = false) }
                            setEffect {
                                LoginContract.Effect.ShowError(context.getString(R.string.error_abnormal_exception))
                            }
                            return@collect
                        }
                        val currentDomain = tempDomain ?: run {
                            setState { copy(isLoading = false) }
                            setEffect {
                                LoginContract.Effect.ShowError(context.getString(R.string.error_abnormal_exception))
                            }
                            return@collect
                        }
                        
                        // SNS 로그인(카카오, 구글 등) 시 nickname(displayName)을 전달하지 않음
                        val displayName = if (currentDomain == Constants.DOMAIN_EMAIL) tempDisplayName else null
                        val profileImageUrl = tempProfileImageUrl
                        
                        
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
     *
     * @param isExistingUser validate API에서 success == true를 반환한 경우 true (이미 회원임)
     *                       이 경우 signIn API가 실패해도 회원가입으로 이동하지 않음
     */
    private suspend fun performSignIn(isExistingUser: Boolean = false) {
        val email = tempEmail ?: return
        val password = tempPassword ?: return
        val domain = tempDomain ?: return

        // domain에 따라 적절한 태그 선택
        val loginTag = when (domain) {
            Constants.DOMAIN_KAKAO -> KAKAO_LOGIN_TAG
            Constants.DOMAIN_GOOGLE -> GOOGLE_LOGIN_TAG
            else -> TAG
        }


        // Device info
        val deviceId = getDeviceId()
        val gmail = getGmail()
        val deviceKey = getFcmToken() // FCM token from DataStore

        // SignIn API 파라미터 로그 출력

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
                }
                is ApiResult.Success -> {
                    val response = result.data

                    // Old 프로젝트: response.optBoolean("success")만 체크
                    // response.data가 null이어도 성공으로 처리 (사용자 정보는 이후에 별도로 가져옴)
                    if (response.success) {

                        // Old 프로젝트: 카카오 로그인 성공 후 unlink 호출
                        if (domain == Constants.DOMAIN_KAKAO) {
                            requestKakaoUnlink()
                        }

                        // response.data가 있는 경우에만 저장 (Old 프로젝트는 afterSignin에서 별도로 처리)
                        if (response.data != null) {
                            val userData = response.data

                            // 1. 인증 정보 저장 (AuthRepository에 저장 - 자동으로 DataStore 및 메모리 캐시)
                            // Old 프로젝트: IdolAccount.createAccount(this, email, hashToken, domain)
                            authRepository.login(
                                email = userData.email,
                                domain = domain,
                                token = userData.token
                            )
                            
                            // 3. 디바이스 ID 저장 (아이디 찾기용)
                            // old 프로젝트: 로그인 시 서버에 deviceId가 전달되지만, 로컬에도 저장하여 재설치 시에도 사용 가능하도록 함
                            preferencesManager.setDeviceId(deviceId)
                            
                            // 4. old 프로젝트와 동일: 최소한의 정보만 저장 (email, domain, token)
                            // 전체 사용자 정보는 StartUpScreen의 getUserSelf에서 가져옴
                            // email을 저장하기 위해 setUserInfo를 최소한의 정보로 호출 (id는 0으로 설정, 나머지는 StartUpScreen에서 업데이트)
                            preferencesManager.setUserInfo(
                                id = 0, // StartUpScreen에서 getUserSelf API로 실제 ID로 업데이트됨
                                email = userData.email,
                                username = "",
                                nickname = null,
                                profileImage = null,
                                heart = null,
                                domain = domain
                            )
                        } else {
                            // response.data가 null인 경우 (Old 프로젝트와 동일)
                            // 사용자 정보는 이후에 별도로 가져옴 (StartUpScreen 또는 getUserSelf에서)

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
                            
                            // 1. AuthRepository에 기본 정보 저장 (토큰은 나중에 업데이트될 수 있음)
                            authRepository.login(
                                email = email,
                                domain = domain,
                                token = hashToken
                            )
                            
                            // 3. 디바이스 ID 저장 (아이디 찾기용)
                            // old 프로젝트: 로그인 시 서버에 deviceId가 전달되지만, 로컬에도 저장하여 재설치 시에도 사용 가능하도록 함
                            preferencesManager.setDeviceId(deviceId)
                            
                            // 4. old 프로젝트와 동일: 최소한의 정보만 저장 (email, domain, token)
                            // 전체 사용자 정보는 StartUpScreen의 getUserSelf에서 가져옴
                            // email을 저장하기 위해 setUserInfo를 최소한의 정보로 호출 (id는 0으로 설정, 나머지는 StartUpScreen에서 업데이트)
                            preferencesManager.setUserInfo(
                                id = 0, // StartUpScreen에서 getUserSelf API로 실제 ID로 업데이트됨
                                email = email,
                                username = "",
                                nickname = null,
                                profileImage = null,
                                heart = null,
                                domain = domain
                            )
                        }


                        setState { copy(isLoading = false, loginType = null) }
                        setEffect { LoginContract.Effect.NavigateToMain }
                    } else {
                        // Old 프로젝트: response.optBoolean("success")가 false인 경우
                        // "이메일이 잘못되었습니다" 메시지가 포함된 경우 신규 회원으로 간주하고 회원가입 플로우로 이동
                        // 구글 로그인의 경우: "이메일이 잘못되었습니다" 에러가 나오면 무조건 회원가입으로 이동
                        // 단, validate API에서 이미 회원으로 확인된 경우(isExistingUser == true)에는
                        // "이메일이 잘못되었습니다"가 아니면 회원가입으로 이동하지 않고 로그인 처리
                        val errorMessage = response.message ?: ""
                        val isEmailError = errorMessage.contains("이메일이 잘못되었습니다", ignoreCase = true) ||
                                errorMessage.contains("email", ignoreCase = true) ||
                                errorMessage.contains("잘못", ignoreCase = true)
                        
                        
                        // Old 프로젝트: validate API에서 이미 회원으로 확인된 경우(isExistingUser == true)
                        // signIn API가 실패해도 회원가입으로 이동하지 않고 로그인 처리
                        // "이메일이 잘못되었습니다" 에러가 나와도 validate API에서 이미 회원으로 확인되었으므로 로그인 처리
                        if (isExistingUser) {
                            // validate API에서 이미 회원으로 확인되었으므로, signIn API 실패해도 로그인 처리
                            logW(loginTag, "========================================")
                            logW(loginTag, "User exists in DB but signIn API failed - proceeding with login")
                            logW(loginTag, "  errorMessage: $errorMessage")
                            logW(loginTag, "  isEmailError: $isEmailError")
                            logW(loginTag, "  Old project: validate success=true means user exists, proceed with login")
                            logW(loginTag, "========================================")
                            
                            // 로그인 처리 (afterSignin과 동일)
                            val email = tempEmail ?: return@collect
                            val password = tempPassword ?: return@collect
                            
                            // Old 프로젝트의 afterSignin 로직과 동일
                            val hashToken = if (domain == Constants.DOMAIN_EMAIL) {
                                password // Email 로그인의 경우 md5salt (현재는 구현하지 않음)
                            } else {
                                password // SNS 로그인의 경우 access token 그대로 사용
                            }
                            
                            // 1. AuthRepository에 기본 정보 저장
                            authRepository.login(
                                email = email,
                                domain = domain,
                                token = hashToken
                            )
                            
                            // 3. 디바이스 ID 저장 (아이디 찾기용)
                            // old 프로젝트: 로그인 시 서버에 deviceId가 전달되지만, 로컬에도 저장하여 재설치 시에도 사용 가능하도록 함
                            preferencesManager.setDeviceId(deviceId)
                            
                            // 4. old 프로젝트와 동일: 최소한의 정보만 저장 (email, domain, token)
                            // 전체 사용자 정보는 StartUpScreen의 getUserSelf에서 가져옴
                            // email을 저장하기 위해 setUserInfo를 최소한의 정보로 호출 (id는 0으로 설정, 나머지는 StartUpScreen에서 업데이트)
                            preferencesManager.setUserInfo(
                                id = 0, // StartUpScreen에서 getUserSelf API로 실제 ID로 업데이트됨
                                email = email,
                                username = "",
                                nickname = null,
                                profileImage = null,
                                heart = null,
                                domain = domain
                            )
                            
                            setState { copy(isLoading = false, loginType = null) }
                            setEffect { LoginContract.Effect.NavigateToMain }
                            return@collect
                        }
                        
                        // validate API에서 회원으로 확인되지 않은 경우(!isExistingUser)
                        // "이메일이 잘못되었습니다" 에러가 나오면 신규 회원으로 간주하고 회원가입 플로우로 이동
                        if (isEmailError && !isExistingUser) {
                            // 이메일 오류인 경우 신규 회원으로 간주하고 회원가입 플로우로 이동
                            
                            val email = tempEmail ?: run {
                                setState { copy(isLoading = false) }
                                setEffect {
                                    LoginContract.Effect.ShowError(context.getString(R.string.error_abnormal_exception))
                                }
                                return@collect
                            }
                            val password = tempPassword ?: run {
                                setState { copy(isLoading = false) }
                                setEffect {
                                    LoginContract.Effect.ShowError(context.getString(R.string.error_abnormal_exception))
                                }
                                return@collect
                            }
                            val currentDomain = tempDomain ?: run {
                                setState { copy(isLoading = false) }
                                setEffect {
                                    LoginContract.Effect.ShowError(context.getString(R.string.error_abnormal_exception))
                                }
                                return@collect
                            }
                            
                            // SNS 로그인(카카오, 구글 등) 시 nickname(displayName)을 전달하지 않음
                            val displayName = if (currentDomain == Constants.DOMAIN_EMAIL) tempDisplayName else null
                            val profileImageUrl = tempProfileImageUrl
                            
                            
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
        
        viewModelScope.launch {
            try {
                setState { copy(isLoading = true, loginType = LoginContract.LoginType.GOOGLE) }

                // Google SDK 호출은 LoginScreen에서 처리
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
     * @param accessToken Google OAuth access token (Old 프로젝트의 mAuthToken과 동일)
     */
    fun handleGoogleLoginResult(
        email: String,
        displayName: String?,
        accessToken: String
    ) {
        
        viewModelScope.launch {
            try {
                // Old 프로젝트와 동일: email이 필수, 없으면 에러 처리
                if (email.isEmpty()) {
                    handleSnsLoginError(context.getString(R.string.facebook_no_email))
                    return@launch
                }

                // Old 프로젝트: access token이 필수 (mPasswd = mAuthToken)
                if (accessToken.isEmpty()) {
                    handleSnsLoginError(context.getString(R.string.error_abnormal_exception))
                    return@launch
                }

                tempEmail = email
                tempPassword = accessToken // Old 프로젝트: mPasswd = mAuthToken (access token)
                tempDomain = Constants.DOMAIN_GOOGLE
                tempDisplayName = displayName
                tempProfileImageUrl = null // Google은 프로필 이미지 URL을 별도로 받지 않음


                // validate API 호출하여 회원 여부 확인
                validateAndSignIn(email, Constants.DOMAIN_GOOGLE)

            } catch (e: Exception) {
                handleError(e)
            }
        }
    }

    /**
     * Line 로그인.
     * Line SDK가 자체 UI를 표시하므로 로딩바는 표시하지 않음
     */
    private fun loginWithLine() {
        viewModelScope.launch {
            try {
                // Line SDK 자체 로딩 UI가 있으므로 isLoading은 false로 유지
                setState { copy(loginType = LoginContract.LoginType.LINE) }
                setEffect { LoginContract.Effect.StartSocialLogin(LoginContract.LoginType.LINE) }
            } catch (e: Exception) {
                handleError(e)
            }
        }
    }

    /**
     * Line 로그인 결과 처리.
     * Line SDK에서 결과를 받은 후 로딩바 시작
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
                // Line SDK UI가 닫힌 후 우리의 로딩바 시작
                setState { copy(isLoading = true) }

                // old 코드: mEmail = "$mid${Const.POSTFIX_LINE}"
                val email = "$userId${Constants.POSTFIX_LINE}"
                tempEmail = email
                tempPassword = accessToken // old 코드: mPasswd = mAuthToken
                tempDomain = Constants.DOMAIN_LINE
                tempDisplayName = displayName
                tempProfileImageUrl = null // Line은 프로필 이미지 URL을 별도로 받지 않음


                // validate API 호출하여 회원 여부 확인
                validateAndSignIn(email, Constants.DOMAIN_LINE)

            } catch (e: Exception) {
                handleError(e)
            }
        }
    }

    /**
     * Facebook 로그인.
     * Facebook SDK가 자체 UI를 표시하므로 로딩바는 표시하지 않음
     */
    private fun loginWithFacebook() {
        viewModelScope.launch {
            try {
                // Facebook SDK 자체 로딩 UI가 있으므로 isLoading은 false로 유지
                setState { copy(loginType = LoginContract.LoginType.FACEBOOK) }
                setEffect { LoginContract.Effect.StartSocialLogin(LoginContract.LoginType.FACEBOOK) }
            } catch (e: Exception) {
                handleError(e)
            }
        }
    }

    /**
     * Facebook 로그인 결과 처리.
     * Facebook SDK에서 결과를 받은 후 로딩바 시작
     *
     * @param email Facebook 계정 이메일
     * @param name 사용자 이름
     * @param facebookId Facebook 사용자 ID (Long)
     * @param accessToken Facebook access token
     */
    fun handleFacebookLoginResult(
        email: String,
        name: String?,
        facebookId: Long?,
        accessToken: String
    ) {
        viewModelScope.launch {
            try {
                // Facebook SDK UI가 닫힌 후 우리의 로딩바 시작
                setState { copy(isLoading = true) }


                tempEmail = email
                tempPassword = accessToken // old 코드: mPasswd = mAuthToken
                tempDomain = Constants.DOMAIN_FACEBOOK
                tempDisplayName = name
                tempProfileImageUrl = null // Facebook은 프로필 이미지 URL을 별도로 받지 않음
                tempFacebookId = facebookId // Old 프로젝트: mFacebookId = jsonObject.optString("id").toLongOrNull()

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
    }

    /**
     * SNS SDK 취소 처리 (LoginScreen에서 SDK 취소 발생 시 호출).
     * 로딩 상태만 해제하고 에러 메시지를 표시하지 않습니다.
     */
    fun handleSnsLoginCancelled() {
        setState {
            copy(
                isLoading = false,
                error = null,
                loginType = null
            )
        }
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

        setEffect { LoginContract.Effect.ShowError(errorMessage ?: context.getString(R.string.error_1031)) }
    }

    /**
     * 디바이스 ID 가져오기 (UUID).
     * 저장된 deviceId를 우선 사용하고, 없으면 DeviceUtil.getDeviceUUID()를 사용하여 생성하고 저장.
     * old 프로젝트의 Util.getDeviceUUID()와 동일한 로직.
     */
    private suspend fun getDeviceId(): String {
        val savedDeviceId = preferencesManager.deviceId.first()
        return if (savedDeviceId != null) {
            savedDeviceId
        } else {
            // 저장된 deviceId가 없으면 DeviceUtil.getDeviceUUID() 사용 (old 프로젝트와 동일)
            val newDeviceId = deviceUtil.getDeviceUUID()
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
        
        viewModelScope.launch {
            try {
                preferencesManager.setFcmToken(token)
            } catch (e: Exception) {
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
        
        viewModelScope.launch {
            try {
                UserApiClient.instance.unlink { throwable ->
                    if (throwable != null) {
                    } else {
                    }
                }
            } catch (e: Exception) {
            }
        }
    }
}
