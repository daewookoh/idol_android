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
 * AwardsGuideSubPage - 어워즈 안내 탭
 *
 * old 프로젝트의 AwardsGuideFragment 참고
 * - 어워즈 투표 안내 및 설명 표시
 */
@Composable
fun AwardsGuideSubPage() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        // TODO: 안내 콘텐츠 구현
        Text(
            text = "Guide",
            color = ColorPalette.textGray
        )
    }
}
