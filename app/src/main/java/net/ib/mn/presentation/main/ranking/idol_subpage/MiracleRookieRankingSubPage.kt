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
import net.ib.mn.domain.ranking.RankingDataSource
import net.ib.mn.ui.components.ExoRankingList

/**
 * 통합 Miracle/Rookie 랭킹 SubPage
 *
 * UnifiedRankingSubPage와 동일한 구조이지만, Top3 기능 없음
 *
 * @param chartCode 차트 코드
 * @param dataSource 랭킹 데이터 소스 (Miracle/Rookie)
 * @param isVisible 화면 가시성
 * @param listState 리스트 스크롤 상태
 * @param modifier Modifier
 */
@Composable
fun MiracleRookieRankingSubPage(
    chartCode: String,
    dataSource: RankingDataSource,
    isVisible: Boolean = true,
    listState: LazyListState? = null,
    modifier: Modifier = Modifier
) {
    android.util.Log.d("MiracleRookieSubPage", "🎨 [Composing] ${dataSource.type} for chartCode: $chartCode")

    // ViewModel key 생성 (각 chartCode별로 독립적인 ViewModel 인스턴스 생성)
    val viewModelKey = "miracle_rookie_${dataSource.type}_$chartCode"
    android.util.Log.d("MiracleRookieSubPage", "🔑 ViewModel key: $viewModelKey")

    // ViewModel 생성
    val viewModel: MiracleRookieRankingSubPageViewModel = hiltViewModel<MiracleRookieRankingSubPageViewModel, MiracleRookieRankingSubPageViewModel.Factory>(
        key = viewModelKey  // 🔑 독립적인 인스턴스를 위한 key
    ) { factory ->
        android.util.Log.d("MiracleRookieSubPage", "🏭 Factory creating ViewModel for type=${dataSource.type}, chartCode=$chartCode")
        factory.create(chartCode, dataSource)
    }

    android.util.Log.d("MiracleRookieSubPage", "✅ ViewModel instance: ${viewModel.hashCode()}, type=${dataSource.type}")

    val uiState by viewModel.uiState.collectAsState()
    val scrollState = listState ?: rememberLazyListState()

    // chartCode가 변경되면 새로운 데이터 로드
    LaunchedEffect(chartCode) {
        android.util.Log.d("MiracleRookieSubPage", "[${dataSource.type}] LaunchedEffect triggered for: $chartCode")
        viewModel.reloadIfNeeded()
    }

    // 화면 가시성 변경 시 UDP 구독 관리 및 데이터 새로고침
    LaunchedEffect(isVisible) {
        if (isVisible) {
            android.util.Log.d("MiracleRookieSubPage", "[SubPage] 👁️ Screen became visible")
            viewModel.onScreenVisible()
        } else {
            android.util.Log.d("MiracleRookieSubPage", "[SubPage] 🙈 Screen hidden")
            viewModel.onScreenHidden()
        }
    }

    when (uiState) {
        is MiracleRookieRankingSubPageViewModel.UiState.Loading -> {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = colorResource(R.color.main))
            }
        }

        is MiracleRookieRankingSubPageViewModel.UiState.Error -> {
            val error = uiState as MiracleRookieRankingSubPageViewModel.UiState.Error
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

        is MiracleRookieRankingSubPageViewModel.UiState.Success -> {
            val success = uiState as MiracleRookieRankingSubPageViewModel.UiState.Success

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
                // Top3 없이 리스트만 표시
                ExoRankingList(
                    items = success.items,
                    topIdol = null,  // Top3 없음
                    isVisible = isVisible,
                    listState = scrollState,
                    onItemClick = { rank, item ->
                        android.util.Log.d("MiracleRookieSubPage", "Clicked: Rank $rank - ${item.name}")
                    },
                    onVoteSuccess = { idolId, voteCount ->
                        android.util.Log.d("MiracleRookieSubPage", "Vote success: idol=$idolId, votes=$voteCount")
                        viewModel.updateVote(idolId, voteCount)
                    }
                )
            }
        }
    }
}
