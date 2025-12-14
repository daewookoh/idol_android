package net.ib.mn.presentation.awards.subpage

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import net.ib.mn.data.remote.dto.AwardRankItem
import net.ib.mn.ui.components.RankingItem
import net.ib.mn.ui.components.RankingItemType
import net.ib.mn.ui.components.exoRankingItems
import net.ib.mn.ui.theme.ColorPalette
import net.ib.mn.ui.theme.ExoTypo
import net.ib.mn.util.IdolImageUtil.toSecureUrl

/**
 * AwardsCumulativeSubPage - 어워즈 누적 순위 집계 탭
 *
 * old 프로젝트의 AwardsAggregatedFragment 기반
 *
 * UI 순서 (상단 고정, 리스트만 리렌더):
 * 1. 배너 이미지 + 카테고리 탭 (AwardsHeader)
 * 2. 타이틀 + 설명 + 투표 기간
 * 3. 집계 기간 바
 * 4. 누적 순위 리스트 (탭 변경시에만 리렌더)
 */
@Composable
fun AwardsCumulativeSubPage(
    viewModel: AwardsCumulativeViewModel = hiltViewModel(),
    onItemClick: (AwardRankItem) -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val awardModel by viewModel.awardModel.collectAsState()
    val selectedChartIndex by viewModel.selectedChartIndex.collectAsState()

    // 차트 목록 가져오기
    val charts = when (val state = uiState) {
        is AwardsCumulativeUiState.Success -> state.charts
        is AwardsCumulativeUiState.Empty -> state.charts
        else -> awardModel?.charts
    }

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
            is AwardsCumulativeUiState.Loading -> {
                CumulativeLoadingContent()
            }

            is AwardsCumulativeUiState.Success -> {
                // maxHeart 계산 (투표 바 길이 비율 계산용)
                val maxHeart = remember(state.rankItems) {
                    state.rankItems.maxOfOrNull { it.score.toLong() } ?: 0L
                }
                val minHeart = remember(state.rankItems) {
                    state.rankItems.minOfOrNull { it.score.toLong() } ?: 0L
                }
                // 순위 리스트
                val rankingItems = remember(state.rankItems) {
                    state.rankItems.mapIndexed { index, item ->
                        item.toRankingItem(index, viewModel.votable == "Y", maxHeart, minHeart)
                    }
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
                                    title = awardModel?.aggTitle.orEmpty(),
                                    period = viewModel.getVotingPeriodText(),
                                    description = awardModel?.aggDesc.orEmpty()
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
                            type = RankingItemType.CUMULATIVE,
                            onItemClick = { index, _ ->
                                state.rankItems.getOrNull(index)?.let { onItemClick(it) }
                            }
                        )
                    }
                }
            }

            is AwardsCumulativeUiState.Empty,
            is AwardsCumulativeUiState.Error -> {
                val message = when (state) {
                    is AwardsCumulativeUiState.Empty -> state.message
                    is AwardsCumulativeUiState.Error -> state.message
                    else -> ""
                }
                CumulativeMessageContent(message = message)
            }
        }
    }
}

/**
 * 로딩 상태
 */
@Composable
private fun CumulativeLoadingContent() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(color = ColorPalette.main)
    }
}

/**
 * 빈/에러 상태 공통 메시지
 */
@Composable
private fun CumulativeMessageContent(message: String) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = message,
            style = ExoTypo.typo14.copy(color = ColorPalette.textGray),
            textAlign = TextAlign.Center
        )
    }
}

/**
 * AwardRankItem을 RankingItem으로 변환
 *
 * @param maxHeart 리스트 내 최대 점수 (투표 바 길이 계산용)
 * @param minHeart 리스트 내 최소 점수 (투표 바 길이 계산용)
 */
private fun AwardRankItem.toRankingItem(
    index: Int,
    isVotable: Boolean,
    maxHeart: Long,
    minHeart: Long
): RankingItem {
    val displayRank = if (rank >= 0) rank + 1 else index + 1
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
        voteCount = score.toString(),
        photoUrl = (idol?.imageUrl ?: imageUrl).toSecureUrl(),
        id = idolId.toString(),
        heartCount = score.toLong(),
        maxHeartCount = maxHeart,
        minHeartCount = minHeart
    )
}
