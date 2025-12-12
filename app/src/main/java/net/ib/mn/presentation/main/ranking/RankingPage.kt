package net.ib.mn.presentation.main.ranking

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.launch
import net.ib.mn.BuildConfig
import net.ib.mn.R
import net.ib.mn.data.model.TypeListModel
import net.ib.mn.data.remote.dto.MainChartModel
import net.ib.mn.di.RankingRepositoryEntryPoint
import net.ib.mn.domain.ranking.GlobalRankingDataSource
import net.ib.mn.domain.ranking.IdolIdsRankingDataSource
import net.ib.mn.domain.ranking.MiracleRookieRankingDataSource
import net.ib.mn.navigation.LocalAppNavigator
import net.ib.mn.navigation.Screen
import net.ib.mn.presentation.main.MainViewModel
import net.ib.mn.presentation.main.ranking.idol_subpage.*
import net.ib.mn.presentation.webview.WebViewScreen
import net.ib.mn.presentation.overlay.themepick.ThemePickDetailScreen
import net.ib.mn.presentation.overlay.themepick.result.ThemePickResultScreen
import net.ib.mn.ui.theme.ColorPalette
import net.ib.mn.ui.theme.ExoTypo
import net.ib.mn.util.Constants
import net.ib.mn.util.ServerUrl

/**
 * Ranking 페이지
 *
 * Best Practice:
 * 1. BuildConfig.CELEB에 따라 UI 구조 분기
 *    - CELEB: Swiper(HorizontalPager)만 사용, 탭바 없음
 *    - 기타: Swiper + PrimaryScrollableTabRow 사용
 * 2. old 프로젝트의 SummaryMainFragment와 RankingPageFragment 구조 참고
 * 3. TypeListModel을 통한 동적 타입 처리
 *
 */
