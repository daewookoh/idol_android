package net.ib.mn.presentation.community.history.idol

import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import net.ib.mn.ui.components.ExoProfileImage
import net.ib.mn.ui.components.ProfileImageType
import net.ib.mn.ui.theme.ExoTypo
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.Description
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.github.mikephil.charting.formatter.ValueFormatter
import net.ib.mn.R
import net.ib.mn.domain.model.IdolRankingHistoryModel
import net.ib.mn.presentation.community.history.daily.DailyRankingHistoryScreen
import net.ib.mn.ui.components.ExoAppBar
import net.ib.mn.ui.components.ExoScaffold
import net.ib.mn.ui.theme.ColorPalette
import net.ib.mn.util.LocaleUtil
import java.text.NumberFormat

/**
 * IdolRankingHistoryScreen - 랭킹 변동 히스토리 화면
 *
 * Old 프로젝트의 HallOfFameAggHistoryActivity를 참고하여 Compose로 구현
 * 특정 아이돌의 누적 순위 변동 그래프 및 상세 내역 표시
 * 아이템 클릭 시 DailyRankingHistoryScreen으로 이동
 *
 * @param idolId 아이돌 ID
 * @param idolName 아이돌 이름 (예: "이름_그룹명")
 * @param onBackClick 뒤로가기 클릭 이벤트
 */
