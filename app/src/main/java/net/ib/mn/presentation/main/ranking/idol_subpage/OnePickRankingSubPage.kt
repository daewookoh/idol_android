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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import net.ib.mn.R
import net.ib.mn.ui.components.ExoSimpleDialog
import net.ib.mn.ui.components.ExoImagePickCard
import net.ib.mn.ui.components.ExoTabSwitch
import net.ib.mn.ui.components.ExoThemePickCard
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
    modifier: Modifier = Modifier,
    onThemePickDetailClick: (Int) -> Unit = {},
    onThemePickResultClick: (Int) -> Unit = {}
) {

    val viewModel: OnePickRankingSubPageViewModel = hiltViewModel<OnePickRankingSubPageViewModel, OnePickRankingSubPageViewModel.Factory> { factory ->
        factory.create(chartCode)
    }

    val uiState by viewModel.uiState.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    val scrollState = listState ?: rememberLazyListState()

    // 참여자 없음 다이얼로그 상태
    var showNoParticipantsDialog by remember { mutableStateOf(false) }

    // 참여자 없음 다이얼로그
    if (showNoParticipantsDialog) {
        ExoSimpleDialog(
            message = stringResource(R.string.onepick_no_votes),
            onDismiss = { showNoParticipantsDialog = false }
        )
    }

    // 초기 로드
    LaunchedEffect(Unit) {
        viewModel.reloadIfNeeded()
    }

    Column(
        modifier = modifier
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
                                            onThemePickResultClick(cardData.id)
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
                                    onCardClick = { },
                                    onVoteClick = { },
                                    onCurrentRankingClick = {
                                        if (cardData.voteCountRaw == 0) {
                                            showNoParticipantsDialog = true
                                        } else {
                                            // TODO: Navigate to ImagePick result
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
}