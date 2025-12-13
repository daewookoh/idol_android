package net.ib.mn.presentation.comment

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.flow.collectLatest
import net.ib.mn.R
import net.ib.mn.presentation.common.CommentInput
import net.ib.mn.presentation.common.EmoticonPreview
import net.ib.mn.presentation.common.commentItems
import net.ib.mn.ui.components.ExoAppBar
import net.ib.mn.ui.components.ExoEmoticonPanel
import net.ib.mn.ui.components.ExoScaffold
import net.ib.mn.ui.theme.ColorPalette

/**
 * 하트픽 응원 댓글 화면 (CommentOnlyScreen)
 *
 * old 프로젝트: CommentOnlyActivity
 * common/CommentSection의 구조를 참고하되 하트픽 전용으로 구현
 */
@Composable
fun CommentOnlyScreen(
    heartPickId: Int,
    modifier: Modifier = Modifier,
    viewModel: CommentOnlyViewModel = hiltViewModel(),
    onBackClick: () -> Unit = {},
    onNavigateToFeed: ((Int, String?) -> Unit)? = null,
    onShowImageDetail: ((String) -> Unit)? = null,
    onCommentSubmitted: ((Int, Int) -> Unit)? = null,
    onCommentDeleted: (() -> Unit)? = null
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val cdnUrl by viewModel.cdnUrl.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val commentFocusRequester = remember { FocusRequester() }
    val listState = rememberLazyListState()

    // 시스템 백버튼 처리
    BackHandler {
        onBackClick()
    }

    // 초기 데이터 로드
    LaunchedEffect(heartPickId) {
        viewModel.sendIntent(CommentOnlyContract.Intent.LoadComments(heartPickId))
    }

    // Effect 처리
    LaunchedEffect(Unit) {
        viewModel.effect.collectLatest { effect ->
            when (effect) {
                is CommentOnlyContract.Effect.ShowToast -> {
                    Toast.makeText(context, effect.message, Toast.LENGTH_SHORT).show()
                }
                is CommentOnlyContract.Effect.ShowToastRes -> {
                    Toast.makeText(context, effect.messageResId, Toast.LENGTH_SHORT).show()
                }
                is CommentOnlyContract.Effect.NavigateToFeed -> {
                    onNavigateToFeed?.invoke(effect.userId, effect.nickname)
                }
                is CommentOnlyContract.Effect.ShowImageDetail -> {
                    onShowImageDetail?.invoke(effect.imageUrl)
                }
                is CommentOnlyContract.Effect.CommentSubmitted -> {
                    onCommentSubmitted?.invoke(effect.heartPickId, effect.commentCount)
                }
                is CommentOnlyContract.Effect.ScrollToTop -> {
                    listState.animateScrollToItem(0)
                }
                is CommentOnlyContract.Effect.NavigateBack -> {
                    onBackClick()
                }
                is CommentOnlyContract.Effect.CommentDeleted -> {
                    onCommentDeleted?.invoke()
                }
                is CommentOnlyContract.Effect.CommentReported -> {
                    Toast.makeText(context, R.string.report_done, Toast.LENGTH_SHORT).show()
                }
                is CommentOnlyContract.Effect.ReportError -> {
                    val messageRes = when (effect.gcode) {
                        2501 -> R.string.comment_report_error_2501
                        2502, 2202 -> R.string.failed_to_report_2202
                        2503, 2203 -> R.string.failed_to_report_2203
                        2504, 2204 -> R.string.not_enough_heart
                        else -> R.string.desc_failed_to_connect_internet
                    }
                    Toast.makeText(context, messageRes, Toast.LENGTH_SHORT).show()
                }
                is CommentOnlyContract.Effect.CopyCompleted -> {
                    Toast.makeText(context, R.string.copied_to_clipboard, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    // 스크롤 끝 감지 (더 불러오기)
    val shouldLoadMore by remember {
        derivedStateOf {
            val lastVisibleItem = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            val totalItems = listState.layoutInfo.totalItemsCount
            lastVisibleItem >= totalItems - 3 && state.hasMore && !state.isLoading
        }
    }

    LaunchedEffect(shouldLoadMore) {
        if (shouldLoadMore) {
            viewModel.sendIntent(CommentOnlyContract.Intent.LoadMoreComments)
        }
    }

    // CommentInput 높이: 약 56dp
    val commentInputHeight = 56.dp
    // ExoEmoticonPanel 높이: 280dp (visible일 때)
    val emoticonPanelHeight = if (state.showEmoticonPanel) 280.dp else 0.dp

    ExoScaffold(
        modifier = modifier,
        topBar = {
            ExoAppBar(
                title = stringResource(R.string.cheering_comments),
                onNavigationClick = onBackClick
            )
        }
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(ColorPalette.background100)
            ) {
                // 댓글 목록
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    commentItems(
                        comments = state.comments,
                        isLoading = state.isLoading && state.comments.isEmpty(),
                        isEmpty = state.comments.isEmpty() && !state.isLoading,
                        hasMore = state.hasMore,
                        currentUserId = viewModel.currentUserId,
                        currentUserLevel = viewModel.currentUserLevel,
                        articleAuthorId = 0, // 하트픽 댓글은 게시글 작성자 개념 없음
                        cdnUrl = cdnUrl,
                        useTranslation = viewModel.useTranslation,
                        translationLocales = viewModel.translationLocales,
                        onLoadMore = {
                            viewModel.sendIntent(CommentOnlyContract.Intent.LoadMoreComments)
                        },
                        onProfileClick = { comment ->
                            viewModel.sendIntent(CommentOnlyContract.Intent.OnProfileClick(comment))
                        },
                        onReportClick = { comment ->
                            viewModel.sendIntent(CommentOnlyContract.Intent.ReportComment(comment.id))
                        },
                        onDeleteClick = { comment ->
                            viewModel.sendIntent(CommentOnlyContract.Intent.DeleteComment(comment.id))
                        },
                        onImageClick = { imageUrl ->
                            viewModel.sendIntent(CommentOnlyContract.Intent.OnImageClick(imageUrl))
                        },
                        onTranslateClick = { comment ->
                            viewModel.translateComment(comment.id)
                        },
                        onTranslatableChecked = { commentId, isTranslatable ->
                            viewModel.updateCommentTranslatable(commentId, isTranslatable)
                        }
                    )
                }

                // 댓글 입력 (old: view_comment)
                CommentInput(
                    value = state.commentText,
                    onValueChange = { text ->
                        viewModel.sendIntent(CommentOnlyContract.Intent.SetCommentText(text))
                    },
                    onSubmit = {
                        viewModel.sendIntent(CommentOnlyContract.Intent.SubmitComment)
                    },
                    onEmoticonClick = {
                        // 이모티콘 버튼 클릭 시 키보드 내리고 패널 토글
                        keyboardController?.hide()
                        focusManager.clearFocus()
                        viewModel.sendIntent(CommentOnlyContract.Intent.ToggleEmoticonPanel)
                    },
                    isEmoticonPanelOpen = state.showEmoticonPanel,
                    selectedEmoticonUrl = state.selectedEmoticonUrl,
                    selectedImageUri = state.selectedImageUri,
                    focusRequester = commentFocusRequester,
                    onInputFocused = {
                        // 입력창 포커스 시 이모티콘 패널 닫기
                        viewModel.sendIntent(CommentOnlyContract.Intent.HideEmoticonPanel)
                    },
                    isLoading = state.isSubmitting
                )

                // 이모티콘 패널 (old: rl_emoticon)
                ExoEmoticonPanel(
                    visible = state.showEmoticonPanel,
                    selectedEmoticonId = state.selectedEmoticonId ?: -1,
                    onEmoticonSelected = { emoticon ->
                        val url = emoticon.imageUrl.ifEmpty { emoticon.thumbnail }
                        viewModel.sendIntent(CommentOnlyContract.Intent.SelectEmoticon(emoticon.id, url))
                        // 이모티콘 선택 후 패널 닫고 댓글창 포커스
                        viewModel.sendIntent(CommentOnlyContract.Intent.HideEmoticonPanel)
                        commentFocusRequester.requestFocus()
                    },
                    onEmoticonDoubleClick = { emoticon ->
                        // 같은 이모티콘 더블 클릭시 바로 전송 (old 프로젝트와 동일)
                        val url = emoticon.imageUrl.ifEmpty { emoticon.thumbnail }
                        viewModel.sendIntent(CommentOnlyContract.Intent.SelectEmoticon(emoticon.id, url))
                        viewModel.sendIntent(CommentOnlyContract.Intent.SubmitComment)
                    }
                )
            }

            // 이모티콘 프리뷰 (old: cl_preview - CommentInput 바로 위에 겹쳐서 표시)
            state.selectedEmoticonUrl?.takeIf { it.isNotEmpty() }?.let { url ->
                EmoticonPreview(
                    emoticonUrl = url,
                    onClose = {
                        viewModel.sendIntent(CommentOnlyContract.Intent.ClearEmoticon)
                    },
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = commentInputHeight + emoticonPanelHeight)
                )
            }

            // 전체 화면 로딩 오버레이 (댓글 작성 중)
            if (state.isSubmitting) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(ColorPalette.textDefault.copy(alpha = 0.3f)),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        color = ColorPalette.main,
                        modifier = Modifier.size(48.dp)
                    )
                }
            }
        }
    }
}
