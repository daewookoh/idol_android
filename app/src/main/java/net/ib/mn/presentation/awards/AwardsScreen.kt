package net.ib.mn.presentation.awards

import androidx.activity.compose.BackHandler
import net.ib.mn.util.IntentUtil
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import net.ib.mn.R
import net.ib.mn.presentation.community.CommunityScreen
import net.ib.mn.presentation.community.IdolRankingHistoryScreen
import net.ib.mn.presentation.awards.subpage.AwardsCumulativeSubPage
import net.ib.mn.presentation.awards.subpage.AwardsDailySubPage
import net.ib.mn.presentation.awards.subpage.AwardsGuideSubPage
import net.ib.mn.presentation.awards.subpage.AwardsLineupSubPage
import net.ib.mn.ui.components.ExoAppBar
import net.ib.mn.ui.components.ExoScaffold
import net.ib.mn.ui.components.LocalRankingItemClick
import net.ib.mn.ui.components.RankingItem
import net.ib.mn.ui.theme.ColorPalette

private const val TAB_HEIGHT = 48
private const val INDICATOR_HEIGHT = 3

/**
 * 탭 타입 정의
 */
private enum class AwardsTabType {
    LINEUP,      // 라인업 (투표 전)
    CUMULATIVE,  // 누적
    REALTIME,    // 실시간
    GUIDE        // 가이드
}

/**
 * 투표 상태별 탭 구성 (old 프로젝트 기준)
 * - BEFORE: 안내, 라인업 (안내가 기본)
 * - RUNNING: 안내, 누적, 실시간 (실시간이 기본)
 * - AFTER: 누적만 (탭 없음)
 */
private fun getTabsForState(votableState: VotableState): List<AwardsTabType> = when (votableState) {
    VotableState.BEFORE -> listOf(AwardsTabType.GUIDE, AwardsTabType.LINEUP)
    VotableState.RUNNING -> listOf(AwardsTabType.GUIDE, AwardsTabType.CUMULATIVE, AwardsTabType.REALTIME)
    VotableState.AFTER -> listOf(AwardsTabType.CUMULATIVE)
}

@Composable
fun AwardsScreen(
    onNavigateBack: () -> Unit = {},
    viewModel: AwardsViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val awardModel by viewModel.awardModel.collectAsState()
    val votableState = viewModel.getVotableState()

    // 투표 상태에 따른 탭 구성
    val tabTypes = remember(votableState) { getTabsForState(votableState) }
    val tabs = tabTypes.map { tabType ->
        when (tabType) {
            AwardsTabType.LINEUP -> stringResource(R.string.award_example)
            AwardsTabType.CUMULATIVE -> stringResource(R.string.aggregation)
            AwardsTabType.REALTIME -> stringResource(R.string.award_realtime)
            AwardsTabType.GUIDE -> stringResource(R.string.award_guide)
        }
    }

    // 투표 중일 때 실시간 탭(index 2)에서 시작, 그 외에는 첫 번째 탭
    val initialPage = if (votableState == VotableState.RUNNING && tabTypes.size > 2) 2 else 0
    val pagerState = rememberPagerState(initialPage = initialPage) { tabs.size }
    val coroutineScope = rememberCoroutineScope()

    // IdolRankingHistoryScreen 상태
    var showIdolIdolRankingHistoryScreen by remember { mutableStateOf(false) }
    var selectedIdolId by remember { mutableIntStateOf(0) }
    var selectedIdolName by remember { mutableStateOf("") }

    // CommunityScreen 상태 (실시간 랭킹 아이템 클릭 시)
    var selectedCommunityRankingItem by remember { mutableStateOf<RankingItem?>(null) }

    // CommunityScreen 백 핸들러
    BackHandler(enabled = selectedCommunityRankingItem != null) {
        selectedCommunityRankingItem = null
    }

    // IdolRankingHistoryScreen 백 핸들러
    BackHandler(enabled = showIdolIdolRankingHistoryScreen) {
        showIdolIdolRankingHistoryScreen = false
    }

    Box(modifier = Modifier.fillMaxSize()) {
        CompositionLocalProvider(
            LocalRankingItemClick provides { item ->
                selectedCommunityRankingItem = item
            }
        ) {
            ExoScaffold(
                topBar = {
                    ExoAppBar(
                        title = awardModel?.awardTitle.orEmpty(),
                        onNavigationClick = onNavigateBack,
                        actions = {
                            IconButton(
                                onClick = {
                                    IntentUtil.shareText(context, viewModel.getShareMessage())
                                },
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.btn_navigation_share),
                                    contentDescription = null,
                                    tint = Color.Unspecified
                                )
                            }
                        }
                    )
                }
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background)
                ) {
                    HorizontalPager(
                        state = pagerState,
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        userScrollEnabled = false
                    ) { page ->
                        val tabType = tabTypes.getOrNull(page) ?: return@HorizontalPager
                        when (tabType) {
                            AwardsTabType.LINEUP -> AwardsLineupSubPage()
                            AwardsTabType.CUMULATIVE -> AwardsCumulativeSubPage(
                                onItemClick = { item ->
                                    selectedIdolId = item.idolId
                                    selectedIdolName = item.idol?.name.orEmpty()
                                    showIdolIdolRankingHistoryScreen = true
                                }
                            )
                            AwardsTabType.REALTIME -> AwardsDailySubPage()
                            AwardsTabType.GUIDE -> AwardsGuideSubPage()
                        }
                    }

                    // 탭이 1개일 때는 탭바 숨김
                    if (tabs.size > 1) {
                        AwardsBottomTabBar(
                            tabs = tabs,
                            pagerState = pagerState,
                            coroutineScope = coroutineScope
                        )
                    }
                }
            }
        }

        // IdolRankingHistoryScreen (랭킹 변동)
        AnimatedVisibility(
            visible = showIdolIdolRankingHistoryScreen,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            IdolRankingHistoryScreen(
                idolId = selectedIdolId,
                idolName = selectedIdolName,
                onBackClick = { showIdolIdolRankingHistoryScreen = false }
            )
        }

        // CommunityScreen (실시간 랭킹 아이템 클릭 시)
        AnimatedVisibility(
            visible = selectedCommunityRankingItem != null,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            selectedCommunityRankingItem?.let { rankingItem ->
                val idolId = rankingItem.id.toIntOrNull() ?: return@let
                CommunityScreen(
                    idolId = idolId,
                    showChattingTab = false,
                    onBackClick = { selectedCommunityRankingItem = null }
                )
            }
        }
    }
}

@Composable
private fun AwardsBottomTabBar(
    tabs: List<String>,
    pagerState: PagerState,
    coroutineScope: CoroutineScope
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(TAB_HEIGHT.dp)
            .background(ColorPalette.background100)
    ) {
        tabs.forEachIndexed { index, title ->
            val isSelected = pagerState.currentPage == index

            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxSize()
                    .clickable {
                        coroutineScope.launch { pagerState.animateScrollToPage(index) }
                    },
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(INDICATOR_HEIGHT.dp)
                        .background(if (isSelected) ColorPalette.main else Color.Transparent)
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = title,
                        color = if (isSelected) ColorPalette.main else ColorPalette.textDimmed,
                        fontSize = 15.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}
