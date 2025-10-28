package com.example.idol_android

import android.graphics.Color
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.navigation.compose.rememberNavController
import com.example.idol_android.navigation.NavGraph
import com.example.idol_android.ui.theme.Idol_androidTheme
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
        // Splash Screen API 사용
        val splashScreen = installSplashScreen()

        super.onCreate(savedInstanceState)

        // ActionBar를 코드 레벨에서 명시적으로 숨김
        supportActionBar?.hide()

        enableEdgeToEdge()

        setContent {
            Idol_androidTheme {
                val navController = rememberNavController()
                NavGraph(navController = navController)
            }
        }
    }

    /**
     * ActionBar를 표시해야 하는 경우 사용.
     * 예: showActionBar()
     */
    fun showActionBar() {
        supportActionBar?.show()
    }

    /**
     * ActionBar를 숨기는 경우 사용.
     * 예: hideActionBar()
     */
    fun hideActionBar() {
        supportActionBar?.hide()
    }

    /**
     * ActionBar 타이틀 설정.
     * 예: setActionBarTitle("제목")
     */
    fun setActionBarTitle(title: String) {
        supportActionBar?.title = title
    }
}