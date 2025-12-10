package net.ib.mn.presentation.community

import android.widget.Toast
import net.ib.mn.util.IntentUtil
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
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
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import kotlin.math.roundToInt
import kotlinx.coroutines.launch
import net.ib.mn.R
import net.ib.mn.domain.model.ApiResult
import net.ib.mn.presentation.community.profile.ProfileScreen
import net.ib.mn.presentation.community.subpage.CommunityChatSubPage
import net.ib.mn.presentation.community.subpage.CommunityFanTalkSubPage
import net.ib.mn.presentation.community.subpage.CommunityFeedSubPage
import net.ib.mn.presentation.community.subpage.CommunityFeedViewModel
import net.ib.mn.presentation.main.freeboard.FreeBoardViewModel
import net.ib.mn.presentation.community.subpage.OrderByType
import net.ib.mn.presentation.community.subpage.CommunityScheduleSubPage
import net.ib.mn.presentation.community.subpage.CommunityScheduleContract
import net.ib.mn.presentation.community.subpage.CommunityScheduleViewModel
import net.ib.mn.domain.model.ArticleModel
import net.ib.mn.domain.model.ScheduleModel
import net.ib.mn.domain.model.TrendsModel
import net.ib.mn.presentation.overlay.articledetail.ArticleDetailScreen
import net.ib.mn.presentation.overlay.photodetail.PhotoDetailScreen
import net.ib.mn.presentation.community.schedule.ScheduleDetailScreen
import net.ib.mn.presentation.community.schedule.ScheduleWriteScreen
import net.ib.mn.presentation.community.chat.create.ChatRoomCreateScreen
import net.ib.mn.presentation.community.chat.room.ChatRoomScreen
import net.ib.mn.presentation.webview.WebViewScreen
import net.ib.mn.presentation.overlay.articlewrite.ArticleWriteScreen
import net.ib.mn.presentation.overlay.articlewrite.ArticleWriteType
import java.util.Date
import net.ib.mn.util.ServerUrl
import net.ib.mn.ui.components.*
import net.ib.mn.ui.theme.ColorPalette
import net.ib.mn.ui.theme.ExoTypo
import net.ib.mn.util.Constants
import net.ib.mn.util.LocaleUtil
import net.ib.mn.util.NumberFormatUtil
import net.ib.mn.util.link.LinkUtil
import net.ib.mn.navigation.LocalAppNavigator
import net.ib.mn.navigation.Screen

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
 * idolId로 로컬 DB에서 아이돌 정보를 Flow로 관찰하여 커뮤니티 화면을 표시합니다.
 * top3 이미지, 구독수 등이 실시간으로 업데이트됩니다.
 *
 * @param idolId 아이돌 ID
 * @param showChattingTab 채팅 탭 표시 여부
 * @param initialTab 초기 탭 (0: FEED, 1: FAN_TALK, 2: CHAT, 3: SCHEDULE)
 * @param sortLatest 최신순 정렬 강제 적용
 * @param onBackClick 뒤로가기 클릭 콜백
 * @param onMostChanged 최애 변경 콜백
 */
@Composable
fun CommunityScreen(
    idolId: Int,
    showChattingTab: Boolean = true,
    initialTab: Int = 0,
    sortLatest: Boolean = false,
    onBackClick: (() -> Unit)? = null,
    onMostChanged: (Boolean) -> Unit = {},
    viewModel: CommunityViewModel = hiltViewModel()
) {
    val navigator = LocalAppNavigator.current
    val idolData by viewModel.idolDataFlow.collectAsState()

    // idolId 설정하여 Flow 관찰 시작
    LaunchedEffect(idolId) {
        viewModel.setIdolId(idolId)
    }

    // 데이터 로딩 중
    if (idolData == null) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(ColorPalette.background100),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = ColorPalette.main)
        }
        return
    }

    CommunityScreenContent(
        idolData = idolData!!,
        showChattingTab = showChattingTab,
        initialTab = initialTab,
        sortLatest = sortLatest,
        onBackClick = onBackClick ?: { navigator.popBackStack(); Unit },
        onMostChanged = onMostChanged,
        viewModel = viewModel
    )
}

/**
 * CommunityScreenContent - 커뮤니티 화면 내부 구현
 *
 * @param idolData 아이돌 실시간 데이터 (Flow로 실시간 업데이트됨)
 * @param showChattingTab 채팅 탭 표시 여부 (최애이거나, 최애의 그룹이거나, 관리자일 경우 true)
 * @param initialTab 초기 탭 인덱스 (0: FEED, 1: FAN_TALK, 2: CHAT, 3: SCHEDULE) - 푸시 알림에서 온 경우
 * @param sortLatest 푸시 알림에서 온 경우 최신순 정렬 강제 적용
 * @param onBackClick 뒤로가기 클릭 이벤트
 * @param onMostChanged 최애 변경 콜백
 * @param viewModel CommunityViewModel (외부에서 주입받아 Flow 상태 공유)
 */
