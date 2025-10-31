package net.ib.mn.presentation.main

import android.app.Activity
import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import net.ib.mn.ui.components.ExoScaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.gson.GsonBuilder
import net.ib.mn.R
import net.ib.mn.data.local.UserInfo
import net.ib.mn.ui.components.ExoAppBar
import net.ib.mn.ui.theme.ExodusTheme
import net.ib.mn.util.ServerUrl

/**
 * 메인 화면.
 * 하단 네비게이션 바를 포함한 메인 컨테이너.
 * AppScaffold를 사용하여 Safe Area 안에서 작동합니다.
 */
@Composable
fun MainScreen(
    viewModel: MainViewModel = hiltViewModel(),
    onLogout: () -> Unit = {}
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val userInfo by viewModel.userInfo.collectAsState()
    val logoutCompleted by viewModel.logoutCompleted.collectAsState()

    // 유저 정보 변경 시 로그
    androidx.compose.runtime.LaunchedEffect(userInfo) {
        if (userInfo != null) {
            android.util.Log.d("USER_INFO", "[MainScreen] ✓ UI received user info from ViewModel")
            android.util.Log.d("USER_INFO", "[MainScreen]   - Displaying user: ${userInfo?.username} (${userInfo?.email})")
        } else {
            android.util.Log.w("USER_INFO", "[MainScreen] ⚠️ UI received null user info")
        }
    }

    // 로그아웃 완료 시 네비게이션 처리
    androidx.compose.runtime.LaunchedEffect(logoutCompleted) {
        if (logoutCompleted) {
            onLogout()
        }
    }

    ExoScaffold(
        topBar = {
            ExoAppBar(
                title = "app"
            )
        },
        bottomBar = {
            NavigationBar {
                bottomNavItems.forEachIndexed { index, item ->
                    NavigationBarItem(
                        icon = { Icon(item.icon, contentDescription = item.label) },
                        label = { Text(item.label) },
                        selected = selectedTab == index,
                        onClick = { selectedTab = index }
                    )
                }
            }
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ) {
            when (selectedTab) {
                0 -> HomeContent(
                    apiUrl = ServerUrl.BASE_URL,
                    userInfo = userInfo,
                    onLogout = { viewModel.logout() }
                )
                1 -> Text("Favorites Screen", fontSize = 24.sp)
                2 -> Text("Profile Screen", fontSize = 24.sp)
            }
        }
    }
}

@Composable
private fun HomeContent(
    apiUrl: String,
    userInfo: net.ib.mn.data.local.UserInfo?,
    onLogout: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = apiUrl,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurface
        )

        // User Info as Pretty JSON
        if (userInfo != null) {
            // JSON 표시 영역
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = MaterialTheme.shapes.medium
                    )
                    .padding(12.dp)
            ) {
                val horizontalScroll = rememberScrollState()

                Text(
                    text = userInfo.toPrettyJson(),
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.horizontalScroll(horizontalScroll)
                )
            }
        } else {
            Text(
                "Loading user info...",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 14.sp
            )
        }

        // 로그아웃 버튼 - 항상 표시
        androidx.compose.material3.Button(
            onClick = onLogout,
            colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.error
            )
        ) {
            Text("로그아웃")
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}


data class BottomNavItem(
    val label: String,
    val icon: ImageVector
)

private val bottomNavItems = listOf(
    BottomNavItem("Home", Icons.Default.Home),
    BottomNavItem("Favorites", Icons.Default.Favorite),
    BottomNavItem("Profile", Icons.Default.Person)
)

@Preview(
    name = "Light Mode",
    showSystemUi = true,
    showBackground = true,
    locale = "ko"
)
@Composable
fun MainScreenPreviewLight() {
    ExodusTheme(darkTheme = false) {
        MainScreen(onLogout = {})
    }
}

@Preview(
    name = "Dark Mode",
    showSystemUi = true,
    showBackground = true,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
    locale = "ko"
)
@Composable
fun MainScreenPreviewDark() {
    ExodusTheme(darkTheme = true) {
        MainScreen(onLogout = {})
    }
}
/**
 * UserInfo를 Pretty JSON 문자열로 변환
 */
private fun UserInfo.toPrettyJson(): String {
    val gson = GsonBuilder().setPrettyPrinting().create()
    return try {
        gson.toJson(this)
    } catch (e: Exception) {
        "Error converting to JSON: ${e.message}"
    }
}
