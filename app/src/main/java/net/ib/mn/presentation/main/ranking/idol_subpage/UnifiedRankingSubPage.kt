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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import net.ib.mn.ui.theme.ColorPalette
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import net.ib.mn.domain.ranking.RankingDataSource
import net.ib.mn.ui.components.ExoRankingList
import net.ib.mn.ui.components.RankingItem

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
 * @param isForFavorite MyFavorite용 여부 (true일 경우 ExoTop3 숨김, 스크롤 핸들링 비활성화)
 * @param onRankItemsLoaded 랭킹 데이터 로드 완료 콜백 (최애 이동 토스트용)
 */
@Composable
fun UnifiedRankingSubPage(
    chartCode: String,
    dataSource: RankingDataSource,
    isVisible: Boolean = true,
    listState: LazyListState? = null,
    modifier: Modifier = Modifier,
    isForFavorite: Boolean = false,
    onRankItemsLoaded: ((List<RankingItem>) -> Unit)? = null
) {
    // ViewModel 생성 (key를 사용하여 각 타입별로 다른 인스턴스 생성)
    val viewModelKey = "unified_ranking_${dataSource.type}_$chartCode"

    val viewModel: UnifiedRankingSubPageViewModel = hiltViewModel<UnifiedRankingSubPageViewModel, UnifiedRankingSubPageViewModel.Factory>(
        key = viewModelKey
    ) { factory ->
        factory.create(chartCode, dataSource)
    }

    val uiState by viewModel.uiState.collectAsState()
    // isForFavorite이 true면 새로운 스크롤 상태 생성 (독립적인 스크롤)
    val scrollState = if (isForFavorite) {
        rememberLazyListState()
    } else {
        listState ?: rememberLazyListState()
    }

    // 초기 로드 또는 chartCode 변경 시 처리
    LaunchedEffect(chartCode) {
        if (dataSource.supportGenderChange()) {
            viewModel.reloadWithNewCode(chartCode)
        } else {
            viewModel.reloadIfNeeded()
        }
    }

    // 화면 가시성 변경 시 UDP 구독 관리 및 데이터 새로고침
    LaunchedEffect(isVisible) {
        if (isVisible) {
            viewModel.onScreenVisible()
        } else {
            viewModel.onScreenHidden()
        }
    }

    when (val state = uiState) {
        is UnifiedRankingSubPageViewModel.UiState.Loading -> {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = ColorPalette.main)
            }
        }

        is UnifiedRankingSubPageViewModel.UiState.Error -> {
            Box(
                modifier = Modifier.fillMaxSize().padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "오류: ${state.message}",
                    fontSize = 16.sp,
                    color = ColorPalette.main
                )
            }
        }

        is UnifiedRankingSubPageViewModel.UiState.Success -> {
            // 랭킹 데이터 로드 완료 콜백 (최초 1회만 호출)
            LaunchedEffect(state.items) {
                if (state.items.isNotEmpty()) {
                    onRankItemsLoaded?.invoke(state.items)
                }
            }

            // 확장된 아이템 ID 목록 (스크롤 시에도 상태 유지)
            var expandedItemIds by remember { mutableStateOf(emptySet<String>()) }

            // 최상단 Top3 비디오 재생 여부: 모든 하위 Top3가 닫혀있을 때만 재생
            val isTop3VideoVisible = isVisible && expandedItemIds.isEmpty()

            if (state.items.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize().padding(16.dp),
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
                    items = state.items,
                    topIdol = if (isForFavorite) null else state.topIdol,
                    isVisible = isTop3VideoVisible,
                    listState = scrollState,
                    onItemClick = { _, _ -> },
                    onVoteSuccess = viewModel::updateVote,
                    disableAnimation = false,
                    expandedItemIds = expandedItemIds,
                    onExpandedChange = { itemKey, isExpanded ->
                        expandedItemIds = if (isExpanded) expandedItemIds + itemKey else expandedItemIds - itemKey
                    }
                )
            }
        }
    }
}
