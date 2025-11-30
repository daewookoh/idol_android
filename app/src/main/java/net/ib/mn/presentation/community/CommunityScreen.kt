package net.ib.mn.presentation.community

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.launch
import net.ib.mn.R
import net.ib.mn.data.repository.WikiRepository
import net.ib.mn.domain.model.ApiResult
import net.ib.mn.presentation.community.subpage.CommunityChatSubPage
import net.ib.mn.presentation.community.subpage.CommunityFanTalkSubPage
import net.ib.mn.presentation.community.subpage.CommunityFeedSubPage
import net.ib.mn.presentation.community.subpage.CommunityScheduleSubPage
import net.ib.mn.presentation.webview.WebViewScreen
import net.ib.mn.ui.components.ExoNameWithGroup
import net.ib.mn.ui.components.ExoProfileImage
import net.ib.mn.ui.components.ExoScaffold
import net.ib.mn.ui.components.ExoTop3
import net.ib.mn.ui.components.ProfileImageType
import net.ib.mn.ui.components.RankingItem
import net.ib.mn.ui.theme.ColorPalette
import net.ib.mn.ui.theme.ExoTypo
import net.ib.mn.util.LocaleUtil
import net.ib.mn.util.NumberFormatUtil
import kotlin.math.roundToInt

/**
 * CommunityTab - 커뮤니티 탭 타입
 */
enum class CommunityTab {
    FEED,
    FAN_TALK,
    CHAT,
    SCHEDULE
}

/**
 * CommunityScreen - 커뮤니티 화면
 *
 * @param rankingItem 선택된 랭킹 아이템 데이터
 * @param wikiRepository WikiRepository 인스턴스
 * @param showChattingTab 채팅 탭 표시 여부 (최애이거나, 최애의 그룹이거나, 관리자일 경우 true)
 * @param fandomName 팬덤 이름
 * @param onBackClick 뒤로가기 클릭 이벤트
 */
