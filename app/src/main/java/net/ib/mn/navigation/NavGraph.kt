package net.ib.mn.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.ui.NavDisplay
import net.ib.mn.presentation.awards.AwardsScreen
import net.ib.mn.presentation.community.CommunityScreen
import net.ib.mn.presentation.friend.FriendScreen
import net.ib.mn.presentation.friend.add.FriendAddScreen
import net.ib.mn.presentation.friend.waiting.FriendWaitingScreen
import net.ib.mn.presentation.friend.delete.FriendDeleteScreen
import net.ib.mn.presentation.search.SearchScreen
import net.ib.mn.presentation.search.result.SearchResultScreen
import net.ib.mn.presentation.login.EmailLoginScreen
import net.ib.mn.presentation.login.LoginScreen
import net.ib.mn.presentation.login.PasswordResetScreen
import net.ib.mn.presentation.main.MainScreen
import net.ib.mn.presentation.signup.SignUpPagesScreen
import net.ib.mn.presentation.startup.StartUpScreen
import net.ib.mn.presentation.webview.WebViewScreen
import net.ib.mn.presentation.overlay.articledetail.ArticleDetailWrapper
import net.ib.mn.util.logD

/**
 * CompositionLocal로 AppNavigator를 하위 컴포저블에 전달.
 * 화면에서 navigator를 직접 주입받지 않고도 네비게이션 가능.
 */
val LocalAppNavigator = compositionLocalOf<AppNavigator> {
    error("No AppNavigator provided")
}

/**
 * Navigation 3 기반 앱 네비게이션 그래프.
 *
 * Navigation 2 대비 개선점:
 * 1. 백스택 직접 제어 - 개발자가 SnapshotStateList<Screen>으로 백스택을 완전히 소유
 * 2. 반응형 UI - Compose 상태 변경이 자동으로 UI에 반영
 * 3. 타입 안전성 - data class로 파라미터 전달 (URL 인코딩 불필요)
 * 4. 유연한 애니메이션 - 화면별 또는 전역 애니메이션 설정 가능
 * 5. 모듈화된 구조 - NavDisplay, SceneStrategy 등 컴포넌트 교체 가능
 */
