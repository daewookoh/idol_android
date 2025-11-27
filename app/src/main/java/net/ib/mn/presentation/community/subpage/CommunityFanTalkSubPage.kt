package net.ib.mn.presentation.community.subpage

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import net.ib.mn.ui.components.RankingItem

/**
 * CommunityFanTalkSubPage - 커뮤니티 팬톡 탭
 *
 * @param rankingItem 선택된 아이돌 정보
 * @param fandomName 팬덤 이름 (없으면 기본값 사용)
 */
@Composable
fun CommunityFanTalkSubPage(
    rankingItem: RankingItem,
    fandomName: String?
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(text = "${fandomName ?: "팬톡"} - ${rankingItem.name}")
    }
}
