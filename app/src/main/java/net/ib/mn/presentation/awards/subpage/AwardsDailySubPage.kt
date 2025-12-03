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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
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
import net.ib.mn.data.remote.dto.AwardIdolItem
import net.ib.mn.data.remote.dto.AwardModel
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
    val isDarkTheme = isSystemInDarkTheme()

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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ColorPalette.background100)
    ) {
        // ========== 상단 고정 영역 ==========

        // 1. 배너 이미지 (votable == "Y"일 때만)
        if (viewModel.votable == "Y" && awardModel != null) {
            DailyBannerImage(
                awardModel = awardModel!!,
                isDarkTheme = isDarkTheme
            )
        }

        // 2. 카테고리 탭
        val charts = when (val state = uiState) {
            is AwardsDailyUiState.Success -> state.charts
            is AwardsDailyUiState.Empty -> state.charts
            else -> awardModel?.charts
        }

        if (viewModel.votable == "Y" && !charts.isNullOrEmpty()) {
            DailyCategoryTabs(
                charts = charts,
                selectedIndex = selectedChartIndex,
                onChartSelected = { viewModel.selectChart(it) }
            )
        }

        // ========== 스크롤 영역 ==========
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
                        // 3. 타이틀 + 설명 + 투표 기간 (votable == "Y"일 때만)
                        if (viewModel.votable == "Y") {
                            item {
                                AwardsDailyHeader(
                                    title = awardModel?.realtimeTitle.orEmpty(),
                                    description = awardModel?.realtimeDesc.orEmpty(),
                                    periodText = viewModel.getVotingPeriodText()
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                            }
                        }

                        // 4. 집계 기간 바
                        item {
                            DailyAggregationPeriodBar(
                                periodText = viewModel.getAggregationPeriodText(),
                                visible = true
                            )
                        }

                        // 5. 랭킹 리스트
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
 * 배너 이미지
 */
@Composable
private fun DailyBannerImage(
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
                    .aspectRatio(3f),
                contentScale = ContentScale.FillBounds
            )
        }
    }
}

/**
 * 카테고리 탭 (charts)
 */
@Composable
private fun DailyCategoryTabs(
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
                            Modifier.background(ColorPalette.textChat)
                        } else {
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
 */
@Composable
private fun AwardsDailyHeader(
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
 */
@Composable
private fun DailyAggregationPeriodBar(
    periodText: String,
    visible: Boolean
) {
    if (visible && periodText.isNotEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(ColorPalette.gray50)
                .padding(10.dp),
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
private fun DailyLoadingContent() {
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
