package net.ib.mn.presentation.main

import android.app.Activity
import android.content.res.Configuration
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import net.ib.mn.R
import net.ib.mn.ui.components.ExoScaffold
import net.ib.mn.ui.components.MainBottomNavigation
import net.ib.mn.ui.components.MainTopBar
import net.ib.mn.ui.components.SwitchToggleButton
import net.ib.mn.ui.theme.ExodusTheme
import net.ib.mn.util.Constants
import net.ib.mn.presentation.main.freeboard.FreeBoardPage
import net.ib.mn.presentation.main.menu.MenuPage
import net.ib.mn.presentation.main.myfavorite.MyFavoritePage
import net.ib.mn.presentation.main.myinfo.MyInfoPage
import net.ib.mn.presentation.main.ranking.RankingPage
import net.ib.mn.domain.model.ArticleModel
import net.ib.mn.presentation.article.detail.ArticleDetailScreen
import net.ib.mn.presentation.webview.WebViewScreen
import net.ib.mn.util.ServerUrl
import net.ib.mn.ui.components.LocalHofDailyItemClick
import net.ib.mn.ui.components.LocalIdolRankingHistoryClick
import net.ib.mn.ui.components.LocalRankingItemClick
import net.ib.mn.ui.components.LocalHeartPickDetailClick
import java.util.Locale
import net.ib.mn.ui.theme.ColorPalette
import net.ib.mn.ui.components.ExoOverlay
import net.ib.mn.navigation.LocalAppNavigator
import net.ib.mn.navigation.Screen
import net.ib.mn.presentation.main.freeboard.FreeBoardViewModel
import net.ib.mn.presentation.friend.invite.FriendInviteScreen
import net.ib.mn.presentation.profile.ProfileScreen
import net.ib.mn.ui.components.LocalThemePickDetailClick
import net.ib.mn.ui.components.LocalThemePickLiveClick
import net.ib.mn.ui.components.LocalImagePickDetailClick
import net.ib.mn.ui.components.LocalImagePickLiveClick

/**
 * 메인 화면.
 * 하단 네비게이션 바와 상단 앱바를 포함한 메인 컨테이너.
 * 각 탭은 별도의 Page로 구성되며, MainScreen에서 topBar를 관리합니다.
 */
