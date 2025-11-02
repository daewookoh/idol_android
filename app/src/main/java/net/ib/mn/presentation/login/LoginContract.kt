package net.ib.mn.presentation.login

import net.ib.mn.base.UiEffect
import net.ib.mn.base.UiIntent
import net.ib.mn.base.UiState

/**
 * Login 화면의 MVI Contract.
 * old 프로젝트의 AuthActivity + SigninFragment 로직을 MVI 패턴으로 구현.
 */
class LoginContract {

    /**
     * UI State for Login screen.
     *
     * @property isLoading 로딩 중 여부
     * @property error 에러 메시지
     * @property loginType 현재 진행 중인 로그인 타입
     */
    data class State(
        val isLoading: Boolean = false,
        val error: String? = null,
        val loginType: LoginType? = null
    ) : UiState

    /**
     * 로그인 타입.
     */
    enum class LoginType {
        KAKAO,
        GOOGLE,
        LINE,
        FACEBOOK,
        EMAIL
    }

    /**
     * User intents (사용자 액션).
     */
    sealed class Intent : UiIntent {
        /**
         * Kakao 로그인.
         */
        data object LoginWithKakao : Intent()

        /**
         * Google 로그인.
         */
        data object LoginWithGoogle : Intent()

        /**
         * Line 로그인.
         */
        data object LoginWithLine : Intent()

        /**
         * Facebook 로그인.
         */
        data object LoginWithFacebook : Intent()

        /**
         * Email 로그인 화면으로 이동.
         */
        data object NavigateToEmailLogin : Intent()
    }

    /**
     * Side effects (일회성 이벤트).
     */
    sealed class Effect : UiEffect {
        /**
         * 로그인 성공 - StartUp 화면으로 이동 (old 프로젝트: StartupActivity로 이동).
         * StartUp 화면에서 getUserSelf API를 호출하여 전체 사용자 정보를 가져옴.
         */
        data object NavigateToStartUp : Effect()

        /**
         * 로그인 성공 - 메인 화면으로 이동.
         */
        data object NavigateToMain : Effect()

        /**
         * Email 로그인 화면으로 이동.
         */
        data object NavigateToEmailLogin : Effect()

        /**
         * 소셜 로그인 프로세스 시작 (Activity Result 필요).
         */
        data class StartSocialLogin(val loginType: LoginType) : Effect()

        /**
         * 회원가입 화면으로 이동 (SNS 로그인 후 신규 회원인 경우).
         * 
         * @param email SNS 로그인에서 받은 이메일
         * @param password SNS 로그인에서 받은 access token (비밀번호로 사용)
         * @param displayName SNS 로그인에서 받은 표시 이름 (옵션)
         * @param domain 로그인 도메인 (kakao, google, line, facebook)
         * @param profileImageUrl 프로필 이미지 URL (옵션)
         */
        data class NavigateToSignUp(
            val email: String,
            val password: String,
            val displayName: String? = null,
            val domain: String,
            val profileImageUrl: String? = null
        ) : Effect()

        /**
         * 에러 표시.
         */
        data class ShowError(val message: String) : Effect()

        /**
         * Toast 메시지 표시.
         */
        data class ShowToast(val message: String) : Effect()
    }
}
