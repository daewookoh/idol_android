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
import net.ib.mn.ui.theme.ColorPalette
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import net.ib.mn.R
import net.ib.mn.domain.ranking.RankingDataSource
import net.ib.mn.ui.components.ExoRankingList

/**
 * 통합 랭킹 SubPage (Global, Group, Solo 모두 지원)
 *
 * 세 개의 SubPage를 하나로 통합:
 * - GlobalRankingSubPage
 * - GroupRankingSubPage
 * - SoloRankingSubPage
 *
 * @param chartCode 차트 코드
 * @param dataSource 랭킹 데이터 소스 (Global/Group/Solo 구분)
 * @param isVisible 화면 가시성 (UDP 리스닝 제어)
 * @param listState LazyList 스크롤 상태
 * @param modifier Modifier
 */
@Composable
fun UnifiedRankingSubPage(
    chartCode: String,
    dataSource: RankingDataSource,
    isVisible: Boolean = true,
    listState: LazyListState? = null,
    modifier: Modifier = Modifier
) {
    android.util.Log.d("UnifiedRankingSubPage", "🎨 [Composing] ${dataSource.type} for chartCode: $chartCode")

    // ViewModel 생성 (key를 사용하여 각 타입별로 다른 인스턴스 생성)
    val viewModelKey = "unified_ranking_${dataSource.type}_$chartCode"
    android.util.Log.d("UnifiedRankingSubPage", "🔑 ViewModel key: $viewModelKey")

    val viewModel: UnifiedRankingSubPageViewModel = hiltViewModel<UnifiedRankingSubPageViewModel, UnifiedRankingSubPageViewModel.Factory>(
        key = viewModelKey
    ) { factory ->
        android.util.Log.d("UnifiedRankingSubPage", "🏭 Factory creating ViewModel for type=${dataSource.type}, chartCode=$chartCode")
        factory.create(chartCode, dataSource)
    }

    android.util.Log.d("UnifiedRankingSubPage", "✅ ViewModel instance: ${viewModel.hashCode()}, type=${dataSource.type}")

    val uiState by viewModel.uiState.collectAsState()
    val scrollState = listState ?: rememberLazyListState()

    // 초기 로드 또는 chartCode 변경 시 처리
    LaunchedEffect(chartCode) {
        android.util.Log.d("UnifiedRankingSubPage", "[${dataSource.type}] LaunchedEffect triggered for: $chartCode")

        if (dataSource.supportGenderChange()) {
            // Group/Solo: 남녀 변경 시 새로운 코드로 재로드
            viewModel.reloadWithNewCode(chartCode)
        } else {
            // Global: 캐시된 데이터가 있으면 사용
            viewModel.reloadIfNeeded()
        }
    }

    // 화면 가시성 변경 시 UDP 구독 관리 및 데이터 새로고침
    LaunchedEffect(isVisible) {
        if (isVisible) {
            android.util.Log.d("UnifiedRankingSubPage", "[${dataSource.type}] 👁️ Screen became visible")
            viewModel.onScreenVisible()
        } else {
            android.util.Log.d("UnifiedRankingSubPage", "[${dataSource.type}] 🙈 Screen hidden")
            viewModel.onScreenHidden()
        }
    }

    when (uiState) {
        is UnifiedRankingSubPageViewModel.UiState.Loading -> {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = ColorPalette.main)
            }
        }

        is UnifiedRankingSubPageViewModel.UiState.Error -> {
            val error = uiState as UnifiedRankingSubPageViewModel.UiState.Error
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "오류: ${error.message}",
                    fontSize = 16.sp,
                    color = ColorPalette.main
                )
            }
        }

        is UnifiedRankingSubPageViewModel.UiState.Success -> {
            val success = uiState as UnifiedRankingSubPageViewModel.UiState.Success

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
                        color = ColorPalette.textDimmed
                    )
                }
            } else {
                ExoRankingList(
                    items = success.items,
                    topIdol = success.topIdol,
                    isVisible = isVisible,
                    listState = scrollState,
                    onItemClick = { rank, item ->
                        android.util.Log.d("UnifiedRankingSubPage", "Clicked: Rank $rank - ${item.name}")
                    },
                    onVoteSuccess = { idolId, voteCount ->
                        viewModel.updateVote(idolId, voteCount)
                    }
                )
            }
        }
    }
}