@Composable
private fun CommunityScreenContent(
    idolData: CommunityViewModel.IdolData,
    showChattingTab: Boolean = false,
    initialTab: Int? = null,
    sortLatest: Boolean = false,
    onBackClick: () -> Unit = {},
    onMostChanged: (Boolean) -> Unit = {},
    viewModel: CommunityViewModel,
    feedViewModel: CommunityFeedViewModel = hiltViewModel(key = "feed_${idolData.id}"),
    fanTalkViewModel: FreeBoardViewModel = hiltViewModel(key = "fanTalk_${idolData.id}"),
    scheduleViewModel: CommunityScheduleViewModel = hiltViewModel(key = "schedule_${idolData.id}")
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // 아이돌 ID
    val idolId = idolData.id.toIntOrNull()

    // ViewModel에서 최애/즐겨찾기 상태 확인
    val initialIsMost = remember(idolId) { viewModel.isMostIdol(idolId) }
    val initialIsFavorite = remember(idolId) { viewModel.isFavoriteIdol(idolId) }

    // ViewModel 상태 구독
    val isUpdatingMost by viewModel.isUpdatingMost.collectAsState()
    val isUpdatingFavorite by viewModel.isUpdatingFavorite.collectAsState()

    var showWikiWebView by remember { mutableStateOf(false) }
    var wikiUrl by remember { mutableStateOf<String?>(null) }
    var isLoadingWiki by remember { mutableStateOf(false) }
    var showIdolDialog by remember { mutableStateOf(false) }
    var showChangeMostDialog by remember { mutableStateOf(false) }
    var showVoterTop100Screen by remember { mutableStateOf(false) }
    var showTrendsScreen by remember { mutableStateOf(false) }
    var showIdolIdolRankingHistoryScreen by remember { mutableStateOf(false) }
    var showBurningDayDialog by remember { mutableStateOf(false) }
    var selectedTrendsItem by remember { mutableStateOf<TrendsModel?>(null) }
    var currentIsMost by remember(initialIsMost) { mutableStateOf(initialIsMost) }
    var currentIsFavorite by remember(initialIsFavorite) { mutableStateOf(initialIsFavorite) }

    // 유저 프로필 화면 상태
    var selectedUserProfile by remember { mutableStateOf<UserProfileInfo?>(null) }

    // 게시글 상세 화면 상태
    var selectedArticle by remember { mutableStateOf<ArticleModel?>(null) }
    var selectedArticleExternalIdolName by remember { mutableStateOf<String?>(null) }
    var isSelectedArticleFromFeed by remember { mutableStateOf(false) } // FeedSubPage에서 진입한 경우 true
    var articleUpdatedCallback by remember { mutableStateOf<((ArticleModel) -> Unit)?>(null) }

    // Notice 상세 화면 상태
    var selectedNoticeArticle by remember { mutableStateOf<ArticleModel?>(null) }

    // 사진 상세 화면 상태 (article + 시작 인덱스)
    var selectedPhotoDetail by remember { mutableStateOf<Pair<ArticleModel, Int>?>(null) }

    // 스케줄 작성 화면 상태
    var showScheduleWriteScreen by remember { mutableStateOf(false) }
    var scheduleWriteInitialDate by remember { mutableStateOf<Date?>(null) }
    var editingSchedule by remember { mutableStateOf<ScheduleModel?>(null) }

    // 채팅방 생성 화면 상태
    var showChatRoomCreateScreen by remember { mutableStateOf(false) }

    // 채팅방 화면 상태
    var selectedChatRoom by remember { mutableStateOf<ChatRoomInfo?>(null) }
    var shouldRefreshChatList by remember { mutableStateOf(false) }

    // 스케줄 상세 화면 상태 (댓글 화면용)
    var selectedScheduleDetail by remember { mutableStateOf<Triple<ScheduleModel, String, String>?>(null) }

    // 글쓰기 화면 상태 (오버레이)
    var showArticleWriteScreen by remember { mutableStateOf(false) }
    var articleWriteType by remember { mutableStateOf(ArticleWriteType.FEED) }
    var editingArticle by remember { mutableStateOf<ArticleModel?>(null) }

    // 월간 스케줄 상세 화면 상태 (스케줄 목록 화면용)
    var selectedMonthScheduleDetail by remember { mutableStateOf<Triple<ScheduleModel, String, String>?>(null) }

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

    val pagerState = rememberPagerState(
        initialPage = initialTab?.coerceIn(0, tabs.size - 1) ?: 0,
        pageCount = { tabs.size }
    )

    // 푸시 알림에서 온 경우 최신순 정렬 적용
    LaunchedEffect(sortLatest) {
        if (sortLatest) {
            feedViewModel.setOrderBy(OrderByType.TIME)
        }
    }

    // 탭 제목 생성
    val tabTitles = remember(idolData.fandomName, showChattingTab) {
        buildList {
            add(context.getString(R.string.community_feed))
            // 팬덤 이름이 있으면 "%s Talk" 형식, 없으면 "Fan Talk"
            add(
                if (idolData.fandomName.isNullOrEmpty()) {
                    context.getString(R.string.community_board)
                } else {
                    context.getString(R.string.community_board2, idolData.fandomName)
                }
            )
            if (showChattingTab) {
                add(context.getString(R.string.community_chat))
            }
            add(context.getString(R.string.community_schedule))
        }
    }

    BackHandler {
        if (selectedChatRoom != null) {
            selectedChatRoom = null
        } else if (showChatRoomCreateScreen) {
            showChatRoomCreateScreen = false
        } else if (showScheduleWriteScreen) {
            showScheduleWriteScreen = false
            scheduleWriteInitialDate = null
            editingSchedule = null
        } else if (selectedMonthScheduleDetail != null) {
            selectedMonthScheduleDetail = null
        } else if (selectedUserProfile != null) {
            selectedUserProfile = null
        } else if (showIdolIdolRankingHistoryScreen) {
            showIdolIdolRankingHistoryScreen = false
        } else if (showTrendsScreen) {
            showTrendsScreen = false
        } else if (showVoterTop100Screen) {
            showVoterTop100Screen = false
        } else if (showWikiWebView) {
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
                        idolData = idolData,
                        isCollapsed = isCollapsed,
                        onProfileImageClick = {
                            val wikiIdolId = idolData.id.toIntOrNull() ?: return@IdolProfile
                            if (wikiIdolId <= 0) return@IdolProfile

                            coroutineScope.launch {
                                isLoadingWiki = true
                                val locale = LocaleUtil.getWikiLocale(context)

                                when (val result = viewModel.wikiRepository.getWikiUrl(wikiIdolId, locale)) {
                                    is ApiResult.Success -> {
                                        wikiUrl = result.data
                                        showWikiWebView = true
                                    }
                                    is ApiResult.Error -> { /* 에러 무시 */ }
                                    is ApiResult.Loading -> { /* no-op */ }
                                }
                                isLoadingWiki = false
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
                            idolData = idolData,
                            onFirstArticleVideoPlaying = { isPlaying ->
                                isFirstArticleVideoPlaying = isPlaying
                            },
                            onNavigateToProfile = { userId, nickname, imageUrl, level, mostIdolName ->
                                selectedUserProfile = UserProfileInfo(
                                    userId = userId,
                                    nickname = nickname,
                                    imageUrl = imageUrl,
                                    level = level,
                                    mostIdolName = mostIdolName
                                )
                            },
                            onNavigateToArticleDetail = { article, _ ->
                                selectedArticle = article
                                isSelectedArticleFromFeed = true
                            },
                            onNavigateToPhotoDetail = { article, mediaIndex ->
                                selectedPhotoDetail = article to mediaIndex
                            },
                            onNavigateToNoticeDetail = { article ->
                                selectedNoticeArticle = article
                            },
                            onNavigateToArticleEdit = { article ->
                                editingArticle = article
                                articleWriteType = ArticleWriteType.FEED
                                showArticleWriteScreen = true
                            },
                            viewModel = feedViewModel
                        )
                        CommunityTab.FAN_TALK -> CommunityFanTalkSubPage(
                            idolData = idolData,
                            onNavigateToArticleDetail = { article, externalTabName, onArticleUpdated ->
                                selectedArticle = article
                                selectedArticleExternalIdolName = externalTabName
                                isSelectedArticleFromFeed = false
                                articleUpdatedCallback = onArticleUpdated
                            },
                            onNavigateToArticleEdit = { article ->
                                editingArticle = article
                                articleWriteType = ArticleWriteType.FAN_TALK
                                showArticleWriteScreen = true
                            },
                            viewModel = fanTalkViewModel
                        )
                        CommunityTab.CHAT -> CommunityChatSubPage(
                            idolData = idolData,
                            shouldRefresh = shouldRefreshChatList,
                            onRefreshConsumed = { shouldRefreshChatList = false },
                            onNavigateToChatRoom = { roomId, nickname, userId, role, isAnonymity, title ->
                                selectedChatRoom = ChatRoomInfo(
                                    roomId = roomId,
                                    nickname = nickname,
                                    userId = userId,
                                    role = role,
                                    isAnonymity = isAnonymity,
                                    title = title
                                )
                            },
                            onNavigateToCreateRoom = {
                                showChatRoomCreateScreen = true
                            }
                        )
                        CommunityTab.SCHEDULE -> CommunityScheduleSubPage(
                            idolData = idolData,
                            onNavigateToScheduleEdit = { schedule ->
                                editingSchedule = schedule
                                showScheduleWriteScreen = true
                            },
                            onNavigateToScheduleDetail = { schedule, yearMonthDay, locale ->
                                selectedScheduleDetail = Triple(schedule, yearMonthDay, locale)
                            },
                            onNavigateToMonthScheduleDetail = { schedule, yearMonth, locale ->
                                selectedMonthScheduleDetail = Triple(schedule, yearMonth, locale)
                            },
                            viewModel = scheduleViewModel
                        )
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
                    id = "community_top3_${idolData.id}",
                    idolId = idolId ?: 0,
                    // Flow에서 관찰된 top3 이미지/비디오 사용 (실시간 업데이트)
                    imageUrls = idolData.top3ImageUrls,
                    videoUrls = idolData.top3VideoUrls,
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
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        when (currentTab) {
                            CommunityTab.FEED -> {
                                articleWriteType = ArticleWriteType.FEED
                                showArticleWriteScreen = true
                            }
                            CommunityTab.FAN_TALK -> {
                                articleWriteType = ArticleWriteType.FAN_TALK
                                showArticleWriteScreen = true
                            }
                            CommunityTab.CHAT -> {
                                showChatRoomCreateScreen = true
                            }
                            CommunityTab.SCHEDULE -> {
                                showScheduleWriteScreen = true
                            }
                            else -> {}
                        }
                    },
                tint = Color.Unspecified
            )
        }
    }

    // 위키 웹뷰 - ExoScaffold 위에 표시
    AnimatedVisibility(
        visible = showWikiWebView && wikiUrl != null,
        enter = fadeIn(),
        exit = fadeOut()
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
            idolData = idolData,
            isFavorite = currentIsFavorite,
            isMost = currentIsMost,
            isFavoriteEnabled = !isUpdatingFavorite,  // 업데이트 중에는 비활성화
            onDismiss = { showIdolDialog = false },
            onFavoriteChange = { newValue ->
                // 디바운스: 이미 업데이트 중이면 무시
                if (isUpdatingFavorite) return@IdolDialog

                // 최애일 경우 즐겨찾기 해제 불가 (Old 프로젝트와 동일)
                if (currentIsMost && currentIsFavorite && !newValue) {
                    // 최애는 즐겨찾기 해제 불가 - 체크박스 상태 유지
                    return@IdolDialog
                }

                val targetIdolId = idolId ?: return@IdolDialog
                coroutineScope.launch {
                    viewModel.toggleFavorite(
                        idolId = targetIdolId,
                        currentIsFavorite = currentIsFavorite,
                        onSuccess = { newIsFavorite ->
                            currentIsFavorite = newIsFavorite
                        },
                        onError = { errorMessage ->
                            Toast.makeText(
                                context,
                                errorMessage ?: context.getString(R.string.error_abnormal_default),
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    )
                }
            },
            onMostChange = { newValue ->
                // 최애 변경 확인 다이얼로그 표시
                showChangeMostDialog = true
            },
            onVoteRankingClick = {
                showIdolDialog = false
                showVoterTop100Screen = true
            },
            onTrendsClick = {
                showIdolDialog = false
                showTrendsScreen = true
            },
            onRankHistoryClick = {
                showIdolDialog = false
                showIdolIdolRankingHistoryScreen = true
            },
            onShareClick = {
                showIdolDialog = false
                // 공유 URL 생성 (old 프로젝트와 동일)
                val params = listOf("community")
                val queries = listOf(
                    "idol" to idolData.id,
                    "group" to (idolData.groupId?.toString() ?: idolData.id)
                )
                val shareUrl = LinkUtil.getAppLinkUrl(context, params, queries)

                // 공유 메시지 생성 (아이돌 이름 포함)
                val idolDisplayName = idolData.name.split("_").firstOrNull() ?: idolData.name
                val shareMessage = context.getString(R.string.community_main_share_msg, idolDisplayName)

                // 공유 Intent
                IntentUtil.shareTextWithUrl(
                    context = context,
                    message = shareMessage,
                    url = shareUrl,
                    chooserTitle = context.getString(R.string.title_share)
                )
            },
            onAllInDayClick = {
                showIdolDialog = false
                showBurningDayDialog = true
            }
        )
    }

    // 최애 변경 확인 다이얼로그 (Old 프로젝트의 showChangeMostDialog 구현)
    if (showChangeMostDialog) {
        val idolName = idolData.name  // 이름_그룹명 전체
        val message = if (currentIsMost) {
            // 최애 해제 시
            // 기존 문구: "최애 재설정은 48시간이 지난 이후에 가능합니다.\n등록된 최애를 해제하시겠습니까?"
            buildAnnotatedString {
                append(context.getString(R.string.msg_favorite_unregi_guide2))
                append("\n")
                append(context.getString(R.string.msg_favorite_unregi_guide1))
            }
        } else {
            // 최애 설정 시 - 아이돌 이름만 빨간색으로 표시
            // Old 문구 순서: "최애는 한 번 설정하면 48시간이 지난 이후에 변경이 가능합니다.\n아이유_솔로를(을) 최애로 등록하시겠습니까?"
            val guide2Template = context.getString(R.string.msg_favorite_guide_2__)  // "%s를(을) 최애로 등록하시겠습니까?"

            buildAnnotatedString {
                append(context.getString(R.string.msg_favorite_guide_1))
                append("\n")
                // %s를 아이돌 이름으로 치환하고, 아이돌 이름만 빨간색으로 표시
                val parts = guide2Template.split("%s")
                if (parts.size >= 2) {
                    append(parts[0])  // "%s" 앞 텍스트 (보통 비어있음)
                    withStyle(style = SpanStyle(color = ColorPalette.main)) {
                        append(idolName)
                    }
                    append(parts[1])  // "%s" 뒤 텍스트 ("를(을) 최애로 등록하시겠습니까?")
                } else {
                    // %s가 없는 경우 (예외 처리)
                    withStyle(style = SpanStyle(color = ColorPalette.main)) {
                        append(idolName)
                    }
                    append(guide2Template)
                }
            }
        }

        ExoConfirmDialog(
            title = context.getString(R.string.my_idol),
            message = message,
            confirmButtonText = stringResource(R.string.yes),
            dismissButtonText = stringResource(R.string.no),
            onConfirm = {
                showChangeMostDialog = false
                // ViewModel의 updateMostIdol 호출
                coroutineScope.launch {
                    viewModel.updateMostIdol(
                        idolData = idolData,
                        currentIsMost = currentIsMost,
                        onSuccess = { newIsMost ->
                            currentIsMost = newIsMost
                            // 최애 설정 시 즐겨찾기도 자동으로 on (Old 프로젝트와 동일)
                            if (newIsMost) {
                                currentIsFavorite = true
                            }
                            onMostChanged(newIsMost)
                        },
                        onError = { errorMessage ->
                            Toast.makeText(
                                context,
                                errorMessage ?: context.getString(R.string.error_abnormal_default),
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    )
                }
            },
            onDismiss = {
                showChangeMostDialog = false
            }
        )
    }

    // 최애 변경 로딩 인디케이터
    if (isUpdatingMost) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.3f)),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = ColorPalette.main)
        }
    }

    // 올인데이 설정 다이얼로그
    if (showBurningDayDialog) {
        BurningDayDialog(
            onDismiss = { showBurningDayDialog = false },
            onSuccess = { burningDay ->
                // 올인데이 설정 성공 시 처리
                // old 프로젝트와 동일하게 처리: 아이돌의 burningDay 업데이트
                // ViewModel을 통해 아이돌 정보 갱신 필요 시 추가 구현
            }
        )
    }

    // VoterTop100 화면 (전체 화면으로 표시)
    // name은 "이름_그룹명" 형식 (예: "Leeseo_IVE")
    val nameParts = idolData.name.split("_")
    val displayIdolName = nameParts.getOrElse(0) { idolData.name }
    val displayGroupName = nameParts.getOrElse(1) { "" }

    AnimatedVisibility(
        visible = showVoterTop100Screen,
        enter = fadeIn(),
        exit = fadeOut()
    ) {
        VoterTop100Screen(
            idolId = idolId ?: 0,
            idolName = displayIdolName,
            groupName = displayGroupName,
            onBackClick = { showVoterTop100Screen = false }
        )
    }

    // TrendsScreen (이붙그램)
    AnimatedVisibility(
        visible = showTrendsScreen,
        enter = fadeIn(),
        exit = fadeOut()
    ) {
        TrendsScreen(
            idolId = idolId ?: 0,
            idolName = idolData.name,
            onBackClick = { showTrendsScreen = false },
            onItemClick = { trendsItem ->
                if (trendsItem.bannerUrl != null) {
                    selectedTrendsItem = trendsItem
                }
            }
        )
    }

    // IdolRankingHistoryScreen (랭킹 변동)
    AnimatedVisibility(
        visible = showIdolIdolRankingHistoryScreen,
        enter = fadeIn(),
        exit = fadeOut()
    ) {
        IdolRankingHistoryScreen(
            idolId = idolId ?: 0,
            idolName = idolData.name,
            onBackClick = { showIdolIdolRankingHistoryScreen = false }
        )
    }

    // Trends Photo Detail
    AnimatedVisibility(
        visible = selectedTrendsItem != null,
        enter = fadeIn(),
        exit = fadeOut()
    ) {
        selectedTrendsItem?.let { trendsItem ->
            PhotoDetailScreen(
                trendsModel = trendsItem,
                onBackClick = { selectedTrendsItem = null }
            )
        }
    }

    // ProfileScreen (전체 화면으로 표시)
    AnimatedVisibility(
        visible = selectedUserProfile != null,
        enter = fadeIn(),
        exit = fadeOut()
    ) {
        selectedUserProfile?.let { userInfo ->
            ProfileScreen(
                userId = userInfo.userId,
                userNickname = userInfo.nickname,
                userImageUrl = userInfo.imageUrl,
                userLevel = userInfo.level,
                mostIdolName = userInfo.mostIdolName,
                isMine = false,  // 타인의 프로필
                onBackClick = { selectedUserProfile = null },
                onNavigateToArticleDetail = { article ->
                    selectedArticle = article
                },
                onNavigateToArticleEdit = { article ->
                    editingArticle = article
                    articleWriteType = ArticleWriteType.FEED
                    showArticleWriteScreen = true
                }
            )
        }
    }

    // ArticleDetailScreen (전체 화면으로 표시)
    AnimatedVisibility(
        visible = selectedArticle != null,
        enter = fadeIn(),
        exit = fadeOut()
    ) {
        selectedArticle?.let { article ->
            ArticleDetailScreen(
                article = article,
                isFeed = isSelectedArticleFromFeed, // FeedSubPage에서 진입한 경우 true
                externalTabName = selectedArticleExternalIdolName,
                onBackClick = {
                    selectedArticle = null
                    selectedArticleExternalIdolName = null
                    isSelectedArticleFromFeed = false
                    articleUpdatedCallback = null
                },
                onArticleUpdated = { updatedArticle ->
                    // 실시간으로 ViewModel 직접 업데이트 (리스트 즉시 반영)
                    selectedArticle = updatedArticle
                    // FAN_TALK 탭에서 온 경우 articleUpdatedCallback 호출
                    articleUpdatedCallback?.invoke(updatedArticle)
                        ?: feedViewModel.updateArticle(updatedArticle)
                },
                onArticleDeleted = {
                    // 삭제 완료 시: 화면 닫고 리스트에서 제거
                    val deletedArticleId = article.id
                    selectedArticle = null
                    feedViewModel.removeArticle(deletedArticleId)
                },
                onNavigateToProfile = { userId, nickname, imageUrl, level, mostIdolName ->
                    selectedUserProfile = UserProfileInfo(
                        userId = userId,
                        nickname = nickname,
                        imageUrl = imageUrl,
                        level = level,
                        mostIdolName = mostIdolName
                    )
                }
            )
        }
    }

    // WebViewScreen (공지사항 상세 화면)
    AnimatedVisibility(
        visible = selectedNoticeArticle != null,
        enter = fadeIn(),
        exit = fadeOut()
    ) {
        selectedNoticeArticle?.let { article ->
            WebViewScreen(
                htmlContent = article.contentHtml ?: article.content,
                baseUrl = ServerUrl.HOST,
                title = article.title,
                onNavigateBack = {
                    selectedNoticeArticle = null
                }
            )
        }
    }

    // PhotoDetailScreen (전체 화면으로 표시)
    AnimatedVisibility(
        visible = selectedPhotoDetail != null,
        enter = fadeIn(),
        exit = fadeOut()
    ) {
        selectedPhotoDetail?.let { (article, mediaIndex) ->
            PhotoDetailScreen(
                article = article,
                initialIndex = mediaIndex,
                onBackClick = { selectedPhotoDetail = null }
            )
        }
    }

    // Month Schedule Detail (ScheduleDetailScreen - 월간 스케줄 목록)
    AnimatedVisibility(
        visible = selectedMonthScheduleDetail != null,
        enter = fadeIn(),
        exit = fadeOut()
    ) {
        selectedMonthScheduleDetail?.let { (schedule, yearMonth, locale) ->
            ScheduleDetailScreen(
                idolId = idolId ?: 0,
                yearMonth = yearMonth,
                locale = locale,
                initialScheduleId = schedule.id,
                onNavigateBack = {
                    selectedMonthScheduleDetail = null
                },
                onNavigateToScheduleEdit = { scheduleToEdit ->
                    editingSchedule = scheduleToEdit
                    showScheduleWriteScreen = true
                },
                onNavigateToComments = { scheduleForComments ->
                    // 댓글 화면으로 이동
                    selectedScheduleDetail = Triple(scheduleForComments, yearMonth, locale)
                }
            )
        }
    }

    // Schedule Detail (ArticleDetailScreen with schedule mode - 댓글 화면)
    AnimatedVisibility(
        visible = selectedScheduleDetail != null,
        enter = fadeIn(),
        exit = fadeOut()
    ) {
        selectedScheduleDetail?.let { (schedule, yearMonthDay, locale) ->
            ArticleDetailScreen(
                article = ArticleModel(id = schedule.articleId.toString()),
                schedule = schedule,
                scheduleIdolId = idolId ?: 0,
                scheduleYearMonthDay = yearMonthDay,
                scheduleLocale = locale,
                scheduleIdolName = displayIdolName,
                scheduleUserName = schedule.userName ?: "",
                scheduleUserLevel = schedule.userLevel,
                scheduleUserId = schedule.userId,
                onBackClick = {
                    selectedScheduleDetail = null
                },
                onScheduleUpdated = { updatedSchedule ->
                    // 스케줄 목록에서 해당 스케줄 업데이트 (댓글 수 등 실시간 반영)
                    scheduleViewModel.updateSchedule(updatedSchedule)
                },
                onScheduleDeleted = { scheduleId ->
                    selectedScheduleDetail = null
                    // 스케줄 목록 새로고침
                    scheduleViewModel.sendIntent(
                        CommunityScheduleContract.Intent.Refresh(idolId ?: 0)
                    )
                },
                onNavigateToScheduleEdit = { scheduleToEdit ->
                    selectedScheduleDetail = null
                    editingSchedule = scheduleToEdit
                    showScheduleWriteScreen = true
                },
                onNavigateToProfile = { userId, nickname, imageUrl, level, mostIdolName ->
                    selectedUserProfile = UserProfileInfo(userId, nickname, imageUrl, level, mostIdolName)
                }
            )
        }
    }

    // ScheduleWriteScreen (전체 화면으로 표시)
    AnimatedVisibility(
        visible = showScheduleWriteScreen,
        enter = fadeIn(),
        exit = fadeOut()
    ) {
        ScheduleWriteScreen(
            idolId = idolId,
            initialDate = scheduleWriteInitialDate,
            editingSchedule = editingSchedule,
            onNavigateBack = {
                showScheduleWriteScreen = false
                scheduleWriteInitialDate = null
                editingSchedule = null
            },
            onNavigateBackWithResult = { schedule ->
                showScheduleWriteScreen = false
                scheduleWriteInitialDate = null
                editingSchedule = null
                // 스케줄 목록 새로고침
                scheduleViewModel.sendIntent(
                    CommunityScheduleContract.Intent.Refresh(idolId ?: 0)
                )
            }
        )
    }

    // ChatRoomCreateScreen (전체 화면으로 표시)
    AnimatedVisibility(
        visible = showChatRoomCreateScreen,
        enter = fadeIn(),
        exit = fadeOut()
    ) {
        ChatRoomCreateScreen(
            idolId = idolId ?: 0,
            onNavigateBack = {
                showChatRoomCreateScreen = false
            },
            onRoomCreated = { roomId, nickname, userId, isAnonymity, title ->
                showChatRoomCreateScreen = false
                // 생성된 채팅방으로 이동
                selectedChatRoom = ChatRoomInfo(
                    roomId = roomId,
                    nickname = nickname,
                    userId = userId,
                    role = "O", // Owner
                    isAnonymity = isAnonymity,
                    title = title
                )
            }
        )
    }

    // ChatRoomScreen (전체 화면으로 표시)
    AnimatedVisibility(
        visible = selectedChatRoom != null,
        enter = fadeIn(),
        exit = fadeOut()
    ) {
        selectedChatRoom?.let { chatRoom ->
            ChatRoomScreen(
                roomId = chatRoom.roomId,
                nickname = chatRoom.nickname,
                userId = chatRoom.userId,
                role = chatRoom.role,
                isAnonymity = chatRoom.isAnonymity,
                title = chatRoom.title,
                onNavigateBack = {
                    selectedChatRoom = null
                    shouldRefreshChatList = true  // 채팅방에서 나올 때 항상 refresh
                },
                onNavigateBackWithRefresh = {
                    selectedChatRoom = null
                    shouldRefreshChatList = true
                }
            )
        }
    }

    // ArticleWriteScreen (전체 화면으로 표시)
    AnimatedVisibility(
        visible = showArticleWriteScreen,
        enter = fadeIn(),
        exit = fadeOut()
    ) {
        ArticleWriteScreen(
            writeType = articleWriteType,
            idolId = editingArticle?.idol?.id ?: idolId,
            editingArticleId = editingArticle?.id,
            onNavigateBack = {
                showArticleWriteScreen = false
                editingArticle = null
            },
            onNavigateBackWithResult = { updatedArticle ->
                showArticleWriteScreen = false
                editingArticle = null
                // 수정 완료 시 리스트의 해당 아이템 업데이트
                updatedArticle?.let { article ->
                    when (articleWriteType) {
                        ArticleWriteType.FEED -> feedViewModel.updateArticle(article)
                        ArticleWriteType.FAN_TALK -> fanTalkViewModel.updateArticle(article)
                        else -> { /* FREE_BOARD 등은 CommunityScreen에서 처리하지 않음 */ }
                    }
                }
            }
        )
    }
}

