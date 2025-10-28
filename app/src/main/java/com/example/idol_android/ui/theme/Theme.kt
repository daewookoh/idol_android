package com.example.idol_android.ui.theme

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
fun Idol_androidTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is disabled by default to use custom colors
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

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}