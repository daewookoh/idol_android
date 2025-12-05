package net.ib.mn.presentation.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import net.ib.mn.domain.model.CommentModel
import net.ib.mn.ui.components.ExoEmoticonPanel
import net.ib.mn.ui.theme.ColorPalette

/**
 * 댓글 목록 확장 함수 (LazyListScope용)
 *
 * LazyColumn 내부에서 사용:
 * ```kotlin
 * LazyColumn {
 *     item { ExoArticle(...) }
 *     commentItems(
 *         comments = comments,
 *         isLoading = isLoading,
 *         isEmpty = comments.isEmpty() && !isLoading
 *     )
 * }
 * ```
 */
fun LazyListScope.commentItems(
    comments: List<CommentModel>,
    isLoading: Boolean,
    isEmpty: Boolean,
    hasMore: Boolean = false,
    currentUserId: Int = 0,
    currentUserLevel: Int = 0,
    articleAuthorId: Int = 0,
    cdnUrl: String,
    useTranslation: Boolean = false,
    translationLocales: List<String> = emptyList(),
    onLoadMore: (() -> Unit)? = null,
    onProfileClick: ((CommentModel) -> Unit)? = null,
    onReportClick: ((CommentModel) -> Unit)? = null,
    onDeleteClick: ((CommentModel) -> Unit)? = null,
    onImageClick: ((String) -> Unit)? = null,
    onTranslateClick: ((CommentModel) -> Unit)? = null,
    onTranslatableChecked: ((Int, Boolean) -> Unit)? = null
) {
    // 빈 상태
    if (isEmpty && !isLoading) {
        item(key = "comment_empty") {
            CommentEmpty()
        }
    }

    // 댓글 목록
    items(
        items = comments,
        key = { it.id }
    ) { comment ->
        ExoCommentItem(
            comment = comment,
            currentUserId = currentUserId,
            currentUserLevel = currentUserLevel,
            articleAuthorId = articleAuthorId,
            cdnUrl = cdnUrl,
            useTranslation = useTranslation,
            translationLocales = translationLocales,
            onProfileClick = onProfileClick,
            onReportClick = onReportClick,
            onDeleteClick = onDeleteClick,
            onImageClick = onImageClick,
            onTranslateClick = onTranslateClick,
            onTranslatableChecked = onTranslatableChecked
        )
    }

    // 로딩 인디케이터 (초기 로딩 또는 더 불러오기)
    if (isLoading) {
        item(key = "comment_loading") {
            CommentLoading()
        }
    }

    // 더 불러오기 트리거
    if (hasMore && !isLoading && comments.isNotEmpty()) {
        item(key = "comment_load_more_trigger") {
            LaunchedEffect(Unit) {
                onLoadMore?.invoke()
            }
        }
    }
}

/**
 * CommentSection - 댓글 섹션 전체 컴포넌트 (LazyColumn 포함)
 *
 * 단독으로 사용할 때:
 * ```kotlin
 * CommentSection(
 *     articleId = article.id.toLongOrNull() ?: 0,
 *     viewModel = hiltViewModel()
 * )
 * ```
 */
