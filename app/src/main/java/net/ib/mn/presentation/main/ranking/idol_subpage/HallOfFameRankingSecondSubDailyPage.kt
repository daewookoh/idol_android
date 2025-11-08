package net.ib.mn.presentation.main.ranking.idol_subpage

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import net.ib.mn.R
import net.ib.mn.ui.components.ExoTabSwitch
import net.ib.mn.ui.theme.ColorPalette
import net.ib.mn.ui.theme.ExoTypo

/**
 * 명예전당 - 일일 순위 서브 페이지
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
fun HallOfFameRankingSecondSubDailyPage(
    chartCode: String,
    tabbarType: Int,
    isVisible: Boolean,
    topThreeTabs: List<String> = emptyList(),
    topThreeChartCodes: List<String> = emptyList(),
    listState: LazyListState = rememberLazyListState(),
    viewModel: HallOfFameRankingSubPageViewModel
) {
    val selectedSubTabIndex by viewModel.dailySubTabIndex.collectAsState()

    // ExoTabSwitch 선택에 따른 차트 코드 결정
    val currentChartCode = topThreeChartCodes.getOrNull(selectedSubTabIndex) ?: chartCode

    // 데이터 로딩용 ViewModel 생성
    val dataViewModel: HallOfFameRankingSecondSubDailyPageViewModel =
        hiltViewModel<HallOfFameRankingSecondSubDailyPageViewModel, HallOfFameRankingSecondSubDailyPageViewModel.Factory> { factory ->
            factory.create(currentChartCode, selectedSubTabIndex)
        }

    val jsonData by dataViewModel.jsonData.collectAsState()
    val isLoading by dataViewModel.isLoading.collectAsState()
    val error by dataViewModel.error.collectAsState()
    val historyYear by dataViewModel.historyYear.collectAsState()
    val historyMonth by dataViewModel.historyMonth.collectAsState()
    val showPrevButton by dataViewModel.showPrevButton.collectAsState()
    val showNextButton by dataViewModel.showNextButton.collectAsState()

    // ExoTabSwitch 선택이 바뀔 때 새로운 차트 코드로 데이터 로드
    LaunchedEffect(selectedSubTabIndex) {
        android.util.Log.d("HoF_Daily", "🔄 ExoTabSwitch changed to index $selectedSubTabIndex")
        dataViewModel.loadData(currentChartCode)
    }

    android.util.Log.d("HoF_Daily", "========================================")
    android.util.Log.d("HoF_Daily", "🎨 Daily Page State")
    android.util.Log.d("HoF_Daily", "  - chartCode: $chartCode")
    android.util.Log.d("HoF_Daily", "  - currentChartCode: $currentChartCode")
    android.util.Log.d("HoF_Daily", "  - tabbarType: $tabbarType (0=30일누적, 1=일일)")
    android.util.Log.d("HoF_Daily", "  - exoTabSwitchType: $selectedSubTabIndex (선택된 서브탭)")
    android.util.Log.d("HoF_Daily", "  - topThreeTabs: $topThreeTabs")
    android.util.Log.d("HoF_Daily", "  - isVisible: $isVisible")
    android.util.Log.d("HoF_Daily", "========================================")

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // ExoTabSwitch: RankingPage 최상단 탭 중 처음 3개
        if (topThreeTabs.size >= 3) {
            ExoTabSwitch(
                tabs = topThreeTabs.take(3),
                selectedIndex = selectedSubTabIndex,
                onTabSelected = { index ->
                    viewModel.setDailySubTabIndex(index)
                    android.util.Log.d("HoF_Daily", "Sub-tab selected: $index")
                }
            )
        }

        // 기간 선택 영역
        Box(
            modifier = Modifier
                .width(150.dp)
                .height(50.dp)
                .align(Alignment.CenterHorizontally)
        ) {
            // Previous button (왼쪽)
            Box(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(end = 20.dp)
            ) {
                AndroidView(
                    factory = { context ->
                        android.widget.ImageView(context).apply {
                            setImageResource(R.drawable.btn_arrow_left_state)
                            val density = context.resources.displayMetrics.density
                            val paddingStartPx = (18 * density).toInt()
                            val paddingTopPx = (16 * density).toInt()
                            val paddingEndPx = (18 * density).toInt()
                            val paddingBottomPx = (16 * density).toInt()
                            setPadding(paddingStartPx, paddingTopPx, paddingEndPx, paddingBottomPx)
                            setOnClickListener {
                                dataViewModel.onPrevClicked(currentChartCode)
                            }
                        }
                    },
                    update = { imageView ->
                        imageView.visibility = if (showPrevButton) {
                            android.view.View.VISIBLE
                        } else {
                            android.view.View.GONE
                        }
                    },
                    modifier = Modifier.size(45.dp)
                )
            }

            // Year and Month display (중앙)
            Column(
                modifier = Modifier.align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = historyYear ?: stringResource(R.string.recent),
                    style = ExoTypo.body11,
                )
                Text(
                    text = historyMonth ?: stringResource(R.string.thirty_days),
                    style = ExoTypo.body15.copy(fontWeight = FontWeight.Bold),
                )
            }

            // Next button (오른쪽, 영역은 항상 유지)
            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(start = 20.dp)
            ) {
                AndroidView(
                    factory = { context ->
                        android.widget.ImageView(context).apply {
                            setImageResource(R.drawable.btn_arrow_right_state)
                            val density = context.resources.displayMetrics.density
                            val paddingStartPx = (18 * density).toInt()
                            val paddingTopPx = (16 * density).toInt()
                            val paddingEndPx = (18 * density).toInt()
                            val paddingBottomPx = (16 * density).toInt()
                            setPadding(paddingStartPx, paddingTopPx, paddingEndPx, paddingBottomPx)
                            setOnClickListener {
                                dataViewModel.onNextClicked(currentChartCode)
                            }
                        }
                    },
                    update = { imageView ->
                        imageView.visibility = if (showNextButton) {
                            android.view.View.VISIBLE
                        } else {
                            android.view.View.INVISIBLE // INVISIBLE로 공간 유지
                        }
                    },
                    modifier = Modifier.size(45.dp)
                )
            }
        }

        HorizontalDivider(
            thickness = 1.dp,
            color = ColorPalette.gray100
        )

        // 데이터 표시
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            when {
                isLoading -> {
                    null
                }
                error != null -> {
                    Text(
                        text = "Error: $error",
                        fontSize = 14.sp,
                        color = ColorPalette.textDimmed,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                else -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                    ) {
                        Text(
                            text = """
                                일일 순위

                                tabbarType: $tabbarType (일일)
                                exoTabSwitchType: $selectedSubTabIndex
                                hofChartCode: $chartCode

                                JSON Data:
                            """.trimIndent(),
                            fontSize = 12.sp,
                            color = ColorPalette.textDimmed
                        )

                        Text(
                            text = jsonData,
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            color = ColorPalette.textDefault,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                }
            }
        }
    }
}
