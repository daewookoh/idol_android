package net.ib.mn.presentation.main.ranking.idol_subpage

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import net.ib.mn.R
import net.ib.mn.ad.RewardAdManager
import net.ib.mn.ui.components.ExoLoading
import net.ib.mn.ui.components.ExoSimpleDialog
import net.ib.mn.ui.components.ExoImagePickCard
import net.ib.mn.ui.components.ExoTabSwitch
import net.ib.mn.ui.components.ExoThemePickCard
import net.ib.mn.ui.components.VoteStatusCode
import net.ib.mn.ui.components.LocalThemePickDetailClick
import net.ib.mn.ui.components.LocalThemePickLiveClick
import net.ib.mn.ui.components.LocalImagePickDetailClick
import net.ib.mn.ui.components.LocalImagePickLiveClick
import net.ib.mn.ui.theme.ColorPalette

/**
 * OnePick (테마픽/이미지픽) 랭킹 SubPage
 *
 * 테마픽과 이미지픽을 탭으로 전환하며 표시
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnePickRankingSubPage(
    chartCode: String,
    isVisible: Boolean = true,
    listState: LazyListState? = null,
    modifier: Modifier = Modifier
) {
    // CompositionLocal을 통해 Navigation으로 화면 전환
    val onThemePickDetailClick = LocalThemePickDetailClick.current
    val onThemePickLiveClick = LocalThemePickLiveClick.current
    val onImagePickDetailClick = LocalImagePickDetailClick.current
    val onImagePickLiveClick = LocalImagePickLiveClick.current

    val context = LocalContext.current
    val activity = context as? android.app.Activity

    val viewModel: OnePickRankingSubPageViewModel = hiltViewModel<OnePickRankingSubPageViewModel, OnePickRankingSubPageViewModel.Factory> { factory ->
        factory.create(chartCode)
    }

    val uiState by viewModel.uiState.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    val scrollState = listState ?: rememberLazyListState()

    // 참여자 없음 다이얼로그 상태
    var showNoParticipantsDialog by remember { mutableStateOf(false) }

    // 광고 에러 다이얼로그 상태
    var showAdErrorDialog by remember { mutableStateOf(false) }

    // 광고 로딩 상태
    var isAdLoading by remember { mutableStateOf(false) }

    // RewardAdManager
    val rewardAdManager = remember { RewardAdManager.getInstance() }

    // 참여자 없음 다이얼로그
    if (showNoParticipantsDialog) {
        ExoSimpleDialog(
            message = stringResource(R.string.onepick_no_votes),
            onDismiss = { showNoParticipantsDialog = false }
        )
    }

    // 광고 에러 다이얼로그
    if (showAdErrorDialog) {
        ExoSimpleDialog(
            message = stringResource(R.string.video_ad_unable),
            onDismiss = { showAdErrorDialog = false }
        )
    }

    // 초기 로드
    LaunchedEffect(Unit) {
        viewModel.reloadIfNeeded()
    }

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(ColorPalette.background400)
        ) {
        // 탭 영역
        val tabs = listOf(
            stringResource(R.string.themepick),
            stringResource(R.string.imagepick)
        )

        val selectedIndex = when (uiState) {
            is OnePickRankingSubPageViewModel.UiState.ThemePickSuccess -> 0
            is OnePickRankingSubPageViewModel.UiState.ImagePickSuccess -> 1
            else -> 0
        }

        ExoTabSwitch(
            tabs = tabs,
            selectedIndex = selectedIndex,
            onTabSelected = { index ->
                val tabType = if (index == 0) {
                    OnePickRankingSubPageViewModel.TabType.THEME_PICK
                } else {
                    OnePickRankingSubPageViewModel.TabType.IMAGE_PICK
                }
                viewModel.switchTab(tabType)
            },
            modifier = Modifier.fillMaxWidth()
        )

        // 컨텐츠 영역 with PullToRefresh
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = { viewModel.refresh() },
            modifier = Modifier.fillMaxSize()
        ) {
            when (val state = uiState) {
                is OnePickRankingSubPageViewModel.UiState.Loading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = ColorPalette.main)
                    }
                }

                is OnePickRankingSubPageViewModel.UiState.Error -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = stringResource(R.string.msg_error_ok),
                            fontSize = 16.sp,
                            color = ColorPalette.main
                        )
                    }
                }

                is OnePickRankingSubPageViewModel.UiState.ThemePickSuccess -> {
                    if (state.items.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = stringResource(R.string.msg_no_data),
                                fontSize = 16.sp,
                                color = ColorPalette.textDimmed
                            )
                        }
                    } else {
                        LazyColumn(
                            state = scrollState,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(state.items) { cardData ->
                                ExoThemePickCard(
                                    state = cardData.state,
                                    title = cardData.title,
                                    subTitle = cardData.subTitle,
                                    imageUrl = cardData.imageUrl,
                                    voteCount = cardData.voteCount,
                                    periodDate = cardData.periodDate,
                                    voteStatus = cardData.voteStatus,
                                    isNew = cardData.isNew,
                                    onCardClick = {
                                        onThemePickDetailClick(cardData.id)
                                    },
                                    onVoteClick = {
                                        onThemePickDetailClick(cardData.id)
                                    },
                                    onCurrentRankingClick = {
                                        if (cardData.voteCountRaw == 0) {
                                            showNoParticipantsDialog = true
                                        } else {
                                            onThemePickLiveClick(cardData.id)
                                        }
                                    }
                                )
                            }
                        }
                    }
                }

                is OnePickRankingSubPageViewModel.UiState.ImagePickSuccess -> {
                    if (state.items.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = stringResource(R.string.msg_no_data),
                                fontSize = 16.sp,
                                color = ColorPalette.textDimmed
                            )
                        }
                    } else {
                        LazyColumn(
                            state = scrollState,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(state.items) { cardData ->
                                ExoImagePickCard(
                                    state = cardData.state,
                                    title = cardData.title,
                                    subTitle = cardData.subTitle,
                                    voteCount = cardData.voteCount,
                                    periodDate = cardData.periodDate,
                                    voteStatus = cardData.voteStatus,
                                    isNew = cardData.isNew,
                                    onCardClick = {
                                        onImagePickDetailClick(cardData.id)
                                    },
                                    onVoteClick = {
                                        when (cardData.voteStatus) {
                                            VoteStatusCode.NEED_VIDEO_AD -> {
                                                // 광고 시청 후 투표 화면 이동
                                                if (activity != null && !isAdLoading) {
                                                    isAdLoading = true
                                                    rewardAdManager.loadAd(
                                                        context = context,
                                                        onLoaded = {
                                                            rewardAdManager.showAd(
                                                                activity = activity,
                                                                onRewarded = {
                                                                    isAdLoading = false
                                                                    onImagePickDetailClick(cardData.id)
                                                                },
                                                                onFailed = { _ ->
                                                                    isAdLoading = false
                                                                    showAdErrorDialog = true
                                                                },
                                                                onDismissed = {
                                                                    isAdLoading = false
                                                                }
                                                            )
                                                        },
                                                        onFailed = { _ ->
                                                            isAdLoading = false
                                                            showAdErrorDialog = true
                                                        }
                                                    )
                                                }
                                            }
                                            VoteStatusCode.VOTED_TODAY -> {
                                                // 이미 투표 완료 - 아무 동작 없음 (버튼 비활성화됨)
                                            }
                                            else -> {
                                                // 첫 투표 가능 - 바로 투표 화면으로
                                                onImagePickDetailClick(cardData.id)
                                            }
                                        }
                                    },
                                    onCurrentRankingClick = {
                                        if (cardData.voteCountRaw == 0) {
                                            showNoParticipantsDialog = true
                                        } else {
                                            onImagePickLiveClick(cardData.id)
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
        }

        // 광고 로딩 중 오버레이
        ExoLoading(isLoading = isAdLoading)
    }
}