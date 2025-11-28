package net.ib.mn.presentation.community.subpage

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import net.ib.mn.presentation.main.freeboard.FreeBoardContent
import net.ib.mn.presentation.main.freeboard.FreeBoardContract
import net.ib.mn.presentation.main.freeboard.FreeBoardViewModel
import net.ib.mn.ui.components.RankingItem

/**
 * CommunityFanTalkSubPage - 커뮤니티 팬톡 탭
 *
 * FreeBoardPage의 최애 탭과 동일한 UI를 재사용하며,
 * 선택된 아이돌(rankingItem.id)의 덕질게시판을 표시합니다.
 */
@Composable
fun CommunityFanTalkSubPage(
    rankingItem: RankingItem,
    fandomName: String?,
    viewModel: FreeBoardViewModel = hiltViewModel(key = "fanTalk_${rankingItem.id}")
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(rankingItem.id) {
        rankingItem.id.toIntOrNull()?.let { viewModel.setExternalIdolId(it) }
        viewModel.sendIntent(FreeBoardContract.Intent.LoadInitialData)
    }

    FreeBoardContent(
        state = state,
        onIntent = viewModel::sendIntent,
        isExternalIdolMode = viewModel.isExternalIdolMode
    )
}
