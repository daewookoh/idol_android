package net.ib.mn.presentation.main.myfavorite

import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.flow.collectLatest
import net.ib.mn.R
import net.ib.mn.ui.components.ExoVoteIcon
import java.text.NumberFormat
import java.util.Locale
import net.ib.mn.domain.ranking.IdolIdsRankingDataSource
import net.ib.mn.domain.repository.RankingRepository
import net.ib.mn.presentation.main.ranking.idol_subpage.rememberMyFavoriteRankingState
import net.ib.mn.presentation.main.ranking.idol_subpage.myFavoriteRankingItems
import net.ib.mn.ui.components.ExoTop3
import net.ib.mn.ui.theme.ColorPalette
import net.ib.mn.ui.theme.ExoTypo
import net.ib.mn.util.NumberFormatUtil
import javax.inject.Inject

/**
 * My Favorite Page (UnifiedRankingSubPage 재사용 버전)
 *
 * 5개 차트별로 내 즐겨찾기 아이돌만 필터링하여 표시
 * UnifiedRankingSubPage를 재사용하여 순위 로직 공유
 */
@Composable
fun MyFavoritePage(
    onNavigateToIdolDetail: (Int) -> Unit = {},
    onNavigateToFavoriteSetting: () -> Unit = {},
    viewModel: MyFavoriteViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val chartSections by viewModel.chartSections.collectAsState()
    val context = LocalContext.current

    // 페이지가 visible될 때마다 데이터 갱신
    LaunchedEffect(Unit) {
        viewModel.sendIntent(MyFavoriteContract.Intent.OnPageVisible)
    }

    LaunchedEffect(Unit) {
        viewModel.effect.collectLatest { effect ->
            when (effect) {
                is MyFavoriteContract.Effect.NavigateToIdolDetail -> {
                    onNavigateToIdolDetail(effect.idolId)
                }
                is MyFavoriteContract.Effect.NavigateToFavoriteSetting -> {
                    onNavigateToFavoriteSetting()
                }
                is MyFavoriteContract.Effect.ShowError -> {
                    Toast.makeText(context, effect.message, Toast.LENGTH_SHORT).show()
                }
                is MyFavoriteContract.Effect.ShowToast -> {
                    Toast.makeText(context, effect.message, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    MyFavoriteContent(
        state = state,
        chartSections = chartSections,
        onIntent = viewModel::sendIntent
    )
}

/**
 * My Favorite Content (UnifiedRankingSubPage 재사용)
 */
@Composable
private fun MyFavoriteContent(
    state: MyFavoriteContract.State,
    chartSections: List<MyFavoriteViewModel.ChartSection>,
    onIntent: (MyFavoriteContract.Intent) -> Unit,
    viewModel: MyFavoriteViewModel = hiltViewModel()
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ColorPalette.background100)
    ) {
        when {
            state.isLoading && chartSections.isEmpty() -> {
                // 로딩 중
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        color = ColorPalette.main
                    )
                }
            }

            chartSections.isEmpty() -> {
                // 빈 화면 (empty_view)
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // 최애가 있으면 메시지 표시, 없으면 비밀의 방
                        if (state.mostFavoriteIdol != null) {
                            Text(
                                text = "즐겨찾기한 아이돌이 랭킹에 없습니다.",
                                style = ExoTypo.body15,
                                color = ColorPalette.gray200,
                                modifier = Modifier.padding(10.dp)
                            )
                        } else {
                            EmptyFavoriteHeader(
                                onSettingClick = {
                                    onIntent(MyFavoriteContract.Intent.OnSettingClick)
                                }
                            )
                        }
                    }
                }
            }

            else -> {
                // 각 섹션의 랭킹 데이터를 미리 가져오기
                val sectionRankingDataList = chartSections.map { section ->
                    section to rememberMyFavoriteRankingState(
                        chartCode = section.chartCode,
                        favoriteIds = section.favoriteIds,
                        isVisible = true,
                        rankingRepository = viewModel.rankingRepository
                    )
                }

                // LazyColumn으로 전체 스크롤 가능하게 (wrapContent 형식)
                LazyColumn(
                    modifier = Modifier.fillMaxSize()
                ) {
                    // 헤더: 최애 MostFavoriteIdol Top3 (있을 경우만)
                    state.mostFavoriteIdol?.let { mostIdol ->
                        item(key = "header_most_favorite") {
                            MostFavoriteIdolHeader(
                                mostFavoriteIdol = mostIdol,
                                onIdolClick = {
                                    onIntent(MyFavoriteContract.Intent.OnIdolClick(mostIdol.idolId))
                                }
                            )
                        }
                    }

                    // 각 차트 섹션별로 아이템들 추가
                    sectionRankingDataList.forEach { (section, rankingData) ->
                        // 섹션 헤더
                        item(key = "section_header_${section.chartCode}") {
                            SectionHeader(sectionName = section.sectionName)
                        }

                        // 랭킹 아이템들을 wrapContent 형식으로 추가
                        myFavoriteRankingItems(
                            chartCode = section.chartCode,
                            data = rankingData
                        )
                    }
                }
            }
        }
    }
}