@Composable
fun CommunityScreen(
    rankingItem: RankingItem,
    wikiRepository: WikiRepository? = null,
    showChattingTab: Boolean = false,
    fandomName: String? = null,
    onBackClick: () -> Unit = {}
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var showWikiWebView by remember { mutableStateOf(false) }
    var wikiUrl by remember { mutableStateOf<String?>(null) }
    var isLoadingWiki by remember { mutableStateOf(false) }
    var showIdolDialog by remember { mutableStateOf(false) }

    // 탭 목록 생성 (showChattingTab이 true인 경우에만 채팅 탭 포함)
    val tabs = remember(showChattingTab) {
        buildList {
            add(CommunityTab.FEED)
            add(CommunityTab.FAN_TALK)
            if (showChattingTab) {
                add(CommunityTab.CHAT)
            }
            add(CommunityTab.SCHEDULE)
        }
    }

    val pagerState = rememberPagerState(pageCount = { tabs.size })

    // 탭 제목 생성
    val tabTitles = remember(fandomName, showChattingTab) {
        buildList {
            add(context.getString(R.string.community_feed))
            // 팬덤 이름이 있으면 "%s Talk" 형식, 없으면 "Fan Talk"
            add(
                if (fandomName.isNullOrEmpty()) {
                    context.getString(R.string.community_board)
                } else {
                    context.getString(R.string.community_board2, fandomName)
                }
            )
            if (showChattingTab) {
                add(context.getString(R.string.community_chat))
            }
            add(context.getString(R.string.community_schedule))
        }
    }

    BackHandler {
        if (showWikiWebView) {
            showWikiWebView = false
            wikiUrl = null
        } else {
            onBackClick()
        }
    }

    // Top3 영역 높이 측정 (스크롤 시 숨겨질 영역)
    var top3HeightPx by remember { mutableFloatStateOf(0f) }

    // 현재 스크롤 offset (0 ~ -top3HeightPx)
    var toolbarOffsetHeightPx by remember { mutableFloatStateOf(0f) }

    // NestedScrollConnection - 스크롤 이벤트를 가로채서 Top3 영역 숨김/표시 처리
    val nestedScrollConnection = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                val delta = available.y
                val newOffset = toolbarOffsetHeightPx + delta
                // offset은 0 ~ -top3HeightPx 사이로 제한
                toolbarOffsetHeightPx = newOffset.coerceIn(-top3HeightPx, 0f)
                return Offset.Zero
            }
        }
    }

    // Top3, 프로필, 탭 영역에서 드래그 시 스크롤 처리 (Old 프로젝트의 AppBarLayout 동작과 동일)
    val headerDraggableState = rememberDraggableState { delta ->
        val newOffset = toolbarOffsetHeightPx + delta
        toolbarOffsetHeightPx = newOffset.coerceIn(-top3HeightPx, 0f)
    }

    // Top3 영역이 완전히 숨겨졌는지 여부
    val isCollapsed by remember {
        derivedStateOf { toolbarOffsetHeightPx <= -top3HeightPx + 1 }
    }

    // 첫 번째 아티클의 비디오/움짤이 재생 중인지 여부 (Top3 비디오 정지용)
    var isFirstArticleVideoPlaying by remember { mutableStateOf(false) }

    ExoScaffold {
        // clipToBounds로 Top3가 SafeArea 밖으로 나가지 않게 함
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clipToBounds()
                .background(ColorPalette.background100)
                .nestedScroll(nestedScrollConnection)
        ) {
            // 메인 컨텐츠 영역 (프로필 + 탭 + 페이저)
            // Top3 영역이 숨겨질수록 위로 올라감
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .offset {
                        IntOffset(
                            x = 0,
                            y = (top3HeightPx + toolbarOffsetHeightPx).roundToInt()
                        )
                    }
            ) {
                // 프로필 + 탭 영역을 하나로 묶어서 draggable 적용
                // (Old의 AppBarLayout 동작과 동일 - 프로필/탭 영역 드래그 시 스크롤)
                Column(
                    modifier = Modifier.draggable(
                        state = headerDraggableState,
                        orientation = Orientation.Vertical
                    )
                ) {
                    // 프로필 영역
                    IdolProfile(
                        rankingItem = rankingItem,
                        isCollapsed = isCollapsed,
                        onProfileImageClick = {
                            wikiRepository?.let { repo ->
                                val idolId = rankingItem.id.toIntOrNull() ?: return@let
                                if (idolId <= 0) return@let

                                coroutineScope.launch {
                                    isLoadingWiki = true
                                    val locale = LocaleUtil.getWikiLocale(context)

                                    when (val result = repo.getWikiUrl(idolId, locale)) {
                                        is ApiResult.Success -> {
                                            wikiUrl = result.data
                                            showWikiWebView = true
                                        }
                                        is ApiResult.Error -> { /* 에러 무시 */ }
                                        is ApiResult.Loading -> { /* no-op */ }
                                    }
                                    isLoadingWiki = false
                                }
                            }
                        },
                        onBackClick = onBackClick,
                        onMoreClick = { showIdolDialog = true }
                    )

                    // 탭 레이아웃
                    CommunityTabRow(
                        tabTitles = tabTitles,
                        selectedTabIndex = pagerState.currentPage,
                        onTabSelected = { index ->
                            coroutineScope.launch {
                                pagerState.animateScrollToPage(index)
                            }
                        }
                    )
                }

                // 탭 컨텐츠 (ViewPager)
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxSize(),
                    userScrollEnabled = false // 좌우 스크롤 막음 (old와 동일)
                ) { page ->
                    when (tabs[page]) {
                        CommunityTab.FEED -> CommunityFeedSubPage(
                            rankingItem = rankingItem,
                            onFirstArticleVideoPlaying = { isPlaying ->
                                isFirstArticleVideoPlaying = isPlaying
                            }
                        )
                        CommunityTab.FAN_TALK -> CommunityFanTalkSubPage(
                            rankingItem = rankingItem,
                            fandomName = fandomName
                        )
                        CommunityTab.CHAT -> CommunityChatSubPage(rankingItem = rankingItem)
                        CommunityTab.SCHEDULE -> CommunityScheduleSubPage(rankingItem = rankingItem)
                    }
                }
            }

            // 상단 ExoTop3 + 뒤로가기 버튼 (스크롤 시 위로 올라가며 숨겨짐)
            // graphicsLayer로 위치 이동 (클리핑되어 SafeArea 바깥으로 나가지 않음)
            // draggable 추가: Top3 영역에서 드래그 시에도 스크롤 동작 (Old의 AppBarLayout 동작)
            Box(
                modifier = Modifier
                    .graphicsLayer {
                        translationY = toolbarOffsetHeightPx
                    }
                    .onSizeChanged { size ->
                        top3HeightPx = size.height.toFloat()
                    }
                    .draggable(
                        state = headerDraggableState,
                        orientation = Orientation.Vertical
                    )
            ) {
                ExoTop3(
                    rankingItemData = rankingItem,
                    // Top3 비디오 정지 조건: 완전히 숨겨졌거나, 첫 번째 아티클 비디오가 재생 중일 때
                    isVisible = !isCollapsed && !isFirstArticleVideoPlaying,
                    onItemClick = { /* TODO */ }
                )

                // 뒤로가기 버튼
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(start = 10.dp, top = 11.dp)
                        .size(28.dp)
                        .background(Color.Black.copy(alpha = 0.3f), CircleShape)
                        .clickable { onBackClick() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(R.drawable.sharp_arrow_back_white_24),
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // 플로팅 버튼 - 탭별로 아이콘/액션이 다름
            // 피드, 팬톡: 글쓰기 버튼 (btn_write_contents)
            // 채팅: 채팅방 추가 버튼 (btn_add_chat)
            // 스케줄: 글쓰기 버튼 (btn_write_contents) - 스케줄 작성
            val currentTab = tabs.getOrNull(pagerState.currentPage)
            val fabIcon = when (currentTab) {
                CommunityTab.CHAT -> R.drawable.btn_add_chat
                else -> R.drawable.btn_write_contents
            }

            Icon(
                painter = painterResource(fabIcon),
                contentDescription = when (currentTab) {
                    CommunityTab.FEED -> "Write Feed"
                    CommunityTab.FAN_TALK -> "Write Fan Talk"
                    CommunityTab.CHAT -> "Create Chat Room"
                    CommunityTab.SCHEDULE -> "Write Schedule"
                    else -> "Write"
                },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 16.dp, bottom = 16.dp)
                    .size(53.dp)
                    .clickable {
                        when (currentTab) {
                            CommunityTab.FEED -> {
                                // TODO: 피드 글쓰기
                            }
                            CommunityTab.FAN_TALK -> {
                                // TODO: 팬톡 글쓰기
                            }
                            CommunityTab.CHAT -> {
                                // TODO: 채팅방 만들기
                            }
                            CommunityTab.SCHEDULE -> {
                                // TODO: 스케줄 작성
                            }
                            else -> {}
                        }
                    },
                tint = Color.Unspecified
            )
        }
    }

    // 위키 웹뷰 (아래에서 올라오는 애니메이션) - ExoScaffold 위에 표시
    AnimatedVisibility(
        visible = showWikiWebView && wikiUrl != null,
        enter = slideInVertically(initialOffsetY = { it }),
        exit = slideOutVertically(targetOffsetY = { it })
    ) {
        wikiUrl?.let { url ->
            WebViewScreen(
                url = url,
                title = "Wiki",
                onNavigateBack = {
                    showWikiWebView = false
                    wikiUrl = null
                }
            )
        }
    }

    // 로딩 인디케이터
    if (isLoadingWiki) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.3f)),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = ColorPalette.main)
        }
    }

    // 아이돌 다이얼로그
    if (showIdolDialog) {
        IdolDialog(
            rankingItem = rankingItem,
            onDismiss = { showIdolDialog = false },
            onVoteRankingClick = {
                showIdolDialog = false
                // TODO: 하트 투표 랭킹 화면으로 이동
            },
            onGalleryClick = {
                showIdolDialog = false
                // TODO: 배너그램 화면으로 이동
            },
            onRankHistoryClick = {
                showIdolDialog = false
                // TODO: 랭킹 변동 화면으로 이동
            },
            onShareClick = {
                showIdolDialog = false
                // TODO: 공유하기
            },
            onAllInDayClick = {
                showIdolDialog = false
                // TODO: 올인데이 설정
            }
        )
    }
}

