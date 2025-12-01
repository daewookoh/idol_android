package net.ib.mn.presentation.community.profile.subpage

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import net.ib.mn.R
import net.ib.mn.ui.theme.ColorPalette

/**
 * ProfilePostPage - 프로필 게시글(Activity) 탭
 *
 * Old 프로젝트의 FeedActivityFragment를 참고하여 Compose로 구현
 * 유저가 작성한 게시글 목록을 표시
 *
 * @param userId 유저 ID
 * @param viewModel ViewModel
 */
@Composable
fun ProfilePostPage(
    userId: Int,
    viewModel: ProfilePostViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val listState = rememberLazyListState()

    LaunchedEffect(userId) {
        viewModel.loadPosts(userId)
    }

    when (val state = uiState) {
        is ProfilePostUiState.Loading -> {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(ColorPalette.background100),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = ColorPalette.main)
            }
        }

        is ProfilePostUiState.Empty -> {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(ColorPalette.background100),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(R.string.feed_no_posts),
                    color = ColorPalette.textGray,
                    fontSize = 14.sp
                )
            }
        }

        is ProfilePostUiState.Error -> {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(ColorPalette.background100),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = state.message,
                    color = ColorPalette.textGray,
                    fontSize = 14.sp
                )
            }
        }

        is ProfilePostUiState.Success -> {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .background(ColorPalette.background100),
                state = listState
            ) {
                items(
                    items = state.posts,
                    key = { it.id }
                ) { post ->
                    // TODO: ExoArticle 컴포넌트 사용하여 게시글 표시
                    // 현재는 placeholder
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp)
                            .padding(8.dp)
                            .background(ColorPalette.gray100)
                    ) {
                        Text(
                            text = post.content,
                            modifier = Modifier.padding(8.dp)
                        )
                    }
                }
            }
        }
    }
}
