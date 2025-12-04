package net.ib.mn.presentation.awards.subpage

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import net.ib.mn.data.remote.dto.AwardChartsModel
import net.ib.mn.data.remote.dto.AwardModel
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
 * 1. 배너 이미지 (votable == "Y"일 때)
 * 2. 카테고리 탭
 * 3. 타이틀 + 설명 + 투표 기간
 * 4. 집계 기간 바
 * 5. 누적 순위 리스트 (탭 변경시에만 리렌더)
 */
@Composable
fun AwardsCumulativeSubPage(
    viewModel: AwardsCumulativeViewModel = hiltViewModel(),
    onItemClick: (AwardRankItem) -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val awardModel by viewModel.awardModel.collectAsState()
    val selectedChartIndex by viewModel.selectedChartIndex.collectAsState()
    val isDarkTheme = isSystemInDarkTheme()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ColorPalette.background100)
    ) {
        // ========== 상단 고정 영역 (리렌더 안됨) ==========

        // 1. 배너 이미지 (votable == "Y"일 때만)
        if (viewModel.votable == "Y" && awardModel != null) {
            BannerImage(
                awardModel = awardModel!!,
                isDarkTheme = isDarkTheme
            )
        }

        // 2. 카테고리 탭
        val charts = when (val state = uiState) {
            is AwardsCumulativeUiState.Success -> state.charts
            is AwardsCumulativeUiState.Empty -> state.charts
            else -> awardModel?.charts
        }

        if (viewModel.votable == "Y" && !charts.isNullOrEmpty()) {
            CategoryTabs(
                charts = charts,
                selectedIndex = selectedChartIndex,
                onChartSelected = { viewModel.selectChart(it) }
            )
        }

        // ========== 스크롤 영역 (타이틀부터 랭킹리스트까지) ==========
        when (val state = uiState) {
            is AwardsCumulativeUiState.Loading -> {
                LoadingContent()
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
                        // 3. 타이틀 + 설명 + 투표 기간 (votable == "Y"일 때만)
                        if (viewModel.votable == "Y") {
                            item {
                                AwardsCumulativeHeader(
                                    title = awardModel?.aggTitle.orEmpty(),
                                    description = awardModel?.aggDesc.orEmpty(),
                                    periodText = viewModel.getVotingPeriodText()
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                            }
                        }

                        // 4. 집계 기간 바
                        item {
                            AggregationPeriodBar(
                                periodText = viewModel.getAggregationPeriodText(),
                                visible = true
                            )
                        }

                        // 5. 랭킹 리스트
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
                MessageContent(message = message)
            }
        }
    }
}

/**
 * 배너 이미지
 * old: item_awards_top.xml - cl_banner
 * - layout_constraintDimensionRatio="H,360:120" (비율 3:1)
 * - paddingStart/End: 18dp
 * - 이미지가 background로 설정되어 전체 영역 채움
 */
@Composable
private fun BannerImage(
    awardModel: AwardModel,
    isDarkTheme: Boolean
) {
    val bannerUrl = if (isDarkTheme) {
        awardModel.bannerDarkImgUrl
    } else {
        awardModel.bannerLightImgUrl
    }

    if (!bannerUrl.isNullOrEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
        ) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(bannerUrl.toSecureUrl())
                    .crossfade(true)
                    .build(),
                contentDescription = "Awards Banner",
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(3f), // old: 360:120 = 3:1
                contentScale = ContentScale.FillBounds
            )
        }
    }
}

/**
 * 카테고리 탭 (charts)
 * old: item_tag.xml
 * - 컨테이너: paddingTop/Bottom 12dp, paddingEnd 6dp
 * - 아이템: minHeight 24dp, paddingStart/End 12dp
 *
 * old 스타일:
 * - 선택됨: textChat(#333333) 배경, fixWhite 텍스트
 * - 미선택: 투명 배경 + gray200 테두리, textDimmed 텍스트
 */
@Composable
private fun CategoryTabs(
    charts: List<AwardChartsModel>,
    selectedIndex: Int,
    onChartSelected: (Int) -> Unit
) {
    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        contentPadding = PaddingValues(horizontal = 14.dp)
    ) {
        itemsIndexed(charts) { index, chart ->
            val isSelected = index == selectedIndex

            Box(
                modifier = Modifier
                    .padding(top = 11.dp, bottom = 11.dp, end = 6.dp)
                    .defaultMinSize(minHeight = 24.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .then(
                        if (isSelected) {
                            // 선택됨: textChat 배경 (old: #333333)
                            Modifier.background(ColorPalette.textChat)
                        } else {
                            // 미선택: 투명 배경 + gray200 테두리
                            Modifier.border(
                                width = 1.dp,
                                color = ColorPalette.gray200,
                                shape = RoundedCornerShape(14.dp)
                            )
                        }
                    )
                    .clickable { onChartSelected(index) }
                    .padding(horizontal = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = chart.name.orEmpty(),
                    style = ExoTypo.body14.copy(
                        color = if (isSelected) ColorPalette.fixWhite else ColorPalette.textDimmed
                    )
                )
            }
        }
    }
}

/**
 * 상단 헤더 (타이틀, 설명, 투표 기간)
 * old: common_awards_header.xml - tvAwardTitle, tvAwardDetail, tvAwardPeriod
 */
@Composable
private fun AwardsCumulativeHeader(
    title: String,
    description: String,
    periodText: String
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (title.isNotEmpty()) {
            Text(
                text = title,
                style = ExoTypo.title16.copy(color = ColorPalette.main),
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
        }
        if (periodText.isNotEmpty()) {
            Text(
                text = periodText,
                style = ExoTypo.body12.copy(color = ColorPalette.textGray),
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
        }
        if (description.isNotEmpty()) {
            Text(
                text = description,
                style = ExoTypo.body12.copy(color = ColorPalette.textDefault),
                textAlign = TextAlign.Center
            )
        }
    }
}

/**
 * 집계 기간 바
 * old: tv_award_today
 */
@Composable
private fun AggregationPeriodBar(
    periodText: String,
    visible: Boolean
) {
    if (visible && periodText.isNotEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(ColorPalette.gray50)
                .padding(vertical = 10.dp, horizontal = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = periodText,
                style = ExoTypo.body12.copy(color = ColorPalette.textDimmed),
                textAlign = TextAlign.Center
            )
        }
    }
}

/**
 * 로딩 상태
 */
@Composable
private fun LoadingContent() {
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
private fun MessageContent(message: String) {
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
