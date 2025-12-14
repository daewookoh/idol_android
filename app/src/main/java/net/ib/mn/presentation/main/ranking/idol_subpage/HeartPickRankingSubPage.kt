package net.ib.mn.presentation.main.ranking.idol_subpage

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import net.ib.mn.ui.theme.ExoTypo
import net.ib.mn.R
import net.ib.mn.ui.components.ExoHeartPickCard
import net.ib.mn.ui.components.LocalHeartPickDetailClick
import net.ib.mn.ui.theme.ColorPalette

/**
 * 기적(HeartPick) 랭킹 SubPage
 *
 * heartpick/ API 사용
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HeartPickRankingSubPage(
    chartCode: String,
    isVisible: Boolean = true,
    listState: LazyListState? = null,
    modifier: Modifier = Modifier
) {

    // 독립적인 HeartPickRankingSubPageViewModel
    val viewModel: HeartPickRankingSubPageViewModel = hiltViewModel<HeartPickRankingSubPageViewModel, HeartPickRankingSubPageViewModel.Factory> { factory ->
        factory.create(chartCode)
    }

    val uiState by viewModel.uiState.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    val scrollState = listState ?: rememberLazyListState()

    // MainScreen에서 제공하는 HeartPickDetailScreen 열기 콜백
    val onHeartPickDetailClick = LocalHeartPickDetailClick.current

    // 초기 로드
    LaunchedEffect(Unit) {
        viewModel.reloadIfNeeded()
    }

    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = { viewModel.refresh() },
        modifier = Modifier.fillMaxSize()
    ) {
        when (uiState) {
            is HeartPickRankingSubPageViewModel.UiState.Loading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = ColorPalette.main)
                }
            }

            is HeartPickRankingSubPageViewModel.UiState.Error -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(R.string.msg_error_ok),
                        style = ExoTypo.typo16.copy(color = ColorPalette.main)
                    )
                }
            }

            is HeartPickRankingSubPageViewModel.UiState.Success -> {
                val success = uiState as HeartPickRankingSubPageViewModel.UiState.Success

                if (success.items.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = stringResource(R.string.msg_no_data),
                            style = ExoTypo.typo16.copy(color = ColorPalette.textDimmed)
                        )
                    }
                } else {
                    LazyColumn(
                        state = scrollState,
                        modifier = Modifier
                            .fillMaxSize()
                            .background(ColorPalette.background400)
                    ) {
                        items(success.items) { cardData ->
                            ExoHeartPickCard(
                                state = cardData.state,
                                title = cardData.title,
                                subTitle = cardData.subTitle,
                                backgroundImageUrl = cardData.backgroundImageUrl,
                                dDay = cardData.dDay,
                                firstPlaceIdol = cardData.firstPlaceIdol,
                                otherIdols = cardData.otherIdols,
                                heartVoteCount = cardData.heartVoteCount,
                                commentCount = cardData.commentCount,
                                periodDate = cardData.periodDate,
                                openDate = cardData.openDate,
                                openPeriod = cardData.openPeriod,
                                isNew = cardData.isNew,
                                onCardClick = {
                                    onHeartPickDetailClick(cardData.id)
                                },
                                onVoteClick = {
                                    onHeartPickDetailClick(cardData.id)
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
