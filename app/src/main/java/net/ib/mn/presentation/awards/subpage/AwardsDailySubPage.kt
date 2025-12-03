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
 * AwardsDailySubPage - 어워즈 실시간 순위 탭
 *
 * old 프로젝트의 AwardsRankingFragment 참고
 * - 실시간 투표 순위 리스트 표시
 * - 타이머로 주기적 업데이트
 */
@Composable
fun AwardsDailySubPage() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        // TODO: 실시간 순위 콘텐츠 구현
        Text(
            text = "Daily",
            color = ColorPalette.textGray
        )
    }
}
