package net.ib.mn.presentation.awards

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import net.ib.mn.ui.components.ExoAppBar
import net.ib.mn.ui.components.ExoScaffold

/**
 * AwardsScreen - 어워즈 화면
 *
 * @param onNavigateBack 뒤로가기 클릭 이벤트
 */
@Composable
fun AwardsScreen(
    onNavigateBack: () -> Unit = {}
) {
    ExoScaffold(
        topBar = {
            ExoAppBar(
                title = "Awards",
                onNavigationClick = onNavigateBack
            )
        }
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
            contentAlignment = Alignment.Center
        ) {
            // TODO: 어워즈 콘텐츠 구현
        }
    }
}