@Composable
fun IdolRankingHistoryScreen(
    idolId: Int,
    idolName: String,
    onBackClick: () -> Unit = {}
) {
    val context = LocalContext.current

    // DailyRankingHistoryScreen 표시 상태
    var showDailyRankingHistory by remember { mutableStateOf(false) }
    var selectedHistoryParam by remember { mutableStateOf("") }
    var selectedDateTitle by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("") }
    var selectedChartCode by remember { mutableStateOf("") }

    // 이름 파싱 (이름_그룹명 형식)
    val nameParts = idolName.split("_")
    val displayIdolName = nameParts.getOrElse(0) { idolName }
    val displayGroupName = nameParts.getOrElse(1) { "" }

    // ViewModel 생성
    val viewModel: IdolRankingHistoryViewModel = hiltViewModel(
        key = "idol_ranking_history_$idolId",
        creationCallback = { factory: IdolRankingHistoryViewModel.Factory ->
            factory.create(
                savedStateHandle = SavedStateHandle(mapOf("idolId" to idolId))
            )
        }
    )

    val uiState by viewModel.uiState.collectAsState()

    val appLocale = remember { LocaleUtil.getAppLocale(context) }
    val numberFormat = remember(appLocale) { NumberFormat.getNumberInstance(appLocale) }

    BackHandler {
        if (showDailyRankingHistory) {
            showDailyRankingHistory = false
        } else {
            onBackClick()
        }
    }

    ExoScaffold(
        topBar = {
            ExoAppBar(
                title = {
                    Row(
                        modifier = Modifier.wrapContentHeight(),
                    ) {
                        Text(
                            text = stringResource(R.string.title_rank_history) + " - ",
                            style = ExoTypo.typo18,
                            color = ColorPalette.textDefault,
                            maxLines = 1,
                            modifier = Modifier.alignByBaseline()
                        )

                        Text(
                            text = displayIdolName,
                            style = ExoTypo.typo18,
                            modifier = Modifier.alignByBaseline()
                        )

                        if (displayGroupName.isNotEmpty()) {
                            Text(
                                text = " $displayGroupName",
                                style = ExoTypo.typo10,
                                modifier = Modifier.alignByBaseline()
                            )
                        }
                    }
                },
                onNavigationClick = onBackClick
            )
        }
    ) {
        when (val state = uiState) {
            is IdolRankingHistoryUiState.Loading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = ColorPalette.main)
                }
            }

            is IdolRankingHistoryUiState.Error -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = state.message,
                        color = ColorPalette.textGray
                    )
                }
            }

            is IdolRankingHistoryUiState.Success -> {
                if (state.items.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = stringResource(R.string.no_data),
                            style = ExoTypo.typo14Gray
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(ColorPalette.background100)
                    ) {
                        // 차트 헤더
                        item(key = "chart_header") {
                            IdolRankingHistoryChart(
                                items = state.items,
                                averageHeart = state.averageHeart,
                                numberFormat = numberFormat
                            )
                        }

                        // 아이템 목록
                        itemsIndexed(
                            items = state.items,
                            key = { index, item -> "history_${index}_${item.refdate}_${item.rank}" }
                        ) { _, item ->
                            IdolRankingHistoryItem(
                                item = item,
                                numberFormat = numberFormat,
                                onClick = {
                                    selectedHistoryParam = item.refdate
                                    selectedDateTitle = item.getFormattedRefDate(context)
                                    selectedType = item.type
                                    selectedCategory = state.category
                                    selectedChartCode = state.chartCode
                                    showDailyRankingHistory = true
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    // DailyRankingHistoryScreen 오버레이
    AnimatedVisibility(
        visible = showDailyRankingHistory,
        enter = fadeIn(),
        exit = fadeOut()
    ) {
        DailyRankingHistoryScreen(
            historyParam = selectedHistoryParam,
            type = selectedType,
            category = selectedCategory,
            chartCode = selectedChartCode,
            dateTitle = selectedDateTitle,
            onBackClick = { showDailyRankingHistory = false }
        )
    }
}

/**
 * IdolRankingHistoryChart - 순위 변동 차트
 *
 * Old 프로젝트의 chart_header.xml과 drawChart() 참고
 */
@Composable
private fun IdolRankingHistoryChart(
    items: List<IdolRankingHistoryModel>,
    averageHeart: Long,
    numberFormat: NumberFormat
) {
    val context = LocalContext.current
    val mainColor = ColorPalette.main.toArgb()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(ColorPalette.background100)
            .padding(bottom = 15.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // LineChart (MPAndroidChart)
        AndroidView(
            factory = { ctx ->
                LineChart(ctx).also { chart ->
                    // 차트 기본 설정
                    val desc = Description()
                    desc.text = ""
                    chart.description = desc

                    chart.setPinchZoom(false)
                    chart.isDoubleTapToZoomEnabled = false
                    chart.setDrawGridBackground(false)
                    chart.legend.isEnabled = false

                    // Y축 숨기기
                    chart.axisLeft.isEnabled = false
                    chart.axisRight.isEnabled = false
                    chart.axisLeft.setDrawGridLines(false)

                    // X축 설정
                    chart.xAxis.position = XAxis.XAxisPosition.BOTTOM
                    chart.xAxis.textSize = 8f
                    chart.xAxis.typeface = Typeface.create("sans-serif-light", Typeface.NORMAL)
                    chart.xAxis.setDrawGridLines(false)
                    chart.xAxis.setDrawAxisLine(false)
                    chart.xAxis.valueFormatter = object : IndexAxisValueFormatter() {
                        override fun getFormattedValue(value: Float): String = ""
                    }

                    chart.isDragEnabled = true
                    chart.setNoDataText(ctx.getString(R.string.loading))
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(67.dp)
                .padding(top = 10.dp),
            update = { lineChart: LineChart ->
                if (items.isEmpty()) {
                    lineChart.setNoDataText(context.getString(R.string.no_data))
                    lineChart.invalidate()
                    return@AndroidView
                }

                // 최고/최저 순위 계산
                var top = 999
                var bottom = 1
                items.forEach { item ->
                    if (item.rank < top) top = item.rank
                    if (item.rank > bottom) bottom = item.rank
                }

                // Y축 범위를 0-100으로 정규화하여 시각적 차이 최대화
                lineChart.axisLeft.axisMinimum = 0f
                lineChart.axisLeft.axisMaximum = 100f

                // 엔트리 생성 (역순으로 - 최신이 오른쪽)
                // Y값을 0-100 범위로 정규화 (최고 순위=100, 최저 순위=0)
                val entries = ArrayList<Entry>()
                val rankRange = (bottom - top).coerceAtLeast(1).toFloat()
                for (index in items.indices) {
                    val item = items[items.size - 1 - index]
                    val rankCount = numberFormat.format(item.rank.toLong())
                    // 정규화: 최고 순위(top)=100, 최저 순위(bottom)=0
                    val normalizedY = ((bottom - item.rank).toFloat() / rankRange) * 100f
                    entries.add(
                        Entry(
                            index.toFloat(),
                            normalizedY,
                            if (item.rank == 999) "-" else rankCount
                        )
                    )
                }

                // 데이터셋 설정
                val set = LineDataSet(entries, "")
                set.color = mainColor
                set.lineWidth = 1f
                set.setDrawValues(true)
                set.valueTextSize = 10f
                set.valueTextColor = 0xFF929292.toInt()
                set.setDrawCircleHole(false)
                set.setCircleColor(mainColor)
                set.circleRadius = 3f

                // 그라데이션 채우기 (Old: 아래 0% -> 위 20% 투명도)
                set.setDrawFilled(true)
                set.fillDrawable = createGradientDrawable(mainColor)

                val lineData = LineData(set)
                lineData.setValueFormatter(object : ValueFormatter() {
                    override fun getPointLabel(entry: Entry): String {
                        return entry.data as String
                    }
                })

                lineChart.data = lineData
                lineChart.notifyDataSetChanged()
                lineChart.invalidate()
            }
        )

        // 차트 아래 구분선 (Old: 0.3dp, gray110)
        HorizontalDivider(
            modifier = Modifier.fillMaxWidth(),
            thickness = 0.3.dp,
            color = ColorPalette.gray110
        )

        Spacer(modifier = Modifier.height(7.dp))

        // 평균 일수 타이틀
        Text(
            text = stringResource(R.string.rank_history_30_average, items.size.toString()),
            style = ExoTypo.typo15Main,
            textAlign = TextAlign.Center
        )

        // 평균 투표수
        Text(
            text = stringResource(R.string.vote_count_format, numberFormat.format(averageHeart)),
            style = ExoTypo.typo13Bold,
            color = ColorPalette.gray900,
            textAlign = TextAlign.Center
        )
    }
}

/**
 * IdolRankingHistoryItem - 히스토리 아이템
 *
 * Old 프로젝트의 hall_agg_top_item.xml 레이아웃과 동일하게 구현
 * 왼쪽: 순위 + 변동, 가운데: 프로필 이미지, 오른쪽: 날짜 + 점수/투표수
 */
@Composable
private fun IdolRankingHistoryItem(
    item: IdolRankingHistoryModel,
    numberFormat: NumberFormat,
    cutLine: Int = 100,
    onClick: () -> Unit = {}
) {
    val context = LocalContext.current

    Column(
        modifier = Modifier.clickable(
            indication = null,
            interactionSource = remember { MutableInteractionSource() }
        ) { onClick() }
    ) {
        // 상단 구분선
        HorizontalDivider(
            modifier = Modifier.fillMaxWidth(),
            thickness = 1.dp,
            color = ColorPalette.gray100
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(ColorPalette.background100)
                .padding(vertical = 7.dp, horizontal = 0.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
        // 왼쪽: 순위 영역 (45dp min width, 중앙 정렬)
        Column(
            modifier = Modifier.widthIn(min = 45.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 순위 텍스트 (예: "11위")
            val idolRank = numberFormat.format(item.rank.toLong())
            val rankText = if (item.rank == 999) "-" else stringResource(
                R.string.rank_count_format,
                idolRank
            )
            Text(
                text = rankText,
                maxLines = 1,
                style = ExoTypo.typo13Main
            )

            Spacer(modifier = Modifier.height(2.dp))

            // 순위 변동 아이콘 + 수치 또는 NEW 아이콘
            if (item.status == "new") {
                Icon(
                    painter = painterResource(R.drawable.icon_change_ranking_new),
                    contentDescription = null,
                    tint = Color.Unspecified,
                    modifier = Modifier.size(width = 15.dp, height = 7.5.dp)
                )
            } else {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    val icon = when (item.status) {
                        "increase" -> R.drawable.icon_change_ranking_up
                        "decrease" -> R.drawable.icon_change_ranking_down
                        else -> R.drawable.icon_change_ranking_no_change
                    }
                    Icon(
                        painter = painterResource(icon),
                        contentDescription = null,
                        tint = Color.Unspecified,
                        modifier = Modifier.size(8.dp)
                    )

                    val changeRankingCount = numberFormat.format(item.difference.toLong())
                    Text(
                        text = changeRankingCount,
                        maxLines = 1,
                        style = ExoTypo.typo9
                    )
                }
            }
        }

        // 프로필 이미지 (35dp, 원형) - ExoProfileImage XSMALL 사용
        ExoProfileImage(
            imageUrl = item.imageUrl,
            type = ProfileImageType.XSMALL,
            rank = 0,
            contentDescription = "프로필 이미지"
        )

        Spacer(modifier = Modifier.width(10.dp))

        // 오른쪽: 날짜 + 점수/투표수
        Column(
            modifier = Modifier.weight(1f)
        ) {
            // 날짜
            val dateText = item.getFormattedRefDate(context)
            if (dateText.isNotEmpty()) {
                Text(
                    text = dateText,
                    maxLines = 1,
                    style = ExoTypo.typo12
                )
            }

            Spacer(modifier = Modifier.height(1.dp))

            // 점수 / 투표수
            Row {
                // 점수 계산: cutLine + 1 - rank (rank < cutLine + 1 인 경우만)
                val score = if (item.rank < (cutLine + 1)) (cutLine + 1) - item.rank else 0
                val scoreCount = numberFormat.format(score.toLong())
                val scoreText = if (item.rank < (cutLine + 1)) {
                    "+${stringResource(R.string.score_format, scoreCount)}"
                } else {
                    stringResource(R.string.score_format, scoreCount)
                }
                Text(
                    text = scoreText,
                    style = ExoTypo.typo12Main
                )

                Text(
                    text = " / ",
                    style = ExoTypo.typo12Gray
                )

                // 투표수
                val voteCountComma = numberFormat.format(item.heart)
                Text(
                    text = stringResource(R.string.vote_count_format, voteCountComma),
                    style = ExoTypo.typo12Gray
                )
            }
        }

        // 우측 화살표 아이콘 (Old: 8dp, marginEnd=20dp)
        Icon(
            painter = painterResource(R.drawable.btn_go),
            contentDescription = null,
            tint = Color.Unspecified,
            modifier = Modifier
                .padding(end = 24.dp)
                .size(12.dp)
        )
        }
    }
}

/**
 * 차트 그라데이션 Drawable 생성
 * Old 프로젝트와 동일: 아래(0% 투명) -> 위(20% 투명)
 * GradientDrawable의 TOP_BOTTOM은 위(start)에서 아래(end)로 진행
 */
private fun createGradientDrawable(color: Int): Drawable {
    // Old: angle=90 (아래에서 위로), startColor=0% 투명, endColor=20% 투명
    // GradientDrawable.Orientation.BOTTOM_TOP = 아래에서 위로
    val startColor = (color and 0x00FFFFFF) or 0x00000000  // 아래: 완전 투명 (0%)
    val endColor = (color and 0x00FFFFFF) or 0x33000000    // 위: 20% 불투명

    return GradientDrawable(
        GradientDrawable.Orientation.BOTTOM_TOP,
        intArrayOf(startColor, endColor)
    )
}
