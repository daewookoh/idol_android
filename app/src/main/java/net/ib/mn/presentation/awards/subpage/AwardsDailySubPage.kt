package net.ib.mn.presentation.awards.subpage

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import net.ib.mn.data.remote.dto.AwardIdolItem
import net.ib.mn.ui.components.RankingItem
import net.ib.mn.ui.components.RankingItemType
import net.ib.mn.ui.components.exoRankingItems
import net.ib.mn.ui.theme.ColorPalette
import net.ib.mn.ui.theme.ExoTypo
import net.ib.mn.util.IdolImageUtil.toSecureUrl

/**
 * AwardsDailySubPage - 어워즈 실시간 순위 탭
 *
 * 기능:
 * 1. 입장 시 모든 차트의 데이터 로드 (ViewModel에서 처리)
 * 2. AwardsRankingManager가 2초마다 DB 기준 랭킹 정렬
 * 3. Flow를 통해 자동으로 UI 업데이트
 */
@Composable
fun AwardsDailySubPage(
    viewModel: AwardsDailyViewModel = hiltViewModel(),
    onItemClick: (AwardIdolItem) -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val awardModel by viewModel.awardModel.collectAsState()
    val selectedChartIndex by viewModel.selectedChartIndex.collectAsState()

    // 백그라운드에서 돌아올 때 데이터 새로고침
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.refresh()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // 차트 목록 가져오기
    val charts = when (val state = uiState) {
        is AwardsDailyUiState.Success -> state.charts
        is AwardsDailyUiState.Empty -> state.charts
        else -> awardModel?.charts
    }

    // 선택된 차트 정보
    val selectedChart = charts?.getOrNull(selectedChartIndex)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ColorPalette.background100)
    ) {
        // 공용 헤더 (배너 이미지 + 카테고리 탭)
        if (viewModel.votable == "Y") {
            AwardsHeader(
                awardModel = awardModel,
                charts = charts,
                selectedChartIndex = selectedChartIndex,
                onChartSelected = { viewModel.selectChart(it) }
            )
        }

        // 스크롤 영역
        when (val state = uiState) {
            is AwardsDailyUiState.Loading -> {
                DailyLoadingContent()
            }

            is AwardsDailyUiState.Success -> {
                // 2초마다 랭킹이 업데이트되므로 remember 사용하지 않음
                // maxHeart 계산 (투표 바 길이 비율 계산용)
                val maxHeart = state.rankItems.maxOfOrNull { it.heart } ?: 0L
                val minHeart = state.rankItems.minOfOrNull { it.heart } ?: 0L
                val rankingItems = state.rankItems.map { item ->
                    item.toRankingItem(maxHeart, minHeart)
                }

                // 탭 변경시 LazyColumn 새로 생성하여 스크롤 맨 위로 리셋
                key(selectedChartIndex) {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        // 헤더 정보 (타이틀 + 기간 + 설명)
                        if (viewModel.votable == "Y") {
                            item {
                                AwardsHeaderInfo(
                                    title = awardModel?.realtimeTitle.orEmpty(),
                                    period = viewModel.getVotingPeriodText(),
                                    description = selectedChart?.desc.orEmpty()
                                )
                            }
                        }

                        // 집계 기간 바
                        item {
                            AwardsAggregationBar(
                                periodText = viewModel.getAggregationPeriodText()
                            )
                        }

                        // 랭킹 리스트
                        exoRankingItems(
                            items = rankingItems,
                            type = RankingItemType.DAILY,
                            onItemClick = { index, _ ->
                                state.rankItems.getOrNull(index)?.let { onItemClick(it) }
                            },
                            onVoteSuccess = { idolId, votedHeart ->
                                viewModel.onVoteSuccess(idolId, votedHeart)
                            }
                        )
                    }
                }
            }

            is AwardsDailyUiState.Empty,
            is AwardsDailyUiState.Error -> {
                val message = when (state) {
                    is AwardsDailyUiState.Empty -> state.message
                    is AwardsDailyUiState.Error -> state.message
                    else -> ""
                }
                DailyMessageContent(message = message)
            }
        }
    }
}

/**
 * 로딩 상태 - 깜빡임 방지를 위해 로딩 인디케이터 숨김
 */
@Composable
private fun DailyLoadingContent() {
    Box(
        modifier = Modifier.fillMaxSize()
    )
}

/**
 * 빈/에러 상태 공통 메시지
 */
@Composable
private fun DailyMessageContent(message: String) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = message,
            style = ExoTypo.body14.copy(color = ColorPalette.textGray),
            textAlign = TextAlign.Center
        )
    }
}

/**
 * AwardIdolItem을 RankingItem으로 변환
 * rank는 AwardsRankingManager에서 이미 계산됨 (0-indexed)
 *
 * @param maxHeart 리스트 내 최대 하트 수 (투표 바 길이 계산용)
 * @param minHeart 리스트 내 최소 하트 수 (투표 바 길이 계산용)
 */
private fun AwardIdolItem.toRankingItem(maxHeart: Long, minHeart: Long): RankingItem {
    val displayRank = rank + 1
    val idolName = idol?.name.orEmpty()
    val groupName = idol?.group.orEmpty()

    return RankingItem(
        rank = displayRank,
        name = if (groupName.isNotEmpty()) "${idolName}_$groupName" else idolName,
        nameEn = buildString {
            append(idol?.nameEn ?: idolName)
            val groupEn = idol?.groupEn ?: groupName
            if (groupEn.isNotEmpty()) append("_$groupEn")
        },
        voteCount = heart.toString(),
        photoUrl = idol?.imageUrl.toSecureUrl(),
        id = id.toString(),
        heartCount = heart,
        maxHeartCount = maxHeart,
        minHeartCount = minHeart
    )
}
