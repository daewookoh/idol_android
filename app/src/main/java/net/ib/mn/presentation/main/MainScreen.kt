package net.ib.mn.presentation.main

import android.content.res.Configuration
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.Alignment
import androidx.hilt.navigation.compose.hiltViewModel
import net.ib.mn.R
import net.ib.mn.ui.components.ExoAppBar
import net.ib.mn.ui.components.ExoScaffold
import net.ib.mn.ui.components.MainBottomNavigation
import net.ib.mn.ui.theme.ExodusTheme
import net.ib.mn.presentation.main.freeboard.FreeBoardPage
import net.ib.mn.presentation.main.menu.MenuPage
import net.ib.mn.presentation.main.myidol.MyIdolPage
import net.ib.mn.presentation.main.profile.ProfilePage
import net.ib.mn.presentation.main.ranking.RankingPage

/**
 * 메인 화면.
 * 하단 네비게이션 바와 상단 앱바를 포함한 메인 컨테이너.
 * 각 탭은 별도의 Page로 구성되며, MainScreen에서 topBar를 관리합니다.
 */
@Composable
fun MainScreen(
    viewModel: MainViewModel = hiltViewModel(),
    onLogout: () -> Unit = {}
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val userInfo by viewModel.userInfo.collectAsState()
    val logoutCompleted by viewModel.logoutCompleted.collectAsState()

    // 로그아웃 완료 시 네비게이션 처리
    androidx.compose.runtime.LaunchedEffect(logoutCompleted) {
        if (logoutCompleted) {
            onLogout()
        }
    }

    // 탭 메뉴 및 아이콘 설정 (Old 프로젝트와 동일)
    val menus = listOf(
        stringResource(id = R.string.hometab_title_rank),
        stringResource(id = R.string.hometab_title_myidol),
        stringResource(id = R.string.hometab_title_profile),
        stringResource(id = R.string.hometab_title_freeboard),
        stringResource(id = R.string.hometab_title_menu)
    )

    val iconsOfSelected = listOf(
        painterResource(id = R.drawable.btn_bottom_nav_ranking_on),
        painterResource(id = R.drawable.btn_bottom_nav_favorite_on),
        painterResource(id = R.drawable.btn_bottom_nav_my_on),
        painterResource(id = R.drawable.btn_bottom_nav_board_on),
        painterResource(id = R.drawable.btn_bottom_nav_menu_on)
    )

    val iconsOfUnSelected = listOf(
        painterResource(id = R.drawable.btn_bottom_nav_ranking_off),
        painterResource(id = R.drawable.btn_bottom_nav_favorite_off),
        painterResource(id = R.drawable.btn_bottom_nav_my_off),
        painterResource(id = R.drawable.btn_bottom_nav_board_off),
        painterResource(id = R.drawable.btn_bottom_nav_menu_off)
    )

    // 현재 선택된 탭의 타이틀
    val currentTitle = menus.getOrNull(selectedTab) ?: ""

    ExoScaffold(
        topBar = {
            ExoAppBar(
                title = currentTitle
            )
        },
        bottomBar = {
            MainBottomNavigation(
                menus = menus,
                iconsOfSelected = iconsOfSelected,
                iconsOfUnSelected = iconsOfUnSelected,
                initialSelectedIndex = selectedTab,
                defaultBackgroundColor = colorResource(id = R.color.background_200),
                defaultBorderColor = colorResource(id = R.color.gray150),
                defaultTextColor = colorResource(id = R.color.text_default),
                onTabSelected = { index ->
                    selectedTab = index
                }
            )
        }
    ) { paddingValues ->
        AnimatedContent(
            targetState = selectedTab,
            transitionSpec = {
                fadeIn(animationSpec = tween(300)) togetherWith fadeOut(animationSpec = tween(300))
            },
            label = "tab_content"
        ) { tab ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                when (tab) {
                    0 -> RankingPage(
                        userInfo = userInfo,
                        onLogout = { viewModel.logout() }
                    )
                    1 -> MyIdolPage()
                    2 -> ProfilePage()
                    3 -> FreeBoardPage()
                    4 -> MenuPage()
                }
            }
        }
    }
}

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