@Composable
fun RankingPage(
    viewModel: RankingPageViewModel = hiltViewModel(),
    mainViewModel: MainViewModel = hiltViewModel()
) {
    // CELEB: typeList 사용
    // 일반: MainChartModel 사용 (old 프로젝트와 동일)
    val typeList by viewModel.typeList.collectAsState()
    val mainChartModel by viewModel.mainChartModel.collectAsState()

    // WebView 상태 관리
    var webViewEventId by rememberSaveable { mutableStateOf<Int?>(null) }
    var webViewTitle by rememberSaveable { mutableStateOf("") }

    // ThemePick 오버레이 상태 관리
    var selectedThemePickDetailId by rememberSaveable { mutableStateOf<Int?>(null) }
    var selectedThemePickResultId by rememberSaveable { mutableStateOf<Int?>(null) }

    // 최애 이동 토스트 상태
    val showMyFavToast by viewModel.showMyFavToast.collectAsState()
    val myFavIdolPosition by viewModel.myFavIdolPosition.collectAsState()

    // 웰컴 미션 버튼 상태
    val showWelcomeMission by viewModel.showWelcomeMission.collectAsState()

    // 이벤트 버튼 상태
    val showAwardButton by viewModel.showAwardButton.collectAsState()
    val awardModel by viewModel.awardModel.collectAsState()

    // NEW 뱃지 표시 상태 (하트픽, 원픽)
    val hasNewHeartPick by viewModel.hasNewHeartPick.collectAsState()
    val hasNewOnePick by viewModel.hasNewOnePick.collectAsState()

    // 첫 번째 탭(개인)의 LazyListState (최애 이동 스크롤용)
    val firstTabListState = rememberLazyListState()

    // 스크롤 시 최애 아이돌이 화면에 보이면 토스트 영구 숨김 (다시 나오지 않음)
    LaunchedEffect(firstTabListState.firstVisibleItemIndex, firstTabListState.layoutInfo.visibleItemsInfo) {
        if (showMyFavToast && myFavIdolPosition >= 0) {
            val visibleItems = firstTabListState.layoutInfo.visibleItemsInfo
            val isMyFavVisible = visibleItems.any { it.index == myFavIdolPosition }
            if (isMyFavVisible) {
                viewModel.onMyFavToastClick()
            }
        }
    }

    val context = LocalContext.current
    val navigator = LocalAppNavigator.current
    val rankingRepository = remember {
        EntryPointAccessors.fromApplication(context.applicationContext, RankingRepositoryEntryPoint::class.java)
            .rankingRepository()
    }

    val globalDataSource = remember { GlobalRankingDataSource(rankingRepository) }
    val groupDataSource = remember { IdolIdsRankingDataSource.forGroup(rankingRepository) }
    val soloDataSource = remember { IdolIdsRankingDataSource.forSolo(rankingRepository) }
    val miracleDataSource = remember { MiracleRookieRankingDataSource.forMiracle(rankingRepository) }
    val rookieDataSource = remember { MiracleRookieRankingDataSource.forRookie(rankingRepository) }

    val currentCategory by mainViewModel.currentCategory.collectAsState()

    // 로딩 체크
    if (currentCategory == null || (BuildConfig.CELEB && typeList.isEmpty()) || (!BuildConfig.CELEB && mainChartModel == null)) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = ColorPalette.main)
        }
        return
    }

    val isMale = currentCategory == Constants.TYPE_MALE

    val tabDataList = if (BuildConfig.CELEB) typeList else buildIdolAppTabList(mainChartModel, viewModel, isMale)
    val savedMainTabIndex by viewModel.selectedTabIndex.collectAsState()

    LaunchedEffect(tabDataList) {
        if (tabDataList.isNotEmpty()) {
            viewModel.initializeTabFromDefaultChartCode(tabDataList) { (it as? TypeListModel)?.code }
        }
    }

    val subPagerState = rememberPagerState(initialPage = savedMainTabIndex, pageCount = { tabDataList.size })
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(subPagerState.currentPage, subPagerState.isScrollInProgress) {
        if (!subPagerState.isScrollInProgress) viewModel.setSelectedTabIndex(subPagerState.currentPage)
    }

    LaunchedEffect(savedMainTabIndex) {
        if (subPagerState.currentPage != savedMainTabIndex) subPagerState.animateScrollToPage(savedMainTabIndex)
    }

    val subPages = remember(tabDataList) { tabDataList.mapIndexed { i, t -> t.code.orEmpty().ifEmpty { "page_$i" } to t } }
    val tabScrollState = rememberScrollState()
    val showLeftGradient by remember { derivedStateOf { tabScrollState.value > 0 } }
    val showRightGradient by remember { derivedStateOf { tabScrollState.value < tabScrollState.maxValue } }

    val backgroundColor = ColorPalette.background100
    val backgroundTransparent = ColorPalette.background100Transparent

    // 탭 라벨 (stringResource는 Composable 컨텍스트 필요)
    val labels = TabLabels(
        solo = stringResource(R.string.lable_individual),
        group = stringResource(R.string.lable_group),
        miracle = stringResource(R.string.miracle),
        rookie = stringResource(R.string.rookie),
        heartpick = stringResource(R.string.heartpick),
        onepick = stringResource(R.string.onepick),
        hof = stringResource(R.string.title_tab_hof),
        maleSinger = stringResource(R.string.actor_male_singer),
        femaleSinger = stringResource(R.string.actor_female_singer),
        actors = stringResource(R.string.lable_actors),
        actresses = stringResource(R.string.lable_actresses)
    )

    val tabs = remember(tabDataList, labels) { buildTabNames(tabDataList, labels) }

    Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        if (!BuildConfig.CELEB) {
            RankingTabRow(
                tabs = tabs,
                tabDataList = tabDataList,
                selectedIndex = subPagerState.currentPage,
                tabScrollState = tabScrollState,
                hasNewHeartPick = hasNewHeartPick,
                hasNewOnePick = hasNewOnePick,
                showLeftGradient = showLeftGradient,
                showRightGradient = showRightGradient,
                backgroundColor = backgroundColor,
                backgroundTransparent = backgroundTransparent,
                onTabClick = { index -> coroutineScope.launch { subPagerState.animateScrollToPage(index) } }
            )
        }

        // 웰컴 미션/이벤트 버튼
        val welcomeButtonPadding by animateDpAsState(
            targetValue = if (showMyFavToast && subPagerState.currentPage == 0) 82.dp else 14.dp,
            animationSpec = tween(300),
            label = "welcomePadding"
        )
        // 이벤트 버튼이 있으면 이벤트 버튼 표시, 없으면 웰컴 미션 버튼 표시
        val showFloatingButton = showAwardButton || showWelcomeMission
        val eventImageUrl = if (showAwardButton) awardModel?.mainFloatingImgUrl else null

        Box(modifier = Modifier.fillMaxSize()) {
            HorizontalPager(
                state = subPagerState,
                modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
                beyondViewportPageCount = 1,
                key = { subPages.getOrNull(it)?.first ?: "page_$it" }
            ) { pageIndex ->
                val (_, currentType) = subPages.getOrNull(pageIndex) ?: return@HorizontalPager
                val isCurrentPage = subPagerState.currentPage == pageIndex
                val chartCode = currentType.code.orEmpty()

                when (currentType.type) {
                    "SOLO" -> UnifiedRankingSubPage(
                        chartCode = chartCode,
                        dataSource = soloDataSource,
                        isVisible = isCurrentPage,
                        listState = if (pageIndex == 0) firstTabListState else null,
                        onRankItemsLoaded = if (pageIndex == 0) viewModel::checkMyFavToast else null
                    )
                    "GROUP" -> UnifiedRankingSubPage(chartCode = chartCode, dataSource = groupDataSource, isVisible = isCurrentPage)
                    "MIRACLE" -> MiracleRookieRankingSubPage(
                        chartCode = chartCode,
                        dataSource = miracleDataSource,
                        isVisible = isCurrentPage,
                        onInfoClick = { webViewEventId = it; webViewTitle = context.getString(R.string.title_miracle_month) }
                    )
                    "ROOKIE" -> MiracleRookieRankingSubPage(
                        chartCode = chartCode,
                        dataSource = rookieDataSource,
                        isVisible = isCurrentPage,
                        onInfoClick = { webViewEventId = it; webViewTitle = "Rookie" }
                    )
                    "HEARTPICK" -> HeartPickRankingSubPage(chartCode = chartCode, isVisible = isCurrentPage)
                    "ONEPICK" -> OnePickRankingSubPage(
                        chartCode = chartCode,
                        isVisible = isCurrentPage,
                        onThemePickDetailClick = { id -> selectedThemePickDetailId = id },
                        onThemePickResultClick = { id -> selectedThemePickResultId = id }
                    )
                    "HOF" -> HallOfFameRankingSubPage(
                        chartCode = chartCode,
                        isVisible = isCurrentPage,
                        topThreeTabs = tabs.take(3),
                        topThreeChartCodes = tabDataList.take(3).map { it.code.orEmpty() }
                    )
                    "GLOBALS" -> UnifiedRankingSubPage(chartCode = chartCode, dataSource = globalDataSource, isVisible = isCurrentPage)
                    else -> Box(Modifier.fillMaxSize(), Alignment.Center) { Text("Unsupported: ${currentType.type}") }
                }
            }

            if (showFloatingButton) {
                WelcomeMissionButton(
                    modifier = Modifier.align(Alignment.BottomEnd).padding(end = 8.dp, bottom = welcomeButtonPadding),
                    eventImageUrl = eventImageUrl,
                    onClick = {
                        if (showAwardButton) {
                            navigator.navigate(Screen.Awards)
                        } else {
                            // TODO: WelcomeMissionFragment로 이동
                        }
                    }
                )
            }

            if (showMyFavToast && subPagerState.currentPage == 0) {
                ShowMyFavToast(
                    modifier = Modifier.align(Alignment.BottomCenter)
                ) {
                    coroutineScope.launch {
                        if (myFavIdolPosition >= 0) firstTabListState.animateScrollToItem((myFavIdolPosition - 2).coerceAtLeast(0))
                        viewModel.onMyFavToastClick()
                    }
                }
            }
        }
    }

    webViewEventId?.let { eventId ->
        Dialog(onDismissRequest = { webViewEventId = null }) {
            WebViewScreen(
                url = "${ServerUrl.HOST}/api/v1/events/$eventId/",
                title = webViewTitle,
                onNavigateBack = { webViewEventId = null }
            )
        }
    }

    // ThemePickDetailScreen 오버레이
    AnimatedVisibility(
        visible = selectedThemePickDetailId != null,
        enter = fadeIn(),
        exit = fadeOut()
    ) {
        selectedThemePickDetailId?.let { themePickId ->
            ThemePickDetailScreen(
                themePickId = themePickId,
                onBackClick = { selectedThemePickDetailId = null },
                onNavigateToResult = { id ->
                    selectedThemePickDetailId = null
                    selectedThemePickResultId = id
                }
            )
        }
    }

    // ThemePickResultScreen 오버레이
    AnimatedVisibility(
        visible = selectedThemePickResultId != null,
        enter = fadeIn(),
        exit = fadeOut()
    ) {
        selectedThemePickResultId?.let { themePickId ->
            ThemePickResultScreen(
                themePickId = themePickId,
                onBackClick = { selectedThemePickResultId = null },
                onNavigateToVote = { id ->
                    selectedThemePickResultId = null
                    selectedThemePickDetailId = id
                },
                onNavigateToCommunity = { idolId ->
                    selectedThemePickResultId = null
                    navigator.navigate(Screen.Community(idolId))
                }
            )
        }
    }
}

