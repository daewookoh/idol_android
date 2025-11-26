package net.ib.mn.presentation.main.ranking

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
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
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import net.ib.mn.ui.theme.ColorPalette
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.launch
import net.ib.mn.BuildConfig
import net.ib.mn.R
import net.ib.mn.domain.ranking.GlobalRankingDataSource
import net.ib.mn.domain.ranking.IdolIdsRankingDataSource
import net.ib.mn.domain.ranking.MiracleRookieRankingDataSource
import net.ib.mn.presentation.main.MainViewModel
import net.ib.mn.ui.theme.ExoTypo
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

    // 최애 이동 토스트 상태
    val showMyFavToast by viewModel.showMyFavToast.collectAsState()
    val myFavIdolPosition by viewModel.myFavIdolPosition.collectAsState()

    // 웰컴 미션 버튼 상태
    val showWelcomeMission by viewModel.showWelcomeMission.collectAsState()

    // 첫 번째 탭(개인)의 LazyListState (최애 이동 스크롤용)
    val firstTabListState = rememberLazyListState()

    // 스크롤 시 최애 아이돌이 화면에 보이면 토스트 영구 숨김 (다시 나오지 않음)
    LaunchedEffect(firstTabListState.firstVisibleItemIndex, firstTabListState.layoutInfo.visibleItemsInfo) {
        if (showMyFavToast && myFavIdolPosition >= 0) {
            val visibleItems = firstTabListState.layoutInfo.visibleItemsInfo
            val isMyFavVisible = visibleItems.any { it.index == myFavIdolPosition }
            if (isMyFavVisible) {
                android.util.Log.d("RankingPage", "👀 My favorite idol is now visible at position $myFavIdolPosition - permanently hiding toast")
                viewModel.onMyFavToastClick()  // 영구적으로 토스트 숨김 (PreferencesManager에 기록)
            }
        }
    }

    // RankingRepository EntryPoint를 통해 주입
    val context = LocalContext.current
    val rankingRepository = remember {
        EntryPointAccessors.fromApplication(
            context.applicationContext,
            net.ib.mn.di.RankingRepositoryEntryPoint::class.java
        ).rankingRepository()
    }

    // DataSource 생성 (remember로 캐싱)
    val globalDataSource = remember {
        val ds = GlobalRankingDataSource(rankingRepository)
        android.util.Log.d("RankingPage", "📦 Created GlobalDataSource: ${ds.hashCode()}, type=${ds.type}")
        ds
    }
    val groupDataSource = remember {
        val ds = IdolIdsRankingDataSource.forGroup(rankingRepository)
        android.util.Log.d("RankingPage", "📦 Created GroupDataSource: ${ds.hashCode()}, type=${ds.type}")
        ds
    }
    val soloDataSource = remember {
        val ds = IdolIdsRankingDataSource.forSolo(rankingRepository)
        android.util.Log.d("RankingPage", "📦 Created SoloDataSource: ${ds.hashCode()}, type=${ds.type}")
        ds
    }
    val miracleDataSource = remember {
        val ds = MiracleRookieRankingDataSource.forMiracle(rankingRepository)
        android.util.Log.d("RankingPage", "📦 Created MiracleDataSource: ${ds.hashCode()}, type=${ds.type}")
        ds
    }
    val rookieDataSource = remember {
        val ds = MiracleRookieRankingDataSource.forRookie(rankingRepository)
        android.util.Log.d("RankingPage", "📦 Created RookieDataSource: ${ds.hashCode()}, type=${ds.type}")
        ds
    }

    // MainScreen에서 관리하는 성별 카테고리 (old 프로젝트와 동일)
    // 즉시 반응하는 로컬 카테고리 상태 사용 (UI 반응성 개선)
    val currentCategory by mainViewModel.currentCategory.collectAsState()

    // 프로세스 복원 시 데이터 재로드
    androidx.compose.runtime.LaunchedEffect(mainChartModel, typeList) {
        if (BuildConfig.CELEB) {
            if (typeList.isEmpty()) {
                android.util.Log.d("RankingPage", "⚠️ TypeList is empty - data may need to be reloaded")
            }
        } else {
            if (mainChartModel == null) {
                android.util.Log.d("RankingPage", "⚠️ MainChartModel is null - RankingPageViewModel will reload")
                // RankingPageViewModel의 init에서 자동으로 재로드됨
            }
        }
    }

    // 카테고리 로딩 중이거나 데이터가 없으면 로딩 표시
    if (currentCategory == null) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            androidx.compose.material3.CircularProgressIndicator(
                color = ColorPalette.main
            )
        }
        return
    }

    val isMale = currentCategory == net.ib.mn.util.Constants.TYPE_MALE

    // CELEB: typeList 확인
    // 일반: mainChartModel 확인
    if (BuildConfig.CELEB) {
        if (typeList.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                androidx.compose.material3.CircularProgressIndicator(
                    color = ColorPalette.main
                )
            }
            return
        }
    } else {
        if (mainChartModel == null) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                androidx.compose.material3.CircularProgressIndicator(
                    color = ColorPalette.main
                )
            }
            return
        }
    }

    // CELEB: typeList 사용
    // 일반: old 프로젝트와 동일한 순서로 탭 생성
    //   1. 개인/그룹 차트 (MainChartModel.males 또는 females)
    //   2. MIRACLE, ROOKIE (objects에서)
    //   3. HEARTPICK, ONEPICK, HOF (하드코딩)
    val tabDataList = if (BuildConfig.CELEB) {
        typeList
    } else {
        android.util.Log.d("RankingPage", "========================================")
        android.util.Log.d("RankingPage", "[RankingPage] Building tab list")
        android.util.Log.d("RankingPage", "  - currentCategory: $currentCategory")
        android.util.Log.d("RankingPage", "  - isMale: $isMale")
        android.util.Log.d("RankingPage", "========================================")
        buildIdolAppTabList(mainChartModel, viewModel, isMale)
    }

    // 메인 탭 선택 상태 (ViewModel에서 관리, 바텀 네비게이션 이동 시에도 유지)
    val savedMainTabIndex by viewModel.selectedTabIndex.collectAsState()

    // 앱 첫 실행 시 defaultChartCode로부터 초기 탭 설정
    LaunchedEffect(tabDataList) {
        if (tabDataList.isNotEmpty()) {
            viewModel.initializeTabFromDefaultChartCode(tabDataList) { tab ->
                // TypeListModel에서 code 추출
                (tab as? net.ib.mn.data.model.TypeListModel)?.code
            }
        }
    }

    val subPagerState = rememberPagerState(
        initialPage = savedMainTabIndex, // 저장된 탭에서 시작
        pageCount = { tabDataList.size }
    )
    val coroutineScope = rememberCoroutineScope()

    // 탭 변경 시 ViewModel에 저장
    LaunchedEffect(subPagerState.currentPage, subPagerState.isScrollInProgress) {
        if (!subPagerState.isScrollInProgress) {
            viewModel.setSelectedTabIndex(subPagerState.currentPage)
        }
    }

    // savedMainTabIndex가 변경되면 pager도 해당 페이지로 이동
    LaunchedEffect(savedMainTabIndex) {
        if (subPagerState.currentPage != savedMainTabIndex) {
            android.util.Log.d("RankingPage", "📌 Moving pager to tab: $savedMainTabIndex")
            subPagerState.animateScrollToPage(savedMainTabIndex)
        }
    }

    // 모든 탭의 SubPage를 미리 생성하여 완전히 독립적으로 관리
    // 각 탭은 자체 ViewModel, LazyListState, UI State를 가짐
    // tabDataList가 변경되면 모든 페이지를 재생성
    val subPages = remember(tabDataList) {
        android.util.Log.d("RankingPage", "📦 [Creating] All ${tabDataList.size} independent SubPages")
        tabDataList.mapIndexed { index, type ->
            android.util.Log.d("RankingPage", "  📄 Creating SubPage for: code=${type.code}, type=${type.type}")
            Pair(type.code ?: "page_$index", type)
        }
    }

    // TabRow의 스크롤 상태
    val tabScrollState = rememberScrollState()

    // 스크롤 상태에 따라 그라데이션 표시/숨김 제어
    val showLeftGradient by remember {
        derivedStateOf {
            tabScrollState.value > 0 // 스크롤이 0보다 크면 왼쪽 그라데이션 표시
        }
    }

    val showRightGradient by remember {
        derivedStateOf {
            tabScrollState.value < tabScrollState.maxValue // 스크롤이 끝까지 안 갔으면 오른쪽 그라데이션 표시
        }
    }

    // old 프로젝트 색상 (R.color 사용)
    val mainColor = ColorPalette.main
    val textDimmedColor = ColorPalette.textDimmed
    val borderColor = ColorPalette.gray100
    val backgroundColor = ColorPalette.background100
    val backgroundTransparent = ColorPalette.background100Transparent

    // 탭 이름 리스트 생성 (old 프로젝트 로직과 동일)
    val tabs = tabDataList.map { type ->
        // old 프로젝트 로직: 먼저 API name을 기본값으로 설정하고, S/A 타입인 경우에만 덮어씀
        var baseName = type.name // API에서 받은 다국어 이름 (기본값)

        android.util.Log.d("TAB_NAME", "type=${type.type}, name=${type.name}, isDivided=${type.isDivided}, isFemale=${type.isFemale}")

        if (BuildConfig.CELEB) {
            // CELEB 앱: 배우/가수 탭 처리 (old celeb flavor 로직)
            val typeCheck = if (type.isDivided == "N" && !type.isFemale) {
                null
            } else {
                if (type.isFemale) "F" else "M"
            }

            // S(Singer) 또는 A(Actor) 타입인 경우에만 성별에 따라 덮어씀
            when (type.type) {
                "S" -> { // Singer
                    baseName = when (typeCheck) {
                        "M" -> stringResource(R.string.actor_male_singer) // 남자 가수
                        "F" -> stringResource(R.string.actor_female_singer) // 여자 가수
                        else -> type.name // 서버에서 받은 이름
                    }
                }
                "A" -> { // Actor
                    baseName = when (typeCheck) {
                        "M" -> stringResource(R.string.lable_actors) // 배우
                        "F" -> stringResource(R.string.lable_actresses) // 여배우
                        else -> type.name // 서버에서 받은 이름
                    }
                }
                // 그 외(GLOBAL 등)는 이미 설정된 type.name 그대로 사용
            }
        } else {
            // 일반 IDOL 앱: 개인/그룹 등 고정 탭 처리 (old app flavor 로직)
            baseName = when (type.type) {
                "SOLO" -> stringResource(R.string.lable_individual) // 개인
                "GROUP" -> stringResource(R.string.lable_group) // 그룹
                "MIRACLE" -> stringResource(R.string.miracle) // 기적
                "ROOKIE" -> stringResource(R.string.rookie) // 루키
                "HEARTPICK" -> stringResource(R.string.heartpick) // 하트픽
                "ONEPICK" -> stringResource(R.string.onepick) // 원픽
                "HOF" -> stringResource(R.string.title_tab_hof) // 명예전당
                else -> type.name // API에서 받은 다국어 이름 (글로벌 등)
            }
        }

        android.util.Log.d("TAB_NAME", "Final baseName=$baseName")

        // baseurl에 www가 포함되지 않으면 개인, 그룹, 글로벌에 (테섭) 추가
        val finalName = if (ServerUrl.isTestServer() &&
            (type.type == "SOLO" || type.type == "GROUP")) {
            "(테섭)$baseName(테섭)"
        } else {
            baseName
        }

        android.util.Log.d("TAB_NAME", "Final tab name=$finalName\n")
        finalName
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // CELEB이 아닌 경우에만 탭바 표시
        if (!BuildConfig.CELEB) {
            // TabRow를 Box로 감싸서 하단 보더 및 양쪽 그라데이션과 함께 배치
            Box(
                modifier = Modifier.fillMaxWidth()
            ) {
            PrimaryScrollableTabRow (
                minTabWidth = 0.dp,
                scrollState = tabScrollState,
                selectedTabIndex = subPagerState.currentPage,
                containerColor = backgroundColor,
                contentColor = mainColor,
                edgePadding = 3.dp,
                divider = {},
                indicator = @Composable {
                    TabRowDefaults.SecondaryIndicator(
                        modifier = Modifier
                            .tabIndicatorOffset(subPagerState.currentPage)
                            .padding(horizontal = 12.dp),
                        color = mainColor
                    )
                }
            ) {
                tabs.forEachIndexed { index, tabName ->
                    Box(
                        modifier = Modifier
                            .wrapContentWidth()
                            .height(48.dp)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) {
                                coroutineScope.launch {
                                    subPagerState.animateScrollToPage(index)
                                }
                            }
                            .padding(horizontal = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = tabName,
                            style = ExoTypo.title14.copy(lineHeight = 14.sp, color=if (subPagerState.currentPage == index) mainColor else textDimmedColor)
                        )
                    }
                }
            }

            // 왼쪽 그라데이션 (스크롤이 왼쪽 끝이 아닐 때 표시)
            if (showLeftGradient) {
                Box(
                    modifier = Modifier
                        .width(40.dp)
                        .height(48.dp)
                        .align(Alignment.CenterStart)
                        .background(
                            brush = Brush.horizontalGradient(
                                colors = listOf(backgroundColor, backgroundTransparent)
                            )
                        )
                )
            }

            // 오른쪽 그라데이션 (스크롤이 오른쪽 끝이 아닐 때 표시)
            if (showRightGradient) {
                Box(
                    modifier = Modifier
                        .width(40.dp)
                        .height(48.dp)
                        .align(Alignment.CenterEnd)
                        .background(
                            brush = Brush.horizontalGradient(
                                colors = listOf(backgroundTransparent, backgroundColor)
                            )
                        )
                )
            }
            }
        }

        // 탭별 컨텐츠 + 최애 이동 토스트 오버레이
        Box(modifier = Modifier.fillMaxSize()) {
            // 탭별 컨텐츠 - 완전히 독립적인 8개의 SubPage
            // 각 페이지는 미리 생성되어 독립적으로 존재함
            // HorizontalPager는 단순히 보여주기만 함
            HorizontalPager(
                state = subPagerState,
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background),
                beyondViewportPageCount = 1,  // 양쪽 1페이지씩 미리 렌더링 (리소스 최적화)
                key = { pageIndex ->
                    // 고유한 key로 각 페이지를 구분
                    subPages.getOrNull(pageIndex)?.first ?: "page_$pageIndex"
                }
            ) { pageIndex ->
                val (pageKey, currentType) = subPages.getOrNull(pageIndex) ?: return@HorizontalPager

                // 각 페이지를 완전히 독립적으로 렌더링
                // key()를 사용하여 Compose가 각 페이지를 별도의 인스턴스로 인식
                // Hilt의 ViewModelStoreOwner를 유지하여 DI가 제대로 작동하도록 함
                androidx.compose.runtime.key(pageKey) {
                    android.util.Log.d("RankingPage", "🎨 [Rendering] SubPage for pageIndex=$pageIndex, key=$pageKey")

                    // 타입에 따라 적절한 SubPage 호출
                    when (currentType.type) {
                        "SOLO" -> {
                            android.util.Log.d("RankingPage", "🎯 Rendering SOLO with dataSource: ${soloDataSource.hashCode()}, type=${soloDataSource.type}")
                            net.ib.mn.presentation.main.ranking.idol_subpage.UnifiedRankingSubPage(
                                chartCode = currentType.code ?: "",
                                dataSource = soloDataSource,
                                isVisible = subPagerState.currentPage == pageIndex,
                                listState = if (pageIndex == 0) firstTabListState else null,
                                onRankItemsLoaded = if (pageIndex == 0) { items ->
                                    // 첫 번째 탭(개인)의 랭킹 데이터가 로드되면 최애 토스트 체크
                                    viewModel.checkMyFavToast(items)
                                } else null
                            )
                        }
                        "GROUP" -> {
                            android.util.Log.d("RankingPage", "🎯 Rendering GROUP with dataSource: ${groupDataSource.hashCode()}, type=${groupDataSource.type}")
                            net.ib.mn.presentation.main.ranking.idol_subpage.UnifiedRankingSubPage(
                                chartCode = currentType.code ?: "",
                                dataSource = groupDataSource,
                                isVisible = subPagerState.currentPage == pageIndex
                            )
                        }
                        "MIRACLE" -> {
                            android.util.Log.d("RankingPage", "🎯 Rendering MIRACLE with dataSource: ${miracleDataSource.hashCode()}, type=${miracleDataSource.type}, code=${currentType.code}")
                            net.ib.mn.presentation.main.ranking.idol_subpage.MiracleRookieRankingSubPage(
                                chartCode = currentType.code ?: "",
                                dataSource = miracleDataSource,
                                isVisible = subPagerState.currentPage == pageIndex,
                                onInfoClick = { eventId ->
                                    webViewEventId = eventId
                                    webViewTitle = context.getString(R.string.title_miracle_month)
                                }
                            )
                        }
                        "ROOKIE" -> {
                            android.util.Log.d("RankingPage", "🎯 Rendering ROOKIE with dataSource: ${rookieDataSource.hashCode()}, type=${rookieDataSource.type}, code=${currentType.code}")
                            net.ib.mn.presentation.main.ranking.idol_subpage.MiracleRookieRankingSubPage(
                                chartCode = currentType.code ?: "",
                                dataSource = rookieDataSource,
                                isVisible = subPagerState.currentPage == pageIndex,
                                onInfoClick = { eventId ->
                                    webViewEventId = eventId
                                    webViewTitle = "Rookie"
                                }
                            )
                        }
                        "HEARTPICK" -> net.ib.mn.presentation.main.ranking.idol_subpage.HeartPickRankingSubPage(
                            chartCode = currentType.code ?: "",
                            isVisible = subPagerState.currentPage == pageIndex
                        )
                        "ONEPICK" -> net.ib.mn.presentation.main.ranking.idol_subpage.OnePickRankingSubPage(
                            chartCode = currentType.code ?: "",
                            isVisible = subPagerState.currentPage == pageIndex
                        )
                        "HOF" -> net.ib.mn.presentation.main.ranking.idol_subpage.HallOfFameRankingSubPage(
                            chartCode = currentType.code ?: "",
                            isVisible = subPagerState.currentPage == pageIndex,
                            topThreeTabs = tabs.take(3),
                            topThreeChartCodes = tabDataList.take(3).map { it.code ?: "" }
                        )
                        "GLOBALS" -> {
                            android.util.Log.d("RankingPage", "🎯 Rendering GLOBALS with dataSource: ${globalDataSource.hashCode()}, type=${globalDataSource.type}")
                            net.ib.mn.presentation.main.ranking.idol_subpage.UnifiedRankingSubPage(
                                chartCode = currentType.code ?: "",
                                dataSource = globalDataSource,
                                isVisible = subPagerState.currentPage == pageIndex
                            )
                        }
                        else -> {
                            // 기본값 또는 에러 처리
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("Unsupported type: ${currentType.type}")
                            }
                        }
                    }
                }
            }

            // 웰컴 미션 버튼 (우측 하단, 최애 토스트 위)
            // old 프로젝트의 iv_mission과 동일한 위치
            // ShowMyFavToast와 동일한 애니메이션 속도로 위아래 이동
            val welcomeButtonBottomPadding by animateDpAsState(
                targetValue = if (showMyFavToast && subPagerState.currentPage == 0) 72.dp else 4.dp,
                animationSpec = tween(durationMillis = 300),  // slideInVertically 기본 속도와 동일
                label = "welcomeButtonBottomPadding"
            )
            androidx.compose.animation.AnimatedVisibility(
                visible = showWelcomeMission,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(
                        end = 8.dp,
                        bottom = welcomeButtonBottomPadding
                    ),
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                WelcomeMissionButton(
                    onClick = {
                        // TODO: WelcomeMissionFragment로 이동
                    }
                )
            }

            // 최애 이동 토스트 (하단에 오버레이)
            // 첫 번째 탭(개인)에서만 표시하고, 최애가 화면에 안 보일 때만 표시
            androidx.compose.animation.AnimatedVisibility(
                visible = showMyFavToast && subPagerState.currentPage == 0,
                modifier = Modifier.align(Alignment.BottomCenter),
                enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
            ) {
                ShowMyFavToast(
                    onClick = {
                        // 최애 아이돌 위치로 스크롤 (3번째 위치에 보이도록)
                        coroutineScope.launch {
                            if (myFavIdolPosition >= 0) {
                                // 최애가 3번째 아이템으로 보이도록 2칸 위로 스크롤
                                val targetIndex = (myFavIdolPosition - 2).coerceAtLeast(0)
                                firstTabListState.animateScrollToItem(targetIndex)
                            }
                            viewModel.onMyFavToastClick()
                        }
                    }
                )
            }
        }
    }

    // WebView 다이얼로그 표시
    webViewEventId?.let { eventId ->
        androidx.compose.ui.window.Dialog(
            onDismissRequest = { webViewEventId = null }
        ) {
            net.ib.mn.presentation.webview.WebViewScreen(
                url = "${ServerUrl.HOST}/api/v1/events/$eventId/",
                title = webViewTitle,
                onNavigateBack = { webViewEventId = null }
            )
        }
    }
}

