package net.ib.mn.presentation.article

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import net.ib.mn.R
import net.ib.mn.domain.model.ArticleModel
import net.ib.mn.presentation.common.ArticleType
import net.ib.mn.presentation.common.ExoArticle
import net.ib.mn.presentation.common.ExoArticleNavigation
import net.ib.mn.presentation.common.ExoArticleViewModel
import net.ib.mn.ui.components.ExoAppBar
import net.ib.mn.ui.components.ExoScaffold

/**
 * ArticleDetailScreen - 게시글 상세 화면
 */
@Composable
fun ArticleDetailScreen(
    article: ArticleModel,
    onBackClick: () -> Unit = {},
    onArticleUpdated: (ArticleModel) -> Unit = {},
    onNavigateToProfile: (userId: Int, nickname: String, imageUrl: String?, level: Int, mostIdolName: String?) -> Unit = { _, _, _, _, _ -> },
    articleViewModel: ExoArticleViewModel = hiltViewModel()
) {
    // ExoArticle 네비게이션 이벤트 처리
    LaunchedEffect(Unit) {
        articleViewModel.navigationEvent.collect { event ->
            if (event is ExoArticleNavigation.Profile) {
                onNavigateToProfile(
                    event.userId,
                    event.nickname,
                    event.imageUrl,
                    event.level,
                    event.mostIdolName
                )
            }
        }
    }

    BackHandler(onBack = onBackClick)

    ExoScaffold(
        topBar = {
            ExoAppBar(
                title = stringResource(R.string.post_detail),
                onNavigationClick = onBackClick
            )
        }
    ) {
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            item {
                ExoArticle(
                    article = article,
                    type = ArticleType.DETAIL,
                    isVisible = true,
                    onArticleUpdated = onArticleUpdated,
                    viewModel = articleViewModel
                )
            }
            // TODO: 댓글 목록
        }
    }
}