/**
 * 섹션 헤더 (ChartCode별 그룹 표시)
 */
@Composable
private fun SectionHeader(sectionName: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(40.dp)
            .background(ColorPalette.gray100)
            .padding(horizontal = 20.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Text(
            text = sectionName,
            style = ExoTypo.body14.copy(fontWeight = FontWeight.Bold),
            color = ColorPalette.textDefault
        )
    }
}

/**
 * 최애 아이돌 헤더 (MostFavoriteIdol)
 */
@Composable
private fun MostFavoriteIdolHeader(
    mostFavoriteIdol: MyFavoriteContract.MostFavoriteIdol,
    onIdolClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(ColorPalette.background100)
    ) {
        // ExoTop3 - 상단 배너 (이미지/동영상)
        ExoTop3(
            id = "most_favorite_${mostFavoriteIdol.idolId}",
            imageUrls = mostFavoriteIdol.top3ImageUrls,
            videoUrls = mostFavoriteIdol.top3VideoUrls,
            isVisible = true,
            onItemClick = { onIdolClick() }
        )

        // Info Bar - 순위, 이름, 하트 수, 투표 버튼
        MostFavoriteInfoBar(
            mostFavoriteIdol = mostFavoriteIdol,
            onVoteClick = { /* TODO: 투표 처리 */ }
        )
    }
}

/**
 * 최애 정보 바 (순위, 이름, 하트 수, 차트 코드)
 *
 * MostFavoriteIdol 데이터 기반 표시
 */
@Composable
private fun MostFavoriteInfoBar(
    mostFavoriteIdol: MyFavoriteContract.MostFavoriteIdol,
    onVoteClick: () -> Unit
) {
    Log.d("MostFavoriteInfoBar", mostFavoriteIdol.toString())
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(50.dp)
            .background(ColorPalette.main)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight()
                .padding(start = 20.dp, end = 14.dp)
        ) {
            // 내용
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight()
                    .padding(vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 순위
                mostFavoriteIdol.rank?.let { rank ->
                    Text(
                        text = rank.toString(),
                        fontSize = 20.sp,
                        lineHeight = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = ColorPalette.textLight,
                        modifier = Modifier.align(Alignment.Bottom)
                    )
                }

                // 이름과 그룹명 파싱
                val nameParts = mostFavoriteIdol.name.split("_")
                val idolName = nameParts.getOrNull(0) ?: mostFavoriteIdol.name
                val groupName = nameParts.getOrNull(1)

                // 이름
                Text(
                    text = idolName,
                    fontSize = 18.sp,
                    lineHeight = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = ColorPalette.textLight,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .padding(start = 8.dp)
                        .align(Alignment.Bottom)
                )

                // 그룹명 (있을 경우)
                groupName?.let { group ->
                    Text(
                        text = group,
                        fontSize = 10.sp,
                        lineHeight = 18.sp,
                        color = ColorPalette.textLight,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .padding(start = 8.dp)
                            .align(Alignment.Bottom)
                    )
                }

                // 하트 수
                mostFavoriteIdol.heart?.let { heart ->
                    Text(
                        text = "${NumberFormat.getInstance(Locale.getDefault()).format(heart)}",
                        fontSize = 10.sp,
                        lineHeight = 18.sp,
                        color = ColorPalette.textLight,
                        modifier = Modifier
                            .padding(start = 8.dp)
                            .align(Alignment.Bottom)
                    )
                }
            }

            ExoVoteIcon(
                idolId = mostFavoriteIdol.idolId,
                fullName = mostFavoriteIdol.name,
                type = "CIRCLE",
                modifier = Modifier.align(Alignment.CenterEnd)
            )
        }
    }
}

/**
 * 최애가 없을 때 표시되는 헤더 (비밀의 방)
 */
@Composable
private fun EmptyFavoriteHeader(
    onSettingClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(ColorPalette.background100)
            .padding(top = 14.dp, bottom = 22.dp, start = 20.dp, end = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // 아이콘
        Image(
            painter = painterResource(R.drawable.img_favorite_idol),
            contentDescription = null,
            modifier = Modifier.size(76.dp)
        )

        Spacer(modifier = Modifier.height(10.dp))

        // 설명 텍스트 1
        Text(
            text = stringResource(R.string.desc_empty_favorite1),
            style = ExoTypo.body14.copy(fontWeight = FontWeight.Bold),
            color = ColorPalette.textDefault,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(7.5.dp))

        // 설명 텍스트 2
        Text(
            text = stringResource(R.string.desc_empty_favorite2),
            style = ExoTypo.body12,
            color = ColorPalette.gray200,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(15.dp))

        // 최애 설정 버튼
        Text(
            text = stringResource(R.string.desc_empty_favorite3),
            style = ExoTypo.body12.copy(fontWeight = FontWeight.Bold),
            color = ColorPalette.textDefault,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .clip(RoundedCornerShape(50.dp))
                .background(ColorPalette.gray100)
                .clickable(onClick = onSettingClick)
                .padding(horizontal = 30.dp, vertical = 10.dp)
        )
    }
}
