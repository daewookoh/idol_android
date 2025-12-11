package net.ib.mn.presentation.overlay.profile.subpage

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import net.ib.mn.R
import net.ib.mn.domain.model.ArticleModel
import net.ib.mn.presentation.common.ArticleItemType
import net.ib.mn.presentation.common.ExoArticleItem
import net.ib.mn.presentation.common.ExoArticleNavigation
import net.ib.mn.presentation.common.ExoArticleViewModel
import net.ib.mn.ui.theme.ColorPalette
import net.ib.mn.util.LocaleUtil

/**
 * ProfilePostPage - 프로필 게시글(Activity) 탭
 */
@Composable
fun ProfilePostPage(
    userId: Int,
    isMine: Boolean = false,
    isFeedPrivate: Boolean = false,
    isBlocked: Boolean = false,
    blockStatusChecked: Boolean = true,
    onNavigateToArticleDetail: (ArticleModel, onArticleUpdated: (ArticleModel) -> Unit, onArticleDeleted: () -> Unit) -> Unit = { _, _, _ -> },
    onNavigateToPhotoDetail: (ArticleModel, Int) -> Unit = { _, _ -> },
    onNavigateToArticleEdit: (ArticleModel) -> Unit = {},
    viewModel: ProfilePostViewModel = hiltViewModel(),
    articleViewModel: ExoArticleViewModel = hiltViewModel()
) {
    // ExoArticle 네비게이션 이벤트 처리
    LaunchedEffect(Unit) {
        articleViewModel.navigationEvent.collect { event ->
            when (event) {
                is ExoArticleNavigation.ArticleDetail -> {
                    onNavigateToArticleDetail(
                        event.article,
                        { updatedArticle -> viewModel.updateArticle(updatedArticle) },
                        { viewModel.removeArticle(event.article.id) }
                    )
                }
                is ExoArticleNavigation.MediaDetail -> {
                    onNavigateToPhotoDetail(event.article, event.mediaIndex)
                }
                is ExoArticleNavigation.EditArticle -> {
                    onNavigateToArticleEdit(event.article)
                }
                else -> { /* 다른 이벤트는 무시 */ }
            }
        }
    }

    // 차단 상태 확인 전에는 로딩 표시
    if (!blockStatusChecked) {
        LoadingContent()
        return
    }

    // 차단된 사용자일 경우 바로 Blocked 상태 표시
    if (isBlocked) {
        BlockedContent()
        return
    }

    // 비공개 피드일 경우 API 호출 없이 바로 Private 상태 표시
    if (isFeedPrivate) {
        PrivateContent()
        return
    }

    val uiState by viewModel.uiState.collectAsState()
    val listState = rememberLazyListState()

    LaunchedEffect(userId) {
        viewModel.loadPosts(userId, isMine)
    }

    // 무한 스크롤
    val shouldLoadMore by remember {
        derivedStateOf {
            val lastVisibleItem = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            lastVisibleItem >= listState.layoutInfo.totalItemsCount - 3
        }
    }

    LaunchedEffect(shouldLoadMore) {
        if (shouldLoadMore && uiState is ProfilePostUiState.Success) {
            if ((uiState as ProfilePostUiState.Success).hasMore) {
                viewModel.loadMore()
            }
        }
    }

    when (val state = uiState) {
        is ProfilePostUiState.Loading -> LoadingContent()
        is ProfilePostUiState.Empty -> EmptyContent(stringResource(R.string.feed_no_posts))
        is ProfilePostUiState.Private -> PrivateContent()
        is ProfilePostUiState.Error -> EmptyContent(state.message)
        is ProfilePostUiState.Success -> PostList(
            state = state,
            listState = listState,
            articleViewModel = articleViewModel,
            postViewModel = viewModel
        )
    }
}

@Composable
private fun LoadingContent() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ColorPalette.background100),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(color = ColorPalette.main)
    }
}

@Composable
private fun EmptyContent(message: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ColorPalette.background100),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = message,
            color = ColorPalette.textGray,
            fontSize = 14.sp
        )
    }
}

@Composable
private fun PrivateContent() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ColorPalette.background100),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                painter = painterResource(R.drawable.icon_feed_lock),
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = Color.Unspecified
            )
            Spacer(modifier = Modifier.height(14.dp))
            Text(
                text = stringResource(R.string.feed_private),
                color = ColorPalette.textGray,
                fontSize = 12.sp
            )
        }
    }
}

@Composable
private fun BlockedContent() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ColorPalette.background100),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                painter = painterResource(R.drawable.icon_feed_lock),
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = Color.Unspecified
            )
            Spacer(modifier = Modifier.height(14.dp))
            Text(
                text = stringResource(R.string.user_blocked),
                color = ColorPalette.textGray,
                fontSize = 12.sp
            )
        }
    }
}

@Composable
private fun PostList(
    state: ProfilePostUiState.Success,
    listState: LazyListState,
    articleViewModel: ExoArticleViewModel,
    postViewModel: ProfilePostViewModel
) {
    val context = LocalContext.current

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(ColorPalette.gray80),
        state = listState
    ) {
        itemsIndexed(
            items = state.posts,
            key = { _, article -> article.id }
        ) { index, article ->
            // 화면에 보이는지 체크 (GIF/비디오 최적화)
            val visibleItems = listState.layoutInfo.visibleItemsInfo
            val isVisible = visibleItems.any { it.index == index }

            // 커뮤니티 이름 (idol 정보에서 가져옴)
            val communityName = remember(article.idol) {
                article.idol?.let { LocaleUtil.getLocalizedIdolName(context, it) }
            }

            // ExoArticleItem - FEED 타입 (커뮤니티 이름 포함)
            ExoArticleItem(
                article = article,
                type = ArticleItemType.FEED,
                externalCommunityName = communityName,
                isVisible = isVisible,
                showTranslation = true,
                onDeleted = { deletedArticleId ->
                    postViewModel.removeArticle(deletedArticleId)
                },
                onArticleUpdated = { updatedArticle ->
                    postViewModel.updateArticle(updatedArticle)
                },
                viewModel = articleViewModel
            )
        }
    }
}