/**
 * 유저 프로필 정보 데이터 클래스
 */
data class UserProfileInfo(
    val userId: Int,
    val nickname: String,
    val imageUrl: String?,
    val level: Int,
    val mostIdolName: String? = null
)

/**
 * 채팅방 정보 데이터 클래스
 */
data class ChatRoomInfo(
    val roomId: Int,
    val nickname: String?,
    val userId: Int?,
    val role: String?,
    val isAnonymity: Boolean,
    val title: String
)

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
 * @param idolData 아이돌 실시간 데이터 (Flow에서 실시간 업데이트)
 * @param isCollapsed Top3가 숨겨졌을 때 true - 간소화된 툴바 형태로 표시
 * @param onBackClick Collapsed 상태에서 뒤로가기 버튼 클릭
 */
@Composable
private fun IdolProfile(
    idolData: CommunityViewModel.IdolData,
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
                fullName = idolData.name,
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
                    imageUrl = idolData.photoUrl ?: "",
                    type = ProfileImageType.MEDIUM_CIRCLE,
                    rank = 0,
                    anniversary = idolData.anniversary ?: "N",
                    anniversaryDays = idolData.anniversaryDays,
                    miracleCount = idolData.miracleCount,
                    fairyCount = idolData.fairyCount,
                    angelCount = idolData.angelCount
                )
            }

            Spacer(modifier = Modifier.width(5.dp))

            // 이름 + 그룹명 + 팔로워
            Column(modifier = Modifier.weight(1f)) {
                ExoNameWithGroup(
                    fullName = idolData.name,
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
                        text = NumberFormatUtil.formatFollowerCount(idolData.mostCount),
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
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) { onMoreClick() }
        )
    }
}

