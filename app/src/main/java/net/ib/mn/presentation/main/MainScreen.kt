package net.ib.mn.presentation.main

import android.app.Activity
import android.content.res.Configuration
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import net.ib.mn.ui.components.ExoScaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import net.ib.mn.R
import net.ib.mn.ui.components.ExoAppBar
import net.ib.mn.ui.theme.ExodusTheme

/**
 * 메인 화면.
 * 하단 네비게이션 바를 포함한 메인 컨테이너.
 * AppScaffold를 사용하여 Safe Area 안에서 작동합니다.
 */
@Composable
fun MainScreen() {
    var selectedTab by remember { mutableIntStateOf(0) }

    ExoScaffold(
        topBar = {
            ExoAppBar(
                title = "app" ,
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
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            when (selectedTab) {
                0 -> Text("Home Screen", fontSize = 24.sp)
                1 -> Text("Favorites Screen", fontSize = 24.sp)
                2 -> Text("Profile Screen", fontSize = 24.sp)
            }
        }
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
        MainScreen()
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
        MainScreen()
    }
}