package net.ib.mn.presentation.main.myfavorite

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.flow.collectLatest
import net.ib.mn.R
import net.ib.mn.domain.model.MostPicksModel
import net.ib.mn.presentation.main.ranking.idol_subpage.myFavoriteRankingItems
import net.ib.mn.presentation.main.ranking.idol_subpage.rememberMyFavoriteRankingState
import net.ib.mn.ui.components.ExoHeartCounter
import net.ib.mn.ui.components.ExoTop3
import net.ib.mn.ui.components.ExoVoteIcon
import net.ib.mn.ui.components.LocalRankingItemClick
import net.ib.mn.ui.theme.ColorPalette
import net.ib.mn.ui.theme.ExoTypo

/**
 * My Favorite Page (UnifiedRankingSubPage 재사용 버전)
 *
 * 5개 차트별로 내 즐겨찾기 아이돌만 필터링하여 표시
 * UnifiedRankingSubPage를 재사용하여 순위 로직 공유
 */
@Composable
fun MyFavoritePage(
    onNavigateToFavoriteSetting: () -> Unit = {},
    viewModel: MyFavoriteViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val chartSections by viewModel.chartSections.collectAsState()
    val mostFavoriteIdol by viewModel.mostFavoriteIdol.collectAsState()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // Lifecycle 이벤트 관찰 - ON_RESUME 시에만 데이터 갱신
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.sendIntent(MyFavoriteContract.Intent.OnPageVisible)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // 페이지 최초 진입 및 Effect 수집
    LaunchedEffect(Unit) {
        viewModel.sendIntent(MyFavoriteContract.Intent.OnPageVisible)
        viewModel.effect.collectLatest { effect ->
            when (effect) {
                is MyFavoriteContract.Effect.NavigateToFavoriteSetting -> onNavigateToFavoriteSetting()
                is MyFavoriteContract.Effect.ShowError -> Toast.makeText(context, effect.message, Toast.LENGTH_SHORT).show()
                is MyFavoriteContract.Effect.ShowToast -> Toast.makeText(context, effect.message, Toast.LENGTH_SHORT).show()
                is MyFavoriteContract.Effect.NavigateToWebPage -> { /* TODO: WebView 또는 외부 브라우저로 열기 */ }
            }
        }
    }

    MyFavoriteContent(
        state = state,
        chartSections = chartSections,
        mostFavoriteIdol = mostFavoriteIdol,
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
    mostFavoriteIdol: MyFavoriteContract.MostFavoriteIdol?,
    onIntent: (MyFavoriteContract.Intent) -> Unit,
    viewModel: MyFavoriteViewModel = hiltViewModel()
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ColorPalette.background100)
    ) {
        when {
            // 초기 로딩 중: mostFavoriteIdol 또는 chartSections가 아직 로드되지 않은 경우
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

                // LocalRankingItemClick은 ExoRankingItem 및 ExoTop3에서 직접 사용됨
                val onRankingItemClick = LocalRankingItemClick.current

                // Top3 펼침 상태 관리
                var expandedItemIds by remember { mutableStateOf(emptySet<String>()) }

                // LazyColumn으로 전체 스크롤 가능하게 (wrapContent 형식)
                LazyColumn(
                    modifier = Modifier.fillMaxSize()
                ) {
                    // 헤더: 최애 MostFavoriteIdol Top3 (무조건 표시)
                    item(key = "header_most_favorite") {
                        if (mostFavoriteIdol != null) {
                            MostFavoriteIdolHeader(
                                mostFavoriteIdol = mostFavoriteIdol,
                                mostPicksModel = state.mostPicksModel,
                                onIdolClick = {
                                    // MostFavoriteIdol을 RankingItem으로 변환하여 CommunityScreen으로 이동
                                    val rankingItem = mostFavoriteIdol.toRankingItem()
                                    onRankingItemClick(rankingItem)
                                },
                                onVoteSuccess = { idolId, votedHeart ->
                                    onIntent(MyFavoriteContract.Intent.OnVoteSuccess(idolId, votedHeart))
                                },
                                onSupportBiasBarClick = { id, kind ->
                                    onIntent(MyFavoriteContract.Intent.OnSupportBiasBarClick(id, kind))
                                }
                            )
                        } else {
                            // mostFavoriteIdol 로딩 중
                            MostFavoriteIdolHeaderLoading()
                        }
                    }

                    // 각 차트 섹션별로 아이템들 추가
                    sectionRankingDataList.forEach { (section, rankingData) ->
                        // 섹션 헤더
                        item(key = "section_header_${section.chartCode}") {
                            SectionHeader(sectionName = section.sectionName)
                        }

                        // 랭킹 아이템들을 wrapContent 형식으로 추가
                        // LocalRankingItemClick은 ExoRankingItem 내부에서 직접 처리됨
                        myFavoriteRankingItems(
                            chartCode = section.chartCode,
                            data = rankingData,
                            expandedItemIds = expandedItemIds,
                            onExpandedChange = { itemKey, isExpanded ->
                                expandedItemIds = if (isExpanded) {
                                    expandedItemIds + setOf(itemKey)
                                } else {
                                    expandedItemIds - setOf(itemKey)
                                }
                            }
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
 *
 * UserCacheRepository와 RankingCacheRepository를 기반으로
 * 실시간으로 업데이트되는 최애 아이돌 정보 표시
 */
@Composable
private fun MostFavoriteIdolHeader(
    mostFavoriteIdol: MyFavoriteContract.MostFavoriteIdol,
    mostPicksModel: MostPicksModel?,
    onIdolClick: () -> Unit,
    onVoteSuccess: (idolId: Int, votedHeart: Long) -> Unit = { _, _ -> },
    onSupportBiasBarClick: (id: Int, kind: String) -> Unit = { _, _ -> }
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
            onVoteSuccess = onVoteSuccess
        )

        // Support Bias Bar - 픽 참여 배너
        if (mostPicksModel != null) {
            SupportBiasBar(
                idolId = mostFavoriteIdol.idolId,
                mostPicksModel = mostPicksModel,
                onClick = onSupportBiasBarClick
            )
        }
    }
}

/**
 * 최애 정보 바 (순위, 이름, 하트 수, 차트 코드)
 *
 * RankingCacheRepository의 실시간 데이터를 기반으로
 * 최애 아이돌의 현재 순위와 하트 수를 표시
 */
@Composable
private fun MostFavoriteInfoBar(
    mostFavoriteIdol: MyFavoriteContract.MostFavoriteIdol,
    onVoteSuccess: (idolId: Int, votedHeart: Long) -> Unit
) {
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
                        text = if(rank == 0) "-" else rank.toString(),
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
                    ExoHeartCounter(
                        count = heart,
                        style = ExoTypo.stat10.copy(lineHeight = 18.sp),
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
                onVoteSuccess = { votedHeart ->
                    onVoteSuccess(mostFavoriteIdol.idolId, votedHeart)
                },
                modifier = Modifier.align(Alignment.CenterEnd)
            )
        }
    }
}

/**
 * 최애 아이돌 헤더 로딩 상태
 */
@Composable
private fun MostFavoriteIdolHeaderLoading() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(ColorPalette.background100)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .background(ColorPalette.gray100),
            contentAlignment = Alignment.Center
        ) { }
    }
}

