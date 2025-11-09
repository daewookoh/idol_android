package net.ib.mn.presentation.main.ranking.idol_subpage

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import net.ib.mn.ui.components.ExoTabSwitch
import net.ib.mn.ui.components.HofAccumulativeRankingItem
import net.ib.mn.ui.components.HofAccumulativeTop1RankingItem
import net.ib.mn.ui.theme.ColorPalette
import java.text.DateFormat
import java.util.Calendar
import java.util.Date
import java.util.GregorianCalendar
import java.util.Locale

/**
 * 명예전당 - 30일 누적 순위 서브 페이지
 *
 * @param chartCode 차트 코드
 * @param tabbarType 상위 탭바 타입 (0 = 30일 누적, 1 = 일일)
 * @param isVisible 화면 가시성
 * @param topThreeTabs RankingPage 최상단 탭 중 처음 3개
 * @param listState LazyList 스크롤 상태
 * @param viewModel 상위 ViewModel (탭 선택 상태 관리)
 *
 * selectedSubTabIndex는 ViewModel의 SavedStateHandle로 저장되어:
 * - 앱을 내렸다 올려도 유지 (바텀 네비게이션 이동 시에도 유지)
 * - 앱을 재시작하면 리셋 (프로세스 종료 후)
 */
@Composable
fun HallOfFameRankingSecondSubAccumulativePage(
    chartCode: String,
    tabbarType: Int,
    isVisible: Boolean,
    topThreeTabs: List<String> = emptyList(),
    topThreeChartCodes: List<String> = emptyList(),
    listState: LazyListState = rememberLazyListState(),
    viewModel: HallOfFameRankingSubPageViewModel
) {
    val selectedSubTabIndex by viewModel.accumulativeSubTabIndex.collectAsState()

    // ExoTabSwitch 선택에 따른 차트 코드 결정
    val currentChartCode = topThreeChartCodes.getOrNull(selectedSubTabIndex) ?: chartCode

    // 데이터 로딩용 ViewModel 생성
    val dataViewModel: HallOfFameRankingSecondSubAccumulativePageViewModel =
        hiltViewModel<HallOfFameRankingSecondSubAccumulativePageViewModel, HallOfFameRankingSecondSubAccumulativePageViewModel.Factory> { factory ->
            factory.create(currentChartCode, selectedSubTabIndex)
        }

    val rankingData by dataViewModel.rankingData.collectAsState()
    val isLoading by dataViewModel.isLoading.collectAsState()
    val error by dataViewModel.error.collectAsState()
    val cdnUrl by dataViewModel.cdnUrl.collectAsState()

    // ExoTabSwitch 선택이 바뀔 때 새로운 차트 코드로 데이터 로드
    LaunchedEffect(selectedSubTabIndex) {
        android.util.Log.d("HoF_Accumulative", "🔄 ExoTabSwitch changed to index $selectedSubTabIndex")
        dataViewModel.loadData(currentChartCode)
    }

    android.util.Log.d("HoF_Accumulative", "========================================")
    android.util.Log.d("HoF_Accumulative", "🎨 Accumulative Page State")
    android.util.Log.d("HoF_Accumulative", "  - chartCode: $chartCode")
    android.util.Log.d("HoF_Accumulative", "  - currentChartCode: $currentChartCode")
    android.util.Log.d("HoF_Accumulative", "  - tabbarType: $tabbarType (0=30일누적, 1=일일)")
    android.util.Log.d("HoF_Accumulative", "  - exoTabSwitchType: $selectedSubTabIndex (선택된 서브탭)")
    android.util.Log.d("HoF_Accumulative", "  - topThreeTabs: $topThreeTabs")
    android.util.Log.d("HoF_Accumulative", "  - isVisible: $isVisible")
    android.util.Log.d("HoF_Accumulative", "========================================")

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // ExoTabSwitch: RankingPage 최상단 탭 중 처음 3개
        if (topThreeTabs.size >= 3) {
            ExoTabSwitch(
                tabs = topThreeTabs.take(3),
                selectedIndex = selectedSubTabIndex,
                onTabSelected = { index ->
                    viewModel.setAccumulativeSubTabIndex(index)
                    android.util.Log.d("HoF_Accumulative", "Sub-tab selected: $index")
                }
            )
        }

        // 데이터 표시
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            when {
                isLoading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center),
                        color = ColorPalette.main
                    )
                }
                error != null -> {
                    Text(
                        text = "Error: $error",
                        fontSize = 14.sp,
                        color = ColorPalette.textDimmed,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                rankingData.isEmpty() -> {
                    Text(
                        text = "데이터가 없습니다",
                        fontSize = 14.sp,
                        color = ColorPalette.textDimmed,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                else -> {
                    // 기간 계산 (old: TopViewHolder.bind() 로직과 동일)
                    val period = remember {
                        val today = Date()
                        var cal = GregorianCalendar()
                        cal.time = today
                        cal.add(Calendar.DATE, -30)
                        cal[Calendar.HOUR_OF_DAY] = 11
                        val fromDate = cal.time

                        cal = GregorianCalendar()
                        cal.time = today
                        cal[Calendar.HOUR_OF_DAY] = 23
                        var toDate = cal.time

                        val f = DateFormat.getDateInstance(DateFormat.MEDIUM, Locale.getDefault())
                        val fromText = f.format(fromDate)

                        if (today.time < toDate.time) {
                            cal = GregorianCalendar()
                            cal.time = today
                            cal.add(Calendar.DATE, -1)
                            cal[Calendar.HOUR_OF_DAY] = 23
                            toDate = cal.time
                        }
                        val toText = f.format(toDate)
                        "$fromText ~ $toText"
                    }

                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        // 1위 (헤더)
                        if (rankingData.isNotEmpty()) {
                            item(key = "top1_${rankingData[0].idolId}") {
                                HofAccumulativeTop1RankingItem(
                                    item = rankingData[0],
                                    cdnUrl = cdnUrl,
                                    period = period,
                                    onItemClick = {
                                        android.util.Log.d("HoF_Accumulative", "Clicked on 1st: ${rankingData[0].name}")
                                    },
                                    onInfoClick = {
                                        android.util.Log.d("HoF_Accumulative", "Info button clicked")
                                        // TODO: LevelHeartGuideActivity로 이동
                                    }
                                )
                            }
                        }

                        // 2위 이하
                        items(
                            items = rankingData.drop(1),
                            key = { item -> item.idolId }
                        ) { item ->
                            HofAccumulativeRankingItem(
                                item = item,
                                cdnUrl = cdnUrl,
                                onItemClick = {
                                    android.util.Log.d("HoF_Accumulative", "Clicked on ${item.name} (rank: ${item.scoreRank})")
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
