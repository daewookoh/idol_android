package net.ib.mn.presentation.main.ranking.idol_subpage

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import net.ib.mn.ui.theme.ColorPalette
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import net.ib.mn.R
import net.ib.mn.domain.ranking.RankingDataSource
import net.ib.mn.ui.components.ExoRankingList

/**
 * 통합 Miracle/Rookie 랭킹 SubPage
 *
 * Old 프로젝트의 MiracleMainFragment 구조 재현:
 * 1. 상단 배너 (이미지 + 공유 버튼)
 * 2. 두 개의 탭 (누적 랭킹 / 실시간 랭킹)
 * 3. 각 탭별 독립적인 랭킹 리스트
 *
 * @param chartCode 차트 코드
 * @param accumulatedChartCode 누적 랭킹 차트 코드 (null이면 누적 탭 숨김)
 * @param bannerImageUrl 배너 이미지 URL
 * @param accumulatedBannerImageUrl 누적 랭킹 배너 이미지 URL
 * @param dataSource 랭킹 데이터 소스 (Miracle/Rookie)
 * @param isVisible 화면 가시성
 * @param listState 리스트 스크롤 상태
 * @param onInfoClick 정보 버튼 클릭 콜백 (eventId를 전달)
 * @param onShare 공유 버튼 클릭 콜백
 * @param modifier Modifier
 */
