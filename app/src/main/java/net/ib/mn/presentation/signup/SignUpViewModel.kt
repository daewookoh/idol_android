package net.ib.mn.presentation.signup

import android.content.Context
import android.util.Patterns
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.google.firebase.messaging.FirebaseMessaging
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.launch
import net.ib.mn.base.BaseViewModel
import net.ib.mn.BuildConfig
import net.ib.mn.data.local.PreferencesManager
import net.ib.mn.data.remote.dto.CommonResponse
import net.ib.mn.data.remote.interceptor.AuthInterceptor
import net.ib.mn.domain.model.ApiResult
import net.ib.mn.domain.usecase.SignInUseCase
import net.ib.mn.domain.usecase.SignUpUseCase
import net.ib.mn.domain.usecase.ValidateUserUseCase
import net.ib.mn.util.Constants
import net.ib.mn.util.DeviceUtil
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import net.ib.mn.data.remote.dto.SignInResponse
import javax.inject.Inject

/**
 * 회원가입 플로우 ViewModel
 *
 * old 프로젝트의 AgreementFragment + SignupFragment 로직을 MVI 패턴으로 구현
 */
@HiltViewModel
class SignUpViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val savedStateHandle: SavedStateHandle?,
    private val validateUserUseCase: ValidateUserUseCase,
    private val signUpUseCase: SignUpUseCase,
    private val signInUseCase: SignInUseCase,
    private val preferencesManager: PreferencesManager,
    private val authInterceptor: AuthInterceptor,
    private val deviceUtil: DeviceUtil
) : BaseViewModel<SignUpContract.State, SignUpContract.Intent, SignUpContract.Effect>() {

    companion object {
        private const val TAG = "SignUpViewModel"
        private const val KAKAO_SIGNUP_TAG = "KAKAO_SIGNUP"
        private const val GOOGLE_SIGNUP_TAG = "GOOGLE_SIGNUP"

        // Navigation arguments (SNS 로그인에서 전달)
        const val ARG_EMAIL = "email"
        const val ARG_PASSWORD = "password"
        const val ARG_DISPLAY_NAME = "displayName"
        const val ARG_DOMAIN = "domain"
        const val ARG_PROFILE_IMAGE_URL = "profileImageUrl"
    }

    /**
     * Domain에 따른 로그 태그 반환
     */
    private fun getSignUpTag(domain: String): String {
        return when (domain) {
            Constants.DOMAIN_KAKAO -> KAKAO_SIGNUP_TAG
            Constants.DOMAIN_GOOGLE -> GOOGLE_SIGNUP_TAG
            else -> TAG
        }
    }

    override fun createInitialState(): SignUpContract.State {
        val email = savedStateHandle?.get<String>(ARG_EMAIL)
        val password = savedStateHandle?.get<String>(ARG_PASSWORD)
        val displayName = savedStateHandle?.get<String>(ARG_DISPLAY_NAME)
        val domain = savedStateHandle?.get<String>(ARG_DOMAIN) ?: "email"

        return SignUpContract.State(
            domain = domain,
            // SNS 로그인의 경우 이메일/비밀번호 자동 입력
            email = email ?: "",
            password = password ?: "",
            nickname = displayName ?: "",
            isEmailValid = !email.isNullOrEmpty(),
            isPasswordValid = !password.isNullOrEmpty()
        )
    }

    override fun handleIntent(intent: SignUpContract.Intent) {
        android.util.Log.d(TAG, "[handleIntent] Received intent: ${intent::class.simpleName}")
        
        when (intent) {
            // Step 1: 약관동의
            is SignUpContract.Intent.UpdateAgreeAll -> updateAgreeAll(intent.checked)
            is SignUpContract.Intent.UpdateAgreeTerms -> updateAgreeTerms(intent.checked)
            is SignUpContract.Intent.UpdateAgreePrivacy -> updateAgreePrivacy(intent.checked)
            is SignUpContract.Intent.UpdateAgreeAge -> updateAgreeAge(intent.checked)
            is SignUpContract.Intent.ShowTermsOfService -> setEffect { SignUpContract.Effect.NavigateToTermsOfService }
            is SignUpContract.Intent.ShowPrivacyPolicy -> setEffect { SignUpContract.Effect.NavigateToPrivacyPolicy }
            is SignUpContract.Intent.ProceedToSignUpForm -> proceedToSignUpForm()
            is SignUpContract.Intent.DismissDialog -> dismissDialog()

            // Step 2: 회원가입 폼
            is SignUpContract.Intent.InitializeFromNavigation -> initializeFromNavigation(intent)
            is SignUpContract.Intent.UpdateEmail -> updateEmail(intent.email)
            is SignUpContract.Intent.UpdatePassword -> updatePassword(intent.password)
            is SignUpContract.Intent.UpdatePasswordConfirm -> updatePasswordConfirm(intent.passwordConfirm)
            is SignUpContract.Intent.UpdateNickname -> {
                android.util.Log.d(TAG, "[handleIntent] UpdateNickname called with: '${intent.nickname}'")
                updateNickname(intent.nickname)
            }
            is SignUpContract.Intent.UpdateRecommenderCode -> updateRecommenderCode(intent.code)
            is SignUpContract.Intent.ValidateEmail -> validateEmail()
            is SignUpContract.Intent.ValidateNickname -> {
                android.util.Log.d(TAG, "[handleIntent] ValidateNickname intent received")
                validateNickname()
            }
            is SignUpContract.Intent.ValidateRecommenderCode -> validateRecommenderCode()
            is SignUpContract.Intent.SignUp -> signUp()

            // Navigation
            is SignUpContract.Intent.GoBack -> goBack()
        }
    }

    // ============================================================
    // Step 1: 약관동의
    // ============================================================

    private fun updateAgreeAll(checked: Boolean) {
        setState {
            copy(
                agreeAll = checked,
                agreeTerms = checked,
                agreePrivacy = checked,
                agreeAge = checked,
                canProceedStep1 = checked
            )
        }
    }

    private fun updateAgreeTerms(checked: Boolean) {
        setState {
            val newState = copy(agreeTerms = checked)
            newState.copy(
                agreeAll = newState.agreeTerms && newState.agreePrivacy && newState.agreeAge,
                canProceedStep1 = newState.agreeTerms && newState.agreePrivacy && newState.agreeAge
            )
        }
    }

    private fun updateAgreePrivacy(checked: Boolean) {
        setState {
            val newState = copy(agreePrivacy = checked)
            newState.copy(
                agreeAll = newState.agreeTerms && newState.agreePrivacy && newState.agreeAge,
                canProceedStep1 = newState.agreeTerms && newState.agreePrivacy && newState.agreeAge
            )
        }
    }

    private fun updateAgreeAge(checked: Boolean) {
        setState {
            val newState = copy(agreeAge = checked)
            newState.copy(
                agreeAll = newState.agreeTerms && newState.agreePrivacy && newState.agreeAge,
                canProceedStep1 = newState.agreeTerms && newState.agreePrivacy && newState.agreeAge
            )
        }
    }

    private fun proceedToSignUpForm() {
        if (!currentState.canProceedStep1) {
            val errorMessage = when {
                !currentState.agreeTerms -> context.getString(net.ib.mn.R.string.need_agree1)
                !currentState.agreePrivacy -> context.getString(net.ib.mn.R.string.need_agree2)
                !currentState.agreeAge -> context.getString(net.ib.mn.R.string.need_agree1)
                else -> context.getString(net.ib.mn.R.string.need_agree1)
            }
            setState { copy(dialogMessage = errorMessage) }
            return
        }

        // Old 프로젝트: 약관 동의 후 회원가입 폼으로 이동 (SNS 로그인도 동일)
        // SNS 로그인의 경우 이메일/비밀번호는 숨기고 닉네임만 입력받음
        setState { copy(currentStep = 1) }
        setEffect { SignUpContract.Effect.NavigateToNextStep }
    }

    private fun dismissDialog() {
        setState { copy(dialogMessage = null) }
    }

    // ============================================================
    // Step 2: 회원가입 폼
    // ============================================================

    /**
     * Navigation에서 전달받은 파라미터로 State 초기화
     */
    private fun initializeFromNavigation(intent: SignUpContract.Intent.InitializeFromNavigation) {
        android.util.Log.d(TAG, "========================================")
        android.util.Log.d(TAG, "initializeFromNavigation() called")
        android.util.Log.d(TAG, "  email: ${intent.email}")
        android.util.Log.d(TAG, "  password: ${intent.password?.take(20)}...")
        android.util.Log.d(TAG, "  displayName: ${intent.displayName}")
        android.util.Log.d(TAG, "  domain: ${intent.domain}")
        android.util.Log.d(TAG, "========================================")

        // SavedStateHandle에 저장 (ViewModel 재생성 시에도 유지됨)
        intent.email?.let { savedStateHandle?.set(ARG_EMAIL, it) }
        intent.password?.let { savedStateHandle?.set(ARG_PASSWORD, it) }
        intent.displayName?.let { savedStateHandle?.set(ARG_DISPLAY_NAME, it) }
        intent.domain?.let { savedStateHandle?.set(ARG_DOMAIN, it) }

        // State 업데이트
        setState {
            val newDomain = intent.domain ?: "email"
            copy(
                domain = newDomain,
                // SNS 로그인의 경우 이메일/비밀번호 자동 입력
                email = intent.email ?: email,
                password = intent.password ?: password,
                nickname = intent.displayName ?: nickname,
                isEmailValid = !intent.email.isNullOrEmpty() || isEmailValid,
                isPasswordValid = !intent.password.isNullOrEmpty() || isPasswordValid,
                // 서버 검증 전이므로 isNicknameValid는 false
                isNicknameValid = false,
                isBadWordsNickName = false, // Old 프로젝트: 초기화 시 false
                // SNS 로그인인 경우 (domain != "email"): 닉네임만 체크, 일반 가입인 경우: 모든 필드 체크
                canProceedStep2 = if (newDomain != "email") {
                    // SNS 로그인: 닉네임만 체크 (서버 검증 통과해야 활성화)
                    false
                } else {
                    // 일반 가입: 모든 필드 체크
                    isEmailValid && isPasswordValid && isPasswordConfirmValid && false
                }
            )
        }
    }

    private fun updateEmail(email: String) {
        setState {
            val isValid = Patterns.EMAIL_ADDRESS.matcher(email).matches()
            copy(
                email = email,
                isEmailValid = isValid,
                emailError = if (email.isNotEmpty() && !isValid) context.getString(net.ib.mn.R.string.invalid_format_email) else null,
                canProceedStep2 = if (domain != "email") {
                    // SNS 로그인: 닉네임만 체크
                    isNicknameValid
                } else {
                    // 일반 가입: 모든 필드 체크
                    isValid && isPasswordValid && isPasswordConfirmValid && isNicknameValid
                }
            )
        }
    }

    private fun updatePassword(password: String) {
        setState {
            // 비밀번호 규칙: 8~20자, 영문+숫자 조합 (old 프로젝트 규칙)
            val isValid = password.length in 8..20 &&
                    password.any { it.isDigit() } &&
                    password.any { it.isLetter() }

            copy(
                password = password,
                isPasswordValid = isValid,
                passwordError = if (password.isNotEmpty() && !isValid) {
                    context.getString(net.ib.mn.R.string.check_pwd_requirement)
                } else null,
                // 비밀번호 확인도 다시 검증
                isPasswordConfirmValid = passwordConfirm.isNotEmpty() && passwordConfirm == password,
                passwordConfirmError = if (passwordConfirm.isNotEmpty() && passwordConfirm != password) {
                    context.getString(net.ib.mn.R.string.passwd_confirm_not_match)
                } else null,
                canProceedStep2 = if (domain != "email") {
                    // SNS 로그인: 닉네임만 체크
                    isNicknameValid
                } else {
                    // 일반 가입: 모든 필드 체크
                    isValid &&
                            (passwordConfirm.isEmpty() || passwordConfirm == password) &&
                            isEmailValid && isNicknameValid
                }
            )
        }
    }

    private fun updatePasswordConfirm(passwordConfirm: String) {
        setState {
            val isValid = passwordConfirm == password
            copy(
                passwordConfirm = passwordConfirm,
                isPasswordConfirmValid = isValid,
                passwordConfirmError = if (passwordConfirm.isNotEmpty() && !isValid) {
                    context.getString(net.ib.mn.R.string.passwd_confirm_not_match)
                } else null,
                canProceedStep2 = if (domain != "email") {
                    // SNS 로그인: 닉네임만 체크
                    isNicknameValid
                } else {
                    // 일반 가입: 모든 필드 체크
                    isValid && isPasswordValid && isEmailValid && isNicknameValid
                }
            )
        }
    }

    private fun updateNickname(nickname: String) {
        android.util.Log.d(TAG, "========================================")
        android.util.Log.d(TAG, "[updateNickname] Nickname changed")
        android.util.Log.d(TAG, "  - nickname: '$nickname'")
        android.util.Log.d(TAG, "  - nickname length: ${nickname.length}")
        android.util.Log.d(TAG, "========================================")
        
        setState {
            // 닉네임 규칙: 2~10자 (old 프로젝트 규칙)
            val isValid = nickname.length in 2..10
            android.util.Log.d(TAG, "[updateNickname] Length validation: isValid=$isValid")
            
            copy(
                nickname = nickname,
                // 닉네임이 변경되면 서버 검증 결과를 초기화 (서버 검증 통과해야 true가 됨)
                isNicknameValid = false,
                isBadWordsNickName = false, // Old 프로젝트: 닉네임 변경 시 초기화
                nicknameError = if (nickname.isNotEmpty() && !isValid) {
                    context.getString(net.ib.mn.R.string.required_field)
                } else null, // 닉네임이 변경되면 이전 서버 검증 에러도 초기화
                canProceedStep2 = if (domain != "email") {
                    // SNS 로그인: 닉네임만 체크 (서버 검증 통과해야 활성화)
                    false
                } else {
                    // 일반 가입: 모든 필드 체크
                    false // isNicknameValid가 false이므로 false
                }
            )
        }
        
        android.util.Log.d(TAG, "[updateNickname] State updated: isNicknameValid=false, canProceedStep2=false")
    }

    private fun updateRecommenderCode(code: String) {
        setState {
            copy(
                recommenderCode = code,
                recommenderError = null,
                isRecommenderValid = false
            )
        }
    }

    private fun validateEmail() {
        val email = currentState.email
        if (email.isEmpty()) return

        viewModelScope.launch {
            validateUserUseCase(
                type = "email",
                value = email,
                appId = Constants.APP_ID
            ).collect { result ->
                when (result) {
                    is ApiResult.Success -> {
                        val response = result.data
                        if (response.success) {
                            // 이미 존재하는 이메일
                            setState {
                                copy(
                                    emailError = context.getString(net.ib.mn.R.string.error_1001),
                                    isEmailValid = false,
                                    canProceedStep2 = false
                                )
                            }
                        } else {
                            // 사용 가능한 이메일
                            // setState 블록 밖에서 현재 state 값들을 먼저 읽어야 함
                            val currentDomain = currentState.domain
                            val currentIsNicknameValid = currentState.isNicknameValid
                            val currentIsPasswordValid = currentState.isPasswordValid
                            val currentIsPasswordConfirmValid = currentState.isPasswordConfirmValid
                            
                            setState {
                                copy(
                                    emailError = null,
                                    isEmailValid = true,
                                    canProceedStep2 = if (currentDomain != "email") {
                                        // SNS 로그인: 닉네임만 체크
                                        currentIsNicknameValid
                                    } else {
                                        // 일반 가입: 모든 필드 체크
                                        true && currentIsPasswordValid && currentIsPasswordConfirmValid && currentIsNicknameValid
                                    }
                                )
                            }
                            // Old 프로젝트: 토스트 없이 체크 아이콘만 표시
                        }
                    }
                    is ApiResult.Error -> {
                        // Old 프로젝트: 에러 시 토스트 대신 TextField에 에러 메시지 표시
                        setState {
                            copy(
                                emailError = result.message ?: context.getString(net.ib.mn.R.string.error_abnormal_exception),
                                isEmailValid = false,
                                canProceedStep2 = false
                            )
                        }
                    }
                    is ApiResult.Loading -> {
                        // 로딩 중
                    }
                }
            }
        }
    }

    private fun validateNickname() {
        val nickname = currentState.nickname
        
        android.util.Log.d(TAG, "========================================")
        android.util.Log.d(TAG, "[validateNickname] Called")
        android.util.Log.d(TAG, "  - nickname: '$nickname'")
        android.util.Log.d(TAG, "  - nickname length: ${nickname.length}")
        android.util.Log.d(TAG, "  - currentState.isNicknameValid: ${currentState.isNicknameValid}")
        android.util.Log.d(TAG, "  - currentState.domain: ${currentState.domain}")
        android.util.Log.d(TAG, "========================================")
        
        if (nickname.isEmpty() || nickname.length < 2) {
            android.util.Log.w(TAG, "[validateNickname] Early return: nickname is empty or too short")
            return
        }

        android.util.Log.d(TAG, "[validateNickname] Calling validateUserUseCase")
        android.util.Log.d(TAG, "  - type: nickname")
        android.util.Log.d(TAG, "  - value: '$nickname'")
        android.util.Log.d(TAG, "  - appId: ${Constants.APP_ID}")

        viewModelScope.launch {
            validateUserUseCase(
                type = "nickname", // Old 프로젝트: "nickname" 사용 (현재는 "username"로 되어 있음)
                value = nickname,
                appId = Constants.APP_ID
            ).collect { result ->
                android.util.Log.d(TAG, "[validateNickname] Received result: ${result::class.simpleName}")
                
                when (result) {
                    is ApiResult.Success -> {
                        val response = result.data

                        // API 응답 로깅 (디버깅용)
                        android.util.Log.d(TAG, "========================================")
                        android.util.Log.d(TAG, "[validateNickname] API Response:")
                        android.util.Log.d(TAG, "  - nickname: $nickname")
                        android.util.Log.d(TAG, "  - success: ${response.success}")
                        android.util.Log.d(TAG, "  - message: ${response.message}")
                        android.util.Log.d(TAG, "  - domain: ${response.domain}")
                        android.util.Log.d(TAG, "  - gcode: ${response.gcode}")
                        android.util.Log.d(TAG, "  - mcode: ${response.mcode}")
                        android.util.Log.d(TAG, "========================================")

                        // Old 프로젝트 로직 분석 (SignupFragment.serverValidate, KakaoMoreFragment.serverValidate):
                        // 
                        // 1. success == false인 경우:
                        //    - 에러 메시지 표시 (response.optString("msg") 또는 ErrorControl.parseError)
                        //    - gcode == ERROR_88888 (88888)이면 isBadWords = true (금지어)
                        //    - gcode == ERROR_1011 (1011)이면 isBadWords = false (중복 닉네임)
                        //    - isNickName = false 설정
                        //    - 버튼 비활성화 (changeSingupBtnStatus)
                        //    - return@validate (여기서 끝)
                        //
                        // 2. success == true인 경우:
                        //    - gcode == 0이면:
                        //      - isNickName = true 설정
                        //      - isBadWords = false 설정
                        //    - 에러 메시지 제거 (error = null)
                        //    - 체크 표시 추가 (setCompoundDrawables)
                        //    - 버튼 활성화 (changeSingupBtnStatus)
                        //
                        // 중요: old 프로젝트에서는 response.optInt("gcode")를 사용하는데,
                        // optInt는 키가 없거나 null이면 0을 반환합니다.
                        // 따라서 success == false일 때도 gcode를 확인해야 합니다.
                        
                        if (!response.success) {
                            // CASE 1: success == false -> 사용 불가 (이미 존재하거나 에러)
                            val gcode = response.gcode ?: 0 // old: response.optInt("gcode")
                            
                            // Old 프로젝트: gcode에 따라 isBadWordsNickName 설정
                            val isBadWords = when (gcode) {
                                88888 -> true  // 욕설/부적절한 단어 필터링 (ERROR_88888)
                                1011 -> false  // 중복 닉네임 (ERROR_1011)
                                else -> false
                            }
                            
                            val errorMessage = response.message ?: when (gcode) {
                                1011 -> context.getString(net.ib.mn.R.string.error_1011) // 중복 닉네임 (ERROR_1011)
                                88888 -> context.getString(net.ib.mn.R.string.bad_words) // 금지어 (ERROR_88888)
                                else -> context.getString(net.ib.mn.R.string.error_1011)
                            }
                            
                            android.util.Log.d(TAG, "[validateNickname] CASE 1: success=false -> Nickname unavailable")
                            android.util.Log.d(TAG, "  - gcode: $gcode")
                            android.util.Log.d(TAG, "  - isBadWordsNickName: $isBadWords")
                            android.util.Log.d(TAG, "  - errorMessage: $errorMessage")
                            
                            setState {
                                android.util.Log.d(TAG, "[validateNickname] Setting state: isNicknameValid=false, isBadWordsNickName=$isBadWords, canProceedStep2=false")
                                copy(
                                    nicknameError = errorMessage, // Old: editText.setError(responseMsg)
                                    isNicknameValid = false, // Old: isNickName = false
                                    isBadWordsNickName = isBadWords, // Old: isBadWordsNickName = true/false
                                    canProceedStep2 = false // Old: changeSingupBtnStatus() -> 버튼 비활성화
                                )
                            }
                            // Old: return@validate (여기서 끝)
                        } else {
                            // CASE 2: success == true인 경우
                            val gcode = response.gcode ?: 0 // null이면 0으로 처리 (old 프로젝트의 optInt 동작)
                            android.util.Log.d(TAG, "[validateNickname] CASE 2: success=true, gcode=$gcode")
                            
                            if (gcode == 0) {
                                // CASE 2-1: success == true && gcode == 0 -> 사용 가능한 닉네임
                                android.util.Log.d(TAG, "[validateNickname] CASE 2-1: gcode=0 -> Nickname available")

                                // setState 블록 밖에서 현재 state 값들을 먼저 읽어야 함
                                val currentDomain = currentState.domain
                                val currentIsEmailValid = currentState.isEmailValid
                                val currentIsPasswordValid = currentState.isPasswordValid
                                val currentIsPasswordConfirmValid = currentState.isPasswordConfirmValid

                                val isEmailLogin = currentDomain == "email"
                                android.util.Log.d(TAG, "  - domain: $currentDomain")
                                android.util.Log.d(TAG, "  - isEmailLogin: $isEmailLogin")
                                android.util.Log.d(TAG, "  - isEmailValid: $currentIsEmailValid")
                                android.util.Log.d(TAG, "  - isPasswordValid: $currentIsPasswordValid")
                                android.util.Log.d(TAG, "  - isPasswordConfirmValid: $currentIsPasswordConfirmValid")

                                val newCanProceedStep2 = if (isEmailLogin) {
                                    // 일반 가입: 모든 필드 체크 (Old: isEmail && isPasswd && isPasswdConfirm && isNickName)
                                    val result = currentIsEmailValid && currentIsPasswordValid && currentIsPasswordConfirmValid && true
                                    android.util.Log.d(TAG, "  - canProceedStep2 (email): $result")
                                    result
                                } else {
                                    // SNS 로그인: 닉네임만 체크 (Old: isNickName)
                                    android.util.Log.d(TAG, "  - canProceedStep2 (SNS): true")
                                    true
                                }

                                setState {
                                    android.util.Log.d(TAG, "[validateNickname] Setting state: isNicknameValid=true, isBadWordsNickName=false, canProceedStep2=$newCanProceedStep2")
                                    copy(
                                        nicknameError = null, // Old: editText.error = null
                                        isNicknameValid = true, // Old: isNickName = true
                                        isBadWordsNickName = false, // Old: isBadWordsNickName = false (gcode == 0)
                                        canProceedStep2 = newCanProceedStep2 // Old: changeSingupBtnStatus() -> 버튼 활성화
                                    )
                                }
                                // Old 프로젝트: 토스트 없이 체크 아이콘만 표시
                            } else {
                                // CASE 2-2: success == true이지만 gcode != 0 -> 사용 불가 (이미 존재 등)
                                // 이 경우는 이론적으로 발생하지 않아야 하지만, 안전을 위해 처리
                                val isBadWords = when (gcode) {
                                    88888 -> true
                                    1011 -> false
                                    else -> false
                                }
                                val errorMessage = response.message ?: when (gcode) {
                                    1011 -> context.getString(net.ib.mn.R.string.error_1011) // 중복 닉네임
                                    88888 -> context.getString(net.ib.mn.R.string.bad_words) // 금지어
                                    else -> context.getString(net.ib.mn.R.string.error_1011)
                                }
                                android.util.Log.w(TAG, "[validateNickname] CASE 2-2: success=true but gcode=$gcode -> Nickname unavailable (unexpected)")
                                android.util.Log.w(TAG, "  - isBadWordsNickName: $isBadWords")
                                android.util.Log.w(TAG, "  - errorMessage: $errorMessage")
                                
                                setState {
                                    android.util.Log.d(TAG, "[validateNickname] Setting state: isNicknameValid=false, isBadWordsNickName=$isBadWords, canProceedStep2=false")
                                    copy(
                                        nicknameError = errorMessage,
                                        isNicknameValid = false,
                                        isBadWordsNickName = isBadWords,
                                        canProceedStep2 = false
                                    )
                                }
                            }
                        }
                    }
                    is ApiResult.Error -> {
                        android.util.Log.e(TAG, "========================================")
                        android.util.Log.e(TAG, "[validateNickname] API Error")
                        android.util.Log.e(TAG, "  - error message: ${result.message}")
                        android.util.Log.e(TAG, "  - error exception: ${result.exception}")
                        android.util.Log.e(TAG, "========================================")
                        // Old 프로젝트: 에러 시 토스트 대신 TextField에 에러 메시지 표시
                        setState {
                            copy(
                                nicknameError = result.message ?: context.getString(net.ib.mn.R.string.error_abnormal_exception),
                                isNicknameValid = false,
                                canProceedStep2 = false
                            )
                        }
                    }
                    is ApiResult.Loading -> {
                        android.util.Log.d(TAG, "[validateNickname] Loading...")
                        // 로딩 중
                    }
                }
            }
        }
    }

    private fun validateRecommenderCode() {
        val recommenderCode = currentState.recommenderCode
        if (recommenderCode.isEmpty()) {
            // 추천인 코드가 비어있으면 검증하지 않음 (선택사항)
            setState {
                copy(
                    recommenderError = null,
                    isRecommenderValid = false
                )
            }
            return
        }

        viewModelScope.launch {
            validateUserUseCase(
                type = "referral_code",
                value = recommenderCode,
                appId = Constants.APP_ID
            ).collect { result ->
                when (result) {
                    is ApiResult.Success -> {
                        val response = result.data
                        if (response.success) {
                            // 유효한 추천인 코드
                            setState {
                                copy(
                                    recommenderError = null,
                                    isRecommenderValid = true
                                )
                            }
                        } else {
                            // 존재하지 않는 추천인 코드 (error_1012: "추천인을 찾지 못했습니다.")
                            setState {
                                copy(
                                    recommenderError = context.getString(net.ib.mn.R.string.error_1012),
                                    isRecommenderValid = false
                                )
                            }
                        }
                    }
                    is ApiResult.Error -> {
                        setState {
                            copy(
                                recommenderError = result.message ?: context.getString(net.ib.mn.R.string.required_field),
                                isRecommenderValid = false
                            )
                        }
                    }
                    is ApiResult.Loading -> {
                        // 로딩 중
                    }
                }
            }
        }
    }

    private fun signUp() {
        if (!currentState.canProceedStep1) {
            setEffect { SignUpContract.Effect.ShowError(context.getString(net.ib.mn.R.string.need_agree1)) }
            return
        }

        // 이메일 로그인인 경우 폼 검증
        if (currentState.domain == "email" && !currentState.canProceedStep2) {
            setEffect { SignUpContract.Effect.ShowError(context.getString(net.ib.mn.R.string.required_field)) }
            return
        }

        // Old 프로젝트: isBadWordsNickName이 true이면 회원가입 API를 호출하지 않음
        if (currentState.isBadWordsNickName) {
            android.util.Log.w(TAG, "========================================")
            android.util.Log.w(TAG, "[signUp] Skipped: isBadWordsNickName=true")
            android.util.Log.w(TAG, "  - nickname: '${currentState.nickname}'")
            android.util.Log.w(TAG, "========================================")
            setState { copy(isLoading = false) }
            // 닉네임 필드에 에러 표시 (old 프로젝트 동일)
            setState {
                copy(
                    nicknameError = context.getString(net.ib.mn.R.string.bad_words),
                    isNicknameValid = false
                )
            }
            return
        }

        val domain = currentState.domain ?: Constants.DOMAIN_EMAIL
        val signUpTag = getSignUpTag(domain)

        // 회원가입 API 호출 전 로그 출력
        android.util.Log.d(signUpTag, "========================================")
        android.util.Log.d(signUpTag, "[signUp] Called")
        android.util.Log.d(signUpTag, "  - email: ${currentState.email}")
        android.util.Log.d(signUpTag, "  - password: ${currentState.password.take(20)}...")
        android.util.Log.d(signUpTag, "  - nickname: '${currentState.nickname}'")
        android.util.Log.d(signUpTag, "  - nickname length: ${currentState.nickname.length}")
        android.util.Log.d(signUpTag, "  - domain: $domain")
        android.util.Log.d(signUpTag, "  - recommenderCode: ${currentState.recommenderCode}")
        android.util.Log.d(signUpTag, "  - appId: ${Constants.APP_ID}")
        android.util.Log.d(signUpTag, "  - isBadWordsNickName: ${currentState.isBadWordsNickName}")
        android.util.Log.d(signUpTag, "========================================")

        viewModelScope.launch {
            setState { copy(isLoading = true) }

            // Old 프로젝트: 회원가입 전에 FCM token 확인 및 가져오기
            try {
                // Old 프로젝트: CHINA 빌드는 FCM 사용 안 함
                if (Constants.IS_CHINA) {
                    android.util.Log.d(signUpTag, "[signUp] CHINA build, skipping FCM token")
                    performSignUp()
                    return@launch
                }

                // Old 프로젝트: DEBUG + 에뮬레이터는 빈 문자열 사용
                if (BuildConfig.DEBUG && android.os.Build.FINGERPRINT.startsWith("generic")) {
                    android.util.Log.d(signUpTag, "[signUp] DEBUG + Emulator, using empty FCM token")
                    preferencesManager.setFcmToken("")
                    performSignUp()
                    return@launch
                }

                val currentFcmToken = preferencesManager.fcmToken.first()
                android.util.Log.d(signUpTag, "========================================")
                android.util.Log.d(signUpTag, "[signUp] Checking FCM token")
                android.util.Log.d(signUpTag, "  - currentFcmToken: ${currentFcmToken?.take(20) ?: "null"}")
                android.util.Log.d(signUpTag, "========================================")

                if (currentFcmToken.isNullOrEmpty()) {
                    // FCM token이 없으면 먼저 가져오기 (Old 프로젝트의 registerDevice와 동일)
                    android.util.Log.d(signUpTag, "[signUp] FCM token is empty, fetching token first...")

                    try {
                        com.google.firebase.messaging.FirebaseMessaging.getInstance().token
                            .addOnSuccessListener { token ->
                                android.util.Log.d(signUpTag, "========================================")
                                android.util.Log.d(signUpTag, "FCM token retrieved successfully")
                                android.util.Log.d(signUpTag, "  - token: ${token.take(20)}...")
                                android.util.Log.d(signUpTag, "========================================")

                                // Token 저장
                                viewModelScope.launch {
                                    preferencesManager.setFcmToken(token)
                                    android.util.Log.d(signUpTag, "FCM token saved to DataStore")

                                    // 이제 회원가입 진행
                                    performSignUp()
                                }
                            }
                            .addOnFailureListener { e ->
                                // Old 프로젝트: 에러 발생 시 조용히 처리하고 빈 문자열로 진행
                                // IOException, FIS_AUTH_ERROR 등 모든 에러를 무시하고 진행
                                android.util.Log.w(signUpTag, "========================================")
                                android.util.Log.w(signUpTag, "Failed to get FCM token (proceeding with empty token)")
                                android.util.Log.w(signUpTag, "  - error: ${e.javaClass.simpleName}: ${e.message}")
                                android.util.Log.w(signUpTag, "  - Old project behavior: ignore error and proceed")
                                android.util.Log.w(signUpTag, "========================================")

                                // Old 프로젝트: FCM token 실패해도 빈 문자열로 진행
                                viewModelScope.launch {
                                    preferencesManager.setFcmToken("")
                                    performSignUp()
                                }
                            }
                    } catch (e: IllegalStateException) {
                        // Old 프로젝트: IllegalStateException만 catch하고 진행
                        android.util.Log.w(signUpTag, "========================================")
                        android.util.Log.w(signUpTag, "IllegalStateException while getting FCM token")
                        android.util.Log.w(signUpTag, "  - Proceeding with empty token (old project behavior)")
                        android.util.Log.w(signUpTag, "========================================")

                        // Old 프로젝트: IllegalStateException 발생해도 빈 문자열로 진행
                        viewModelScope.launch {
                            preferencesManager.setFcmToken("")
                            performSignUp()
                        }
                    } catch (e: Exception) {
                        // Old 프로젝트: 모든 에러를 무시하고 진행
                        android.util.Log.w(signUpTag, "========================================")
                        android.util.Log.w(signUpTag, "Exception while getting FCM token (proceeding with empty token)")
                        android.util.Log.w(signUpTag, "  - error: ${e.javaClass.simpleName}: ${e.message}")
                        android.util.Log.w(signUpTag, "  - Old project behavior: ignore error and proceed")
                        android.util.Log.w(signUpTag, "========================================")

                        // Old 프로젝트: FCM token 실패해도 빈 문자열로 진행
                        viewModelScope.launch {
                            preferencesManager.setFcmToken("")
                            performSignUp()
                        }
                    }
                } else {
                    // FCM token이 이미 있으면 바로 회원가입 진행
                    android.util.Log.d(signUpTag, "[signUp] FCM token exists, proceeding with signup")
                    performSignUp()
                }
            } catch (e: Exception) {
                android.util.Log.e(signUpTag, "========================================")
                android.util.Log.e(signUpTag, "SignUp Exception", e)
                android.util.Log.e(signUpTag, "========================================")
                setState { copy(isLoading = false) }
                setEffect { SignUpContract.Effect.ShowError(e.message ?: context.getString(net.ib.mn.R.string.error_abnormal_exception)) }
            }
        }
    }

    /**
     * 실제 회원가입 API 호출
     */
    private suspend fun performSignUp() {
        val domain = currentState.domain ?: Constants.DOMAIN_EMAIL
        val signUpTag = getSignUpTag(domain)

        try {
            signUpUseCase(
                    email = currentState.email,
                    password = currentState.password,
                    nickname = currentState.nickname,
                    domain = domain,
                    recommenderCode = currentState.recommenderCode,
                    appId = Constants.APP_ID
                ).collect { result ->
                    when (result) {
                        is ApiResult.Success -> {
                            val response = result.data
                            android.util.Log.d(signUpTag, "========================================")
                            android.util.Log.d(signUpTag, "SignUp API Response received")
                            android.util.Log.d(signUpTag, "  success: ${response.success}")
                            android.util.Log.d(signUpTag, "  message: ${response.message}")
                            android.util.Log.d(signUpTag, "  gcode: ${response.gcode}")
                            android.util.Log.d(signUpTag, "  mcode: ${response.mcode}")
                            android.util.Log.d(signUpTag, "========================================")
                            
                            if (response.success) {
                                // Old 프로젝트: 회원가입 성공 후 trySignin() 호출
                                android.util.Log.d(signUpTag, "SignUp success - calling signIn API")
                                performSignInAfterSignUp()
                            } else {
                                setState { copy(isLoading = false) }
                                android.util.Log.e(signUpTag, "========================================")
                                android.util.Log.e(signUpTag, "SignUp API failed")
                                android.util.Log.e(signUpTag, "  message: ${response.message}")
                                android.util.Log.e(signUpTag, "  gcode: ${response.gcode}")
                                android.util.Log.e(signUpTag, "  mcode: ${response.mcode}")
                                android.util.Log.e(signUpTag, "========================================")
                                
                                // Old 프로젝트: gcode에 따라 다른 필드에 에러 표시
                                val gcode = response.gcode ?: 0
                                val errorMessage = response.message
                                
                                when (gcode) {
                                    88888 -> {
                                        // 욕설/부적절한 단어 필터링 (ERROR_88888)
                                        // Old 프로젝트: 회원가입 API에서도 gcode: 88888이 나올 수 있음
                                        // 닉네임 필드에 에러 표시하고 isBadWordsNickName = true로 설정
                                        val badWordsMessage = errorMessage ?: context.getString(net.ib.mn.R.string.bad_words)
                                        android.util.Log.e(signUpTag, "  -> Setting nickname error: $badWordsMessage")
                                        android.util.Log.e(signUpTag, "  -> Setting isBadWordsNickName = true")
                                        setState {
                                            copy(
                                                isLoading = false,
                                                nicknameError = badWordsMessage,
                                                isNicknameValid = false,
                                                isBadWordsNickName = true // Old 프로젝트: 회원가입 API에서도 gcode: 88888이 나오면 true로 설정
                                            )
                                        }
                                    }
                                    1011 -> {
                                        // 중복 닉네임 (ERROR_1011)
                                        // 닉네임 필드에 에러 표시
                                        val duplicateMessage = errorMessage ?: context.getString(net.ib.mn.R.string.error_1011)
                                        android.util.Log.e(signUpTag, "  -> Setting nickname error: $duplicateMessage")
                                        setState {
                                            copy(
                                                isLoading = false,
                                                nicknameError = duplicateMessage,
                                                isNicknameValid = false
                                            )
                                        }
                                    }
                                    1012 -> {
                                        // 추천인을 찾지 못함 (ERROR_1012)
                                        // 추천인 필드에 에러 표시
                                        val recommenderMessage = errorMessage ?: context.getString(net.ib.mn.R.string.error_1012)
                                        android.util.Log.e(signUpTag, "  -> Setting recommender error: $recommenderMessage")
                                        setState {
                                            copy(
                                                isLoading = false,
                                                recommenderError = recommenderMessage,
                                                isRecommenderValid = false
                                            )
                                        }
                                        // Toast로도 표시 (old 프로젝트 동일)
                                        setEffect {
                                            SignUpContract.Effect.ShowError(recommenderMessage)
                                        }
                                    }
                                    1013 -> {
                                        // 10분 이내에 두번 이상 가입할 수 없음 (ERROR_1013)
                                        // 다이얼로그로 표시
                                        val tooManyAttemptsMessage = errorMessage ?: context.getString(net.ib.mn.R.string.error_1013)
                                        android.util.Log.e(signUpTag, "  -> Showing dialog: $tooManyAttemptsMessage")
                                        setEffect {
                                            SignUpContract.Effect.ShowError(tooManyAttemptsMessage)
                                        }
                                    }
                                    else -> {
                                        // 그 외: 일반 에러 메시지 표시
                                        val defaultMessage = errorMessage ?: context.getString(net.ib.mn.R.string.error_abnormal_exception)
                                        android.util.Log.e(signUpTag, "  -> Showing error: $defaultMessage")
                                        setEffect {
                                            SignUpContract.Effect.ShowError(defaultMessage)
                                        }
                                    }
                                }
                            }
                        }
                        is ApiResult.Error -> {
                            setState { copy(isLoading = false) }
                            android.util.Log.e(signUpTag, "========================================")
                            android.util.Log.e(signUpTag, "SignUp API Error")
                            android.util.Log.e(signUpTag, "  message: ${result.message}")
                            android.util.Log.e(signUpTag, "  code: ${result.code}")
                            android.util.Log.e(signUpTag, "  exception: ${result.exception?.message}")
                            android.util.Log.e(signUpTag, "========================================")
                            setEffect {
                                SignUpContract.Effect.ShowError(result.message ?: context.getString(net.ib.mn.R.string.error_abnormal_exception))
                            }
                        }
                        is ApiResult.Loading -> {
                            android.util.Log.d(signUpTag, "SignUp API: Loading...")
                        }
                    }
                }
        } catch (e: Exception) {
            setState { copy(isLoading = false) }
            android.util.Log.e(signUpTag, "========================================")
            android.util.Log.e(signUpTag, "SignUp Exception", e)
            android.util.Log.e(signUpTag, "========================================")
            setEffect { SignUpContract.Effect.ShowError(e.message ?: context.getString(net.ib.mn.R.string.error_abnormal_exception)) }
        }
    }

    // ============================================================
    // Navigation
    // ============================================================

    /**
     * 회원가입 성공 후 로그인 API 호출 (Old 프로젝트의 trySignin() 로직)
     */
    private fun performSignInAfterSignUp() {
        viewModelScope.launch {
            try {
                val currentState = this@SignUpViewModel.currentState
                val email = currentState.email
                val password = currentState.password
                val domain = currentState.domain ?: Constants.DOMAIN_EMAIL
                val signUpTag = getSignUpTag(domain)

                android.util.Log.d(signUpTag, "========================================")
                android.util.Log.d(signUpTag, "performSignInAfterSignUp() called")
                android.util.Log.d(signUpTag, "  email: $email")
                android.util.Log.d(signUpTag, "  domain: $domain")
                android.util.Log.d(signUpTag, "========================================")

                // Device info
                val deviceId = deviceUtil.getDeviceUUID()
                val gmail = deviceUtil.getGmail()
                val deviceKey = preferencesManager.fcmToken.first() ?: ""

                android.util.Log.d(signUpTag, "========================================")
                android.util.Log.d(signUpTag, "SignIn API Parameters:")
                android.util.Log.d(signUpTag, "  domain: $domain")
                android.util.Log.d(signUpTag, "  email: $email")
                android.util.Log.d(signUpTag, "  passwd: ${password.take(20)}...")
                android.util.Log.d(signUpTag, "  push_key: $deviceKey")
                android.util.Log.d(signUpTag, "  gmail: $gmail")
                android.util.Log.d(signUpTag, "  device_id: $deviceId")
                android.util.Log.d(signUpTag, "  app_id: ${Constants.APP_ID}")
                android.util.Log.d(signUpTag, "========================================")

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
                            android.util.Log.d(signUpTag, "SignIn API: Loading...")
                        }
                        is ApiResult.Success -> {
                            val response = result.data
                            android.util.Log.d(signUpTag, "========================================")
                            android.util.Log.d(signUpTag, "SignIn API: Success")
                            android.util.Log.d(signUpTag, "  response.success: ${response.success}")
                            android.util.Log.d(signUpTag, "  response.data: ${response.data != null}")
                            android.util.Log.d(signUpTag, "  response.message: ${response.message}")
                            android.util.Log.d(signUpTag, "========================================")

                            if (response.success) {
                                android.util.Log.d(signUpTag, "========================================")
                                android.util.Log.d(signUpTag, "Login SUCCESS after signup")
                                android.util.Log.d(signUpTag, "========================================")

                                // 인증 정보 저장
                                if (response.data != null) {
                                    val userData = response.data
                                    val token = userData.token
                                    
                                    // 1. AuthInterceptor에 인증 정보 설정
                                    authInterceptor.setAuthCredentials(
                                        email = userData.email,
                                        domain = domain,
                                        token = token
                                    )
                                    
                                    // 2. DataStore에 로그인 정보 저장 (StartUpScreen에서 확인용)
                                    preferencesManager.setAccessToken(token)
                                    preferencesManager.setLoginDomain(domain)
                                    
                                    // 3. 사용자 정보 저장
                                    preferencesManager.setUserInfo(
                                        id = userData.userId,
                                        email = userData.email,
                                        username = userData.username,
                                        nickname = userData.nickname,
                                        profileImage = userData.profileImage,
                                        hearts = null,
                                        domain = domain
                                    )
                                    
                                    android.util.Log.d(signUpTag, "✓ Login credentials saved to DataStore")
                                    android.util.Log.d(signUpTag, "  - Email: ${userData.email}")
                                    android.util.Log.d(signUpTag, "  - Domain: $domain")
                                    android.util.Log.d(signUpTag, "  - Token: ${token.take(20)}...")
                                } else {
                                    // response.data가 null인 경우 (Old 프로젝트와 동일)
                                    // 사용자 정보는 이후에 별도로 가져옴 (StartUpScreen에서 getUserSelf 호출)
                                    android.util.Log.d(signUpTag, "========================================")
                                    android.util.Log.d(signUpTag, "User data is null - will be fetched separately")
                                    android.util.Log.d(signUpTag, "  email: $email")
                                    android.util.Log.d(signUpTag, "  domain: $domain")
                                    android.util.Log.d(signUpTag, "========================================")
                                    
                                    // 1. AuthInterceptor에 기본 정보 저장
                                    authInterceptor.setAuthCredentials(
                                        email = email,
                                        domain = domain,
                                        token = password
                                    )
                                    
                                    // 2. DataStore에 로그인 정보 저장 (StartUpScreen에서 확인용)
                                    preferencesManager.setAccessToken(password)
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
                                    
                                    android.util.Log.d(signUpTag, "✓ Basic login credentials saved to DataStore")
                                    android.util.Log.d(signUpTag, "  - Email: $email")
                                    android.util.Log.d(signUpTag, "  - Domain: $domain")
                                    android.util.Log.d(signUpTag, "  - Token: ${password.take(20)}...")
                                    android.util.Log.d(signUpTag, "  - Note: User info will be fetched in StartUpScreen")
                                }

                                setState { copy(isLoading = false) }
                                setEffect { SignUpContract.Effect.NavigateToMain }
                            } else {
                                android.util.Log.e(signUpTag, "========================================")
                                android.util.Log.e(signUpTag, "SignIn API: FAILED")
                                android.util.Log.e(signUpTag, "  response.success: ${response.success}")
                                android.util.Log.e(signUpTag, "  response.message: ${response.message}")
                                android.util.Log.e(signUpTag, "========================================")

                                setState { copy(isLoading = false) }
                                setEffect {
                                    SignUpContract.Effect.ShowError(
                                        response.message ?: context.getString(net.ib.mn.R.string.error_abnormal_exception)
                                    )
                                }
                            }
                        }
                        is ApiResult.Error -> {
                            android.util.Log.e(signUpTag, "========================================")
                            android.util.Log.e(signUpTag, "SignIn API: ERROR")
                            android.util.Log.e(signUpTag, "  error message: ${result.message}")
                            android.util.Log.e(signUpTag, "  error code: ${result.code}")
                            android.util.Log.e(signUpTag, "  exception: ${result.exception?.message}")
                            android.util.Log.e(signUpTag, "========================================")

                            setState { copy(isLoading = false) }
                            setEffect {
                                SignUpContract.Effect.ShowError(
                                    result.message ?: context.getString(net.ib.mn.R.string.error_abnormal_exception)
                                )
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                val currentState = this@SignUpViewModel.currentState
                val domain = currentState.domain ?: Constants.DOMAIN_EMAIL
                val signUpTag = getSignUpTag(domain)
                android.util.Log.e(signUpTag, "========================================")
                android.util.Log.e(signUpTag, "performSignInAfterSignUp() EXCEPTION", e)
                android.util.Log.e(signUpTag, "========================================")
                setState { copy(isLoading = false) }
                setEffect {
                    SignUpContract.Effect.ShowError(
                        e.message ?: context.getString(net.ib.mn.R.string.error_abnormal_exception)
                    )
                }
            }
        }
    }

    private fun goBack() {
        if (currentState.currentStep > 0) {
            setState { copy(currentStep = currentStep - 1) }
            setEffect { SignUpContract.Effect.NavigateToPreviousStep }
        }
    }
}