/**
 * IdolDialog - 아이돌 정보 다이얼로그
 * Old 프로젝트의 IdolCommunityDialogFragment를 Compose로 구현
 *
 * @param idolData 아이돌 실시간 데이터
 * @param isFavorite 즐겨찾기 여부
 * @param isMost 최애 여부
 * @param isFavoriteEnabled 즐겨찾기 버튼 활성화 여부 (디바운스용)
 * @param onDismiss 다이얼로그 닫기
 * @param onFavoriteChange 즐겨찾기 변경
 * @param onMostChange 최애 변경
 * @param onVoteRankingClick 하트 투표 랭킹 클릭
 * @param onTrendsClick 이붙그램 클릭
 * @param onRankHistoryClick 랭킹 변동 클릭
 * @param onShareClick 공유 클릭
 * @param onAllInDayClick 올인데이 설정 클릭
 */
@Composable
private fun IdolDialog(
    idolData: CommunityViewModel.IdolData,
    isFavorite: Boolean = false,
    isMost: Boolean = false,
    isFavoriteEnabled: Boolean = true,
    onDismiss: () -> Unit = {},
    onFavoriteChange: (Boolean) -> Unit = {},
    onMostChange: (Boolean) -> Unit = {},
    onVoteRankingClick: () -> Unit = {},
    onTrendsClick: () -> Unit = {},
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
                // 비밀의 방 아이돌인 경우 숨김
                val isSecretRoom = idolData.id.toIntOrNull() == Constants.SECRET_ROOM_IDOL_ID
                if (!isSecretRoom) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 15.dp, end = 14.dp),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // 최애 버튼 (Old: btn_most = 하트, 아이콘 17dp)
                        Icon(
                            painter = painterResource(
                                if (isMost) R.drawable.btn_favorite_on else R.drawable.btn_favorite_off
                            ),
                            contentDescription = "Most Favorite",
                            tint = Color.Unspecified,
                            modifier = Modifier
                                .size(17.dp)
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null
                                ) { onMostChange(!isMost) }
                        )

                        Spacer(modifier = Modifier.width(12.dp))

                        // 즐겨찾기 버튼 (Old: btn_favorite = 별, 아이콘 17dp)
                        // 최애인 경우 즐겨찾기 해제 불가
                        val canClickFavorite = isFavoriteEnabled && !(isMost && isFavorite)
                        Icon(
                            painter = painterResource(
                                if (isFavorite) R.drawable.btn_bookmark_on else R.drawable.btn_bookmark_off
                            ),
                            contentDescription = "Favorite",
                            tint = Color.Unspecified,
                            modifier = Modifier
                                .size(17.dp)
                                .then(
                                    if (canClickFavorite) {
                                        Modifier.clickable(
                                            interactionSource = remember { MutableInteractionSource() },
                                            indication = null
                                        ) { onFavoriteChange(!isFavorite) }
                                    } else {
                                        Modifier // 비활성화 시 클릭 안됨
                                    }
                                )
                        )
                    }
                } else {
                    // 비밀의 방 아이돌: 버튼 영역 높이만 확보
                    Spacer(modifier = Modifier.height(15.dp))
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
                        imageUrl = idolData.photoUrl ?: "",
                        type = ProfileImageType.XLARGE,
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
                        fullName = idolData.name,
                        nameFontSize = 17.sp,
                        groupFontSize = 15.sp
                    )

                    // 생일 (Old: birthday와 most_count는 붙어 있음)
                    idolData.birthday?.let { birthday ->
                        if (birthday.isNotEmpty()) {
                            Text(
                                text = birthday,
                                fontSize = 10.sp,
                                lineHeight = 14.sp,
                                color = ColorPalette.textGray
                            )
                        }
                    }

                    // 최애 수 (Old: 10sp, text_gray)
                    Text(
                        text = stringResource(R.string.most_favorite) + " : " +
                                NumberFormatUtil.formatFollowerCount(idolData.mostCount),
                        fontSize = 10.sp,
                        lineHeight = 14.sp,
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
                            onClick = onTrendsClick
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
                if (idolData.angelCount > 0) {
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
                            text = idolData.angelCount.toString(),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = ColorPalette.textAngel,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                }
                // Fairy 배지
                if (idolData.fairyCount > 0) {
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
                            text = idolData.fairyCount.toString(),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = ColorPalette.textFairy,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                }
                // Miracle 배지
                if (idolData.miracleCount > 0) {
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
                            text = idolData.miracleCount.toString(),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = ColorPalette.textMiracle,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                }
                // Rookie 배지
                if (idolData.rookieCount > 0) {
                    val isSuper = idolData.rookieCount >= 3
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
                            text = if (isSuper) "S" else idolData.rookieCount.toString(),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isSuper) ColorPalette.textSuperRookie else ColorPalette.textRookie,
                            modifier = Modifier.padding(top = 8.dp)
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
