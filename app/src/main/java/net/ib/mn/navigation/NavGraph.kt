package net.ib.mn.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import net.ib.mn.presentation.login.EmailLoginScreen
import net.ib.mn.presentation.login.LoginScreen
import net.ib.mn.presentation.login.PasswordResetScreen
import net.ib.mn.presentation.main.MainScreen
import net.ib.mn.presentation.signup.SignUpPagesScreen
import net.ib.mn.presentation.startup.StartUpScreen
import net.ib.mn.presentation.webview.WebViewScreen
import net.ib.mn.util.ToastUtil
import java.net.URLDecoder
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

/**
 * 앱의 네비게이션 그래프.
 * Splash -> StartUp -> Login (guest) / Main (logged in) 순서로 화면 전환.
 *
 * 화면 전환 애니메이션:
 * - 새 화면 진입: 우측에서 슬라이드 인
 * - 이전 화면 퇴장: 좌측으로 슬라이드 아웃
 * - 뒤로가기 진입: 좌측에서 슬라이드 인
 * - 뒤로가기 퇴장: 우측으로 슬라이드 아웃
 * - 지속 시간: 600ms (0.6초)
 */
@Composable
fun NavGraph(
    navController: NavHostController,
    startDestination: String = Screen.StartUp.route
) {
    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        // StartUp 화면
        composable(
            route = Screen.StartUp.route,
            enterTransition = {
                slideIntoContainer(
                    towards = AnimatedContentTransitionScope.SlideDirection.Left,
                    animationSpec = tween(600)
                )
            },
        ) {
            StartUpScreen(
                onNavigateToMain = {
                    navController.navigate(Screen.Main.route) {
                        popUpTo(Screen.StartUp.route) { inclusive = true }
                    }
                },
                onNavigateToLogin = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.StartUp.route) { inclusive = true }
                    }
                }
            )
        }

        // Login 화면
        composable(
            route = Screen.Login.route,
//            enterTransition = {
//                slideIntoContainer(
//                    towards = AnimatedContentTransitionScope.SlideDirection.Up,
//                    animationSpec = tween(600)
//                )
//            },
//            exitTransition = {
//                slideOutOfContainer(
//                    towards = AnimatedContentTransitionScope.SlideDirection.Down,
//                    animationSpec = tween(600)
//                )
//            }
        ) {
            LoginScreen(
                onNavigateToMain = {
                    // Old 프로젝트: afterSignin()에서 StartupActivity로 이동하고 finish() 호출
                    // 로그인 성공 시 StartUp으로 이동하여 사용자 정보를 가져온 후 Main으로 이동
                    navController.navigate(Screen.StartUp.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                },
                onNavigateToEmailLogin = {
                    navController.navigate(Screen.EmailLogin.route)
                },
                onNavigateToSignUp = { email, password, displayName, domain, profileImageUrl ->
                    navController.navigate(
                        Screen.SignUpPages.createRoute(
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

        // Email Login 화면
        composable(
            route = Screen.EmailLogin.route,
//            enterTransition = {
//                // 위에서 아래로 슬라이드 인
//                slideIntoContainer(
//                    towards = AnimatedContentTransitionScope.SlideDirection.Down,
//                    animationSpec = tween(600)
//                )
//            },
//            exitTransition = {
//                // 아래로 슬라이드 아웃
//                slideOutOfContainer(
//                    towards = AnimatedContentTransitionScope.SlideDirection.Down,
//                    animationSpec = tween(600)
//                )
//            },
            popEnterTransition = {
                // 뒤로가기 시 위로 슬라이드 인
                slideIntoContainer(
                    towards = AnimatedContentTransitionScope.SlideDirection.Up,
                    animationSpec = tween(600)
                )
            },
            popExitTransition = {
                // 뒤로가기 시 위로 슬라이드 아웃
                slideOutOfContainer(
                    towards = AnimatedContentTransitionScope.SlideDirection.Up,
                    animationSpec = tween(600)
                )
            }
        ) {
            EmailLoginScreen(
                onNavigateToMain = {
                    // 로그인 성공 시 StartUp으로 이동 (old 프로젝트의 StartupActivity와 동일)
                    // StartUpScreen에서 API 호출 후 Main으로 이동
                    navController.navigate(Screen.StartUp.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                },
                onNavigateToSignUp = {
                    navController.navigate(Screen.SignUpPages.route)
                },
                onNavigateToForgotId = {
                    // NOTE: 아이디 찾기 화면 미구현 - 구현 시 Screen.ForgotId.route로 navigate
                    ToastUtil.show(
                        navController.context,
                        "아이디 찾기 화면은 추후 구현 예정입니다"
                    )
                },
                onNavigateToForgotPassword = {
                    navController.navigate(Screen.ForgotPassword.route)
                },
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        // Password Reset 화면 (비밀번호 찾기)
        composable(
            route = Screen.ForgotPassword.route,
            enterTransition = {
                slideIntoContainer(
                    towards = AnimatedContentTransitionScope.SlideDirection.Left,
                    animationSpec = tween(600)
                )
            },
            exitTransition = {
                slideOutOfContainer(
                    towards = AnimatedContentTransitionScope.SlideDirection.Right,
                    animationSpec = tween(600)
                )
            },
            popEnterTransition = {
                slideIntoContainer(
                    towards = AnimatedContentTransitionScope.SlideDirection.Right,
                    animationSpec = tween(600)
                )
            },
            popExitTransition = {
                slideOutOfContainer(
                    towards = AnimatedContentTransitionScope.SlideDirection.Left,
                    animationSpec = tween(600)
                )
            }
        ) {
            PasswordResetScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        // SignUp Pages 화면 (파라미터 없이 일반 회원가입)
        composable(
            route = Screen.SignUpPages.route
        ) {
            SignUpPagesScreen(
                navController = navController,
                email = null,
                password = null,
                displayName = null,
                domain = null,
                onSignUpComplete = {
                    // 회원가입 완료 시 StartUp으로 이동 (API 호출 후 Main으로 이동)
                    navController.navigate(Screen.StartUp.route) {
                        popUpTo(0) { inclusive = true }
                    }
                },
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        // SignUp Pages 화면 (파라미터 포함 - SNS 로그인 후 신규 회원)
        composable(
            route = Screen.SignUpPages.routeWithArgs,
            arguments = listOf(
                navArgument("email") { type = NavType.StringType },
                navArgument("password") { type = NavType.StringType },
                navArgument("displayName") {
                    type = NavType.StringType
                    nullable = true
                },
                navArgument("domain") { type = NavType.StringType },
                navArgument("profileImageUrl") {
                    type = NavType.StringType
                    nullable = true
                }
            )
        ) { backStackEntry ->
            val encodedEmail = backStackEntry.arguments?.getString("email") ?: ""
            val encodedPassword = backStackEntry.arguments?.getString("password") ?: ""
            val encodedDisplayName = backStackEntry.arguments?.getString("displayName")
            val encodedDomain = backStackEntry.arguments?.getString("domain") ?: ""
            val encodedProfileImageUrl = backStackEntry.arguments?.getString("profileImageUrl")
            
            val email = URLDecoder.decode(encodedEmail, StandardCharsets.UTF_8.toString())
            val password = URLDecoder.decode(encodedPassword, StandardCharsets.UTF_8.toString())
            val displayName = encodedDisplayName?.let {
                URLDecoder.decode(it, StandardCharsets.UTF_8.toString())
            }
            val domain = URLDecoder.decode(encodedDomain, StandardCharsets.UTF_8.toString())
            val profileImageUrl = encodedProfileImageUrl?.let {
                URLDecoder.decode(it, StandardCharsets.UTF_8.toString())
            }
            
            SignUpPagesScreen(
                navController = navController,
                email = email.takeIf { it.isNotEmpty() },
                password = password.takeIf { it.isNotEmpty() },
                displayName = displayName,
                domain = domain.takeIf { it.isNotEmpty() },
                onSignUpComplete = {
                    // 회원가입 완료 시 StartUp으로 이동 (API 호출 후 Main으로 이동)
                    // old 프로젝트: afterSignin()에서 StartupActivity로 이동하고 finish() 호출
                    // 모든 이전 화면을 제거하고 StartUp으로 이동
                    navController.navigate(Screen.StartUp.route) {
                        popUpTo(0) { inclusive = true }
                    }
                },
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        // Main 화면
        composable(
            route = Screen.Main.route,
            enterTransition = {
                // 우측에서 슬라이드 인 (Android 기본 애니메이션)
                slideIntoContainer(
                    towards = AnimatedContentTransitionScope.SlideDirection.Left,
                    animationSpec = tween(600)
                )
            },
            exitTransition = {
                // 좌측으로 슬라이드 아웃
                slideOutOfContainer(
                    towards = AnimatedContentTransitionScope.SlideDirection.Left,
                    animationSpec = tween(600)
                )
            },
            popEnterTransition = {
                // 뒤로가기 시 좌측에서 슬라이드 인
                slideIntoContainer(
                    towards = AnimatedContentTransitionScope.SlideDirection.Right,
                    animationSpec = tween(600)
                )
            },
            popExitTransition = {
                // 뒤로가기 시 우측으로 슬라이드 아웃
                slideOutOfContainer(
                    towards = AnimatedContentTransitionScope.SlideDirection.Right,
                    animationSpec = tween(600)
                )
            }
        ) {
            MainScreen(
                onLogout = {
                    // 로그아웃 시 StartUp으로 이동 (모든 네비게이션 스택 제거)
                    navController.navigate(Screen.StartUp.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }

        // WebView 화면
        composable(
            route = Screen.WebView.route,
            arguments = listOf(
                navArgument("url") { type = NavType.StringType },
                navArgument("title") {
                    type = NavType.StringType
                    nullable = true
                }
            ),
            enterTransition = {
                slideIntoContainer(
                    towards = AnimatedContentTransitionScope.SlideDirection.Left,
                    animationSpec = tween(600)
                )
            },
            exitTransition = {
                slideOutOfContainer(
                    towards = AnimatedContentTransitionScope.SlideDirection.Left,
                    animationSpec = tween(600)
                )
            },
            popEnterTransition = {
                slideIntoContainer(
                    towards = AnimatedContentTransitionScope.SlideDirection.Right,
                    animationSpec = tween(600)
                )
            },
            popExitTransition = {
                slideOutOfContainer(
                    towards = AnimatedContentTransitionScope.SlideDirection.Right,
                    animationSpec = tween(600)
                )
            }
        ) { backStackEntry ->
            val encodedUrl = backStackEntry.arguments?.getString("url") ?: ""
            val url = URLDecoder.decode(encodedUrl, StandardCharsets.UTF_8.toString())
            val encodedTitle = backStackEntry.arguments?.getString("title")
            val title = encodedTitle?.let {
                URLDecoder.decode(it, StandardCharsets.UTF_8.toString())
            }

            WebViewScreen(
                url = url,
                title = title,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
    }
}

/**
 * 화면 라우트 정의.
 */
sealed class Screen(val route: String) {
    data object StartUp : Screen("startup")
    data object Login : Screen("login")
    data object EmailLogin : Screen("email_login")
    data object ForgotPassword : Screen("forgot_password")
    data object SignUpPages : Screen("signup_pages") {
        // 파라미터 없이 회원가입 화면 이동 (일반 회원가입)
        val routeWithArgs = "signup_pages/{email}/{password}?displayName={displayName}&domain={domain}&profileImageUrl={profileImageUrl}"
        
        /**
         * 회원가입 화면으로 이동하는 라우트 생성 (SNS 로그인 후 신규 회원인 경우)
         * @param email SNS 로그인에서 받은 이메일
         * @param password SNS 로그인에서 받은 access token
         * @param displayName SNS 로그인에서 받은 표시 이름 (옵션)
         * @param domain 로그인 도메인 (kakao, google, line, facebook)
         * @param profileImageUrl 프로필 이미지 URL (옵션)
         */
        fun createRoute(
            email: String,
            password: String,
            displayName: String? = null,
            domain: String,
            profileImageUrl: String? = null
        ): String {
            val encodedEmail = URLEncoder.encode(email, StandardCharsets.UTF_8.toString())
            val encodedPassword = URLEncoder.encode(password, StandardCharsets.UTF_8.toString())
            val encodedDisplayName = displayName?.let {
                URLEncoder.encode(it, StandardCharsets.UTF_8.toString())
            }
            val encodedDomain = URLEncoder.encode(domain, StandardCharsets.UTF_8.toString())
            val encodedProfileImageUrl = profileImageUrl?.let {
                URLEncoder.encode(it, StandardCharsets.UTF_8.toString())
            }
            
            return buildString {
                append("signup_pages/$encodedEmail/$encodedPassword")
                if (encodedDisplayName != null) {
                    append("?displayName=$encodedDisplayName")
                }
                if (encodedDomain.isNotEmpty()) {
                    append(if (encodedDisplayName != null) "&" else "?")
                    append("domain=$encodedDomain")
                }
                if (encodedProfileImageUrl != null) {
                    append(if (encodedDisplayName != null || encodedDomain.isNotEmpty()) "&" else "?")
                    append("profileImageUrl=$encodedProfileImageUrl")
                }
            }
        }
    }
    data object Main : Screen("main")
    data object WebView : Screen("webview/{url}?title={title}") {
        /**
         * WebView 화면으로 이동하는 라우트 생성
         * @param url 로드할 URL
         * @param title AppBar 타이틀 (옵션)
         */
        fun createRoute(url: String, title: String? = null): String {
            val encodedUrl = URLEncoder.encode(url, StandardCharsets.UTF_8.toString())
            val encodedTitle = title?.let {
                URLEncoder.encode(it, StandardCharsets.UTF_8.toString())
            }
            return if (encodedTitle != null) {
                "webview/$encodedUrl?title=$encodedTitle"
            } else {
                "webview/$encodedUrl"
            }
        }
    }
}
