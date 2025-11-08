package net.ib.mn

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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

    @Inject
    lateinit var broadcastManager: net.ib.mn.data.remote.udp.IdolBroadcastManager

    @Inject
    lateinit var idolDao: net.ib.mn.data.local.dao.IdolDao

    @Inject
    lateinit var configRepository: net.ib.mn.domain.repository.ConfigRepository

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

        // UDP 브로드캐스트 연결 시작 (실시간 아이돌 데이터 업데이트)
        // StartupViewModel의 loadConfigSelf()에서 /configs/self/ API를 호출하여 UDP 설정을 저장함
        lifecycleScope.launch {
            try {
                // 사용자 ID 조회
                val userInfo = preferencesManager.userInfo.first()
                val userId = userInfo?.id ?: 0

                // UDP 설정 조회 (ConfigSelf API 응답에서 저장된 값)
                val udpBroadcastUrl = preferencesManager.udpBroadcastUrl.first()
                val udpStage = preferencesManager.udpStage.first()

                android.util.Log.d("MainActivity", "========================================")
                android.util.Log.d("MainActivity", "🔌 UDP Configuration")
                android.util.Log.d("MainActivity", "  - UDP Broadcast URL: $udpBroadcastUrl")
                android.util.Log.d("MainActivity", "  - UDP Stage: $udpStage")
                android.util.Log.d("MainActivity", "  - User ID: $userId")
                android.util.Log.d("MainActivity", "========================================")

                // UDP Stage가 0보다 클 때만 연결 (old 프로젝트와 동일)
                if (udpStage > 0 && !udpBroadcastUrl.isNullOrEmpty()) {
                    android.util.Log.d("MainActivity", "✓ UDP enabled - Starting connection...")
                    broadcastManager.setupConnection(udpBroadcastUrl, userId)
                } else {
                    if (udpStage <= 0) {
                        android.util.Log.w("MainActivity", "⚠️ UDP disabled (stage=$udpStage)")
                    }
                    if (udpBroadcastUrl.isNullOrEmpty()) {
                        android.util.Log.w("MainActivity", "⚠️ UDP URL not configured")
                    }
                    android.util.Log.w("MainActivity", "Skipping UDP connection")
                }
            } catch (e: Exception) {
                android.util.Log.e("MainActivity", "❌ Failed to setup UDP connection", e)
            }
        }

        setContent {
            // PreferencesManager에서 테마 설정 구독
            val themeString by preferencesManager.theme.collectAsState(initial = null)

            // theme 문자열을 Boolean?으로 변환
            // null or "system" -> null (시스템 설정 사용)
            // "light" -> false
            // "dark" -> true
            val darkTheme = when (themeString?.lowercase()) {
                "dark" -> true
                "light" -> false
                else -> null  // "system" or null -> 시스템 설정 사용
            }

            ExodusTheme(darkTheme = darkTheme) {
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

            android.util.Log.d("MainActivity", "========================================")
            android.util.Log.d("MainActivity", "🔄 URL Scheme: Server Change Detected")
            android.util.Log.d("MainActivity", "  - New Server: $fullHost")
            android.util.Log.d("MainActivity", "========================================")

            // 서버 변경 시 데이터 리셋 (로그인 정보는 유지)
            runBlocking {
                android.util.Log.d("MainActivity", "🗑️ Clearing local data (keeping auth credentials)...")

                // 1. 로그인 정보(토큰, 이메일, 도메인)를 제외한 모든 로컬 데이터 삭제
                preferencesManager.clearAllExceptAuth()
                android.util.Log.d("MainActivity", "✅ Local data cleared (auth credentials preserved)")

                // 2. Room DB 데이터 삭제 (Idol 데이터 등)
                try {
                    idolDao.deleteAll()
                    android.util.Log.d("MainActivity", "✅ Room DB data cleared")
                } catch (e: Exception) {
                    android.util.Log.e("MainActivity", "❌ Failed to clear Room DB: ${e.message}", e)
                }

                // 3. ConfigRepository 메모리 캐시 삭제
                configRepository.clearAllCache()
                android.util.Log.d("MainActivity", "✅ ConfigRepository cache cleared")

                // 4. 새 서버 URL 저장
                preferencesManager.setServerUrl(fullHost)
                android.util.Log.d("MainActivity", "✅ New server URL saved: $fullHost")
            }

            // ServerUrl 변경 (Retrofit 인스턴스 재생성에 필요)
            ServerUrl.setHost(fullHost)

            // 이미지 캐시 삭제
            try {
                cacheDir.deleteRecursively()
                android.util.Log.d("MainActivity", "✅ Image cache cleared")
            } catch (e: Exception) {
                android.util.Log.e("MainActivity", "⚠️ Failed to clear image cache: ${e.message}", e)
            }

            android.util.Log.d("MainActivity", "🔄 Killing process to fully restart app...")
            android.util.Log.d("MainActivity", "========================================")

            // 프로세스 완전 종료 및 재시작 (모든 메모리, ViewModel, 싱글톤 등 완전 초기화)
            val restartIntent = Intent(this@MainActivity, MainActivity::class.java)
            restartIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            startActivity(restartIntent)

            // 프로세스 완전 종료 (모든 것이 완전히 초기화됨)
            android.os.Process.killProcess(android.os.Process.myPid())
            return true  // 프로세스 종료
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
     * Activity가 화면에 보이지 않을 때 (백그라운드 진입)
     * UDP heartbeat 중지하여 배터리 절약
     */
    override fun onPause() {
        super.onPause()
        android.util.Log.d("MainActivity", "⏸️ onPause - Stopping UDP heartbeat")
        broadcastManager.stopHeartbeat()
    }

    /**
     * Activity가 화면에 보일 때 (포그라운드 복귀)
     * UDP heartbeat 재시작하여 실시간 업데이트 재개
     */
    override fun onResume() {
        super.onResume()
        android.util.Log.d("MainActivity", "▶️ onResume - Starting UDP heartbeat")
        broadcastManager.startHeartbeat()
    }

    /**
     * Activity가 완전히 종료될 때
     * UDP 연결 해제 및 리소스 정리
     */
    override fun onDestroy() {
        android.util.Log.d("MainActivity", "🔴 onDestroy - Disconnecting UDP")
        lifecycleScope.launch {
            broadcastManager.disconnect()
        }
        super.onDestroy()
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