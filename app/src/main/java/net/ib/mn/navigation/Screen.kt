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
     * @param initialTab 초기 탭 인덱스 (0: 홈, 1: 차트, 2: 커뮤니티, 3: 자유게시판, 4: 마이)
     * @param initialIdolId 초기 아이돌 ID (푸시 알림에서 커뮤니티로 이동 시)
     * @param initialCommunityTab 초기 커뮤니티 탭 (0: FEED, 1: FAN_TALK)
     * @param initialFreeBoardTagId 초기 자유게시판 태그 ID (푸시 알림에서 해당 카테고리로 이동 시)
     */
    @Serializable
    data class Main(
        val initialTab: Int = 3,
        val initialIdolId: Int? = null,
        val initialCommunityTab: Int? = null,
        val initialFreeBoardTagId: Int? = null
    ) : Screen()

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

    /**
     * Search 화면 - 검색 입력 화면
     * old 프로젝트의 SearchHistoryActivity에 해당
     */
    @Serializable
    data object Search : Screen()

    /**
     * SearchResult 화면 - 검색 결과 화면
     * old 프로젝트의 SearchResultActivity에 해당
     * @param keyword 검색 키워드
     * @param timestamp 화면 생성 시간 (같은 키워드라도 새로 진입 시 새 ViewModel 생성)
     */
    @Serializable
    data class SearchResult(
        val keyword: String,
        val timestamp: Long = System.currentTimeMillis()
    ) : Screen()

    /**
     * Community 화면 - 아이돌 커뮤니티 화면
     * 배너 클릭, 검색 결과에서 아이돌 클릭 등으로 진입
     * @param idolId 아이돌 ID
     * @param initialTab 초기 탭 (0: FEED, 1: FAN_TALK)
     * @param sortLatest 최신순 정렬 여부 (푸시 알림에서 진입 시 true)
     */
    @Serializable
    data class Community(
        val idolId: Int,
        val initialTab: Int = 0,
        val sortLatest: Boolean = false
    ) : Screen()

    /**
     * ArticleDetail 화면 - 게시글 상세 (댓글 포함)
     * @param articleId 게시글 ID
     * @param isFeed FEED 타입 여부 (하트 투표 표시)
     */
    @Serializable
    data class ArticleDetail(
        val articleId: String,
        val isFeed: Boolean = true
    ) : Screen()

    /**
     * Friend 화면 - 친구 목록 및 관리
     */
    @Serializable
    data object Friend : Screen()

    /**
     * FriendAdd 화면 - 뉴프렌즈 (새 친구 추가)
     */
    @Serializable
    data object FriendAdd : Screen()

    /**
     * FriendRequest 화면 - 친구 신청 관리
     * @param initialTab 초기 탭 인덱스 (0: 받은 요청, 1: 보낸 요청)
     */
    @Serializable
    data class FriendRequest(val initialTab: Int = 1) : Screen()

    /**
     * FriendDelete 화면 - 친구 삭제
     */
    @Serializable
    data object FriendDelete : Screen()
}
