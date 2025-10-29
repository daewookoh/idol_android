package net.ib.mn

import android.graphics.Color
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.navigation.compose.rememberNavController
import net.ib.mn.navigation.NavGraph
import net.ib.mn.ui.theme.ExodusTheme
import net.ib.mn.util.SetupSystemBars
import dagger.hilt.android.AndroidEntryPoint

/**
 * Main activity with Hilt support.
 * AppCompatActivity를 상속하여 ActionBar를 코드 레벨에서 제어.
 *
 * Splash Screen -> StartUp Screen -> Main Screen 순서로 화면 전환.
 */
@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()

        super.onCreate(savedInstanceState)

        setContent {
            ExodusTheme {
                SetupSystemBars(
                    statusBarColor = androidx.compose.ui.graphics.Color.Transparent,
                    navigationBarColor = androidx.compose.ui.graphics.Color.Transparent
                )

                val navController = rememberNavController()
                NavGraph(navController = navController)
            }
        }
    }
}