package net.ib.mn.presentation.community.subpage

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import net.ib.mn.ui.components.RankingItem

/**
 * CommunityFeedSubPage - 커뮤니티 피드 탭
 *
 * @param rankingItem 선택된 아이돌 정보
 */
@Composable
fun CommunityFeedSubPage(
    rankingItem: RankingItem
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(text = "Feed - ${rankingItem.name}")
    }
}
