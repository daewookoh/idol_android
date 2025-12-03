package net.ib.mn.navigation

import kotlinx.serialization.Serializable

/**
 * Navigation 3 화면 라우트 정의.
 *
 * Navigation 3 장점:
 * - 개발자가 백스택을 직접 소유하고 제어
 * - Compose 상태 기반의 반응형 네비게이션
 * - 타입 안전한 네비게이션 (data class 사용)
 * - 애니메이션을 개별 또는 전역적으로 커스터마이징 가능
 * - URL 인코딩/디코딩이 불필요
 */
sealed class Screen {

    /**
     * StartUp 화면 - 앱 초기 로딩 및 인증 체크
     * @param isEmailSignup 이메일 회원가입 후 이메일 인증이 필요한 경우 true
     */
    @Serializable
    data class StartUp(val isEmailSignup: Boolean = false) : Screen()

    /**
     * Login 화면 - 소셜 로그인 화면
     */
    @Serializable
    data object Login : Screen()

    /**
     * EmailLogin 화면 - 이메일 기반 로그인
     */
    @Serializable
    data object EmailLogin : Screen()

    /**
     * ForgotPassword 화면 - 비밀번호 재설정
     */
    @Serializable
    data object ForgotPassword : Screen()

    /**
     * SignUpPages 화면 - 회원가입
     * @param email SNS 로그인에서 받은 이메일 (null이면 일반 회원가입)
     * @param password SNS 로그인에서 받은 access token
     * @param displayName SNS 로그인에서 받은 표시 이름 (옵션)
     * @param domain 로그인 도메인 (kakao, google, line, facebook)
     * @param profileImageUrl 프로필 이미지 URL (옵션)
     */
    @Serializable
    data class SignUpPages(
        val email: String? = null,
        val password: String? = null,
        val displayName: String? = null,
        val domain: String? = null,
        val profileImageUrl: String? = null
    ) : Screen()

    /**
     * Main 화면 - 메인 앱 컨테이너
     */
    @Serializable
    data object Main : Screen()

    /**
     * WebView 화면 - 웹 콘텐츠 표시
     * @param url 로드할 URL
     * @param title AppBar 타이틀 (옵션)
     */
    @Serializable
    data class WebView(
        val url: String,
        val title: String? = null
    ) : Screen()

    /**
     * PostDetail 화면 - 게시글 상세 (댓글 포함)
     * @param postId 게시글 ID
     */
    @Serializable
    data class PostDetail(
        val postId: Int
    ) : Screen()

    /**
     * Awards 화면 - 어워즈
     */
    @Serializable
    data object Awards : Screen()
}