/**
 * CommunityTabRow - 커뮤니티 탭 레이아웃 (RankingPage와 동일한 스타일)
 * - PrimaryScrollableTabRow 사용 (탭이 wrap_content, 스크롤 가능)
 * - 탭 인디케이터는 텍스트 너비만큼만 표시
 * - 탭 간격 최소화, 왼쪽 정렬
 */
@Composable
private fun CommunityTabRow(
    tabTitles: List<String>,
    selectedTabIndex: Int,
    onTabSelected: (Int) -> Unit
) {
    Column {
        PrimaryScrollableTabRow(
            selectedTabIndex = selectedTabIndex,
            minTabWidth = 0.dp,
            containerColor = ColorPalette.background100,
            contentColor = ColorPalette.textDefault,
            edgePadding = 3.dp,
            divider = {},
            indicator = @Composable {
                TabRowDefaults.SecondaryIndicator(
                    modifier = Modifier
                        .tabIndicatorOffset(selectedTabIndex)
                        .padding(horizontal = 12.dp)
                        .height(2.dp),
                    color = ColorPalette.textDefault
                )
            }
        ) {
            tabTitles.forEachIndexed { index, title ->
                Box(
                    modifier = Modifier
                        .wrapContentWidth()
                        .height(48.dp)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { onTabSelected(index) }
                        .padding(horizontal = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = title,
                        style = ExoTypo.title14.copy(
                            lineHeight = 14.sp,
                            color = if (selectedTabIndex == index) ColorPalette.textDefault else ColorPalette.textDimmed
                        )
                    )
                }
            }
        }
        // 하단 구분선 (old의 border_gray100과 동일)
        HorizontalDivider(
            thickness = 1.dp,
            color = ColorPalette.gray100
        )
    }
}

