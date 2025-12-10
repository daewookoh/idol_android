package net.ib.mn.presentation.community.subpage

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import net.ib.mn.domain.model.ArticleModel
import net.ib.mn.navigation.LocalAppNavigator
import net.ib.mn.navigation.Screen
import net.ib.mn.presentation.common.ExoArticleNavigation
import net.ib.mn.presentation.common.ExoArticleViewModel
import net.ib.mn.presentation.community.CommunityViewModel
import net.ib.mn.presentation.main.freeboard.FreeBoardContent
import net.ib.mn.presentation.main.freeboard.FreeBoardContract
import net.ib.mn.presentation.main.freeboard.FreeBoardViewModel
import net.ib.mn.util.LocaleUtil

/**
 * CommunityFanTalkSubPage - 커뮤니티 팬톡 탭
 *
 * FreeBoardPage의 최애 탭과 동일한 UI를 재사용하며,
 * 선택된 아이돌(idolData.id)의 덕질게시판을 표시합니다.
 */
@Composable
fun CommunityFanTalkSubPage(
    idolData: CommunityViewModel.IdolData,
    onNavigateToArticleDetail: (ArticleModel, externalTabName: String?, onArticleUpdated: (ArticleModel) -> Unit) -> Unit = { _, _, _ -> },
    viewModel: FreeBoardViewModel = hiltViewModel(key = "fanTalk_${idolData.id}"),
    articleViewModel: ExoArticleViewModel = hiltViewModel()
) {
    val navigator = LocalAppNavigator.current
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val idolId = idolData.id.toIntOrNull() ?: 0

    LaunchedEffect(idolId) {
        if (idolId > 0) {
            viewModel.setExternalIdolId(idolId)
            viewModel.sendIntent(FreeBoardContract.Intent.LoadInitialData)
        }
    }

    // ExoArticle 네비게이션 이벤트 처리
    LaunchedEffect(Unit) {
        articleViewModel.navigationEvent.collect { event ->
            when (event) {
                is ExoArticleNavigation.ArticleDetail -> {
                    // externalIdol이 있으면 다국어 이름 전달
                    val externalTabName = state.externalIdol?.let {
                        LocaleUtil.getLocalizedIdolName(context, it)
                    }
                    onNavigateToArticleDetail(event.article, externalTabName) { updatedArticle ->
                        viewModel.updateArticle(updatedArticle)
                    }
                }
                is ExoArticleNavigation.EditArticle -> {
                    navigator.navigate(
                        Screen.ArticleWrite(
                            writeType = "FAN_TALK",
                            idolId = event.article.idol?.id ?: idolId,
                            editingArticleId = event.article.id
                        )
                    )
                }
                else -> { /* 다른 이벤트는 무시 */ }
            }
        }
    }

    FreeBoardContent(
        state = state,
        onIntent = viewModel::sendIntent,
        isExternalIdolMode = viewModel.isExternalIdolMode,
        articleViewModel = articleViewModel
    )
}
