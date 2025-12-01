package net.ib.mn.presentation.community.profile.subpage

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import net.ib.mn.R
import net.ib.mn.ui.components.ArticleType
import net.ib.mn.ui.components.ExoArticle
import net.ib.mn.ui.components.ExoArticleViewModel
import net.ib.mn.ui.theme.ColorPalette

/**
 * ProfilePostPage - 프로필 게시글(Activity) 탭
 */
@Composable
fun ProfilePostPage(
    userId: Int,
    isMine: Boolean = false,
    isFeedPrivate: Boolean = false,
    viewModel: ProfilePostViewModel = hiltViewModel(),
    articleViewModel: ExoArticleViewModel = hiltViewModel()
) {
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
            articleViewModel = articleViewModel
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
private fun PostList(
    state: ProfilePostUiState.Success,
    listState: androidx.compose.foundation.lazy.LazyListState,
    articleViewModel: ExoArticleViewModel
) {
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

            // ExoArticle - COMMUNITY 타입에서는 프로필 클릭 비활성화
            ExoArticle(
                article = article,
                type = ArticleType.COMMUNITY,
                isVisible = isVisible,
                showTranslation = true,
                viewModel = articleViewModel
            )
        }
    }
}