/**
 * IdolProfile - 아이돌 프로필 컴포넌트
 *
 * @param isCollapsed Top3가 숨겨졌을 때 true - 간소화된 툴바 형태로 표시
 * @param onBackClick Collapsed 상태에서 뒤로가기 버튼 클릭
 */
@Composable
private fun IdolProfile(
    rankingItem: RankingItem,
    isCollapsed: Boolean = false,
    onProfileImageClick: () -> Unit = {},
    onBackClick: () -> Unit = {},
    onMoreClick: () -> Unit = {}
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(if (isCollapsed) 56.dp else 70.dp)
            .background(ColorPalette.background100),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (isCollapsed) {
            // Collapsed 상태: 뒤로가기 + 이름/그룹 + 더보기
            // 뒤로가기 버튼
            Box(
                modifier = Modifier
                    .padding(start = 10.dp)
                    .size(28.dp)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { onBackClick() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(R.drawable.btn_navigation_back),
                    contentDescription = "Back",
                    tint = Color.Unspecified,
                    modifier = Modifier.size(28.dp)
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            // 이름 + 그룹명만 표시
            ExoNameWithGroup(
                fullName = rankingItem.name,
                nameFontSize = 16.sp,
                groupFontSize = 11.sp,
                modifier = Modifier.weight(1f)
            )
        } else {
            // Expanded 상태: 프로필 이미지 + 이름/그룹 + 팔로워 + 더보기
            Spacer(modifier = Modifier.width(10.dp))

            // 프로필 이미지
            Box(
                modifier = Modifier.clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) { onProfileImageClick() }
            ) {
                ExoProfileImage(
                    imageUrl = rankingItem.photoUrl ?: "",
                    type = ProfileImageType.MEDIUM_CIRCLE,
                    rank = 0,
                    anniversary = rankingItem.anniversary ?: "N",
                    anniversaryDays = rankingItem.anniversaryDays
                )
            }

            Spacer(modifier = Modifier.width(5.dp))

            // 이름 + 그룹명 + 팔로워
            Column(modifier = Modifier.weight(1f)) {
                ExoNameWithGroup(
                    fullName = rankingItem.name,
                    nameFontSize = 16.sp,
                    groupFontSize = 11.sp
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        painter = painterResource(R.drawable.icon_community_person),
                        contentDescription = null,
                        tint = Color.Unspecified,
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(2.dp))
                    Text(
                        text = NumberFormatUtil.formatFollowerCount(rankingItem.mostCount),
                        fontSize = 12.sp,
                        lineHeight = 12.sp,
                        color = ColorPalette.textDimmed
                    )
                }
            }
        }

        // 더보기 버튼 (공통)
        Icon(
            painter = painterResource(R.drawable.btn_navigation_view_more),
            contentDescription = null,
            tint = Color.Unspecified,
            modifier = Modifier
                .padding(end = 16.dp)
                .clickable { onMoreClick() }
        )
    }
}

/**
 * IdolDialog - 아이돌 정보 다이얼로그
 * Old 프로젝트의 IdolCommunityDialogFragment를 Compose로 구현
 *
 * @param rankingItem 아이돌 정보
 * @param isFavorite 즐겨찾기 여부
 * @param isMost 최애 여부
 * @param onDismiss 다이얼로그 닫기
 * @param onFavoriteChange 즐겨찾기 변경
 * @param onMostChange 최애 변경
 * @param onVoteRankingClick 하트 투표 랭킹 클릭
 * @param onGalleryClick 배너그램 클릭
 * @param onRankHistoryClick 랭킹 변동 클릭
 * @param onShareClick 공유 클릭
 * @param onAllInDayClick 올인데이 설정 클릭
 */
