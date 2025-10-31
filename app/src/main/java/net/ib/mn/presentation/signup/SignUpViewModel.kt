package net.ib.mn.presentation.signup

import android.content.Context
import android.util.Patterns
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.launch
import net.ib.mn.base.BaseViewModel
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

        // Navigation arguments (SNS 로그인에서 전달)
        const val ARG_EMAIL = "email"
        const val ARG_PASSWORD = "password"
        const val ARG_DISPLAY_NAME = "displayName"
        const val ARG_DOMAIN = "domain"
        const val ARG_PROFILE_IMAGE_URL = "profileImageUrl"
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
                !currentState.agreeAge -> context.getString(net.ib.mn.R.string.error_need_agree_age)
                else -> context.getString(net.ib.mn.R.string.error_need_agree_all)
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
                    context.getString(net.ib.mn.R.string.error_invalid_password_format)
                } else null,
                // 비밀번호 확인도 다시 검증
                isPasswordConfirmValid = passwordConfirm.isNotEmpty() && passwordConfirm == password,
                passwordConfirmError = if (passwordConfirm.isNotEmpty() && passwordConfirm != password) {
                    context.getString(net.ib.mn.R.string.error_password_not_match)
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
                    context.getString(net.ib.mn.R.string.error_password_not_match)
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
                nicknameError = if (nickname.isNotEmpty() && !isValid) {
                    context.getString(net.ib.mn.R.string.error_invalid_nickname_length)
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
                                    emailError = context.getString(net.ib.mn.R.string.error_email_already_used),
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
                                emailError = result.message ?: context.getString(net.ib.mn.R.string.error_email_check_failed),
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
                            val errorMessage = response.message ?: when (gcode) {
                                1011 -> context.getString(net.ib.mn.R.string.error_nickname_already_used) // 중복 닉네임 (ERROR_1011)
                                88888 -> context.getString(net.ib.mn.R.string.error_2090) // 금지어 (ERROR_88888) - old: R.string.bad_words
                                else -> context.getString(net.ib.mn.R.string.error_nickname_already_used)
                            }
                            
                            android.util.Log.d(TAG, "[validateNickname] CASE 1: success=false -> Nickname unavailable")
                            android.util.Log.d(TAG, "  - gcode: $gcode")
                            android.util.Log.d(TAG, "  - errorMessage: $errorMessage")
                            
                            setState {
                                android.util.Log.d(TAG, "[validateNickname] Setting state: isNicknameValid=false, canProceedStep2=false")
                                copy(
                                    nicknameError = errorMessage, // Old: editText.setError(responseMsg)
                                    isNicknameValid = false, // Old: isNickName = false
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
                                    android.util.Log.d(TAG, "[validateNickname] Setting state: isNicknameValid=true, canProceedStep2=$newCanProceedStep2")
                                    copy(
                                        nicknameError = null, // Old: editText.error = null
                                        isNicknameValid = true, // Old: isNickName = true
                                        canProceedStep2 = newCanProceedStep2 // Old: changeSingupBtnStatus() -> 버튼 활성화
                                    )
                                }
                                // Old 프로젝트: 토스트 없이 체크 아이콘만 표시
                            } else {
                                // CASE 2-2: success == true이지만 gcode != 0 -> 사용 불가 (이미 존재 등)
                                // 이 경우는 이론적으로 발생하지 않아야 하지만, 안전을 위해 처리
                                val errorMessage = response.message ?: when (gcode) {
                                    1011 -> context.getString(net.ib.mn.R.string.error_nickname_already_used) // 중복 닉네임
                                    88888 -> context.getString(net.ib.mn.R.string.error_2090) // 금지어
                                    else -> context.getString(net.ib.mn.R.string.error_nickname_already_used)
                                }
                                android.util.Log.w(TAG, "[validateNickname] CASE 2-2: success=true but gcode=$gcode -> Nickname unavailable (unexpected)")
                                android.util.Log.w(TAG, "  - errorMessage: $errorMessage")
                                
                                setState {
                                    android.util.Log.d(TAG, "[validateNickname] Setting state: isNicknameValid=false, canProceedStep2=false")
                                    copy(
                                        nicknameError = errorMessage,
                                        isNicknameValid = false,
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
                                nicknameError = result.message ?: context.getString(net.ib.mn.R.string.error_nickname_check_failed),
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
                                recommenderError = result.message ?: context.getString(net.ib.mn.R.string.error_check_input_fields),
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
            setEffect { SignUpContract.Effect.ShowError(context.getString(net.ib.mn.R.string.error_please_agree_terms)) }
            return
        }

        // 이메일 로그인인 경우 폼 검증
        if (currentState.domain == "email" && !currentState.canProceedStep2) {
            setEffect { SignUpContract.Effect.ShowError(context.getString(net.ib.mn.R.string.error_check_input_fields)) }
            return
        }

        viewModelScope.launch {
            setState { copy(isLoading = true) }

            try {
                signUpUseCase(
                    email = currentState.email,
                    password = currentState.password,
                    nickname = currentState.nickname,
                    domain = currentState.domain ?: Constants.DOMAIN_EMAIL,
                    recommenderCode = currentState.recommenderCode,
                    appId = Constants.APP_ID
                ).collect { result ->
                    when (result) {
                        is ApiResult.Success -> {
                            val response = result.data
                            if (response.success) {
                                // Old 프로젝트: 회원가입 성공 후 trySignin() 호출
                                android.util.Log.d(TAG, "SignUp success - calling signIn API")
                                performSignInAfterSignUp()
                            } else {
                                setState { copy(isLoading = false) }
                                setEffect {
                                    SignUpContract.Effect.ShowError(
                                        response.message ?: context.getString(net.ib.mn.R.string.error_signup_failed)
                                    )
                                }
                            }
                        }
                        is ApiResult.Error -> {
                            setState { copy(isLoading = false) }
                            setEffect {
                                SignUpContract.Effect.ShowError(result.message ?: context.getString(net.ib.mn.R.string.error_signup_failed))
                            }
                        }
                        is ApiResult.Loading -> {
                            // 로딩 중
                        }
                    }
                }
            } catch (e: Exception) {
                setState { copy(isLoading = false) }
                setEffect { SignUpContract.Effect.ShowError(e.message ?: context.getString(net.ib.mn.R.string.error_signup_failed)) }
            }
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

                android.util.Log.d(TAG, "========================================")
                android.util.Log.d(TAG, "performSignInAfterSignUp() called")
                android.util.Log.d(TAG, "  email: $email")
                android.util.Log.d(TAG, "  domain: $domain")
                android.util.Log.d(TAG, "========================================")

                // Device info
                val deviceId = deviceUtil.getDeviceUUID()
                val gmail = deviceUtil.getGmail()
                val deviceKey = preferencesManager.fcmToken.first() ?: ""

                android.util.Log.d(TAG, "========================================")
                android.util.Log.d(TAG, "SignIn API Parameters:")
                android.util.Log.d(TAG, "  domain: $domain")
                android.util.Log.d(TAG, "  email: $email")
                android.util.Log.d(TAG, "  passwd: ${password.take(20)}...")
                android.util.Log.d(TAG, "  push_key: $deviceKey")
                android.util.Log.d(TAG, "  gmail: $gmail")
                android.util.Log.d(TAG, "  device_id: $deviceId")
                android.util.Log.d(TAG, "  app_id: ${Constants.APP_ID}")
                android.util.Log.d(TAG, "========================================")

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
                            android.util.Log.d(TAG, "SignIn API: Loading...")
                        }
                        is ApiResult.Success -> {
                            val response = result.data
                            android.util.Log.d(TAG, "========================================")
                            android.util.Log.d(TAG, "SignIn API: Success")
                            android.util.Log.d(TAG, "  response.success: ${response.success}")
                            android.util.Log.d(TAG, "  response.data: ${response.data != null}")
                            android.util.Log.d(TAG, "  response.message: ${response.message}")
                            android.util.Log.d(TAG, "========================================")

                            if (response.success) {
                                android.util.Log.d(TAG, "========================================")
                                android.util.Log.d(TAG, "Login SUCCESS after signup")
                                android.util.Log.d(TAG, "========================================")

                                // 인증 정보 저장
                                if (response.data != null) {
                                    val userData = response.data
                                    authInterceptor.setAuthCredentials(
                                        email = userData.email,
                                        domain = domain,
                                        token = userData.token
                                    )
                                    preferencesManager.setUserInfo(
                                        id = userData.userId,
                                        email = userData.email,
                                        username = userData.username,
                                        nickname = userData.nickname,
                                        profileImage = userData.profileImage,
                                        hearts = null,
                                        domain = domain
                                    )
                                } else {
                                    // response.data가 null인 경우 기본 정보만 저장
                                    authInterceptor.setAuthCredentials(
                                        email = email,
                                        domain = domain,
                                        token = password
                                    )
                                }

                                setState { copy(isLoading = false) }
                                setEffect { SignUpContract.Effect.NavigateToMain }
                            } else {
                                android.util.Log.e(TAG, "========================================")
                                android.util.Log.e(TAG, "SignIn API: FAILED")
                                android.util.Log.e(TAG, "  response.success: ${response.success}")
                                android.util.Log.e(TAG, "  response.message: ${response.message}")
                                android.util.Log.e(TAG, "========================================")

                                setState { copy(isLoading = false) }
                                setEffect {
                                    SignUpContract.Effect.ShowError(
                                        response.message ?: context.getString(net.ib.mn.R.string.error_signup_failed)
                                    )
                                }
                            }
                        }
                        is ApiResult.Error -> {
                            android.util.Log.e(TAG, "========================================")
                            android.util.Log.e(TAG, "SignIn API: ERROR")
                            android.util.Log.e(TAG, "  error message: ${result.message}")
                            android.util.Log.e(TAG, "========================================")

                            setState { copy(isLoading = false) }
                            setEffect {
                                SignUpContract.Effect.ShowError(
                                    result.message ?: context.getString(net.ib.mn.R.string.error_signup_failed)
                                )
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e(TAG, "performSignInAfterSignUp() EXCEPTION", e)
                setState { copy(isLoading = false) }
                setEffect {
                    SignUpContract.Effect.ShowError(
                        e.message ?: context.getString(net.ib.mn.R.string.error_signup_failed)
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
