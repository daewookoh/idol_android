package com.example.idol_android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.idol_android.presentation.users.UserScreen
import com.example.idol_android.ui.theme.Idol_androidTheme
import dagger.hilt.android.AndroidEntryPoint

/**
 * Main activity with Hilt support.
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            Idol_androidTheme {
                UserScreen()
            }
        }
    }
}