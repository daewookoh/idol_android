package net.ib.mn.presentation.main.ranking.idol_subpage

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import net.ib.mn.R
import net.ib.mn.ui.components.MainRankingList
import net.ib.mn.util.IdolImageUtil

/**
 * 기적(HeartPick) 랭킹 SubPage
 *
 * 완전히 독립적인 페이지로, 자체 ViewModel과 상태를 관리합니다.
 * charts/ranks/ API 사용, 남녀 변경에 영향 받지 않음
 */
@Composable
fun HeartPickRankingSubPage(
    chartCode: String,
    isVisible: Boolean = true,
    listState: LazyListState? = null,
    modifier: Modifier = Modifier
) {
    android.util.Log.d("HeartPickRankingSubPage", "🎨 [Composing] HeartPick for chartCode: $chartCode")

    // 독립적인 HeartPickRankingSubPageViewModel
    val viewModel: HeartPickRankingSubPageViewModel = hiltViewModel<HeartPickRankingSubPageViewModel, HeartPickRankingSubPageViewModel.Factory> { factory ->
        factory.create(chartCode)
    }

    val uiState by viewModel.uiState.collectAsState()
    val scrollState = listState ?: rememberLazyListState()

    // 초기 로드
    LaunchedEffect(Unit) {
        android.util.Log.d("HeartPickRankingSubPage", "[HeartPick] LaunchedEffect triggered")
        viewModel.reloadIfNeeded()
    }

    // 화면 가시성 변경 시 UDP 구독 관리 및 데이터 새로고침
    LaunchedEffect(isVisible) {
        if (isVisible) {
            android.util.Log.d("HeartPickRankingSubPage", "[SubPage] 👁️ Screen became visible")
            viewModel.onScreenVisible()
        } else {
            android.util.Log.d("HeartPickRankingSubPage", "[SubPage] 🙈 Screen hidden")
            viewModel.onScreenHidden()
        }
    }

    when (uiState) {
        is HeartPickRankingSubPageViewModel.UiState.Loading -> {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = colorResource(R.color.main))
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
                    text = "오류: ${error.message}",
                    fontSize = 16.sp,
                    color = colorResource(R.color.main)
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
                        text = "랭킹 데이터가 없습니다.",
                        fontSize = 16.sp,
                        color = colorResource(R.color.text_dimmed)
                    )
                }
            } else {
                // ExoTop3 데이터 생성
                val exoTop3Data = success.topIdol?.let { topIdol ->
                    val imageUrls = IdolImageUtil.getTop3ImageUrls(topIdol)
                    val videoUrls = IdolImageUtil.getTop3VideoUrls(topIdol)

                    net.ib.mn.ui.components.ExoTop3Data(
                        id = "ranking_heartpick_$chartCode",
                        imageUrls = imageUrls,
                        videoUrls = videoUrls,
                        isVisible = isVisible
                    )
                }

                MainRankingList(
                    items = success.items,
                    exoTop3Data = exoTop3Data,
                    listState = scrollState,
                    onItemClick = { rank, item ->
                        android.util.Log.d("HeartPickRankingSubPage", "Clicked: Rank $rank - ${item.name}")
                    }
                )
            }
        }
    }
}