@Composable
private fun IdolDialog(
    rankingItem: RankingItem,
    isFavorite: Boolean = false,
    isMost: Boolean = false,
    onDismiss: () -> Unit = {},
    onFavoriteChange: (Boolean) -> Unit = {},
    onMostChange: (Boolean) -> Unit = {},
    onVoteRankingClick: () -> Unit = {},
    onGalleryClick: () -> Unit = {},
    onRankHistoryClick: () -> Unit = {},
    onShareClick: () -> Unit = {},
    onAllInDayClick: () -> Unit = {}
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true
        )
    ) {
        // 외부 컨테이너: 320dp (Old: android:layout_width="320dp", clipChildren="false")
        Box(
            modifier = Modifier.width(320.dp),
            contentAlignment = Alignment.TopCenter
        ) {
            // 메인 다이얼로그 컨텐츠: 300dp (Old: marginTop으로 배지 공간 확보)
            Column(
                modifier = Modifier
                    .padding(top = 10.dp)  // 배지가 삐져나갈 공간
                    .width(300.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .border(
                        width = 1.dp,
                        color = ColorPalette.gray150,
                        shape = RoundedCornerShape(6.dp)
                    )
                    .background(ColorPalette.background100)
                    .padding(bottom = 10.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // 상단 최애/즐겨찾기 버튼 영역 (Old: marginTop="15dp", marginEnd="14dp")
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 15.dp, end = 14.dp),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 최애 버튼 (Old: btn_most = 하트, 24dp x 24dp)
                    Icon(
                        painter = painterResource(
                            if (isMost) R.drawable.btn_favorite_on else R.drawable.btn_favorite_off
                        ),
                        contentDescription = "Most Favorite",
                        tint = Color.Unspecified,
                        modifier = Modifier
                            .size(24.dp)
                            .clickable { onMostChange(!isMost) }
                    )

                    Spacer(modifier = Modifier.width(9.dp))

                    // 즐겨찾기 버튼 (Old: btn_favorite = 별, 24dp x 24dp)
                    Icon(
                        painter = painterResource(
                            if (isFavorite) R.drawable.btn_bookmark_on else R.drawable.btn_bookmark_off
                        ),
                        contentDescription = "Favorite",
                        tint = Color.Unspecified,
                        modifier = Modifier
                            .size(24.dp)
                            .clickable { onFavoriteChange(!isFavorite) }
                    )
                }

                // 스크롤 가능한 컨텐츠 영역
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // 프로필 이미지 (Old: 90dp x 90dp, marginTop="20dp")
                    // 다이얼로그 내에서는 뱃지 표기 없음
                    Spacer(modifier = Modifier.height(20.dp))
                    ExoProfileImage(
                        imageUrl = rankingItem.photoUrl ?: "",
                        type = ProfileImageType.LARGE,
                        rank = 0,
                        anniversary = "N",
                        anniversaryDays = 0,
                        miracleCount = 0,
                        fairyCount = 0,
                        angelCount = 0
                    )

                    // 이름 + 그룹명 (Old: marginTop="10dp", name=17sp bold, group=15sp bold marginStart=5dp)
                    Spacer(modifier = Modifier.height(10.dp))
                    ExoNameWithGroup(
                        fullName = rankingItem.name,
                        nameFontSize = 17.sp,
                        groupFontSize = 15.sp
                    )

                    // 생일 (Old: marginTop="10dp", 10sp, text_gray)
                    rankingItem.birthday?.let { birthday ->
                        if (birthday.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = birthday,
                                fontSize = 10.sp,
                                color = ColorPalette.textGray
                            )
                        }
                    }

                    // 최애 수 (Old: 10sp, text_gray)
                    Text(
                        text = stringResource(R.string.most_favorite) + " : " +
                                NumberFormatUtil.formatFollowerCount(rankingItem.mostCount),
                        fontSize = 10.sp,
                        color = ColorPalette.textGray
                    )

                    // 메뉴 버튼 1행 (Old: marginTop="14dp", marginStart/End="10dp", paddingStart/End="5dp")
                    Spacer(modifier = Modifier.height(14.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        IdolDialogMenuItem(
                            iconRes = R.drawable.btn_community_menu_ranking,
                            title = stringResource(R.string.title_heart_voting_ranking),
                            onClick = onVoteRankingClick
                        )
                        IdolDialogMenuItem(
                            iconRes = R.drawable.btn_community_menu_bannergram,
                            title = stringResource(R.string.gallery_title),
                            onClick = onGalleryClick
                        )
                        IdolDialogMenuItem(
                            iconRes = R.drawable.btn_community_menu_change_rank,
                            title = stringResource(R.string.title_rank_history),
                            onClick = onRankHistoryClick
                        )
                    }

                    // 메뉴 버튼 2행 (Old: marginTop="14dp", marginBottom="14dp")
                    Spacer(modifier = Modifier.height(14.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp),
                        horizontalArrangement = Arrangement.Start
                    ) {
                        Spacer(modifier = Modifier.width(5.dp))
                        IdolDialogMenuItem(
                            iconRes = R.drawable.btn_community_menu_share,
                            title = stringResource(R.string.title_share),
                            onClick = onShareClick
                        )
                        // 최애일 경우에만 올인데이 버튼 표시
                        if (isMost) {
                            IdolDialogMenuItem(
                                iconRes = R.drawable.btn_community_menu_allinday,
                                title = stringResource(R.string.set_all_in_day),
                                onClick = onAllInDayClick
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                }
            }

            // 좌측 상단 배지 (다이얼로그 위로 삐져나감)
            // Old: marginStart="13dp", marginTop="-8dp" (다이얼로그 상단 기준)
            Row(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(start = 23.dp, top = 2.dp),  // 10dp(다이얼로그 margin) - 8dp(배지 offset) = 2dp
                horizontalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                // Angel 배지 (Old: 30dp x 35dp, paddingTop="12dp", textSize="10sp")
                if (rankingItem.angelCount > 0) {
                    Box(
                        modifier = Modifier.size(30.dp, 35.dp),
                        contentAlignment = Alignment.TopCenter
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.charity_angel_badge),
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            tint = Color.Unspecified
                        )
                        Text(
                            text = rankingItem.angelCount.toString(),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = ColorPalette.textAngel,
                            modifier = Modifier.padding(top = 12.dp)
                        )
                    }
                }
                // Fairy 배지
                if (rankingItem.fairyCount > 0) {
                    Box(
                        modifier = Modifier.size(30.dp, 35.dp),
                        contentAlignment = Alignment.TopCenter
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.charity_fairy_badge),
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            tint = Color.Unspecified
                        )
                        Text(
                            text = rankingItem.fairyCount.toString(),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = ColorPalette.textFairy,
                            modifier = Modifier.padding(top = 12.dp)
                        )
                    }
                }
                // Miracle 배지
                if (rankingItem.miracleCount > 0) {
                    Box(
                        modifier = Modifier.size(30.dp, 35.dp),
                        contentAlignment = Alignment.TopCenter
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.charity_miracle_badge),
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            tint = Color.Unspecified
                        )
                        Text(
                            text = rankingItem.miracleCount.toString(),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = ColorPalette.textMiracle,
                            modifier = Modifier.padding(top = 12.dp)
                        )
                    }
                }
                // Rookie 배지
                if (rankingItem.rookieCount > 0) {
                    val isSuper = rankingItem.rookieCount >= 3
                    Box(
                        modifier = Modifier.size(30.dp, 35.dp),
                        contentAlignment = Alignment.TopCenter
                    ) {
                        Icon(
                            painter = painterResource(
                                if (isSuper) R.drawable.charity_super_rookie_badge
                                else R.drawable.charity_rookie_badge
                            ),
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            tint = Color.Unspecified
                        )
                        Text(
                            text = if (isSuper) "S" else rankingItem.rookieCount.toString(),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isSuper) ColorPalette.textSuperRookie else ColorPalette.textRookie,
                            modifier = Modifier.padding(top = 12.dp)
                        )
                    }
                }
            }
        }
    }
}

/**
 * IdolDialogMenuItem - 다이얼로그 메뉴 아이템
 * Old: 90dp 너비, 아이콘 50dp, 텍스트 13sp gray580, marginTop="10dp"
 */
@Composable
private fun IdolDialogMenuItem(
    iconRes: Int,
    title: String,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .width(90.dp)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { onClick() },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = title,
            tint = Color.Unspecified,
            modifier = Modifier.size(50.dp)
        )
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = title,
            fontSize = 13.sp,
            color = ColorPalette.gray580,
            textAlign = TextAlign.Center
        )
    }
}
