package net.ib.mn.presentation.main.ranking.idol_subpage

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.SecondaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import net.ib.mn.R
import net.ib.mn.ui.theme.ColorPalette
import net.ib.mn.ui.theme.ExoTypo

/**
 * 기적(HallOfFame) 랭킹 SubPage
 *
 * 완전히 독립적인 페이지로, 자체 ViewModel과 상태를 관리합니다.
 * charts/ranks/ API 사용, 남녀 변경에 영향 받지 않음
 */
@Composable
fun HallOfFameRankingSubPage(
    chartCode: String,
    modifier: Modifier = Modifier,
    isVisible: Boolean = true,
    topThreeTabs: List<String> = emptyList(),
    topThreeChartCodes: List<String> = emptyList(),
    listState: LazyListState? = null
) {
    android.util.Log.d("HallOfFameRankingSubPage", "========================================")
    android.util.Log.d("HallOfFameRankingSubPage", "🎨 [Composing] HallOfFame")
    android.util.Log.d("HallOfFameRankingSubPage", "  - chartCode: '$chartCode' (isEmpty: ${chartCode.isEmpty()})")
    android.util.Log.d("HallOfFameRankingSubPage", "  - topTabs: $topThreeTabs")
    android.util.Log.d("HallOfFameRankingSubPage", "  - topThreeChartCodes: $topThreeChartCodes")
    android.util.Log.d("HallOfFameRankingSubPage", "  - isVisible: $isVisible")
    android.util.Log.d("HallOfFameRankingSubPage", "========================================")

    // 독립적인 HallOfFameRankingSubPageViewModel
    val viewModel: HallOfFameRankingSubPageViewModel = hiltViewModel<HallOfFameRankingSubPageViewModel, HallOfFameRankingSubPageViewModel.Factory> { factory ->
        factory.create(chartCode)
    }

    val selectedTabIndex by viewModel.selectedTabIndex.collectAsState()
    val scrollState = listState ?: rememberLazyListState()

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // 탭바: 30일 누적 순위 / 일일 순위
        SecondaryTabRow(
            selectedTabIndex = selectedTabIndex,
            modifier = Modifier.fillMaxWidth(),
            containerColor = ColorPalette.background100,
            indicator = @Composable {
                TabRowDefaults.SecondaryIndicator(
                    modifier = Modifier.tabIndicatorOffset(selectedTabIndex).height(2.dp),
                    color = ColorPalette.textDefault
                )
            },
            divider = {
                HorizontalDivider(
                    thickness = 1.dp,
                    color = ColorPalette.gray100
                )
            }
        ) {
            Tab(
                selected = selectedTabIndex == 0,
                onClick = { viewModel.onTabSelected(0) },
                modifier = Modifier.height(48.dp),
                interactionSource = remember { MutableInteractionSource() }
            ) {
                Text(
                    text = stringResource(R.string.hof_cumulative_tab),
                    style = ExoTypo.body13.copy(color = if (selectedTabIndex == 0) ColorPalette.textDefault else ColorPalette.textDimmed)
                )
            }
            Tab(
                selected = selectedTabIndex == 1,
                onClick = { viewModel.onTabSelected(1) },
                modifier = Modifier.height(48.dp),
                interactionSource = remember { MutableInteractionSource() }
            ) {
                Text(
                    text = stringResource(R.string.hof_daily_tab),
                    style = ExoTypo.body13.copy(color = if (selectedTabIndex == 1) ColorPalette.textDefault else ColorPalette.textDimmed)
                )
            }
        }

        // 컨텐츠: 탭에 따라 다른 서브 페이지 표시
        when (selectedTabIndex) {
            0 -> {
                // 30일 누적 순위
                HallOfFameRankingSecondSubAccumulativePage(
                    chartCode = chartCode,
                    tabbarType = selectedTabIndex, // 0 = 30일 누적
                    isVisible = isVisible && selectedTabIndex == 0,
                    topThreeTabs = topThreeTabs,
                    topThreeChartCodes = topThreeChartCodes,
                    listState = scrollState,
                    viewModel = viewModel
                )
            }
            1 -> {
                // 일일 순위
                HallOfFameRankingSecondSubDailyPage(
                    chartCode = chartCode,
                    tabbarType = selectedTabIndex, // 1 = 일일
                    isVisible = isVisible && selectedTabIndex == 1,
                    topThreeTabs = topThreeTabs,
                    topThreeChartCodes = topThreeChartCodes,
                    listState = scrollState,
                    viewModel = viewModel
                )
            }
        }
    }
}