// 탭 라벨 데이터 클래스
private data class TabLabels(
    val solo: String, val group: String, val miracle: String, val rookie: String,
    val heartpick: String, val onepick: String, val hof: String,
    val maleSinger: String, val femaleSinger: String, val actors: String, val actresses: String
)

private fun buildTabNames(tabDataList: List<TypeListModel>, labels: TabLabels): List<String> {
    return tabDataList.map { type ->
        val baseName = if (BuildConfig.CELEB) {
            val typeCheck = if (type.isDivided == "N" && !type.isFemale) null else if (type.isFemale) "F" else "M"
            when (type.type) {
                "S" -> when (typeCheck) { "M" -> labels.maleSinger; "F" -> labels.femaleSinger; else -> type.name }
                "A" -> when (typeCheck) { "M" -> labels.actors; "F" -> labels.actresses; else -> type.name }
                else -> type.name
            }
        } else {
            when (type.type) {
                "SOLO" -> labels.solo; "GROUP" -> labels.group; "MIRACLE" -> labels.miracle
                "ROOKIE" -> labels.rookie; "HEARTPICK" -> labels.heartpick
                "ONEPICK" -> labels.onepick; "HOF" -> labels.hof; else -> type.name
            }
        }
        if (ServerUrl.isTestServer() && type.type in listOf("SOLO", "GROUP")) "(테섭)$baseName(테섭)" else baseName
    }
}

