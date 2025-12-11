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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import net.ib.mn.ui.theme.ColorPalette
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import net.ib.mn.R
import net.ib.mn.ui.components.ExoHeartPickCard
import net.ib.mn.ui.components.LocalHeartPickDetailClick

/**
 * 기적(HeartPick) 랭킹 SubPage
 *
 * heartpick/ API 사용
 */
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
    val scrollState = listState ?: rememberLazyListState()

    // MainScreen에서 제공하는 HeartPickDetailScreen 열기 콜백
    val onHeartPickDetailClick = LocalHeartPickDetailClick.current

    // 초기 로드
    LaunchedEffect(Unit) {
        viewModel.reloadIfNeeded()
    }

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
            val error = uiState as HeartPickRankingSubPageViewModel.UiState.Error
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
                        fontSize = 16.sp,
                        color = ColorPalette.textDimmed
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
                                // MainScreen에서 HeartPickDetailScreen 오버레이로 표시
                                onHeartPickDetailClick(cardData.id)
                            },
                            onVoteClick = {
                                // MainScreen에서 HeartPickDetailScreen 오버레이로 표시
                                onHeartPickDetailClick(cardData.id)
                            }
                        )
                    }
                }
            }
        }
    }
}
