package net.ib.mn.presentation.awards.subpage

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import net.ib.mn.ui.theme.ColorPalette

/**
 * AwardsCumulativeSubPage - 어워즈 누적 순위 집계 탭
 *
 * old 프로젝트의 AwardsAggregatedFragment 참고
 * - 누적 투표 순위 리스트 표시
 */
@Composable
fun AwardsCumulativeSubPage() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        // TODO: 누적 순위 집계 콘텐츠 구현
        Text(
            text = "Cumulative",
            color = ColorPalette.textGray
        )
    }
}
