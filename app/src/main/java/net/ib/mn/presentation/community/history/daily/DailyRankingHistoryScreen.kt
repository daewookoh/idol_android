package net.ib.mn.presentation.community.history.daily

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import net.ib.mn.R
import net.ib.mn.domain.model.DailyRankingHistoryModel
import net.ib.mn.ui.components.ExoAppBar
import net.ib.mn.ui.components.ExoScaffold
import net.ib.mn.ui.components.RankingItem
import net.ib.mn.ui.components.cumulativeRankingItems
import net.ib.mn.ui.theme.ColorPalette

/**
 * DailyRankingHistoryScreen - 특정 날짜 전체 랭킹 히스토리 화면
 *
 * Old 프로젝트의 HallOfFameTopHistoryActivity를 참고하여 Compose로 구현
 * 특정 날짜의 전체 아이돌 랭킹 표시
 *
 * @param historyParam 날짜 파라미터 (예: "2024-01-01")
 * @param type 타입 (예: "S", "G") - CELEB 플레이버용
 * @param category 카테고리 (예: "M", "F") - CELEB 플레이버용
 * @param chartCode 차트 코드 - app 플레이버용
 * @param dateTitle 화면에 표시할 날짜 타이틀 (예: "2024년 1월 1일")
 * @param onBackClick 뒤로가기 클릭 이벤트
 */
@Composable
fun DailyRankingHistoryScreen(
    historyParam: String,
    type: String = "",
    category: String = "",
    chartCode: String = "",
    dateTitle: String,
    onBackClick: () -> Unit = {}
) {
    // ViewModel 생성
    val viewModel: DailyRankingHistoryViewModel = hiltViewModel(
        key = "daily_ranking_history_${historyParam}_${type}_${category}_$chartCode",
        creationCallback = { factory: DailyRankingHistoryViewModel.Factory ->
            factory.create(
                savedStateHandle = SavedStateHandle(
                    mapOf(
                        "historyParam" to historyParam,
                        "type" to type,
                        "category" to category,
                        "chartCode" to chartCode
                    )
                )
            )
        }
    )

    val uiState by viewModel.uiState.collectAsState()

    BackHandler { onBackClick() }

    // 타이틀: 날짜 + 순위 (Old: date + " " + getString(R.string.title_daily_rank))
    val dailyRankTitle = stringResource(R.string.title_daily_rank)

    ExoScaffold(
        topBar = {
            ExoAppBar(
                title = "$dateTitle $dailyRankTitle",
                onNavigationClick = onBackClick
            )
        }
    ) {
        when (val state = uiState) {
            is DailyRankingHistoryUiState.Loading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = ColorPalette.main)
                }
            }

            is DailyRankingHistoryUiState.Error -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = state.message,
                        color = ColorPalette.textGray
                    )
                }
            }

            is DailyRankingHistoryUiState.Success -> {
                if (state.items.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = stringResource(R.string.no_data),
                            color = ColorPalette.textGray,
                            fontSize = 14.sp
                        )
                    }
                } else {
                    DailyRankingHistoryContent(items = state.items)
                }
            }
        }
    }
}

/**
 * DailyRankingHistoryContent - 랭킹 히스토리 콘텐츠
 */
@Composable
private fun DailyRankingHistoryContent(
    items: List<DailyRankingHistoryModel>
) {
    // DailyRankingHistoryModel을 RankingItem으로 변환
    // id는 rank를 포함하여 고유하게 생성 (LazyColumn key 중복 방지)
    val rankingItems = remember(items) {
        items.mapIndexed { index, model ->
            RankingItem(
                id = "daily_${index}_${model.idolId}",
                name = model.idolName,
                rank = model.rank,
                voteCount = model.heart.toString(),
                heartCount = model.heart,
                photoUrl = model.secureImageUrl,
                status = model.status.ifEmpty { null },
                difference = model.difference
            )
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(ColorPalette.background100)
    ) {
        cumulativeRankingItems(
            items = rankingItems,
            onItemClick = { _, _ -> },
            showArrow = false,
            clickEnabled = false,
            countSuffix = "표"
        )
    }
}
