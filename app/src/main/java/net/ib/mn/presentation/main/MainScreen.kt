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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import kotlinx.coroutines.launch
import net.ib.mn.R
import net.ib.mn.ui.components.ExoScaffold
import net.ib.mn.ui.components.MainBottomNavigation
import net.ib.mn.ui.components.MainTopBar
import net.ib.mn.ui.components.SwitchToggleButton
import net.ib.mn.ui.theme.ExodusTheme
import net.ib.mn.util.Constants
import net.ib.mn.presentation.main.freeboard.FreeBoardPage
import net.ib.mn.presentation.main.menu.MenuPage
import net.ib.mn.presentation.main.myidol.MyIdolPage
import net.ib.mn.presentation.main.profile.ProfilePage
import net.ib.mn.presentation.main.ranking.RankingPage
import java.util.Locale

/**
 * 메인 화면.
 * 하단 네비게이션 바와 상단 앱바를 포함한 메인 컨테이너.
 * 각 탭은 별도의 Page로 구성되며, MainScreen에서 topBar를 관리합니다.
 */
@Composable
fun MainScreen(
    viewModel: MainViewModel = hiltViewModel(),
    topBarViewModel: MainTopBarViewModel = hiltViewModel(),
    onLogout: () -> Unit = {}
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val userInfo by viewModel.userInfo.collectAsState()
    val logoutCompleted by viewModel.logoutCompleted.collectAsState()
    val timerText by topBarViewModel.timerText.collectAsState()
    val defaultCategory by viewModel.preferencesManager.defaultCategory.collectAsState(initial = Constants.TYPE_MALE)
    
    val configuration = LocalConfiguration.current
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // 타이머 시작
    LaunchedEffect(Unit) {
        topBarViewModel.startTimer()
    }

    // 로그아웃 완료 시 네비게이션 처리
    LaunchedEffect(logoutCompleted) {
        if (logoutCompleted) {
            onLogout()
        }
    }

    // Locale에 따른 Gender 문자열 리스트 구성
    val genderStrings = remember {
        val currentLocale = configuration.locales[0]
        val localeString = "${currentLocale.language}_${currentLocale.country}"
        
        when (localeString) {
            "ko_KR" -> getGenderString(context, Locale.KOREA, false)
            "ja_JP" -> getGenderString(context, Locale.JAPAN, false)
            "zh_CN" -> getGenderString(context, Locale("zh", "CN"), false)
            "zh_TW" -> getGenderString(context, Locale("zh", "TW"), false)
            else -> getGenderString(context, Locale.ENGLISH, true)
        }
    }

    // SwitchToggleButton 상태 계산 (defaultCategory를 직접 사용)
    val maleIndex = remember { genderStrings.indexOfFirst { it.second == Constants.TYPE_MALE } }
    val isMaleSelected = remember(defaultCategory, maleIndex) {
        if (maleIndex == 0) {
            defaultCategory == Constants.TYPE_MALE
        } else {
            defaultCategory != Constants.TYPE_MALE
        }
    }

    // 탭 메뉴 및 아이콘 설정
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

    // 탭에 따른 TopBar 설정
    val showToggleButton = selectedTab == 0
    val showMainMenu = selectedTab in 0..3
    val showMyInfoMenu = selectedTab == 4

    ExoScaffold(
        topBar = {
            MainTopBar(
                timerText = timerText,
                showToggleButton = showToggleButton,
                showMainMenu = showMainMenu,
                showMyInfoMenu = showMyInfoMenu,
                toggleButton = {
                    SwitchToggleButton(
                        genderList = genderStrings,
                        isMaleSelected = isMaleSelected,
                        boxBackgroundColor = colorResource(id = R.color.gray100),
                        boxTextColor = colorResource(id = R.color.text_gray),
                        thumbBackgroundColor = colorResource(id = R.color.text_default),
                        thumbTextColor = colorResource(id = R.color.text_white_black),
                        onCategoryChanged = { category ->
                            coroutineScope.launch {
                                // PreferencesManager에 저장 (영구 저장)
                                viewModel.preferencesManager.setDefaultCategory(category)
                                // defaultCategory Flow가 자동으로 업데이트되어 UI 반영
                            }
                        }
                    )
                },
                onSearchClick = { /* TODO */ },
                onFriendsClick = { /* TODO */ },
                onAttendanceClick = { /* TODO */ },
                onNotificationClick = { /* TODO */ },
                onSettingClick = { /* TODO */ }
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
                onTabSelected = { selectedTab = it }
            )
        }
    ) {
        AnimatedContent(
            targetState = selectedTab,
            transitionSpec = {
                fadeIn(animationSpec = tween(300)) togetherWith fadeOut(animationSpec = tween(300))
            },
            label = "tab_content"
        ) { tab ->
            Box(
                modifier = Modifier.fillMaxSize()
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

/**
 * Locale에 따른 Gender 문자열 리스트 구성
 */
private fun getGenderString(
    context: android.content.Context,
    locale: Locale,
    isExceptCondition: Boolean
): List<Pair<String, String>> {
    return if (isExceptCondition) {
        listOf(
            Constants.TYPE_MALE to Constants.TYPE_MALE,
            Constants.TYPE_FEMALE to Constants.TYPE_FEMALE
        )
    } else {
        val config = Configuration(context.resources.configuration)
        config.setLocale(locale)
        val localizedContext = context.createConfigurationContext(config)
        
        listOf(
            localizedContext.getString(R.string.male) to Constants.TYPE_MALE,
            localizedContext.getString(R.string.female) to Constants.TYPE_FEMALE
        )
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
