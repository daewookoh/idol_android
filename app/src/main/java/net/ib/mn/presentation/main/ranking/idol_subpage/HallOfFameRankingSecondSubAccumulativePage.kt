package net.ib.mn.presentation.main.ranking.idol_subpage

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import net.ib.mn.ui.components.ExoTabSwitch
import net.ib.mn.ui.theme.ColorPalette

/**
 * 명예전당 - 30일 누적 순위 서브 페이지
 *
 * @param chartCode 차트 코드
 * @param tabbarType 상위 탭바 타입 (0 = 30일 누적, 1 = 일일)
 * @param isVisible 화면 가시성
 * @param topThreeTabs RankingPage 최상단 탭 중 처음 3개
 * @param listState LazyList 스크롤 상태
 */
@Composable
fun HallOfFameRankingSecondSubAccumulativePage(
    chartCode: String,
    tabbarType: Int,
    isVisible: Boolean,
    topThreeTabs: List<String> = emptyList(),
    listState: LazyListState = rememberLazyListState()
) {
    var selectedSubTabIndex by remember { mutableStateOf(0) }

    android.util.Log.d("HoF_Accumulative", "========================================")
    android.util.Log.d("HoF_Accumulative", "🎨 Accumulative Page State")
    android.util.Log.d("HoF_Accumulative", "  - chartCode: $chartCode")
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
                    selectedSubTabIndex = index
                    android.util.Log.d("HoF_Accumulative", "Sub-tab selected: $index")
                }
            )
        }

        // TODO: 여기에 자체 ViewModel을 생성하고 데이터 로드 로직 구현
        // 현재는 placeholder UI만 표시
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = """
                    tabbarType: $tabbarType (30일 누적)
                    exoTabSwitchType: $selectedSubTabIndex
                    hofChartCode: $chartCode
                """.trimIndent(),
                fontSize = 14.sp,
                color = ColorPalette.textDimmed
            )
        }
    }
}
