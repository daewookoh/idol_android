package net.ib.mn.presentation.main.ranking.idol_subpage

import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import net.ib.mn.domain.ranking.MyFavoriteRankingDataSource
import net.ib.mn.domain.repository.RankingRepository
import net.ib.mn.ui.components.exoRankingItems
import net.ib.mn.ui.components.RankingItem
import net.ib.mn.ui.theme.ColorPalette
import androidx.hilt.navigation.compose.hiltViewModel

/**
 * My Favorite Ranking State Holder
 *
 * ViewModel과 state를 관리하고 결과를 반환
 */
@Composable
fun rememberMyFavoriteRankingState(
    chartCode: String,
    favoriteIds: Set<Int>,
    isVisible: Boolean = true,
    rankingRepository: RankingRepository
): MyFavoriteRankingData {
    if (favoriteIds.isEmpty()) {
        return MyFavoriteRankingData.Empty
    }

    // ViewModel 생성
    val viewModelKey = "favorite_ranking_$chartCode"
    val viewModel: UnifiedRankingSubPageViewModel = hiltViewModel<UnifiedRankingSubPageViewModel, UnifiedRankingSubPageViewModel.Factory>(
        key = viewModelKey
    ) { factory ->
        val dataSource = MyFavoriteRankingDataSource(
            rankingRepository = rankingRepository,
            favoriteIds = favoriteIds,
            chartCode = chartCode
        )
        factory.create(chartCode, dataSource)
    }

    val uiState by viewModel.uiState.collectAsState()

    // 초기 로드
    LaunchedEffect(chartCode) {
        viewModel.reloadIfNeeded()
    }

    // 화면 가시성 변경 시
    LaunchedEffect(isVisible) {
        if (isVisible) {
            viewModel.onScreenVisible()
        } else {
            viewModel.onScreenHidden()
        }
    }

    return when (uiState) {
        is UnifiedRankingSubPageViewModel.UiState.Loading -> MyFavoriteRankingData.Loading
        is UnifiedRankingSubPageViewModel.UiState.Error -> {
            MyFavoriteRankingData.Error((uiState as UnifiedRankingSubPageViewModel.UiState.Error).message)
        }
        is UnifiedRankingSubPageViewModel.UiState.Success -> {
            val success = uiState as UnifiedRankingSubPageViewModel.UiState.Success
            // ✨ 중요: 순위는 전체 목록에서 이미 계산됨
            // 여기서는 favoriteIds로 노출만 필터링
            val favoriteIdSet: Set<Int> = favoriteIds
            val filteredItems = success.items.filter { item: RankingItem ->
                val itemIdInt = item.id.toIntOrNull()
                itemIdInt != null && favoriteIdSet.contains(itemIdInt)
            }
            android.util.Log.d(
                "MyFavoriteRanking",
                "📊 Chart $chartCode: ${success.items.size} total → ${filteredItems.size} favorites (ranks preserved)"
            )
            MyFavoriteRankingData.Success(filteredItems, viewModel)
        }
    }
}

/**
 * My Favorite Ranking Data (sealed class for state)
 */
sealed class MyFavoriteRankingData {
    data object Empty : MyFavoriteRankingData()
    data object Loading : MyFavoriteRankingData()
    data class Error(val message: String) : MyFavoriteRankingData()
    data class Success(
        val items: List<RankingItem>,
        val viewModel: UnifiedRankingSubPageViewModel
    ) : MyFavoriteRankingData()
}

/**
 * LazyListScope extension to add My Favorite ranking items
 *
 * LocalRankingItemClick은 ExoRankingItem 내부에서 직접 처리됨
 *
 * @param chartCode 차트 코드
 * @param data 랭킹 데이터
 * @param expandedItemIds 확장된 아이템 ID 목록
 * @param onExpandedChange 확장 상태 변경 콜백
 */
fun LazyListScope.myFavoriteRankingItems(
    chartCode: String,
    data: MyFavoriteRankingData,
    expandedItemIds: Set<String> = emptySet(),
    onExpandedChange: (String, Boolean) -> Unit = { _, _ -> }
) {
    when (data) {
        is MyFavoriteRankingData.Empty -> {
            item(key = "empty_${chartCode}") {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "즐겨찾기한 아이돌이 없습니다.",
                        color = ColorPalette.textDimmed
                    )
                }
            }
        }
        is MyFavoriteRankingData.Loading -> {
            item(key = "loading_${chartCode}") {
                Box(
                    modifier = Modifier.height(60.dp),
                ) {
                }
            }
        }
        is MyFavoriteRankingData.Error -> {
            item(key = "error_${chartCode}") {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = data.message,
                        color = ColorPalette.main
                    )
                }
            }
        }
        is MyFavoriteRankingData.Success -> {
            exoRankingItems(
                items = data.items,
                type = "MAIN",
                // LocalRankingItemClick은 ExoRankingItem 내부에서 직접 처리됨
                onItemClick = { _, _ -> },
                onVoteSuccess = { idolId, voteCount ->
                    data.viewModel.updateVote(idolId, voteCount)
                },
                disableAnimation = true,  // MyFavoritePage에서는 애니메이션 비활성화
                expandedItemIds = expandedItemIds,
                onExpandedChange = onExpandedChange
            )
        }
    }
}

/**
 * RankingRepository 주입용 ViewModel
 */
@dagger.hilt.android.lifecycle.HiltViewModel
class MyFavoriteRankingViewModel @javax.inject.Inject constructor(
    val rankingRepository: RankingRepository
) : androidx.lifecycle.ViewModel()
