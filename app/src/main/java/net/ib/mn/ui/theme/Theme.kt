package net.ib.mn.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

// Material3 ColorScheme에서 사용할 색상들은 고정값 사용
private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFFE24848),
    secondary = Color(0xFFE24848),
    tertiary = Color(0xFFE53D3D),
    background = Color(0xFF151515),
    surface = Color(0xFF151515),
    onPrimary = Color(0xFF000000),
    onSecondary = Color(0xFFdbdbdb),
    onTertiary = Color(0xFF000000),
    onBackground = Color(0xFFdbdbdb),
    onSurface = Color(0xFFdbdbdb)
)

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFFFF4444),
    secondary = Color(0xFFFF6666),
    tertiary = Color(0xFFE53D3D),
    background = Color(0xFFFFFFFF),
    surface = Color(0xFFFFFFFF),
    onPrimary = Color(0xFFFFFFFF),
    onSecondary = Color(0xFF333333),
    onTertiary = Color(0xFFFFFFFF),
    onBackground = Color(0xFF333333),
    onSurface = Color(0xFF333333)
)

/**
 * Exodus Theme Wrapper
 *
 * - darkTheme: 다크 모드 여부 (null이면 시스템 설정 사용)
 * - LocalDarkTheme CompositionLocal을 통해 ColorPalette에 테마 전달
 */
@Composable
fun ExodusTheme(
    darkTheme: Boolean? = null,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    // darkTheme이 null이면 시스템 설정 사용
    val effectiveDarkTheme = darkTheme ?: isSystemInDarkTheme()

    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (effectiveDarkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        effectiveDarkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    // ColorPalette가 앱 내부 테마 설정을 사용할 수 있도록 CompositionLocal 제공
    CompositionLocalProvider(LocalDarkTheme provides darkTheme) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}