@Composable
fun NavGraph(
    navigator: AppNavigator
) {
    // navigator를 CompositionLocal로 제공
    CompositionLocalProvider(LocalAppNavigator provides navigator) {
        NavDisplay(
            backStack = navigator.backStack,
            onBack = { navigator.popBackStack() },
            entryProvider = { screen ->
                when (screen) {
                    // StartUp 화면
                    is Screen.StartUp -> NavEntry(screen) {
                        StartUpScreen(
                            onNavigateToMain = {
                                navigator.navigateAndClearStack(Screen.Main())
                            },
                            onNavigateToLogin = {
                                // 이메일 회원가입 후인 경우 EmailLogin으로 이동
                                if (screen.isEmailSignup) {
                                    navigator.navigateAndClearStack(Screen.EmailLogin)
                                } else {
                                    navigator.navigateAndClearStack(Screen.Login)
                                }
                            }
                        )
                    }

                    // Login 화면
                    is Screen.Login -> NavEntry(screen) {
                        LoginScreen(
                            onNavigateToMain = {
                                // 로그인 성공 시 StartUp으로 이동하여 사용자 정보를 가져온 후 Main으로 이동
                                navigator.navigateAndClearStack(Screen.StartUp())
                            },
                            onNavigateToEmailLogin = {
                                navigator.navigate(Screen.EmailLogin)
                            },
                            onNavigateToSignUp = { email, password, displayName, domain, profileImageUrl ->
                                navigator.navigate(
                                    Screen.SignUpPages(
                                        email = email,
                                        password = password,
                                        displayName = displayName,
                                        domain = domain,
                                        profileImageUrl = profileImageUrl
                                    )
                                )
                            }
                        )
                    }

                    // EmailLogin 화면
                    is Screen.EmailLogin -> NavEntry(screen) {
                        EmailLoginScreen(
                            onNavigateToStartUp = {
                                navigator.navigateAndClearStack(Screen.StartUp())
                            },
                            onNavigateToMain = {
                                navigator.navigateAndClearStack(Screen.StartUp())
                            },
                            onNavigateToSignUp = {
                                navigator.navigate(Screen.SignUpPages())
                            },
                            onNavigateToForgotId = {
                                // NOTE: 아이디 찾기 화면 미구현
                            },
                            onNavigateToForgotPassword = {
                                navigator.navigate(Screen.ForgotPassword)
                            },
                            onNavigateBack = {
                                navigator.popBackStack()
                            }
                        )
                    }

                    // ForgotPassword 화면
                    is Screen.ForgotPassword -> NavEntry(screen) {
                        PasswordResetScreen(
                            onNavigateBack = {
                                navigator.popBackStack()
                            }
                        )
                    }

                    // SignUpPages 화면
                    is Screen.SignUpPages -> NavEntry(screen) {
                        SignUpPagesScreen(
                            navigator = navigator,
                            email = screen.email,
                            password = screen.password,
                            displayName = screen.displayName,
                            domain = screen.domain,
                            onSignUpComplete = {
                                // 회원가입 완료 시 StartUp으로 이동 (이메일 인증 필요)
                                navigator.navigateAndClearStack(Screen.StartUp(isEmailSignup = true))
                            },
                            onNavigateBack = {
                                navigator.popBackStack()
                            }
                        )
                    }

                    // Main 화면
                    is Screen.Main -> NavEntry(screen) {
                        MainScreen(
                            initialTab = screen.initialTab,
                            initialIdolId = screen.initialIdolId,
                            initialCommunityTab = screen.initialCommunityTab,
                            initialFreeBoardTagId = screen.initialFreeBoardTagId,
                            onLogout = {
                                // 로그아웃 시 StartUp으로 이동 (모든 네비게이션 스택 제거)
                                navigator.navigateAndClearStack(Screen.StartUp())
                            }
                        )
                    }

                    // WebView 화면
                    is Screen.WebView -> NavEntry(screen) {
                        WebViewScreen(
                            url = screen.url,
                            title = screen.title,
                            onNavigateBack = {
                                navigator.popBackStack()
                            }
                        )
                    }

                    // PostDetail 화면 - CommunityScreen 내부에서 직접 표시하므로 여기서는 사용하지 않음
                    is Screen.PostDetail -> NavEntry(screen) {
                        // 빈 화면 (실제로는 CommunityScreen에서 AnimatedVisibility로 표시)
                    }

                    // Awards 화면
                    is Screen.Awards -> NavEntry(screen) {
                        AwardsScreen(
                            onNavigateBack = {
                                navigator.popBackStack()
                            }
                        )
                    }

                    // ArticleWrite 화면은 오버레이로만 열림 (NavGraph에서 제거됨)
                    // ArticleDetailScreen, CommunityScreen 등에서 직접 오버레이로 표시

                    // Search 화면 (검색 입력)
                    // Navigation 3: LocalAppNavigator를 통해 Screen에서 직접 네비게이션 처리
                    is Screen.Search -> NavEntry(screen) {
                        SearchScreen()
                    }

                    // SearchResult 화면 (검색 결과)
                    // Navigation 3: LocalAppNavigator를 통해 Screen에서 직접 네비게이션 처리
                    is Screen.SearchResult -> NavEntry(screen) {
                        SearchResultScreen(
                            keyword = screen.keyword,
                            timestamp = screen.timestamp
                        )
                    }

                    // Community 화면 (독립적인 커뮤니티 화면)
                    // 배너 클릭, 검색 결과에서 아이돌 클릭 등으로 진입
                    is Screen.Community -> NavEntry(screen) {
                        CommunityScreen(
                            idolId = screen.idolId,
                            initialTab = screen.initialTab,
                            sortLatest = screen.sortLatest
                        )
                    }

                    // ArticleDetail 화면 (게시글 상세)
                    is Screen.ArticleDetail -> NavEntry(screen) {
                        ArticleDetailWrapper(
                            articleId = screen.articleId,
                            isFeed = screen.isFeed,
                            onBackClick = { navigator.popBackStack() }
                        )
                    }

                    // Friend 화면 (친구 목록)
                    is Screen.Friend -> NavEntry(screen) {
                        FriendScreen()
                    }

                    // FriendAdd 화면 (뉴프렌즈)
                    is Screen.FriendAdd -> NavEntry(screen) {
                        FriendAddScreen()
                    }

                    // FriendWaiting 화면 (친구 신청 관리)
                    is Screen.FriendWaiting -> NavEntry(screen) {
                        FriendWaitingScreen()
                    }

                    // FriendDelete 화면 (친구 삭제)
                    is Screen.FriendDelete -> NavEntry(screen) {
                        FriendDeleteScreen()
                    }
                }
            }
        )
    }
}
