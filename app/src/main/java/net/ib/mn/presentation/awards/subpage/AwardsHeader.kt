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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import net.ib.mn.R
import net.ib.mn.data.remote.dto.AwardChartsModel
import net.ib.mn.data.remote.dto.AwardModel
import net.ib.mn.ui.theme.ColorPalette
import net.ib.mn.ui.theme.ExoTypo
import net.ib.mn.util.IdolImageUtil.toSecureUrl

/**
 * AwardsHeader - 어워즈 공용 헤더 컴포넌트
 *
 * 누적, 실시간, 라인업에서 공통으로 사용하는 배너 이미지 + 카테고리 탭
 */
@Composable
fun AwardsHeader(
    awardModel: AwardModel?,
    charts: List<AwardChartsModel>?,
    selectedChartIndex: Int,
    onChartSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val isDarkTheme = isSystemInDarkTheme()

    Column(modifier = modifier.fillMaxWidth()) {
        // 배너 이미지
        if (awardModel != null) {
            AwardsBannerImage(
                awardModel = awardModel,
                isDarkTheme = isDarkTheme
            )
        }

        // 카테고리 탭
        if (!charts.isNullOrEmpty()) {
            AwardsCategoryTabs(
                charts = charts,
                selectedIndex = selectedChartIndex,
                onChartSelected = onChartSelected
            )
        }
    }
}

/**
 * 배너 이미지
 */
@Composable
private fun AwardsBannerImage(
    awardModel: AwardModel,
    isDarkTheme: Boolean
) {
    val bannerUrl = if (isDarkTheme) {
        awardModel.bannerDarkImgUrl
    } else {
        awardModel.bannerLightImgUrl
    }

    if (!bannerUrl.isNullOrEmpty()) {
        Box(modifier = Modifier.fillMaxWidth()) {
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
 * 카테고리 탭
 */
@Composable
private fun AwardsCategoryTabs(
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
 * 헤더 정보 영역 (타이틀, 기간, 설명)
 *
 * @param title 타이틀 (메인 색상)
 * @param period 기간 텍스트
 * @param description 설명 텍스트
 */
@Composable
fun AwardsHeaderInfo(
    title: String,
    period: String,
    description: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 18.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (title.isNotEmpty()) {
            Text(
                text = title,
                style = ExoTypo.title16.copy(color = ColorPalette.main),
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(10.dp))
        }
        if (period.isNotEmpty()) {
            Text(
                text = period,
                style = ExoTypo.body12.copy(color = ColorPalette.textDefault),
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(10.dp))
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
 * 라인업용 헤더 정보 영역 (투표 전)
 *
 * @param exampleTitle 라인업 타이틀
 * @param exampleDesc 라인업 설명
 */
@Composable
fun AwardsLineupHeaderInfo(
    exampleTitle: String,
    exampleDesc: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 18.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (exampleTitle.isNotEmpty()) {
            Text(
                text = exampleTitle,
                style = ExoTypo.title16.copy(color = ColorPalette.main),
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(10.dp))
        }
        // "지금은 투표 기간이 아닙니다!"
        Text(
            text = stringResource(R.string.award_before_title),
            style = ExoTypo.body12.copy(color = ColorPalette.textDefault),
            textAlign = TextAlign.Center
        )
        if (exampleDesc.isNotEmpty()) {
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = exampleDesc,
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
fun AwardsAggregationBar(
    periodText: String,
    modifier: Modifier = Modifier
) {
    if (periodText.isNotEmpty()) {
        Box(
            modifier = modifier
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