/**
 * Support Bias Bar
 * 최애가 픽에 참여하고 있을 때 표시되는 배너
 */
@Composable
private fun SupportBiasBar(
    idolId: Int,
    mostPicksModel: MostPicksModel,
    onClick: (id: Int, kind: String) -> Unit
) {
    // 픽 참여 정보 결정 (old 로직과 동일)
    val (bannerTitle, id, kind) = getBannerInfo(mostPicksModel)

    if (kind.isEmpty()) return

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .clickable { onClick(id, kind) }
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Heart Icon
            Image(
                painter = painterResource(R.drawable.img_popup_heart),
                contentDescription = null,
                modifier = Modifier.size(21.dp, 17.dp)
            )

            // Banner Text (Bold 처리된 bannerTitle)
            val message = stringResource(R.string.most_in_picks_banner_msg, bannerTitle)
            val start = message.indexOf(bannerTitle)
            val end = start + bannerTitle.length

            val annotatedString = buildAnnotatedString {
                append(message)
                if (start >= 0) {
                    addStyle(SpanStyle(fontWeight = FontWeight.Bold), start, end)
                }
            }

            Text(
                text = annotatedString,
                style = ExoTypo.body15,
                color = ColorPalette.textDefault,
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 9.dp)
            )

            // Arrow Icon
            Image(
                painter = painterResource(R.drawable.arrow_left_to_right),
                contentDescription = null,
                modifier = Modifier.size(8.dp, 12.dp)
            )
        }
    }
}

/**
 * 배너 정보 결정 (old 로직과 동일)
 */
private fun getBannerInfo(model: MostPicksModel): Triple<String, Int, String> {
    val themepick = "themepick"
    val heartpick = "heartpick"
    val miracle = "miracle"
    val onepick = "onepick"

    val theme: List<String> = listOf(themepick, heartpick, miracle)

    return when {
        model.heartpick?.isNotEmpty() == true && model.themepick?.isNotEmpty() == true && model.miracle == true -> {
            val randomItem = theme.random()
            when (randomItem) {
                themepick -> Triple("테마픽", model.themepick.random(), themepick)
                heartpick -> Triple("하트픽", model.heartpick.random(), heartpick)
                miracle -> Triple("기적의 달", 0, miracle)
                else -> Triple("", 0, "")
            }
        }
        model.heartpick?.isNotEmpty() == true && model.themepick?.isNotEmpty() == true -> {
            if (kotlin.random.Random.nextBoolean()) {
                Triple("하트픽", model.heartpick.random(), heartpick)
            } else {
                Triple("테마픽", model.themepick.random(), themepick)
            }
        }
        model.heartpick?.isNotEmpty() == true && model.miracle == true -> {
            if (kotlin.random.Random.nextBoolean()) {
                Triple("하트픽", model.heartpick.random(), heartpick)
            } else {
                Triple("기적의 달", 0, miracle)
            }
        }
        model.themepick?.isNotEmpty() == true && model.miracle == true -> {
            if (kotlin.random.Random.nextBoolean()) {
                Triple("테마픽", model.themepick.random(), themepick)
            } else {
                Triple("기적의 달", 0, miracle)
            }
        }
        model.heartpick?.isNotEmpty() == true -> Triple("하트픽", model.heartpick.random(), heartpick)
        model.themepick?.isNotEmpty() == true -> Triple("테마픽", model.themepick.random(), themepick)
        model.onepick?.isNotEmpty() == true -> Triple("이미지픽", model.onepick.random(), onepick)
        model.miracle == true -> Triple("기적의 달", 0, miracle)
        else -> Triple("", 0, "")
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
