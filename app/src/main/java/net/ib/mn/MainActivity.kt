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

    @Inject
    lateinit var chartDatabaseRepository: net.ib.mn.data.repository.ChartRankingRepository

    @Inject
    lateinit var cacheDataSourceFactory: androidx.media3.datasource.cache.CacheDataSource.Factory

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()

        super.onCreate(savedInstanceState)

        // URL scheme으로 서버 변경 처리 (old 프로젝트와 동일: onCreate 초기에 처리)
        // 액티비티 재시작이 필요한 경우 setContent 전에 return하여 StartupScreen이 두 번 실행되지 않도록 함
        if (handleUrlScheme()) {
            // 액티비티 재시작이 필요한 경우 여기서 종료
            return
        }

        // 서버 URL은 IdolApplication.onCreate()에서 이미 설정되었으므로 여기서는 로깅만
        android.util.Log.d("MainActivity", "Current server URL: ${ServerUrl.HOST}")

        enableEdgeToEdge()

        // UDP 연결은 IdolApplication에서 앱 전체 생명주기에 맞춰 관리됨
        // ProcessLifecycleOwner를 사용하여 포그라운드/백그라운드 진입 시 자동으로 시작/중지

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
                // ExoTop3Manager에 비디오 캐시 제공
                androidx.compose.runtime.CompositionLocalProvider(
                    net.ib.mn.ui.components.LocalExoTop3Manager provides
                        net.ib.mn.ui.components.ExoTop3Manager(cacheDataSourceFactory)
                ) {
                    val navController = rememberNavController()
                    NavGraph(navController = navController)
                }
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

            // 서버 변경 시 모든 데이터 리셋 (인증 정보 포함)
            runBlocking {
                android.util.Log.d("MainActivity", "========================================")
                android.util.Log.d("MainActivity", "🗑️ SERVER URL CHANGED - Clearing ALL data...")
                android.util.Log.d("MainActivity", "  - Old URL: ${ServerUrl.HOST}")
                android.util.Log.d("MainActivity", "  - New URL: $fullHost")
                android.util.Log.d("MainActivity", "========================================")

                // 1. 차트 랭킹 데이터 삭제 (메모리 캐시 포함)
                try {
                    chartDatabaseRepository.clearAll()
                    android.util.Log.d("MainActivity", "✅ Chart Rankings cleared (including memory cache)")
                } catch (e: Exception) {
                    android.util.Log.e("MainActivity", "❌ Failed to clear Chart DB: ${e.message}", e)
                }

                // 2. 모든 Room DB 데이터 삭제
                try {
                    idolDao.deleteAll()
                    android.util.Log.d("MainActivity", "✅ Room DB (Idol) cleared")
                } catch (e: Exception) {
                    android.util.Log.e("MainActivity", "❌ Failed to clear Idol DB: ${e.message}", e)
                }

                // 3. 인증 정보를 제외한 모든 DataStore 데이터 삭제 (유저 정보, 캐시 등 삭제)
                try {
                    preferencesManager.clearAllExceptAuth()
                    android.util.Log.d("MainActivity", "✅ DataStore cleared except auth (user info, cache)")
                } catch (e: Exception) {
                    android.util.Log.e("MainActivity", "❌ Failed to clear DataStore: ${e.message}", e)
                }

                // 4. ConfigRepository 메모리 캐시 삭제
                configRepository.clearAllCache()
                android.util.Log.d("MainActivity", "✅ ConfigRepository cache cleared")

                // 5. 새 서버 URL 저장 (clearAll 후에 저장해야 함)
                // DataStore와 SharedPreferences 양쪽에 저장 (IdolApplication이 SharedPreferences를 읽음)
                preferencesManager.setServerUrl(fullHost)

                // SharedPreferences에도 저장 (IdolApplication.onCreate()가 먼저 읽음)
                // ⚠️ CRITICAL: commit()을 사용하여 동기적으로 저장 (프로세스 종료 전에 디스크에 flush)
                val serverPrefs = getSharedPreferences("idol_server_config", android.content.Context.MODE_PRIVATE)
                serverPrefs.edit().putString("server_url", fullHost).commit()  // commit() = 동기, apply() = 비동기

                android.util.Log.d("MainActivity", "💾 Saving new server URL: $fullHost")

                // 6. 저장 확인
                val savedUrlSharedPrefs = serverPrefs.getString("server_url", null)
                val savedUrlDataStore = preferencesManager.serverUrl.first()
                android.util.Log.d("MainActivity", "✅ Server URL saved:")
                android.util.Log.d("MainActivity", "  - SharedPreferences: $savedUrlSharedPrefs")
                android.util.Log.d("MainActivity", "  - DataStore: $savedUrlDataStore")

                android.util.Log.d("MainActivity", "========================================")
                android.util.Log.d("MainActivity", "✅ All data cleared - App will restart")
                android.util.Log.d("MainActivity", "  - Old Server: ${ServerUrl.HOST}")
                android.util.Log.d("MainActivity", "  - New Server: $fullHost")
                android.util.Log.d("MainActivity", "========================================")
            }

            // 이미지 캐시 삭제
            try {
                cacheDir.deleteRecursively()
                android.util.Log.d("MainActivity", "✅ Image cache cleared")
            } catch (e: Exception) {
                android.util.Log.e("MainActivity", "⚠️ Failed to clear image cache: ${e.message}", e)
            }

            android.util.Log.d("MainActivity", "🔄 Restarting app with new server URL...")
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

    // UDP 생명주기는 IdolApplication의 ProcessLifecycleOwner에서 관리됨
    // - onStart (앱 포그라운드 진입): setupConnection() + startHeartbeat()
    // - onStop (앱 백그라운드 진입): stopHeartbeat()
    // 이렇게 하면:
    // - 앱 전체가 백그라운드로 가도 UDP 유지
    // - Activity 간 전환 시 UDP 연결 끊김 방지
    // - 앱이 완전히 종료되어야만 UDP 중지

    /**
     * Facebook SDK Activity Result 처리.
     * LoginScreen에서 설정한 callbackManager에 결과 전달.
     */
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        callbackManager?.onActivityResult(requestCode, resultCode, data)
        super.onActivityResult(requestCode, resultCode, data)
    }
}