@Composable
private fun RankingTabRow(
    tabs: List<String>,
    tabDataList: List<TypeListModel>,
    selectedIndex: Int,
    tabScrollState: androidx.compose.foundation.ScrollState,
    hasNewHeartPick: Boolean,
    hasNewOnePick: Boolean,
    showLeftGradient: Boolean,
    showRightGradient: Boolean,
    backgroundColor: androidx.compose.ui.graphics.Color,
    backgroundTransparent: androidx.compose.ui.graphics.Color,
    onTabClick: (Int) -> Unit
) {
    Box(modifier = Modifier.fillMaxWidth()) {
        PrimaryScrollableTabRow(
            minTabWidth = 0.dp,
            scrollState = tabScrollState,
            selectedTabIndex = selectedIndex,
            containerColor = backgroundColor,
            contentColor = ColorPalette.main,
            edgePadding = 3.dp,
            divider = {},
            indicator = {
                TabRowDefaults.SecondaryIndicator(
                    modifier = Modifier.tabIndicatorOffset(selectedIndex).padding(horizontal = 12.dp),
                    color = ColorPalette.main
                )
            }
        ) {
            tabs.forEachIndexed { index, tabName ->
                val showNewBadge = when (tabDataList.getOrNull(index)?.type) {
                    "HEARTPICK" -> hasNewHeartPick
                    "ONEPICK" -> hasNewOnePick
                    else -> false
                }
                Box(
                    modifier = Modifier
                        .wrapContentWidth()
                        .height(48.dp)
                        .clickable(remember { MutableInteractionSource() }, null) { onTabClick(index) }
                        .padding(horizontal = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = tabName,
                        style = ExoTypo.title14.copy(
                            lineHeight = 14.sp,
                            color = if (selectedIndex == index) ColorPalette.main else ColorPalette.textDimmed
                        )
                    )
                    if (showNewBadge) {
                        val fontSizeInDp = with(LocalDensity.current) { 8.dp.toSp() }
                        Text(
                            text = "NEW",
                            style = TextStyle(fontSize = fontSizeInDp, fontWeight = FontWeight.Bold, color = ColorPalette.main),
                            modifier = Modifier.align(Alignment.TopCenter).padding(top = 6.dp)
                        )
                    }
                }
            }
        }
        if (showLeftGradient) {
            Box(Modifier.width(40.dp).height(48.dp).align(Alignment.CenterStart)
                .background(Brush.horizontalGradient(listOf(backgroundColor, backgroundTransparent))))
        }
        if (showRightGradient) {
            Box(Modifier.width(40.dp).height(48.dp).align(Alignment.CenterEnd)
                .background(Brush.horizontalGradient(listOf(backgroundTransparent, backgroundColor))))
        }
    }
}

