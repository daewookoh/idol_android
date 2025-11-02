package net.ib.mn

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.rememberNavController
import com.facebook.CallbackManager
import net.ib.mn.data.local.PreferencesManager
import net.ib.mn.navigation.NavGraph
import net.ib.mn.ui.theme.ExodusTheme
import net.ib.mn.util.Constants
import net.ib.mn.util.ServerUrl
import net.ib.mn.util.SetupSystemBars
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import javax.inject.Inject

/**
 * Main activity with Hilt support.
 * AppCompatActivity를 상속하여 ActionBar를 코드 레벨에서 제어.
 *
 * Splash Screen -> StartUp Screen -> Main Screen 순서로 화면 전환.
 *
 * Facebook SDK Activity Result 처리:
 * - Facebook SDK는 startActivityForResult를 사용하므로 onActivityResult에서 처리 필요
 *
 * URL Scheme 처리:
 * - devloveidol:// 또는 myloveactor:// 스킴으로 테스트 서버 전환 가능
 * - 예: devloveidol://?host=test.myloveidol.com
 * - 예: devloveidol://?reset_auth=true
 */
@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    // Facebook CallbackManager를 static으로 저장하여 LoginScreen에서 접근 가능하도록
    companion object {
        var callbackManager: CallbackManager? = null
    }

    @Inject
    lateinit var preferencesManager: PreferencesManager

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()

        super.onCreate(savedInstanceState)

        // URL scheme으로 서버 변경 처리 (old 프로젝트와 동일: onCreate 초기에 처리)
        // 액티비티 재시작이 필요한 경우 setContent 전에 return하여 StartupScreen이 두 번 실행되지 않도록 함
        if (handleUrlScheme()) {
            // 액티비티 재시작이 필요한 경우 여기서 종료
            return
        }

        // 저장된 서버 URL이 있으면 적용 (IdolApplication에서 이미 처리하지만, 재확인)
        // old 프로젝트는 동기적으로 처리하므로 동기 처리로 변경
        runBlocking {
            val savedServerUrl = preferencesManager.serverUrl.first()
            if (!savedServerUrl.isNullOrEmpty()) {
                ServerUrl.setHost(savedServerUrl)
            }
        }

        enableEdgeToEdge()

        setContent {
            ExodusTheme {
                val navController = rememberNavController()
                NavGraph(navController = navController)
            }
        }
    }

    /**
     * URL Scheme 처리
     * old 프로젝트의 StartupActivity.kt:266-297 로직 반영
     *
     * 사용 예시:
     * - adb shell am start -a android.intent.action.VIEW -d "devloveidol://?host=test.myloveidol.com"
     * - adb shell am start -a android.intent.action.VIEW -d "devloveidol://?reset_auth=true"
     * 
     * @return true if activity restart is needed (host parameter was processed), false otherwise
     */
    private fun handleUrlScheme(): Boolean {
        if (Intent.ACTION_VIEW != intent.action) {
            return false
        }

        val uri = intent.data ?: return false
        val devScheme = if (Constants.IS_CELEB) "myloveactor" else "devloveidol"

        if (!devScheme.equals(uri.scheme, ignoreCase = true)) {
            return false
        }

        // 1. host 파라미터 처리 - 서버 URL 변경
        val host = uri.getQueryParameter("host")
        if (!host.isNullOrEmpty()) {
            val fullHost = if (!host.startsWith("http")) {
                "http://$host"
            } else {
                host
            }

            // ServerUrl 변경
            ServerUrl.setHost(fullHost)

            // PreferencesManager에 저장 (동기적으로 처리하여 저장 완료 후 재시작)
            runBlocking {
                preferencesManager.setServerUrl(fullHost)
            }

            // 변경된 호스트가 retrofit에 반영되도록 액티비티 재시작
            // old 프로젝트와 동일: onCreate에서 return하여 StartupScreen이 두 번 실행되지 않도록 함
            val restartIntent = Intent(this@MainActivity, MainActivity::class.java)
            restartIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(restartIntent)
            finish()
            return true  // 액티비티 재시작 필요
        }

        // 2. reset_auth 파라미터 처리 - 인증 정보 삭제
        val resetAuth = uri.getQueryParameter("reset_auth")
        if (resetAuth.equals("true", ignoreCase = true)) {
            lifecycleScope.launch {
                // 토큰 삭제 (로그아웃 효과)
                preferencesManager.setAccessToken("")
            }
        }
        
        return false  // 액티비티 재시작 불필요
    }

    /**
     * 액티비티가 이미 실행 중일 때 새로운 Intent가 들어올 때 호출됨
     * URL scheme이 들어올 때 처리
     */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)  // 새로운 intent로 업데이트
        
        // URL scheme으로 서버 변경 처리
        if (handleUrlScheme()) {
            // 액티비티 재시작이 필요한 경우 종료
            return
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