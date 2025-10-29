package net.ib.mn.util

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

@Composable
fun SetupSystemBars(
    statusBarColor: Color = Color.Transparent,
    navigationBarColor: Color = Color.Transparent,
    isAppearanceLightStatusBars: Boolean? = null,
    isAppearanceLightNavigationBars: Boolean? = null
) {
    val view = LocalView.current
    val darkTheme = isSystemInDarkTheme()
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
