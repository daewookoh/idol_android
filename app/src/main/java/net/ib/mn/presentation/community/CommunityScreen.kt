package net.ib.mn.presentation.community

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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

    Box(modifier = Modifier.fillMaxSize()) {
        ExoScaffold {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(ColorPalette.background100)
            ) {
                // 상단 ExoTop3 + 뒤로가기 버튼
                Box {
                    ExoTop3(
                        rankingItemData = rankingItem,
                        isVisible = true,
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

                // 프로필 영역
                IdolProfile(
                    rankingItem = rankingItem,
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
                    onMoreClick = { /* TODO */ }
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

                // 탭 컨텐츠 (ViewPager)
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxSize(),
                    userScrollEnabled = false // 좌우 스크롤 막음 (old와 동일)
                ) { page ->
                    when (tabs[page]) {
                        CommunityTab.FEED -> CommunityFeedSubPage(rankingItem = rankingItem)
                        CommunityTab.FAN_TALK -> CommunityFanTalkSubPage(
                            rankingItem = rankingItem,
                            fandomName = fandomName
                        )
                        CommunityTab.CHAT -> CommunityChatSubPage(rankingItem = rankingItem)
                        CommunityTab.SCHEDULE -> CommunityScheduleSubPage(rankingItem = rankingItem)
                    }
                }
            }
        }

        // 위키 웹뷰 (아래에서 올라오는 애니메이션)
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
    val coroutineScope = rememberCoroutineScope()
    val textDefaultColor = ColorPalette.textDefault
    val textDimmedColor = ColorPalette.textDimmed

    Column {
        PrimaryScrollableTabRow(
            selectedTabIndex = selectedTabIndex,
            minTabWidth = 0.dp,
            containerColor = ColorPalette.background100,
            contentColor = textDefaultColor,
            edgePadding = 3.dp,
            divider = {},
            indicator = @Composable {
                TabRowDefaults.SecondaryIndicator(
                    modifier = Modifier
                        .tabIndicatorOffset(selectedTabIndex)
                        .padding(horizontal = 12.dp),
                    color = textDefaultColor
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
                        ) {
                            onTabSelected(index)
                        }
                        .padding(horizontal = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = title,
                        style = ExoTypo.title14.copy(
                            lineHeight = 14.sp,
                            color = if (selectedTabIndex == index) textDefaultColor else textDimmedColor
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
 */
@Composable
private fun IdolProfile(
    rankingItem: RankingItem,
    onProfileImageClick: () -> Unit = {},
    onMoreClick: () -> Unit = {}
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(70.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
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

        // 더보기 버튼
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