@Composable
private fun buildIdolAppTabList(
    mainChartModel: MainChartModel?,
    viewModel: RankingPageViewModel,
    isMale: Boolean
): List<TypeListModel> {
    val tabList = mutableListOf<TypeListModel>()
    val chartList = if (isMale) mainChartModel?.males else mainChartModel?.females

    chartList?.forEach { chartInfo ->
        tabList.add(TypeListModel(
            id = 0, name = chartInfo.name.orEmpty(), type = extractTypeFromCode(chartInfo.code.orEmpty()),
            code = chartInfo.code, isDivided = "N", isFemale = !isMale, showDivider = false
        ))
    }

    viewModel.configRepository.getChartObjects()?.forEach { chart ->
        val (name, type) = when (chart.type) {
            "M" -> "MIRACLE" to "MIRACLE"
            "R" -> "ROOKIE" to "ROOKIE"
            else -> return@forEach
        }
        tabList.add(TypeListModel(id = 0, name = name, type = type, code = chart.code, isDivided = "N", isFemale = false, showDivider = false))
    }

    val suffix = if (isMale) "_M" else "_F"
    listOf("HEARTPICK" to "HEARTPICK", "ONEPICK" to "ONEPICK").forEach { (name, type) ->
        tabList.add(TypeListModel(id = 0, name = name, type = type, code = "$type$suffix", isDivided = "N", isFemale = false, showDivider = false))
    }

    val hofCode = viewModel.configRepository.getChartObjects()?.firstOrNull {
        it.code?.contains("HOF", true) == true || it.code?.contains("HALL", true) == true
    }?.code ?: "HOF$suffix"
    tabList.add(TypeListModel(id = 0, name = "HOF", type = "HOF", code = hofCode, isDivided = "N", isFemale = false, showDivider = false))

    return tabList
}

/**
 * 차트 코드에서 타입 추출
 *
 * 예: "SOLO_M" -> "SOLO", "GROUP_F" -> "GROUP", "PR_S_M" -> "SOLO", "PR_G_M" -> "GROUP"
 */
private fun extractTypeFromCode(code: String): String {
    return when {
        code.startsWith("SOLO") -> "SOLO"
        code.startsWith("GROUP") -> "GROUP"
        code.contains("_S_") -> "SOLO"   // PR_S_M, PR_S_F 등 처리
        code.contains("_G_") -> "GROUP"  // PR_G_M, PR_G_F 등 처리
        else -> code
    }
}

/**
 * 웰컴 미션/이벤트 플로팅 버튼 컴포넌트
 * old 프로젝트의 iv_mission (btn_welcome.xml) 및 iv_awards 통합
 *
 * @param eventImageUrl 이벤트 이미지 URL (null이면 기본 웰컴 미션 아이콘 표시)
 * @param onClick 클릭 이벤트
 */
@Composable
private fun WelcomeMissionButton(
    modifier: Modifier = Modifier,
    eventImageUrl: String? = null,
    onClick: () -> Unit
) {
    if (!eventImageUrl.isNullOrEmpty()) {
        // 이벤트 이미지 + Y축 회전 애니메이션 (old 프로젝트 animateAwardButton)
        val infiniteTransition = rememberInfiniteTransition(label = "awardRotation")
        val rotationY by infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 360f,
            animationSpec = infiniteRepeatable(
                animation = keyframes {
                    durationMillis = 2600  // 0->180(300ms) + 180->0(300ms) + delay(2000ms)
                    0f at 0
                    180f at 300
                    0f at 600
                    0f at 2600
                },
                repeatMode = RepeatMode.Restart
            ),
            label = "rotationY"
        )

        coil.compose.AsyncImage(
            model = eventImageUrl,
            contentDescription = "Event",
            modifier = modifier
                .size(64.dp)
                .graphicsLayer {
                    this.rotationY = rotationY
                    cameraDistance = 12f * density
                }
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onClick
                ),
            contentScale = androidx.compose.ui.layout.ContentScale.Fit
        )
    } else {
        // 기본 웰컴 미션 아이콘 (애니메이션 없음)
        Image(
            painter = painterResource(id = R.drawable.btn_welcome),
            contentDescription = "Welcome Mission",
            modifier = modifier
                .size(64.dp)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onClick
                )
        )
    }
}

/**
 * 최애 이동 토스트 컴포넌트
 * old 프로젝트의 MostToast.kt와 동일한 UI
 */
@Composable
private fun ShowMyFavToast(
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .padding(start = 8.dp, end = 8.dp, bottom = 8.dp)
            .fillMaxWidth()
            .background(
                color = colorResource(id = R.color.main100),
                shape = RoundedCornerShape(20.dp)
            )
            .border(
                width = 1.dp,
                color = colorResource(id = R.color.main300),
                shape = RoundedCornerShape(20.dp)
            )
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier
                .padding(20.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Image(
                    painter = painterResource(id = R.drawable.icon_toast_heart),
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = stringResource(id = R.string.banner_go_myidol_title),
                    color = colorResource(id = R.color.text_default),
                    style = TextStyle(
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                )
            }
            Text(
                text = stringResource(id = R.string.banner_go_myidol_btn),
                color = colorResource(id = R.color.main_light),
                textDecoration = TextDecoration.Underline,
                style = TextStyle(
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                ),
                maxLines = 1
            )
        }
    }
}
