package net.ib.mn

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.navigation.compose.rememberNavController
import com.facebook.CallbackManager
import net.ib.mn.navigation.NavGraph
import net.ib.mn.ui.theme.ExodusTheme
import net.ib.mn.util.SetupSystemBars
import dagger.hilt.android.AndroidEntryPoint

/**
 * Main activity with Hilt support.
 * AppCompatActivity를 상속하여 ActionBar를 코드 레벨에서 제어.
 *
 * Splash Screen -> StartUp Screen -> Main Screen 순서로 화면 전환.
 *
 * Facebook SDK Activity Result 처리:
 * - Facebook SDK는 startActivityForResult를 사용하므로 onActivityResult에서 처리 필요
 */
@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    // Facebook CallbackManager를 static으로 저장하여 LoginScreen에서 접근 가능하도록
    companion object {
        var callbackManager: CallbackManager? = null
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()

        super.onCreate(savedInstanceState)

        setContent {
            ExodusTheme {
                val backgroundColor = androidx.compose.ui.res.colorResource(id = R.color.background_100)

                SetupSystemBars(
                    statusBarColor = backgroundColor,
                    navigationBarColor = backgroundColor
                )

                val navController = rememberNavController()
                NavGraph(navController = navController)
            }
        }
    }

    /**
     * Facebook SDK Activity Result 처리.
     * LoginScreen에서 설정한 callbackManager에 결과 전달.
     */
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        callbackManager?.onActivityResult(requestCode, resultCode, data)
        super.onActivityResult(requestCode, resultCode, data)
    }
}