/**
 * 일반 앱의 탭 리스트 생성 (old 프로젝트와 동일)
 *
 * 순서:
 * 1. 개인/그룹 차트 (MainChartModel.males 또는 females)
 * 2. MIRACLE, ROOKIE (objects에서 type으로 찾기)
 * 3. HEARTPICK, ONEPICK, HOF (하드코딩)
 */
@Composable
private fun buildIdolAppTabList(
    mainChartModel: net.ib.mn.data.remote.dto.MainChartModel?,
    viewModel: RankingPageViewModel,
    isMale: Boolean
): List<net.ib.mn.data.model.TypeListModel> {
    val tabList = mutableListOf<net.ib.mn.data.model.TypeListModel>()

    // 1. 개인/그룹 차트 (성별에 따라 males/females 선택)
    val chartList = if (isMale) mainChartModel?.males else mainChartModel?.females
    chartList?.forEach { chartInfo ->
        val typeListModel = net.ib.mn.data.model.TypeListModel(
            id = 0,
            name = chartInfo.name ?: "",
            type = extractTypeFromCode(chartInfo.code ?: ""),
            code = chartInfo.code, // 원본 code 저장 (예: "SOLO_M", "PR_G_M")
            isDivided = "N",
            isFemale = !isMale,
            showDivider = false
        )
        tabList.add(typeListModel)
    }

    // 2. objects에서 MIRACLE, ROOKIE 등 추가 (old 프로젝트와 동일)
    val chartObjects = viewModel.configRepository.getChartObjects()
    chartObjects?.forEach { chart ->
        when (chart.type) {
            "M" -> { // MIRACLE (기적)
                tabList.add(
                    net.ib.mn.data.model.TypeListModel(
                        id = 0,
                        name = "MIRACLE",
                        type = "MIRACLE",
                        code = chart.code, // API의 원본 code 사용
                        isDivided = "N",
                        isFemale = false,
                        showDivider = false
                    )
                )
            }
            "R" -> { // ROOKIE (루키)
                tabList.add(
                    net.ib.mn.data.model.TypeListModel(
                        id = 0,
                        name = "ROOKIE",
                        type = "ROOKIE",
                        code = chart.code, // API의 원본 code 사용
                        isDivided = "N",
                        isFemale = false,
                        showDivider = false
                    )
                )
            }
        }
    }

    // 3. 하드코딩 메뉴 (old 프로젝트와 동일)
    // HEARTPICK
    val heartPickChartCode = if (isMale) "HEARTPICK_M" else "HEARTPICK_F"
    tabList.add(
        net.ib.mn.data.model.TypeListModel(
            id = 0,
            name = "HEARTPICK",
            type = "HEARTPICK",
            code = heartPickChartCode,
            isDivided = "N",
            isFemale = false,
            showDivider = false
        )
    )

    // ONEPICK
    val onePickChartCode = if (isMale) "ONEPICK_M" else "ONEPICK_F"
    tabList.add(
        net.ib.mn.data.model.TypeListModel(
            id = 0,
            name = "ONEPICK",
            type = "ONEPICK",
            code = onePickChartCode,
            isDivided = "N",
            isFemale = false,
            showDivider = false
        )
    )

    // HOF (명예의 전당)
    // objects에서 HOF 차트를 찾거나, 없으면 기본값 사용
    val hofChart = chartObjects?.firstOrNull { chart ->
        chart.code?.contains("HOF", ignoreCase = true) == true ||
        chart.code?.contains("HALL", ignoreCase = true) == true
    }
    val hofChartCode = hofChart?.code ?: if (isMale) "HOF_M" else "HOF_F"
    tabList.add(
        net.ib.mn.data.model.TypeListModel(
            id = 0,
            name = "HOF",
            type = "HOF",
            code = hofChartCode,
            isDivided = "N",
            isFemale = false,
            showDivider = false
        )
    )

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
 * 웰컴 미션 버튼 컴포넌트
 * old 프로젝트의 iv_mission (btn_welcome.xml)과 동일한 UI
 */
@Composable
private fun WelcomeMissionButton(
    onClick: () -> Unit
) {
    Image(
        painter = painterResource(id = R.drawable.btn_welcome),
        contentDescription = "Welcome Mission",
        modifier = Modifier
            .size(64.dp)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
    )
}

/**
 * 최애 이동 토스트 컴포넌트
 * old 프로젝트의 MostToast.kt와 동일한 UI
 */
@Composable
private fun ShowMyFavToast(
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
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
