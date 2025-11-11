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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
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
import net.ib.mn.ui.components.ExoTop3
import net.ib.mn.ui.components.ExoVoteIcon
import net.ib.mn.ui.components.RankingItemData
import net.ib.mn.ui.components.exoRankingItems
import net.ib.mn.ui.theme.ColorPalette
import net.ib.mn.ui.theme.ExoTypo
import java.text.NumberFormat
import java.util.Locale

/**
 * My Favorite Page
 *
 * OLD 프로젝트의 FavoriteIdolBaseFragment UI를 Compose로 재현
 */
@Composable
fun MyFavoritePage(
    onNavigateToIdolDetail: (Int) -> Unit = {},
    onNavigateToFavoriteSetting: () -> Unit = {},
    viewModel: MyFavoriteViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // 페이지가 visible될 때마다 데이터 갱신
    LaunchedEffect(Unit) {
        viewModel.sendIntent(MyFavoriteContract.Intent.OnPageVisible)
    }

    // Lifecycle 이벤트 감지하여 UDP 구독 관리
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> {
                    android.util.Log.d("MyFavoritePage", "👁️ Screen visible (ON_RESUME)")
                    viewModel.sendIntent(MyFavoriteContract.Intent.OnScreenVisible)
                }
                Lifecycle.Event.ON_PAUSE -> {
                    android.util.Log.d("MyFavoritePage", "🙈 Screen hidden (ON_PAUSE)")
                    viewModel.sendIntent(MyFavoriteContract.Intent.OnScreenHidden)
                }
                else -> {}
            }
        }

        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
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
        onIntent = viewModel::sendIntent
    )
}

/**
 * My Favorite Content (Stateless)
 *
 * OLD fragment_favorit.xml 레이아웃을 Compose로 변환
 * ExoRankingItem의 DAILY 타입 사용
 */
@Composable
private fun MyFavoriteContent(
    state: MyFavoriteContract.State,
    onIntent: (MyFavoriteContract.Intent) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ColorPalette.background100)
    ) {
            when {
                state.isLoading && state.favoriteIdols.isEmpty() -> {
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

                state.favoriteIdols.isEmpty() -> {
                    // 빈 화면 (empty_view)
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = stringResource(R.string.lable_get_info),
                            style = ExoTypo.body15,
                            color = ColorPalette.gray200,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }

                else -> {
                    // ExoRankingItem의 DAILY 타입 사용
                    LazyColumn(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        // 헤더: 최애가 있으면 Top3 이미지, 없으면 비밀의 방
                        item(key = "header") {
                            if (state.topFavorite != null) {
                                Column(modifier = Modifier.fillMaxWidth()) {
                                    // Top3 이미지 표시
                                    ExoTop3(
                                        id = "favorite_top3_${state.topFavorite.idolId}",
                                        imageUrls = state.topFavorite.top3ImageUrls,
                                        videoUrls = state.topFavorite.top3VideoUrls,
                                        isVisible = true,
                                        onItemClick = { index ->
                                            onIntent(MyFavoriteContract.Intent.OnIdolClick(state.topFavorite.idolId))
                                        }
                                    )

                                    // Info Bar (리그, 순위, 이름, 하트 수)
                                    FavoriteInfoBar(
                                        topFavorite = state.topFavorite,
                                        onVoteClick = {
                                            // TODO: 하트 투표 처리
                                            Log.d("MyFavorite", "Vote clicked for idol: ${state.topFavorite.idolId}")
                                        }
                                    )
                                }
                            } else {
                                // 최애가 없을 경우 - 비밀의 방
                                EmptyFavoriteHeader(
                                    onSettingClick = {
                                        onIntent(MyFavoriteContract.Intent.OnSettingClick)
                                    }
                                )
                            }
                        }

                        // 최애 목록을 섹션별로 그루핑하여 표시
                        val groupedIdols = state.favoriteIdols.groupBy { it.isSection }
                        var currentSectionIndex = 0

                        while (currentSectionIndex < state.favoriteIdols.size) {
                            val currentItem = state.favoriteIdols[currentSectionIndex]

                            if (currentItem.isSection) {
                                // 섹션 헤더 표시
                                item(key = "section_${currentItem.chartCode}_$currentSectionIndex") {
                                    SectionHeader(sectionName = currentItem.sectionName ?: "")
                                }
                                currentSectionIndex++

                                // 섹션에 속한 아이돌들 찾기
                                val sectionIdols = mutableListOf<MyFavoriteContract.FavoriteIdol>()
                                while (currentSectionIndex < state.favoriteIdols.size &&
                                       !state.favoriteIdols[currentSectionIndex].isSection) {
                                    sectionIdols.add(state.favoriteIdols[currentSectionIndex])
                                    currentSectionIndex++
                                }

                                // exoRankingItems 사용하여 해당 섹션의 아이돌들 표시
                                exoRankingItems(
                                    items = sectionIdols.map { idol ->
                                        RankingItemData(
                                            id = idol.idolId.toString(),
                                            rank = idol.rank ?: 0,
                                            name = idol.name,
                                            voteCount = NumberFormat.getInstance().format(idol.score) ?: "0",
                                            photoUrl = idol.imageUrl,
                                            heartCount = idol.score ?: 0L,
                                            maxHeartCount = idol.sectionMaxScore ?: 0L,
                                            isFavorite = false
                                        )
                                    },
                                    onItemClick = { _, item ->
                                        onIntent(MyFavoriteContract.Intent.OnIdolClick(item.id.toIntOrNull() ?: 0))
                                    }
                                )
                            } else {
                                currentSectionIndex++
                            }
                        }
                    }
                }
            }
        }
    }

/**
 * 섹션 헤더 (ChartCode별 그룹 표시)
 *
 * OLD 프로젝트의 section_rank_header.xml 레이아웃 참고
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
 * 최애 정보 바 (리그, 순위, 이름, 하트 수)
 *
 * OLD 프로젝트의 favorite_header.xml rl_rank_name_score 영역 참고
 */
@Composable
private fun FavoriteInfoBar(
    topFavorite: MyFavoriteContract.TopFavorite,
    onVoteClick: () -> Unit
) {
    Log.d("FavoriteInfoBar", topFavorite.toString())
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
                    .fillMaxHeight().padding(vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {

                // 순위
                topFavorite.rank?.let { rank ->
                    Text(
                        text = rank.toString(),
                        fontSize = 20.sp,
                        lineHeight = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = ColorPalette.textLight,
                        modifier = Modifier
                            .align(Alignment.Bottom)
                    )
                }

                // 이름과 그룹명 파싱
                val nameParts = topFavorite.name.split("_")
                val idolName = nameParts.getOrNull(0) ?: topFavorite.name
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
                topFavorite.heart?.let { heart ->
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
                idolId = topFavorite.idolId,
                fullName = topFavorite.name,
                type = "CIRCLE",
                modifier = Modifier.align(Alignment.CenterEnd)
            )
        }
    }
}

/**
 * 최애가 없을 때 표시되는 헤더 (비밀의 방)
 *
 * OLD 프로젝트의 empty_favorite_header 레이아웃 참고
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
