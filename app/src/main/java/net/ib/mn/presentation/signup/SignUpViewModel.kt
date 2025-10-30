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
import net.ib.mn.domain.usecase.SignUpUseCase
import net.ib.mn.domain.usecase.ValidateUserUseCase
import net.ib.mn.util.Constants
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
    private val preferencesManager: PreferencesManager,
    private val authInterceptor: AuthInterceptor
) : BaseViewModel<SignUpContract.State, SignUpContract.Intent, SignUpContract.Effect>() {

    companion object {
        private const val TAG = "SignUpViewModel"

        // Navigation arguments (SNS 로그인에서 전달)
        const val ARG_EMAIL = "email"
        const val ARG_PASSWORD = "password"
        const val ARG_DISPLAY_NAME = "displayName"
        const val ARG_DOMAIN = "domain"
    }

    override fun createInitialState(): SignUpContract.State {
        val email = savedStateHandle?.get<String>(ARG_EMAIL)
        val password = savedStateHandle?.get<String>(ARG_PASSWORD)
        val displayName = savedStateHandle?.get<String>(ARG_DISPLAY_NAME)
        val domain = savedStateHandle?.get<String>(ARG_DOMAIN)

        return SignUpContract.State(
            preFilledEmail = email,
            preFilledPassword = password,
            preFilledDisplayName = displayName,
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
        when (intent) {
            // Step 1: 약관동의
            is SignUpContract.Intent.UpdateAgreeAll -> updateAgreeAll(intent.checked)
            is SignUpContract.Intent.UpdateAgreeTerms -> updateAgreeTerms(intent.checked)
            is SignUpContract.Intent.UpdateAgreePrivacy -> updateAgreePrivacy(intent.checked)
            is SignUpContract.Intent.UpdateAgreeAge -> updateAgreeAge(intent.checked)
            is SignUpContract.Intent.ShowTermsOfService -> setEffect { SignUpContract.Effect.NavigateToTermsOfService }
            is SignUpContract.Intent.ShowPrivacyPolicy -> setEffect { SignUpContract.Effect.NavigateToPrivacyPolicy }
            is SignUpContract.Intent.ProceedToSignUpForm -> proceedToSignUpForm()

            // Step 2: 회원가입 폼
            is SignUpContract.Intent.UpdateEmail -> updateEmail(intent.email)
            is SignUpContract.Intent.UpdatePassword -> updatePassword(intent.password)
            is SignUpContract.Intent.UpdatePasswordConfirm -> updatePasswordConfirm(intent.passwordConfirm)
            is SignUpContract.Intent.UpdateNickname -> updateNickname(intent.nickname)
            is SignUpContract.Intent.UpdateRecommenderCode -> updateRecommenderCode(intent.code)
            is SignUpContract.Intent.ValidateEmail -> validateEmail()
            is SignUpContract.Intent.ValidateNickname -> validateNickname()
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
            setEffect { SignUpContract.Effect.ShowError(errorMessage) }
            return
        }

        // SNS 로그인인 경우 바로 회원가입 처리 (old 프로젝트처럼)
        if (currentState.domain != null) {
            signUp()
        } else {
            // 일반 이메일 가입인 경우 회원가입 폼으로 이동
            setState { copy(currentStep = 1) }
            setEffect { SignUpContract.Effect.NavigateToNextStep }
        }
    }

    // ============================================================
    // Step 2: 회원가입 폼
    // ============================================================

    private fun updateEmail(email: String) {
        setState {
            val isValid = Patterns.EMAIL_ADDRESS.matcher(email).matches()
            copy(
                email = email,
                isEmailValid = isValid,
                emailError = if (email.isNotEmpty() && !isValid) context.getString(net.ib.mn.R.string.invalid_format_email) else null,
                canProceedStep2 = isValid && isPasswordValid && isPasswordConfirmValid && isNicknameValid
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
                canProceedStep2 = isValid &&
                        (passwordConfirm.isEmpty() || passwordConfirm == password) &&
                        isEmailValid && isNicknameValid
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
                canProceedStep2 = isValid && isPasswordValid && isEmailValid && isNicknameValid
            )
        }
    }

    private fun updateNickname(nickname: String) {
        setState {
            // 닉네임 규칙: 2~10자 (old 프로젝트 규칙)
            val isValid = nickname.length in 2..10
            copy(
                nickname = nickname,
                isNicknameValid = isValid,
                nicknameError = if (nickname.isNotEmpty() && !isValid) {
                    context.getString(net.ib.mn.R.string.error_invalid_nickname_length)
                } else null,
                canProceedStep2 = isValid && isPasswordValid && isPasswordConfirmValid && isEmailValid
            )
        }
    }

    private fun updateRecommenderCode(code: String) {
        setState { copy(recommenderCode = code) }
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
                            setState {
                                copy(
                                    emailError = null,
                                    isEmailValid = true
                                )
                            }
                            setEffect { SignUpContract.Effect.ShowToast(context.getString(net.ib.mn.R.string.success_email_available)) }
                        }
                    }
                    is ApiResult.Error -> {
                        setEffect { SignUpContract.Effect.ShowError(result.message ?: context.getString(net.ib.mn.R.string.error_email_check_failed)) }
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
        if (nickname.isEmpty() || nickname.length < 2) return

        viewModelScope.launch {
            validateUserUseCase(
                type = "username",
                value = nickname,
                appId = Constants.APP_ID
            ).collect { result ->
                when (result) {
                    is ApiResult.Success -> {
                        val response = result.data
                        if (response.success) {
                            // 이미 존재하는 닉네임
                            setState {
                                copy(
                                    nicknameError = context.getString(net.ib.mn.R.string.error_nickname_already_used),
                                    isNicknameValid = false,
                                    canProceedStep2 = false
                                )
                            }
                        } else {
                            // 사용 가능한 닉네임
                            setState {
                                copy(
                                    nicknameError = null,
                                    isNicknameValid = true
                                )
                            }
                            setEffect { SignUpContract.Effect.ShowToast(context.getString(net.ib.mn.R.string.success_nickname_available)) }
                        }
                    }
                    is ApiResult.Error -> {
                        setEffect { SignUpContract.Effect.ShowError(result.message ?: context.getString(net.ib.mn.R.string.error_nickname_check_failed)) }
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

        // SNS 로그인이 아닌 경우 폼 검증
        if (currentState.domain == null && !currentState.canProceedStep2) {
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
                                // 회원가입 성공 후 토큰 저장 (old 프로젝트 방식)
                                preferencesManager.setAccessToken(currentState.password)
                                authInterceptor.setToken(currentState.password)

                                android.util.Log.d(TAG, "SignUp success")
                                setState { copy(isLoading = false) }
                                setEffect { SignUpContract.Effect.NavigateToMain }
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

    private fun goBack() {
        if (currentState.currentStep > 0) {
            setState { copy(currentStep = currentStep - 1) }
            setEffect { SignUpContract.Effect.NavigateToPreviousStep }
        }
    }
}
