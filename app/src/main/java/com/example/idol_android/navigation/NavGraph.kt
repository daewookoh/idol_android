package com.example.idol_android.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.idol_android.presentation.main.MainScreen
import com.example.idol_android.presentation.startup.StartUpScreen

/**
 * 앱의 네비게이션 그래프.
 * Splash -> StartUp -> Main 순서로 화면 전환.
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
            exitTransition = {
                slideOutOfContainer(
                    towards = AnimatedContentTransitionScope.SlideDirection.Left,
                    animationSpec = tween(600)
                )
            }
        ) {
            StartUpScreen(
                onNavigateToMain = {
                    navController.navigate(Screen.Main.route) {
                        popUpTo(Screen.StartUp.route) { inclusive = true }
                    }
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
    data object Main : Screen("main")
}
