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
import net.ib.mn.presentation.overlay.articledetail.ArticleDetailScreen
import net.ib.mn.presentation.webview.WebViewScreen
import net.ib.mn.util.ServerUrl
import net.ib.mn.presentation.community.CommunityScreen
import net.ib.mn.presentation.community.IdolRankingHistoryScreen
import net.ib.mn.presentation.community.profile.ProfileScreen
import net.ib.mn.ui.components.LocalHofDailyItemClick
import net.ib.mn.ui.components.LocalIdolRankingHistoryClick
import net.ib.mn.ui.components.LocalRankingItemClick
import net.ib.mn.presentation.community.DailyRankingHistoryScreen
import java.util.Locale
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import net.ib.mn.ui.theme.ColorPalette
import net.ib.mn.navigation.LocalAppNavigator
import net.ib.mn.navigation.Screen
import net.ib.mn.presentation.overlay.articlewrite.ArticleWriteScreen
import net.ib.mn.presentation.overlay.articlewrite.ArticleWriteType
import net.ib.mn.presentation.main.freeboard.FreeBoardViewModel

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

    // 푸시 알림에서 아이돌 커뮤니티로 이동 시 자동으로 커뮤니티 열기
    var pendingCommunityTab by remember { mutableStateOf(initialCommunityTab) }
    val logoutCompleted by viewModel.logoutCompleted.collectAsState()
    val timerText by topBarViewModel.timerText.collectAsState()
    val hasNewNotification by topBarViewModel.hasNewNotification.collectAsState()
    val selectedRankingItem by viewModel.selectedRankingItem.collectAsState()
    val selectedIdolRankingHistoryItem by viewModel.selectedIdolRankingHistoryItem.collectAsState()
    val selectedHofDailyItem by viewModel.selectedHofDailyItem.collectAsState()
    val currentCategory by viewModel.currentCategory.collectAsState()
    val defaultCategory = currentCategory ?: Constants.TYPE_MALE

    // MyInfo 프로필 클릭 시 ProfileScreen 표시 상태
    var showMyProfile by remember { mutableStateOf(false) }
    val userData by viewModel.userCacheRepository.userData.collectAsState(initial = null)

    // FreeBoard 게시글 상세 화면 상태
    var selectedFreeBoardArticle by remember { mutableStateOf<ArticleModel?>(null) }
    var selectedFreeBoardExternalIdolName by remember { mutableStateOf<String?>(null) }
    var freeBoardArticleUpdatedCallback by remember { mutableStateOf<((ArticleModel) -> Unit)?>(null) }

    // Notice 상세 화면 상태
    var selectedNoticeArticle by remember { mutableStateOf<ArticleModel?>(null) }

    // ArticleWrite 오버레이 상태
    var showArticleWriteScreen by remember { mutableStateOf(false) }
    var articleWriteType by remember { mutableStateOf(ArticleWriteType.FREE_BOARD) }
    var articleWriteIdolId by remember { mutableStateOf<Int?>(null) }
    var articleWriteTagId by remember { mutableStateOf<Int?>(null) }
    var editingArticle by remember { mutableStateOf<ArticleModel?>(null) }

    // FreeBoardViewModel (MainScreen과 FreeBoardPage에서 공유)
    val freeBoardViewModel: FreeBoardViewModel = hiltViewModel()

    val configuration = LocalConfiguration.current
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val navigator = LocalAppNavigator.current

    // 백버튼 처리: 탭이 0이 아니면 탭 0으로 이동, 탭이 0이면 앱 종료
    BackHandler {
        if (selectedTab != 0) {
            selectedTab = 0
            viewModel.onTabSelected(0)
        } else {
            (context as? Activity)?.finish()
        }
    }

    LaunchedEffect(Unit) {
        topBarViewModel.startTimer()
        topBarViewModel.checkNewNotification()
        viewModel.checkEvent()
        viewModel.onTabSelected(selectedTab)

        // 푸시 알림에서 아이돌 커뮤니티로 이동 시 자동으로 열기
        initialIdolId?.let { idolId ->
            viewModel.openCommunityByIdolId(idolId)
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
        LocalRankingItemClick provides viewModel::openCommunity,
        LocalIdolRankingHistoryClick provides viewModel::openIdolRankingHistory,
        LocalHofDailyItemClick provides viewModel::openDailyRankingHistory
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
                    onFriendsClick = { },
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
                        onNavigateToProfile = { showMyProfile = true }
                    )
                    3 -> FreeBoardPage(
                        initialTagId = initialFreeBoardTagId,
                        onNavigateToWrite = { tagId ->
                            articleWriteType = ArticleWriteType.FREE_BOARD
                            articleWriteIdolId = null
                            articleWriteTagId = tagId
                            editingArticle = null
                            showArticleWriteScreen = true
                        },
                        onNavigateToArticleDetail = { article, externalTabName, onArticleUpdated ->
                            selectedFreeBoardArticle = article
                            selectedFreeBoardExternalIdolName = externalTabName
                            freeBoardArticleUpdatedCallback = onArticleUpdated
                        },
                        onNavigateToNoticeDetail = { article ->
                            selectedNoticeArticle = article
                        },
                        onNavigateToArticleEdit = { article ->
                            editingArticle = article
                            articleWriteType = ArticleWriteType.FREE_BOARD
                            articleWriteIdolId = article.idol?.id
                            articleWriteTagId = article.tagId.takeIf { it > 0 }
                            showArticleWriteScreen = true
                        },
                        viewModel = freeBoardViewModel
                    )
                    4 -> MenuPage()
                }
            }
        }
    }

    AnimatedVisibility(
        visible = selectedRankingItem != null,
        enter = fadeIn(),
        exit = fadeOut()
    ) {
        selectedRankingItem?.let { rankingItem ->
            val idolId = rankingItem.id.toIntOrNull() ?: return@let

            var showChattingTab by remember { mutableStateOf(false) }
            LaunchedEffect(rankingItem) {
                showChattingTab = viewModel.shouldShowChattingTab(rankingItem)
            }

            // 푸시 알림에서 온 경우 초기 탭 및 최신순 정렬 설정 (한 번만 적용)
            // remember로 초기 값 캡처하여 recomposition에서도 유지
            val communityInitialTab = remember(rankingItem) { pendingCommunityTab ?: 0 }
            val sortLatest = remember(rankingItem) { pendingCommunityTab != null }
            LaunchedEffect(rankingItem) {
                pendingCommunityTab = null // 한 번 사용 후 초기화
            }

            CommunityScreen(
                idolId = idolId,
                showChattingTab = showChattingTab,
                initialTab = communityInitialTab,
                sortLatest = sortLatest,
                onBackClick = viewModel::closeCommunity
            )
        }
    }

    // CUMULATIVE 아이템 클릭 시 IdolRankingHistoryScreen 표시
    selectedIdolRankingHistoryItem?.let { rankingItem ->
        IdolRankingHistoryScreen(
            idolId = rankingItem.id.toIntOrNull() ?: 0,
            idolName = rankingItem.name,
            onBackClick = viewModel::closeIdolRankingHistory
        )
    }

    // HofDailyRankingItem 클릭 시 DailyRankingHistoryScreen 표시
    selectedHofDailyItem?.let { (dailyRankModel, chartCode) ->
        // createdAt 형식: "2024-01-01T00:00:00" -> 날짜 부분만 추출
        val historyParam = remember(dailyRankModel.createdAt) {
            dailyRankModel.createdAt.substringBefore("T")
        }
        // 날짜 타이틀 포맷: "2024-01-01" -> "2024년 1월 1일"
        val dateTitle = remember(historyParam) {
            try {
                val parts = historyParam.split("-")
                if (parts.size == 3) {
                    "${parts[0]}년 ${parts[1].toInt()}월 ${parts[2].toInt()}일"
                } else {
                    historyParam
                }
            } catch (e: Exception) {
                historyParam
            }
        }
        DailyRankingHistoryScreen(
            historyParam = historyParam,
            type = dailyRankModel.idol?.type ?: "",
            chartCode = chartCode,
            dateTitle = dateTitle,
            onBackClick = viewModel::closeDailyRankingHistory
        )
    }

    // MyInfo 프로필 클릭 시 ProfileScreen 표시
    AnimatedVisibility(
        visible = showMyProfile && userData != null,
        enter = fadeIn(),
        exit = fadeOut()
    ) {
        userData?.let { user ->
            ProfileScreen(
                userId = user.id ?: 0,
                userNickname = user.nickname ?: "",
                userImageUrl = user.profileImage,
                userLevel = user.level ?: 0,
                mostIdolName = user.most?.name,
                isMine = true,
                onBackClick = { showMyProfile = false },
                onNavigateToArticleEdit = { article ->
                    editingArticle = article
                    articleWriteType = ArticleWriteType.FEED
                    articleWriteIdolId = article.idol?.id
                    articleWriteTagId = null
                    showArticleWriteScreen = true
                }
            )
        }
    }

    // FreeBoard 게시글 상세 화면
    AnimatedVisibility(
        visible = selectedFreeBoardArticle != null,
        enter = fadeIn(),
        exit = fadeOut()
    ) {
        selectedFreeBoardArticle?.let { article ->
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
                }
            )
        }
    }

    // Notice 상세 화면
    AnimatedVisibility(
        visible = selectedNoticeArticle != null,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = Modifier.fillMaxSize()
    ) {
        selectedNoticeArticle?.let { article ->
            Box(modifier = Modifier.fillMaxSize().background(ColorPalette.background100)) {
                WebViewScreen(
                    htmlContent = article.contentHtml ?: article.content,
                    baseUrl = ServerUrl.HOST,
                    screenTitle = stringResource(R.string.title_notice),
                    contentTitle = article.title,
                    onNavigateBack = {
                        selectedNoticeArticle = null
                    }
                )
            }
        }
    }

    // ArticleWriteScreen 오버레이
    AnimatedVisibility(
        visible = showArticleWriteScreen,
        enter = fadeIn(),
        exit = fadeOut()
    ) {
        ArticleWriteScreen(
            writeType = articleWriteType,
            idolId = editingArticle?.idol?.id ?: articleWriteIdolId,
            tagId = articleWriteTagId,
            editingArticleId = editingArticle?.id,
            onNavigateBack = {
                showArticleWriteScreen = false
                editingArticle = null
                articleWriteIdolId = null
                articleWriteTagId = null
            },
            onNavigateBackWithResult = { updatedArticle ->
                showArticleWriteScreen = false
                editingArticle = null
                articleWriteIdolId = null
                articleWriteTagId = null
                // 수정 완료 시 FreeBoardPage 리스트 업데이트
                updatedArticle?.let { article ->
                    freeBoardViewModel.updateArticle(article)
                }
            }
        )
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