@Composable
fun CommentSection(
    articleId: Long,
    articleAuthorId: Int = 0,
    currentUserId: Int = 0,
    currentUserLevel: Int = 0,
    viewModel: CommentSectionViewModel = hiltViewModel(),
    onProfileClick: ((CommentModel) -> Unit)? = null,
    onReportClick: ((CommentModel) -> Unit)? = null,
    onDeleteClick: ((CommentModel) -> Unit)? = null,
    onImageClick: ((String) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val commentFocusRequester = remember { FocusRequester() }

    val uiState by viewModel.uiState.collectAsState()
    val commentText by viewModel.commentText.collectAsState()
    val isSubmitting by viewModel.isSubmitting.collectAsState()
    val isFullScreenLoading by viewModel.isFullScreenLoading.collectAsState()
    val scrollToTopEvent by viewModel.scrollToTopEvent.collectAsState()

    // 이모티콘 관련 상태
    val showEmoticonPanel by viewModel.showEmoticonPanel.collectAsState()
    val selectedEmoticonUrl by viewModel.selectedEmoticonUrl.collectAsState()
    val cdnUrl by viewModel.cdnUrl.collectAsState()

    val listState = rememberLazyListState()

    // 초기 로드
    LaunchedEffect(articleId) {
        viewModel.loadComments(articleId)
    }

    // 댓글 작성 완료 후 첫 댓글로 스크롤
    LaunchedEffect(scrollToTopEvent) {
        if (scrollToTopEvent) {
            listState.animateScrollToItem(0)
            viewModel.consumeScrollToTopEvent()
        }
    }

    // 스크롤 끝 감지 (더 불러오기)
    val shouldLoadMore by remember {
        derivedStateOf {
            val lastVisibleItem = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            val totalItems = listState.layoutInfo.totalItemsCount
            lastVisibleItem >= totalItems - 3 && uiState.hasMore && !uiState.isLoading
        }
    }

    LaunchedEffect(shouldLoadMore) {
        if (shouldLoadMore) {
            viewModel.loadMoreComments()
        }
    }

    // CommentInput 높이: 약 56dp
    val commentInputHeight = 56.dp
    // ExoEmoticonPanel 높이: 280dp (visible일 때)
    val emoticonPanelHeight = if (showEmoticonPanel) 280.dp else 0.dp

    Box(modifier = modifier.fillMaxSize()) {
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
                    comments = uiState.comments,
                    isLoading = uiState.isLoading,
                    isEmpty = uiState.comments.isEmpty() && !uiState.isLoading,
                    hasMore = uiState.hasMore,
                    currentUserId = currentUserId,
                    currentUserLevel = currentUserLevel,
                    articleAuthorId = articleAuthorId,
                    cdnUrl = cdnUrl,
                    useTranslation = viewModel.useTranslation,
                    translationLocales = viewModel.translationLocales,
                    onLoadMore = { viewModel.loadMoreComments() },
                    onProfileClick = onProfileClick,
                    onReportClick = onReportClick,
                    onDeleteClick = onDeleteClick,
                    onImageClick = onImageClick,
                    onTranslateClick = { comment -> viewModel.translateComment(comment.id) },
                    onTranslatableChecked = { commentId, isTranslatable ->
                        viewModel.updateCommentTranslatable(commentId, isTranslatable)
                    }
                )
            }

            // 댓글 입력 (old: view_comment)
            CommentInput(
                value = commentText,
                onValueChange = viewModel::setCommentText,
                onSubmit = { viewModel.submitComment(articleId) },
                onEmoticonClick = {
                    // 이모티콘 버튼 클릭 시 키보드 내리고 패널 토글
                    keyboardController?.hide()
                    focusManager.clearFocus()
                    viewModel.toggleEmoticonPanel()
                },
                isEmoticonPanelOpen = showEmoticonPanel,
                selectedEmoticonUrl = selectedEmoticonUrl,
                focusRequester = commentFocusRequester,
                onInputFocused = {
                    // 입력창 포커스 시 이모티콘 패널 닫기
                    viewModel.hideEmoticonPanel()
                },
                isLoading = isSubmitting
            )

            // 이모티콘 패널 (old: rl_emoticon)
            ExoEmoticonPanel(
                visible = showEmoticonPanel,
                selectedEmoticonId = viewModel.selectedEmoticonId.collectAsState().value ?: -1,
                onEmoticonSelected = { emoticon ->
                    val url = emoticon.imageUrl.ifEmpty { emoticon.thumbnail }
                    viewModel.selectEmoticon(emoticon.id, url)
                    // 이모티콘 선택 후 패널 닫고 댓글창 포커스
                    viewModel.hideEmoticonPanel()
                    commentFocusRequester.requestFocus()
                },
                onEmoticonDoubleClick = { emoticon ->
                    // 같은 이모티콘 더블 클릭시 바로 전송 (old 프로젝트와 동일)
                    val url = emoticon.imageUrl.ifEmpty { emoticon.thumbnail }
                    viewModel.selectEmoticon(emoticon.id, url)
                    viewModel.submitComment(articleId)
                }
            )
        }

        // 이모티콘 프리뷰 (old: cl_preview - CommentInput 바로 위에 겹쳐서 표시)
        // 댓글 목록 위에 overlay로 표시되어 뒤가 투명하게 보임
        selectedEmoticonUrl?.takeIf { it.isNotEmpty() }?.let { url ->
            EmoticonPreview(
                emoticonUrl = url,
                onClose = { viewModel.clearEmoticon() },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = commentInputHeight + emoticonPanelHeight)
            )
        }

        // 전체 화면 로딩 오버레이 (댓글 작성 중)
        if (isFullScreenLoading) {
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
