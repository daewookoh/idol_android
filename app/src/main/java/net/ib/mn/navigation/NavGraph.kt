package net.ib.mn.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import net.ib.mn.presentation.login.EmailLoginScreen
import net.ib.mn.presentation.login.LoginScreen
import net.ib.mn.presentation.main.MainScreen
import net.ib.mn.presentation.startup.StartUpScreen

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
                    navController.navigate(Screen.Main.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                },
                onNavigateToEmailLogin = {
                    navController.navigate(Screen.EmailLogin.route)
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
                    // NOTE: 회원가입 화면 미구현 - 구현 시 Screen.SignUp.route로 navigate
                    // navController.navigate(Screen.SignUp.route)
                    android.widget.Toast.makeText(
                        navController.context,
                        "회원가입 화면은 추후 구현 예정입니다",
                        android.widget.Toast.LENGTH_SHORT
                    ).show()
                },
                onNavigateToForgotId = {
                    // NOTE: 아이디 찾기 화면 미구현 - 구현 시 Screen.ForgotId.route로 navigate
                    // navController.navigate(Screen.ForgotId.route)
                    android.widget.Toast.makeText(
                        navController.context,
                        "아이디 찾기 화면은 추후 구현 예정입니다",
                        android.widget.Toast.LENGTH_SHORT
                    ).show()
                },
                onNavigateToForgotPassword = {
                    // NOTE: 비밀번호 찾기 화면 미구현 - 구현 시 Screen.ForgotPassword.route로 navigate
                    // navController.navigate(Screen.ForgotPassword.route)
                    android.widget.Toast.makeText(
                        navController.context,
                        "비밀번호 찾기 화면은 추후 구현 예정입니다",
                        android.widget.Toast.LENGTH_SHORT
                    ).show()
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
            MainScreen()
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
    data object Main : Screen("main")
}
