package net.ib.mn.presentation.main.ranking.idol_subpage

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import net.ib.mn.R
import net.ib.mn.ui.components.ExoOnePickCard
import net.ib.mn.ui.components.ExoTabSwitch
import net.ib.mn.ui.theme.ColorPalette

/**
 * OnePick (테마픽/이미지픽) 랭킹 SubPage
 *
 * 테마픽과 이미지픽을 탭으로 전환하며 표시
 */
@Composable
fun OnePickRankingSubPage(
    chartCode: String,
    isVisible: Boolean = true,
    listState: LazyListState? = null,
    modifier: Modifier = Modifier
) {
    android.util.Log.d("OnePickRankingSubPage", "🎨 [Composing] OnePick for chartCode: $chartCode")

    val viewModel: OnePickRankingSubPageViewModel = hiltViewModel<OnePickRankingSubPageViewModel, OnePickRankingSubPageViewModel.Factory> { factory ->
        factory.create(chartCode)
    }

    val uiState by viewModel.uiState.collectAsState()
    val scrollState = listState ?: rememberLazyListState()

    // 초기 로드
    LaunchedEffect(Unit) {
        android.util.Log.d("OnePickRankingSubPage", "[OnePick] LaunchedEffect triggered")
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

        when (val state = uiState) {
            is OnePickRankingSubPageViewModel.UiState.Success -> {
                val selectedIndex = when (state.selectedTab) {
                    OnePickRankingSubPageViewModel.TabType.THEME_PICK -> 0
                    OnePickRankingSubPageViewModel.TabType.IMAGE_PICK -> 1
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
            }
            else -> {
                // Loading이나 Error 상태에서도 탭 표시
                ExoTabSwitch(
                    tabs = tabs,
                    selectedIndex = 0,
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
            }
        }

        // 컨텐츠 영역
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

            is OnePickRankingSubPageViewModel.UiState.Success -> {
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
                            ExoOnePickCard(
                                state = cardData.state,
                                title = cardData.title,
                                subTitle = cardData.subTitle,
                                imageUrl = cardData.imageUrl,
                                voteCount = cardData.voteCount,
                                periodDate = cardData.periodDate,
                                onCardClick = {
                                    android.util.Log.d("OnePickRankingSubPage", "Card clicked: ${cardData.title}")
                                },
                                onVoteClick = {
                                    android.util.Log.d("OnePickRankingSubPage", "Vote clicked: ${cardData.title}")
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}