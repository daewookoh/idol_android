package net.ib.mn.presentation.overlay.profile.subpage

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import net.ib.mn.R
import net.ib.mn.ui.theme.ColorPalette

/**
 * ProfileCommentPage - 프로필 댓글 탭
 *
 * Old 프로젝트의 FeedCommentFragment를 참고하여 Compose로 구현
 * 본인 프로필에서만 표시되는 댓글 목록
 *
 * @param userId 유저 ID
 * @param viewModel ViewModel
 */
@Composable
fun ProfileCommentPage(
    userId: Int,
    viewModel: ProfileCommentViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val listState = rememberLazyListState()

    LaunchedEffect(userId) {
        viewModel.loadComments(userId)
    }

    when (val state = uiState) {
        is ProfileCommentUiState.Loading -> {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(ColorPalette.background100),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = ColorPalette.main)
            }
        }

        is ProfileCommentUiState.Empty -> {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(ColorPalette.background100),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(R.string.feed_no_comments),
                    color = ColorPalette.textGray,
                    fontSize = 14.sp
                )
            }
        }

        is ProfileCommentUiState.Error -> {
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

        is ProfileCommentUiState.Success -> {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .background(ColorPalette.background100),
                state = listState
            ) {
                items(
                    items = state.comments,
                    key = { it.id }
                ) { comment ->
                    // TODO: 댓글 아이템 컴포넌트로 표시
                    // 현재는 placeholder
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(80.dp)
                            .padding(8.dp)
                            .background(ColorPalette.gray100)
                    ) {
                        Text(
                            text = comment.content,
                            modifier = Modifier.padding(8.dp)
                        )
                    }
                }
            }
        }
    }
}
