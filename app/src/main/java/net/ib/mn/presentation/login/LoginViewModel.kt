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
                        // 회원이 존재함 -> 로그인 진행
                        android.util.Log.d(TAG, "User exists - proceeding to sign in")
                        performSignIn()
                    } else {
                        // 회원이 존재하지 않음 -> 회원가입 필요
                        android.util.Log.d(TAG, "User not found - sign up required")
                        setState { copy(isLoading = false) }
                        setEffect {
                            LoginContract.Effect.ShowToast("회원가입이 필요합니다. (회원가입 화면 미구현)")
                        }
                        // NOTE: 회원가입 화면 구현 시 NavigateToSignUp Effect 추가
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
     */
    private suspend fun performSignIn() {
        val email = tempEmail ?: return
        val password = tempPassword ?: return
        val domain = tempDomain ?: return

        // Device info
        val deviceId = getDeviceId()
        val gmail = getGmail()
        val deviceKey = getFcmToken() // FCM token from DataStore

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
                    val token = response.data?.token

                    if (token != null) {
                        // 토큰 저장
                        preferencesManager.setAccessToken(token)
                        authInterceptor.setToken(token)

                        android.util.Log.d(TAG, "Login success - token saved")

                        setState { copy(isLoading = false, loginType = null) }
                        setEffect { LoginContract.Effect.NavigateToMain }
                    } else {
                        setState { copy(isLoading = false) }
                        setEffect { LoginContract.Effect.ShowError("토큰을 받지 못했습니다") }
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
