package net.ib.mn.presentation.signup

import net.ib.mn.base.UiEffect
import net.ib.mn.base.UiIntent
import net.ib.mn.base.UiState

/**
 * 회원가입 플로우 MVI Contract
 *
 * old 프로젝트의 AgreementFragment + SignupFragment를 MVI 패턴으로 구현
 *
 * 플로우:
 * 1. Step 1: 약관동의 (AgreementScreen)
 * 2. Step 2: 회원가입 폼 (SignUpFormScreen)
 */
class SignUpContract {

    /**
     * UI State
     */
    data class State(
        val currentStep: Int = 0, // 0: 약관동의, 1: 회원가입 폼
        val isLoading: Boolean = false,
        val error: String? = null,
        val dialogMessage: String? = null, // 다이얼로그 메시지

        // 로그인 도메인 (email: 이메일 로그인, kakao/google/line/facebook: SNS 로그인)
        val domain: String = "email", // email, google, kakao, line, facebook

        // Step 1: 약관동의
        val agreeAll: Boolean = false,
        val agreeTerms: Boolean = false,
        val agreePrivacy: Boolean = false,
        val agreeAge: Boolean = false,

        // Step 2: 회원가입 폼
        val email: String = "",
        val emailError: String? = null,
        val isEmailValid: Boolean = false,

        val password: String = "",
        val passwordError: String? = null,
        val isPasswordValid: Boolean = false,
        val isPasswordFocused: Boolean = false, // 비밀번호 필드 포커스 상태

        val passwordConfirm: String = "",
        val passwordConfirmError: String? = null,
        val isPasswordConfirmValid: Boolean = false,
        val isPasswordConfirmFocused: Boolean = false, // 비밀번호 확인 필드 포커스 상태

        val nickname: String = "",
        val nicknameError: String? = null,
        val isNicknameValid: Boolean = false,
        val isBadWordsNickName: Boolean = false, // Old 프로젝트: gcode: 88888일 때 true

        val recommenderCode: String = "",
        val recommenderError: String? = null,
        val isRecommenderValid: Boolean = false,

        // 가입 가능 여부
        val canProceedStep1: Boolean = false,
        val canProceedStep2: Boolean = false
    ) : UiState

    /**
     * User Intent
     */
    sealed class Intent : UiIntent {
        // Step 1: 약관동의
        data class UpdateAgreeAll(val checked: Boolean) : Intent()
        data class UpdateAgreeTerms(val checked: Boolean) : Intent()
        data class UpdateAgreePrivacy(val checked: Boolean) : Intent()
        data class UpdateAgreeAge(val checked: Boolean) : Intent()
        object ShowTermsOfService : Intent()
        object ShowPrivacyPolicy : Intent()
        object ProceedToSignUpForm : Intent()
        object DismissDialog : Intent()

        // Step 2: 회원가입 폼
        data class InitializeFromNavigation(
            val email: String?,
            val password: String?,
            val displayName: String?,
            val domain: String?
        ) : Intent()
        data class UpdateEmail(val email: String) : Intent()
        data class UpdatePassword(val password: String) : Intent()
        data class UpdatePasswordConfirm(val passwordConfirm: String) : Intent()
        data class PasswordFocusChanged(val isFocused: Boolean) : Intent()
        data class PasswordConfirmFocusChanged(val isFocused: Boolean) : Intent()
        data class UpdateNickname(val nickname: String) : Intent()
        data class UpdateRecommenderCode(val code: String) : Intent()
        object ValidateEmail : Intent()
        object ValidateNickname : Intent()
        object ValidateRecommenderCode : Intent()
        object SignUp : Intent()
        object ConfirmEmailVerification : Intent() // 이메일 발송 확인 다이얼로그 확인 버튼 클릭 (old 프로젝트와 동일)

        // Navigation
        object GoBack : Intent()
    }

    /**
     * Side Effect
     */
    sealed class Effect : UiEffect {
        data class ShowToast(val message: String) : Effect()
        data class ShowError(val message: String) : Effect()
        data class ShowEmailVerificationDialog(val message: String) : Effect() // 이메일 발송 확인 다이얼로그 (old 프로젝트와 동일)
        object NavigateToTermsOfService : Effect()
        object NavigateToPrivacyPolicy : Effect()
        object NavigateToNextStep : Effect()
        object NavigateToPreviousStep : Effect()
        object NavigateToMain : Effect() // 회원가입 완료 후 메인으로 이동
        object NavigateToStartUp : Effect() // 이메일 발송 확인 다이얼로그 확인 후 StartUp으로 이동 (old 프로젝트와 동일)
    }
}