@Composable
fun MiracleRookieRankingSubPage(
    chartCode: String,
    accumulatedChartCode: String? = null,
    bannerImageUrl: String? = null,
    accumulatedBannerImageUrl: String? = null,
    dataSource: RankingDataSource,
    isVisible: Boolean = true,
    listState: LazyListState? = null,
    onInfoClick: (Int) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    android.util.Log.d("MiracleRookieSubPage", "🎨 [Composing] ${dataSource.type} for chartCode: $chartCode")

    // ViewModel key 생성 (각 chartCode별로 독립적인 ViewModel 인스턴스 생성)
    val viewModelKey = "miracle_rookie_${dataSource.type}_$chartCode"
    android.util.Log.d("MiracleRookieSubPage", "🔑 ViewModel key: $viewModelKey")

    // ViewModel 생성
    val viewModel: MiracleRookieRankingSubPageViewModel = hiltViewModel<MiracleRookieRankingSubPageViewModel, MiracleRookieRankingSubPageViewModel.Factory>(
        key = viewModelKey
    ) { factory ->
        android.util.Log.d("MiracleRookieSubPage", "🏭 Factory creating ViewModel for type=${dataSource.type}, chartCode=$chartCode")
        factory.create(chartCode, dataSource)
    }

    android.util.Log.d("MiracleRookieSubPage", "✅ ViewModel instance: ${viewModel.hashCode()}, type=${dataSource.type}")

    val uiState by viewModel.uiState.collectAsState()
    val scrollState = listState ?: rememberLazyListState()

    // ViewModel 상태에서 배너 URL, 누적 차트 코드, 정보 이벤트 ID 가져오기
    val vmAccumulatedChartCode = (uiState as? MiracleRookieRankingSubPageViewModel.UiState.Success)?.accumulatedChartCode
    val vmBannerUrl = (uiState as? MiracleRookieRankingSubPageViewModel.UiState.Success)?.bannerUrl
    val vmAccumulatedBannerUrl = (uiState as? MiracleRookieRankingSubPageViewModel.UiState.Success)?.accumulatedBannerUrl
    val infoEventId = (uiState as? MiracleRookieRankingSubPageViewModel.UiState.Success)?.infoEventId ?: 0

    // 누적 차트 코드 결정 (ViewModel 상태 우선)
    val effectiveAccumulatedChartCode = vmAccumulatedChartCode ?: accumulatedChartCode

    // 탭 상태: 0 = 누적 랭킹, 1 = 실시간 랭킹
    // 초기 탭은 항상 실시간 랭킹(1)
    var selectedTabIndex by remember { mutableIntStateOf(1) }

    // 현재 탭에 따른 배너 이미지 결정 (ViewModel 상태 우선)
    // remember를 사용하여 selectedTabIndex 변경 시 재계산되도록 함
    val currentBannerUrl = remember(selectedTabIndex, vmBannerUrl, vmAccumulatedBannerUrl, accumulatedBannerImageUrl, bannerImageUrl) {
        if (selectedTabIndex == 0 && (vmAccumulatedBannerUrl != null || accumulatedBannerImageUrl != null)) {
            vmAccumulatedBannerUrl ?: accumulatedBannerImageUrl
        } else {
            vmBannerUrl ?: bannerImageUrl
        }
    }

    // 화면 가시성 변경 시 UDP 구독 관리 및 데이터 새로고침
    LaunchedEffect(isVisible) {
        if (isVisible) {
            android.util.Log.d("MiracleRookieSubPage", "[SubPage] 👁️ Screen became visible")
            viewModel.onScreenVisible()
        } else {
            android.util.Log.d("MiracleRookieSubPage", "[SubPage] 🙈 Screen hidden")
            viewModel.onScreenHidden()
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(ColorPalette.background100)
    ) {
        // 1. 상단 배너 + 정보 버튼 + 공유 버튼
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(3f) // 3:1 비율
        ) {
            // 배너 이미지
            AsyncImage(
                model = currentBannerUrl,
                contentDescription = "Banner",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )

            // 정보 버튼 (우측 상단)
            if (infoEventId > 0) {
                Icon(
                    painter = painterResource(R.drawable.btn_info_black),
                    contentDescription = "Info",
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .size(36.dp)
                        .padding(9.dp)
                        .clickable { onInfoClick(infoEventId) },
                    tint = Color.Unspecified
                )
            }

            // 공유 버튼 (우측 하단)
            Icon(
                painter = painterResource(R.drawable.btn_share_black),
                contentDescription = "Share",
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .size(36.dp)
                    .padding(8.dp)
                    .clickable {
                        // 현재 상태에서 랭킹 데이터 가져오기
                        val currentState = uiState
                        val rankingItems = if (currentState is MiracleRookieRankingSubPageViewModel.UiState.Success) {
                            currentState.items
                        } else {
                            emptyList()
                        }

                        // 공유 URL 생성 (Miracle 또는 Rookie)
                        val shareType = when (dataSource.type) {
                            "Miracle" -> "miracle"
                            "Rookie" -> "rookie"
                            else -> dataSource.type.lowercase()
                        }
                        val shareUrl = "${net.ib.mn.util.ServerUrl.HOST}/$shareType/"

                        // 공유 메시지 생성
                        val shareMessage = when (dataSource.type) {
                            "Miracle" -> {
                                // Miracle: 상위 3명의 랭킹 데이터로 포맷
                                if (rankingItems.size >= 3) {
                                    val top3 = rankingItems.take(3)
                                    java.lang.String.format(
                                        java.util.Locale.getDefault(),
                                        context.getString(R.string.miracle_n_share_msg),
                                        "", // %1$s: targetMonth (빈 문자열)
                                        top3[0].name, top3[0].rank.toString(), // %2$s, %3$s
                                        top3[1].name, top3[1].rank.toString(), // %4$s, %5$s
                                        top3[2].name, top3[2].rank.toString()  // %6$s, %7$s
                                    )
                                } else {
                                    // 데이터 부족 시 간단한 메시지
                                    "[Miracle of the Month🎂]\n\nBirthday of the month voting in progress!\nVote for your bias and give your bias a birthday ad💖\n\nSupport #Kpop idol on #CHOEAEDOL"
                                }
                            }
                            "Rookie" -> {
                                // Rookie: 1위 데이터로 포맷
                                if (rankingItems.isNotEmpty()) {
                                    val first = rankingItems.first()
                                    java.lang.String.format(
                                        java.util.Locale.getDefault(),
                                        context.getString(R.string.rookie_share_msg),
                                        "", // %1$s: 사용 안 함
                                        first.name // %2$s: 1위 이름
                                    )
                                } else {
                                    // 데이터 부족 시 간단한 메시지
                                    "Support #Kpop #Rookie_Idol on #CHOEAEDOL!\n\n[CHOEAEDOL Rookie👼🏻]\n\nGive a special gift to my idol on CHOEAEDOL!💖"
                                }
                            }
                            else -> ""
                        }

                        // 메시지와 URL 결합
                        val textToShare = if (shareMessage.isNotEmpty()) {
                            "$shareMessage\n$shareUrl"
                        } else {
                            shareUrl
                        }

                        // Android 공유 시트 열기
                        val shareIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(android.content.Intent.EXTRA_TEXT, textToShare)
                        }
                        context.startActivity(
                            android.content.Intent.createChooser(
                                shareIntent,
                                context.getString(R.string.title_share)
                            )
                        )
                    },
                tint = Color.Unspecified
            )
        }

        // 2. 탭 (누적 랭킹 / 실시간 랭킹)
        TabRow(
            selectedTabIndex = selectedTabIndex,
            containerColor = ColorPalette.background100,
            contentColor = ColorPalette.main,
            indicator = { tabPositions ->
                TabRowDefaults.SecondaryIndicator(
                    Modifier.tabIndicatorOffset(tabPositions[selectedTabIndex]),
                    color = ColorPalette.main
                )
            }
        ) {
                Tab(
                    selected = selectedTabIndex == 0,
                    onClick = { selectedTabIndex = 0 },
                    text = {
                        Text(
                            text = stringResource(R.string.cumulative_rankings),
                            fontSize = 15.sp,
                            color = if (selectedTabIndex == 0) {
                                ColorPalette.textDefault
                            } else {
                                ColorPalette.textDimmed
                            }
                        )
                    },
                    modifier = Modifier.height(42.dp)
                )
                Tab(
                    selected = selectedTabIndex == 1,
                    onClick = { selectedTabIndex = 1 },
                    text = {
                        Text(
                            text = stringResource(R.string.award_realtime),
                            fontSize = 15.sp,
                            color = if (selectedTabIndex == 1) {
                                ColorPalette.textDefault
                            } else {
                                ColorPalette.textDimmed
                            }
                        )
                    },
                    modifier = Modifier.height(42.dp)
                )
        }

        // 3. 랭킹 리스트
        when (val currentState = uiState) {
            is MiracleRookieRankingSubPageViewModel.UiState.Loading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = ColorPalette.main)
                }
            }

            is MiracleRookieRankingSubPageViewModel.UiState.Error -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "오류: ${currentState.message}",
                        fontSize = 16.sp,
                        color = ColorPalette.main
                    )
                }
            }

            is MiracleRookieRankingSubPageViewModel.UiState.Success -> {
                if (currentState.items.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "랭킹 데이터가 없습니다.",
                            fontSize = 16.sp,
                            color = ColorPalette.textDimmed
                        )
                    }
                } else {
                    // 누적 랭킹(AGGREGATE)과 실시간 랭킹(DAILY)에 따라 itemType 설정
                    val itemType = if (selectedTabIndex == 0) "AGGREGATE" else "DAILY"

                    ExoRankingList(
                        items = currentState.items,
                        itemType = itemType,
                        isVisible = isVisible && (effectiveAccumulatedChartCode == null || selectedTabIndex == 1),  // 실시간 탭일 때만 타이머 동작
                        listState = scrollState,
                        onItemClick = { rank, item ->
                            android.util.Log.d("MiracleRookieSubPage", "Clicked: Rank $rank - ${item.name}")
                        },
                        onVoteSuccess = { idolId, voteCount ->
                            android.util.Log.d("MiracleRookieSubPage", "Vote success: idol=$idolId, votes=$voteCount")
                            viewModel.updateVote(idolId, voteCount)
                        }
                    )
                }
            }
        }
    }
}