@Composable
fun MainScreen(
    initialTab: Int = 0,
    initialIdolId: Int? = null,
    initialCommunityTab: Int? = null,
    initialFreeBoardTagId: Int? = null,
    viewModel: MainViewModel = hiltViewModel(),
    topBarViewModel: MainTopBarViewModel = hiltViewModel(),
    onLogout: () -> Unit = {}
) {
    var selectedTab by rememberSaveable { mutableIntStateOf(initialTab) }

    val logoutCompleted by viewModel.logoutCompleted.collectAsState()
    val timerText by topBarViewModel.timerText.collectAsState()
    val hasNewNotification by topBarViewModel.hasNewNotification.collectAsState()
    val currentCategory by viewModel.currentCategory.collectAsState()
    val defaultCategory = currentCategory ?: Constants.TYPE_MALE

    // MyInfo 프로필 클릭 시 ProfileScreen으로 이동하기 위한 사용자 데이터
    val userData by viewModel.userCacheRepository.userData.collectAsState(initial = null)

    // FreeBoard 게시글 상세 화면 상태
    var selectedFreeBoardArticle by remember { mutableStateOf<ArticleModel?>(null) }
    var selectedFreeBoardExternalIdolName by remember { mutableStateOf<String?>(null) }
    var freeBoardArticleUpdatedCallback by remember { mutableStateOf<((ArticleModel) -> Unit)?>(null) }

    // FreeBoard 게시글 상세에서 프로필 클릭 시 상태
    var freeBoardSelectedUserProfile by remember { mutableStateOf<FreeBoardUserProfileInfo?>(null) }

    // Notice 상세 화면 상태
    var selectedNoticeArticle by remember { mutableStateOf<ArticleModel?>(null) }

    // FriendInvite 화면 상태
    var showFriendInviteScreen by remember { mutableStateOf(false) }
    var friendInviteToken by remember { mutableStateOf("") }
    var friendInviteLanguage by remember { mutableStateOf("") }

    // FreeBoardViewModel (MainScreen과 FreeBoardPage에서 공유)
    val freeBoardViewModel: FreeBoardViewModel = hiltViewModel()


    val configuration = LocalConfiguration.current
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val navigator = LocalAppNavigator.current

    // 백버튼 처리: 오버레이 먼저 닫기 -> 탭 0으로 이동 -> 앱 종료
    // Navigation 화면들은 NavGraph에서 자동 처리됨
    BackHandler {
        when {
            // Notice 상세 화면 오버레이
            selectedNoticeArticle != null -> {
                selectedNoticeArticle = null
            }
            // FreeBoard 프로필 오버레이 (FreeBoard 게시글 위에 열림)
            freeBoardSelectedUserProfile != null -> {
                freeBoardSelectedUserProfile = null
            }
            // FreeBoard 게시글 상세 오버레이
            selectedFreeBoardArticle != null -> {
                selectedFreeBoardArticle = null
                selectedFreeBoardExternalIdolName = null
                freeBoardArticleUpdatedCallback = null
            }
            // FriendInviteScreen 오버레이
            showFriendInviteScreen -> {
                showFriendInviteScreen = false
            }
            // 탭이 0이 아니면 탭 0으로 이동
            selectedTab != 0 -> {
                selectedTab = 0
                viewModel.onTabSelected(0)
            }
            // 탭이 0이면 앱 종료
            else -> {
                (context as? Activity)?.finish()
            }
        }
    }

    LaunchedEffect(Unit) {
        topBarViewModel.startTimer()
        topBarViewModel.checkNewNotification()
        viewModel.checkEvent()
        viewModel.onTabSelected(selectedTab)

        // 푸시 알림에서 아이돌 커뮤니티로 이동 시 Navigation으로 열기
        initialIdolId?.let { idolId ->
            navigator.navigate(Screen.Community(
                idolId = idolId,
                initialTab = initialCommunityTab ?: 0,
                sortLatest = initialCommunityTab != null
            ))
        }
    }

    LaunchedEffect(logoutCompleted) {
        if (logoutCompleted) onLogout()
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> viewModel.onAppResume()
                Lifecycle.Event.ON_PAUSE -> viewModel.onAppPause()
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val genderStrings = remember(configuration) {
        val locale = configuration.locales[0]
        when ("${locale.language}_${locale.country}") {
            "ko_KR" -> getGenderString(context, Locale.KOREA)
            "ja_JP" -> getGenderString(context, Locale.JAPAN)
            "zh_CN" -> getGenderString(context, Locale("zh", "CN"))
            "zh_TW" -> getGenderString(context, Locale("zh", "TW"))
            else -> listOf(Constants.TYPE_MALE to Constants.TYPE_MALE, Constants.TYPE_FEMALE to Constants.TYPE_FEMALE)
        }
    }

    val maleIndex = remember(genderStrings) { genderStrings.indexOfFirst { it.second == Constants.TYPE_MALE } }
    val isMaleSelected = remember(defaultCategory, maleIndex) {
        if (maleIndex == 0) defaultCategory == Constants.TYPE_MALE else defaultCategory != Constants.TYPE_MALE
    }

    val menus = listOf(
        stringResource(R.string.hometab_title_rank),
        stringResource(R.string.hometab_title_myidol),
        stringResource(R.string.hometab_title_profile),
        stringResource(R.string.hometab_title_freeboard),
        stringResource(R.string.hometab_title_menu)
    )

    val iconsOfSelected = listOf(
        painterResource(R.drawable.btn_bottom_nav_ranking_on),
        painterResource(R.drawable.btn_bottom_nav_favorite_on),
        painterResource(R.drawable.btn_bottom_nav_my_on),
        painterResource(R.drawable.btn_bottom_nav_board_on),
        painterResource(R.drawable.btn_bottom_nav_menu_on)
    )

    val iconsOfUnSelected = listOf(
        painterResource(R.drawable.btn_bottom_nav_ranking_off),
        painterResource(R.drawable.btn_bottom_nav_favorite_off),
        painterResource(R.drawable.btn_bottom_nav_my_off),
        painterResource(R.drawable.btn_bottom_nav_board_off),
        painterResource(R.drawable.btn_bottom_nav_menu_off)
    )

    CompositionLocalProvider(
        LocalRankingItemClick provides { rankingItem ->
            navigator.navigate(Screen.Community(idolId = rankingItem.id.toIntOrNull() ?: return@provides))
        },
        LocalIdolRankingHistoryClick provides { rankingItem ->
            navigator.navigate(Screen.IdolRankingHistory(
                idolId = rankingItem.id.toIntOrNull() ?: return@provides,
                idolName = rankingItem.name
            ))
        },
        LocalHofDailyItemClick provides { dailyRankModel, chartCode ->
            val historyParam = dailyRankModel.createdAt.substringBefore("T")
            val dateTitle = try {
                val parts = historyParam.split("-")
                if (parts.size == 3) {
                    "${parts[0]}년 ${parts[1].toInt()}월 ${parts[2].toInt()}일"
                } else {
                    historyParam
                }
            } catch (e: Exception) {
                historyParam
            }
            navigator.navigate(Screen.DailyRankingHistory(
                historyParam = historyParam,
                type = dailyRankModel.idol?.type ?: "",
                chartCode = chartCode,
                dateTitle = dateTitle
            ))
        },
        LocalHeartPickDetailClick provides { heartPickId ->
            navigator.navigate(Screen.HeartPickDetail(heartPickId))
        },
        LocalThemePickDetailClick provides { themePickId ->
            navigator.navigate(Screen.ThemePickDetail(themePickId))
        },
        LocalThemePickLiveClick provides { themePickId ->
            navigator.navigate(Screen.ThemePickLive(themePickId))
        },
        LocalImagePickDetailClick provides { imagePickId ->
            navigator.navigate(Screen.ImagePickDetail(imagePickId))
        },
        LocalImagePickLiveClick provides { imagePickId ->
            navigator.navigate(Screen.ImagePickLive(imagePickId))
        }
    ) {
        ExoScaffold(
            topBar = {
                MainTopBar(
                    timerText = timerText,
                    showToggleButton = selectedTab == 0,
                    showMainMenu = selectedTab in 0..3,
                    showMyInfoMenu = selectedTab == 4,
                    hasNewNotification = hasNewNotification,
                    toggleButton = {
                        SwitchToggleButton(
                            genderList = genderStrings,
                            isMaleSelected = isMaleSelected,
                            boxBackgroundColor = colorResource(R.color.gray100),
                            boxTextColor = colorResource(R.color.text_gray),
                            thumbBackgroundColor = colorResource(R.color.text_default),
                            thumbTextColor = colorResource(R.color.text_white_black),
                            onCategoryChanged = viewModel::setCategory
                        )
                    },
                    onSearchClick = { navigator.navigate(Screen.Search) },
                    onFriendsClick = { navigator.navigate(Screen.Friend) },
                    onAttendanceClick = { },
                    onNotificationClick = { },
                    onSettingClick = { }
                )
            },
            bottomBar = {
                MainBottomNavigation(
                    menus = menus,
                    iconsOfSelected = iconsOfSelected,
                    iconsOfUnSelected = iconsOfUnSelected,
                    initialSelectedIndex = selectedTab,
                    defaultBackgroundColor = colorResource(R.color.background_200),
                    defaultBorderColor = colorResource(R.color.gray150),
                    defaultTextColor = colorResource(R.color.text_default),
                    onTabSelected = { tab ->
                        selectedTab = tab
                        viewModel.onTabSelected(tab)
                    }
                )
            }
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                when (selectedTab) {
                    0 -> RankingPage()
                    1 -> MyFavoritePage()
                    2 -> MyInfoPage(
                        onNavigateToProfile = {
                            userData?.let { user ->
                                navigator.navigate(Screen.Profile(
                                    userId = user.id ?: 0,
                                    nickname = user.nickname ?: "",
                                    imageUrl = user.profileImage,
                                    level = user.level ?: 0,
                                    mostIdolName = user.most?.name,
                                    isMine = true
                                ))
                            }
                        }
                    )
                    3 -> FreeBoardPage(
                        initialTagId = initialFreeBoardTagId,
                        onNavigateToArticleDetail = { article, externalTabName, onArticleUpdated ->
                            selectedFreeBoardArticle = article
                            selectedFreeBoardExternalIdolName = externalTabName
                            freeBoardArticleUpdatedCallback = onArticleUpdated
                        },
                        onNavigateToNoticeDetail = { article ->
                            selectedNoticeArticle = article
                        },
                        viewModel = freeBoardViewModel
                    )
                    4 -> MenuPage(
                        onNavigateToFriendInvite = { token, language ->
                            friendInviteToken = token
                            friendInviteLanguage = language
                            showFriendInviteScreen = true
                        }
                    )
                }
            }
        }
    }

    // FriendInvite 화면 오버레이
    ExoOverlay(
        visible = showFriendInviteScreen,
        onDismiss = { showFriendInviteScreen = false },
        enableBackHandler = false
    ) {
        FriendInviteScreen(
            token = friendInviteToken,
            language = friendInviteLanguage,
            onBackClick = { showFriendInviteScreen = false }
        )
    }

    // FreeBoard 게시글 상세 오버레이
    ExoOverlay(
        data = selectedFreeBoardArticle,
        onDismiss = {
            selectedFreeBoardArticle = null
            selectedFreeBoardExternalIdolName = null
            freeBoardArticleUpdatedCallback = null
        },
        enableBackHandler = false
    ) { article ->
        ArticleDetailScreen(
            article = article,
            externalTabName = selectedFreeBoardExternalIdolName,
            onBackClick = {
                selectedFreeBoardArticle = null
                selectedFreeBoardExternalIdolName = null
                freeBoardArticleUpdatedCallback = null
            },
            onArticleUpdated = { updatedArticle ->
                freeBoardArticleUpdatedCallback?.invoke(updatedArticle)
            },
            onNavigateToProfile = { userId, nickname, imageUrl, level, mostIdolName ->
                freeBoardSelectedUserProfile = FreeBoardUserProfileInfo(
                    userId = userId,
                    nickname = nickname,
                    imageUrl = imageUrl,
                    level = level,
                    mostIdolName = mostIdolName
                )
            }
        )
    }

    // FreeBoard 프로필 오버레이
    ExoOverlay(
        data = freeBoardSelectedUserProfile,
        onDismiss = { freeBoardSelectedUserProfile = null },
        enableBackHandler = false
    ) { userInfo ->
        ProfileScreen(
            userId = userInfo.userId,
            userNickname = userInfo.nickname,
            userImageUrl = userInfo.imageUrl,
            userLevel = userInfo.level,
            mostIdolName = userInfo.mostIdolName,
            isMine = false,
            onBackClick = { freeBoardSelectedUserProfile = null }
        )
    }

    // Notice 상세 오버레이
    ExoOverlay(
        data = selectedNoticeArticle,
        onDismiss = { selectedNoticeArticle = null },
        enableBackHandler = false
    ) { article ->
        Box(modifier = Modifier.fillMaxSize().background(ColorPalette.background100)) {
            WebViewScreen(
                htmlContent = article.contentHtml ?: article.content,
                baseUrl = ServerUrl.HOST,
                screenTitle = stringResource(R.string.title_notice),
                contentTitle = article.title,
                onNavigateBack = { selectedNoticeArticle = null }
            )
        }
    }

}

private fun getGenderString(context: android.content.Context, locale: Locale): List<Pair<String, String>> {
    val config = Configuration(context.resources.configuration).apply { setLocale(locale) }
    val localizedContext = context.createConfigurationContext(config)
    return listOf(
        localizedContext.getString(R.string.male) to Constants.TYPE_MALE,
        localizedContext.getString(R.string.female) to Constants.TYPE_FEMALE
    )
}

/**
 * FreeBoard 게시글 상세에서 프로필 클릭 시 사용되는 데이터 클래스
 */
private data class FreeBoardUserProfileInfo(
    val userId: Int,
    val nickname: String,
    val imageUrl: String?,
    val level: Int,
    val mostIdolName: String? = null
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
