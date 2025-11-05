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
import net.ib.mn.ui.components.ExoRankingList
import net.ib.mn.util.IdolImageUtil

/**
 * 기적(Global) 랭킹 SubPage
 *
 * 완전히 독립적인 페이지로, 자체 ViewModel과 상태를 관리합니다.
 * charts/ranks/ API 사용, 남녀 변경에 영향 받지 않음
 */
@Composable
fun GlobalRankingSubPage(
    chartCode: String,
    isVisible: Boolean = true,
    listState: LazyListState? = null,
    modifier: Modifier = Modifier
) {
    android.util.Log.d("GlobalRankingSubPage", "🎨 [Composing] Global for chartCode: $chartCode")

    // 독립적인 GlobalRankingSubPageViewModel
    val viewModel: GlobalRankingSubPageViewModel = hiltViewModel<GlobalRankingSubPageViewModel, GlobalRankingSubPageViewModel.Factory> { factory ->
        factory.create(chartCode)
    }

    val uiState by viewModel.uiState.collectAsState()
    val scrollState = listState ?: rememberLazyListState()

    // 초기 로드
    LaunchedEffect(Unit) {
        android.util.Log.d("GlobalRankingSubPage", "[Global] LaunchedEffect triggered")
        viewModel.reloadIfNeeded()
    }

    // 화면 가시성 변경 시 UDP 구독 관리 및 데이터 새로고침
    LaunchedEffect(isVisible) {
        if (isVisible) {
            android.util.Log.d("GlobalRankingSubPage", "[SubPage] 👁️ Screen became visible")
            viewModel.onScreenVisible()
        } else {
            android.util.Log.d("GlobalRankingSubPage", "[SubPage] 🙈 Screen hidden")
            viewModel.onScreenHidden()
        }
    }

    when (uiState) {
        is GlobalRankingSubPageViewModel.UiState.Loading -> {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = colorResource(R.color.main))
            }
        }

        is GlobalRankingSubPageViewModel.UiState.Error -> {
            val error = uiState as GlobalRankingSubPageViewModel.UiState.Error
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

        is GlobalRankingSubPageViewModel.UiState.Success -> {
            val success = uiState as GlobalRankingSubPageViewModel.UiState.Success

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
                        id = "ranking_global_$chartCode",
                        imageUrls = imageUrls,
                        videoUrls = videoUrls,
                        isVisible = isVisible
                    )
                }

                ExoRankingList(
                    items = success.items,
                    exoTop3Data = exoTop3Data,
                    listState = scrollState,
                    onItemClick = { rank, item ->
                        android.util.Log.d("GlobalRankingSubPage", "Clicked: Rank $rank - ${item.name}")
                    }
                )
            }
        }
    }
}

