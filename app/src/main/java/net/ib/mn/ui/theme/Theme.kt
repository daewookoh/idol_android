package net.ib.mn.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
private val DarkColorScheme = darkColorScheme(
    primary = MainDark,
    secondary = MainDarkVariant,
    tertiary = RedClickBorderDark,
    background = Background300Dark,
    surface = Background100Dark,
    onPrimary = TextWhiteBlackDark,
    onSecondary = TextDefaultDark,
    onTertiary = TextWhiteBlackDark,
    onBackground = TextDefaultDark,
    onSurface = TextDefaultDark
)

private val LightColorScheme = lightColorScheme(
    primary = MainLight,
    secondary = MainLightVariant,
    tertiary = RedClickBorderLight,
    background = Background100Light,
    surface = Background100Light,
    onPrimary = TextWhiteBlackLight,
    onSecondary = TextDefaultLight,
    onTertiary = TextWhiteBlackLight,
    onBackground = TextDefaultLight,
    onSurface = TextDefaultLight
)

@Composable
fun ExodusTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window

            // Status Bar 배경색 (어두운 색으로)
            window.statusBarColor = Color(0xFF1C1C1E).toArgb()

            // Status Bar 아이콘/텍스트를 밝은 색으로 (항상!)
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}