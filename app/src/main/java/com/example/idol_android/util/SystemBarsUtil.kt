package com.example.idol_android.util

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

/**
 * System bars (status bar, navigation bar) 설정 유틸리티.
 *
 * Theme에 따라 status bar 아이콘 색상을 자동으로 변경:
 * - Light theme: 어두운 아이콘 (시계가 검은색)
 * - Dark theme: 밝은 아이콘 (시계가 흰색)
 */

/**
 * Status bar와 navigation bar 아이콘 색상을 자동으로 설정하는 Composable.
 *
 * Theme에 따라 자동으로 변경:
 * - Light theme (배경이 밝음): 어두운 아이콘 사용
 * - Dark theme (배경이 어두움): 밝은 아이콘 사용
 *
 * @param statusBarColor Status bar 배경 색상 (투명 또는 화면 배경색)
 * @param navigationBarColor Navigation bar 배경 색상 (투명 또는 화면 배경색)
 * @param isAppearanceLightStatusBars Light theme 여부를 수동으로 지정 (null이면 자동 감지)
 * @param isAppearanceLightNavigationBars Navigation bar가 light theme인지 수동 지정 (null이면 자동 감지)
 *
 * 사용 예시:
 * ```
 * @Composable
 * fun MyScreen() {
 *     SetupSystemBars(
 *         statusBarColor = Color.Transparent,
 *         navigationBarColor = Color.Transparent
 *     )
 *     // 화면 UI...
 * }
 * ```
 */
@Composable
fun SetupSystemBars(
    statusBarColor: Color = Color.Transparent,
    navigationBarColor: Color = Color.Transparent,
    isAppearanceLightStatusBars: Boolean? = null,
    isAppearanceLightNavigationBars: Boolean? = null
) {
    val view = LocalView.current
    val darkTheme = isSystemInDarkTheme()

    // Light theme이면 어두운 아이콘, Dark theme이면 밝은 아이콘
    val lightStatusBars = isAppearanceLightStatusBars ?: !darkTheme
    val lightNavigationBars = isAppearanceLightNavigationBars ?: !darkTheme

    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = statusBarColor.toArgb()
            window.navigationBarColor = navigationBarColor.toArgb()

            val windowInsetsController = WindowCompat.getInsetsController(window, view)
            windowInsetsController.isAppearanceLightStatusBars = lightStatusBars
            windowInsetsController.isAppearanceLightNavigationBars = lightNavigationBars
        }
    }
}

/**
 * 배경색이 밝은지 어두운지 판단하는 함수.
 * 밝은 배경이면 true (어두운 아이콘 사용), 어두운 배경이면 false (밝은 아이콘 사용).
 *
 * @param color 배경색
 * @return 밝은 배경이면 true
 */
fun Color.isLight(): Boolean {
    val luminance = (0.299 * red + 0.587 * green + 0.114 * blue)
    return luminance > 0.5
}
