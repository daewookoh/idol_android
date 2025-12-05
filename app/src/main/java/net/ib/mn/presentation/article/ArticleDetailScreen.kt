package net.ib.mn.presentation.article

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import com.theartofdev.edmodo.cropper.CropImage
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.hilt.navigation.compose.hiltViewModel
import net.ib.mn.R
import net.ib.mn.domain.model.ArticleModel
import net.ib.mn.domain.model.CommentModel
import net.ib.mn.presentation.common.ArticleType
import net.ib.mn.presentation.common.ExoArticle
import net.ib.mn.presentation.common.ExoArticleNavigation
import net.ib.mn.presentation.common.ExoArticleViewModel
import net.ib.mn.ui.components.ExoAppBar
import net.ib.mn.ui.components.ExoBottomSheetAction
import net.ib.mn.ui.components.ExoBottomSheetActionItem
import net.ib.mn.presentation.common.CommentInput
import net.ib.mn.presentation.common.CommentSectionViewModel
import net.ib.mn.presentation.common.EmoticonPreview
import net.ib.mn.presentation.common.ImagePreview
import net.ib.mn.presentation.common.commentItems
import net.ib.mn.ui.components.ExoConfirmDialog
import net.ib.mn.ui.components.ExoEmoticonPanel
import net.ib.mn.ui.components.ExoErrorDialog
import net.ib.mn.ui.components.ExoScaffold
import net.ib.mn.ui.theme.ColorPalette
import net.ib.mn.util.Constants
import net.ib.mn.util.LocaleUtil
import net.ib.mn.util.ServerUrl

/**
 * ArticleDetailScreen - 게시글 상세 화면
 */
@Composable
fun ArticleDetailScreen(
    article: ArticleModel,
    isFeed: Boolean = false, // FeedSubPage에서 진입한 경우 true (하트 투표 표시)
    externalTabName: String? = null, // 외부에서 전달된 아이돌 이름 (다국어 적용, 태그 표시용)
    onBackClick: () -> Unit = {},
    onArticleUpdated: (ArticleModel) -> Unit = {},
    onArticleDeleted: (() -> Unit)? = null,
    onNavigateToProfile: (userId: Int, nickname: String, imageUrl: String?, level: Int, mostIdolName: String?) -> Unit = { _, _, _, _, _ -> },
    onNavigateToPhotoDetail: (ArticleModel, Int) -> Unit = { _, _ -> },
    articleViewModel: ExoArticleViewModel = hiltViewModel(),
    commentViewModel: CommentSectionViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val commentFocusRequester = remember { FocusRequester() }

    // 게시글 상태 (로컬에서 관리하여 댓글 수 등 업데이트 반영)
    var currentArticle by remember { mutableStateOf(article) }

    // 댓글 상태
    val commentUiState by commentViewModel.uiState.collectAsState()
    val commentText by commentViewModel.commentText.collectAsState()
    val isFullScreenLoading by commentViewModel.isFullScreenLoading.collectAsState()
    val scrollToTopEvent by commentViewModel.scrollToTopEvent.collectAsState()
    val commentCountDelta by commentViewModel.commentCountDelta.collectAsState()
    val showEmoticonPanel by commentViewModel.showEmoticonPanel.collectAsState()
    val selectedEmoticonId by commentViewModel.selectedEmoticonId.collectAsState()
    val selectedEmoticonUrl by commentViewModel.selectedEmoticonUrl.collectAsState()
    val selectedImageUri by commentViewModel.selectedImageUri.collectAsState()
    val isGifImage by commentViewModel.isGifImage.collectAsState()
    val cdnUrl by commentViewModel.cdnUrl.collectAsState()
    val listState = rememberLazyListState()

    // 선택된 원본 이미지 URI (크롭용)
    var pendingCropUri by remember { mutableStateOf<Uri?>(null) }

    // CropImage 결과 launcher (old: CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE)
    val cropImageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val cropResult = CropImage.getActivityResult(result.data)
            cropResult?.uri?.let { croppedUri ->
                try {
                    // 크롭된 이미지 바이트 읽기
                    val inputStream = context.contentResolver.openInputStream(croppedUri)
                    val bytes = inputStream?.readBytes()
                    inputStream?.close()

                    if (bytes != null) {
                        // 리사이즈 (old: onArticlePhotoSelected)
                        val resizedBytes = resizeImage(bytes, 1080)
                        commentViewModel.selectImage(croppedUri, resizedBytes, false)
                    }
                } catch (e: Exception) {
                    Toast.makeText(context, R.string.image_permission_error, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    // 이미지 피커 launcher (old: btnGallery 클릭 -> UtilK.getPhoto)
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { selectedUri ->
            try {
                // 이미지 바이트 읽기
                val inputStream = context.contentResolver.openInputStream(selectedUri)
                val bytes = inputStream?.readBytes()
                inputStream?.close()

                if (bytes != null) {
                    // GIF 여부 확인 (old: cropArticlePhoto에서 GIF 헤더 검사)
                    val header = bytes.take(6).toByteArray()
                    val gif87a = "GIF87a".toByteArray()
                    val gif89a = "GIF89a".toByteArray()
                    val isGif = header.contentEquals(gif87a) || header.contentEquals(gif89a)

                    if (isGif) {
                        // GIF는 크롭 없이 그대로 사용
                        commentViewModel.selectImage(selectedUri, bytes, true)
                    } else {
                        // 일반 이미지는 CropImage로 크롭 (old: chooseInternalEditor)
                        pendingCropUri = selectedUri
                        val cropIntent = CropImage.activity(selectedUri)
                            .setAllowFlipping(false)
                            .setAllowRotation(false)
                            .setAllowCounterRotation(false)
                            .setInitialCropWindowPaddingRatio(0f)
                            .getIntent(context)
                        cropImageLauncher.launch(cropIntent)
                    }
                }
            } catch (e: Exception) {
                Toast.makeText(context, R.string.image_permission_error, Toast.LENGTH_SHORT).show()
            }
        }
    }

    // 화면 진입 시 최신 article 데이터 로드 및 댓글 초기 로드
    LaunchedEffect(article.id) {
        val articleIdLong = article.id.toLongOrNull() ?: return@LaunchedEffect

        // 댓글 입력 영역 초기화 (이전 상태 클리어)
        commentViewModel.resetInputState()

        // 최신 article 데이터 로드
        articleViewModel.loadArticle(
            articleId = articleIdLong,
            onSuccess = { latestArticle ->
                currentArticle = latestArticle
                onArticleUpdated(latestArticle)
            }
        )

        // 댓글 초기 로드
        commentViewModel.loadComments(articleIdLong)
    }

    // 댓글 수 변경 시 게시글 업데이트 및 백스택 동기화
    LaunchedEffect(commentCountDelta) {
        if (commentCountDelta != 0) {
            val newCommentCount = (currentArticle.commentCount + commentCountDelta).coerceAtLeast(0)
            currentArticle = currentArticle.copy(commentCount = newCommentCount)
            onArticleUpdated(currentArticle)
            commentViewModel.consumeCommentCountDelta()
        }
    }

    // 댓글 더 불러오기 (스크롤 끝 감지)
    val shouldLoadMoreComments by remember {
        derivedStateOf {
            val lastVisibleItem = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            val totalItems = listState.layoutInfo.totalItemsCount
            lastVisibleItem >= totalItems - 3 && commentUiState.hasMore && !commentUiState.isLoading
        }
    }

    LaunchedEffect(shouldLoadMoreComments) {
        if (shouldLoadMoreComments) {
            commentViewModel.loadMoreComments()
        }
    }

    // 댓글 작성 완료 후 첫 댓글로 스크롤 (index 0은 게시글, index 1이 첫 댓글)
    LaunchedEffect(scrollToTopEvent) {
        if (scrollToTopEvent) {
            listState.animateScrollToItem(1) // 첫 번째 댓글 위치
            commentViewModel.consumeScrollToTopEvent()
        }
    }

    // 메뉴 관련 상태
    var showMoreBottomSheet by remember { mutableStateOf(false) }
    var showReportDialog by remember { mutableStateOf(false) }
    var showReportErrorDialog by remember { mutableStateOf(false) }
    var reportErrorMessage by remember { mutableStateOf("") }
    var showDeleteDialog by remember { mutableStateOf(false) }

    // 댓글 삭제 관련 상태
    var showCommentDeleteDialog by remember { mutableStateOf(false) }
    var commentToDelete by remember { mutableStateOf<CommentModel?>(null) }

    // 댓글 신고 관련 상태
    var showCommentReportDialog by remember { mutableStateOf(false) }
    var showCommentReportErrorDialog by remember { mutableStateOf(false) }
    var commentReportErrorMessage by remember { mutableStateOf("") }
    var commentToReport by remember { mutableStateOf<CommentModel?>(null) }

    // 댓글 길이 validation 상태
    var showCommentLengthDialog by remember { mutableStateOf(false) }

    // 작성자 여부 확인
    val myUserId = articleViewModel.myUserId
    val isAdmin = articleViewModel.isAdmin
    val isMine = myUserId != null && article.user?.id == myUserId

    // ExoArticle 네비게이션 이벤트 처리
    LaunchedEffect(Unit) {
        articleViewModel.navigationEvent.collect { event ->
            when (event) {
                is ExoArticleNavigation.Profile -> {
                    onNavigateToProfile(
                        event.userId,
                        event.nickname,
                        event.imageUrl,
                        event.level,
                        event.mostIdolName
                    )
                }
                is ExoArticleNavigation.MediaDetail -> {
                    onNavigateToPhotoDetail(event.article, event.mediaIndex)
                }
                else -> { /* 다른 이벤트는 무시 */ }
            }
        }
    }

    // 백버튼 처리: 키보드/포커스/이모티콘 패널이 열려있으면 먼저 닫기
    BackHandler {
        when {
            showEmoticonPanel -> {
                // 이모티콘 패널이 열려있으면 먼저 닫기
                commentViewModel.hideEmoticonPanel()
            }
            else -> {
                // 키보드 내리고 포커스 해제 후 백버튼 처리
                keyboardController?.hide()
                focusManager.clearFocus()
                onBackClick()
            }
        }
    }

    ExoScaffold(
        excludeBottomPadding = true,
        topBar = {
            ExoAppBar(
                title = stringResource(R.string.post_detail),
                onNavigationClick = onBackClick,
                actions = {
                    IconButton(onClick = { showMoreBottomSheet = true }) {
                        Icon(
                            painter = painterResource(R.drawable.icon_view_more),
                            contentDescription = "Menu",
                            tint = ColorPalette.textDefault
                        )
                    }
                }
            )
        }
    ) {
        // isFeed 파라미터로 타입 결정
        // FeedSubPage에서 진입: FEED (하트 투표 있음)
        // 그 외 (FreeBoardPage, FanTalkSubPage 등): FREE_BOARD (하트 투표 없음)
        val detailType = if (isFeed) ArticleType.FEED else ArticleType.FREE_BOARD

        // Navigation bar 높이
        val navigationBarHeight = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(ColorPalette.background100)
        ) {
            // 스크롤 가능한 콘텐츠 (게시글 + 댓글 목록) - 키보드에 영향받지 않음
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                // 하단에 CommentInput 높이(약 56dp) + navigation bar 높이 + 여유 공간
                contentPadding = PaddingValues(bottom = 60.dp + navigationBarHeight)
            ) {
                // 게시글
                item(key = "article") {
                    ExoArticle(
                        article = currentArticle,
                        type = detailType,
                        isVisible = true,
                        externalTabName = externalTabName,
                        onArticleUpdated = { updatedArticle ->
                            currentArticle = updatedArticle
                            onArticleUpdated(updatedArticle)
                        },
                        viewModel = articleViewModel
                    )
                }

                // 댓글 목록
                commentItems(
                    comments = commentUiState.comments,
                    isLoading = commentUiState.isLoading,
                    isEmpty = commentUiState.comments.isEmpty() && !commentUiState.isLoading,
                    hasMore = commentUiState.hasMore,
                    currentUserId = myUserId ?: 0,
                    currentUserLevel = if (isAdmin) Constants.LEVEL_ADMIN else 0,
                    articleAuthorId = currentArticle.user?.id ?: 0,
                    cdnUrl = cdnUrl,
                    useTranslation = commentViewModel.useTranslation,
                    translationLocales = commentViewModel.translationLocales,
                    onLoadMore = { commentViewModel.loadMoreComments() },
                    onProfileClick = { comment ->
                        comment.user?.let { user ->
                            onNavigateToProfile(
                                user.id,
                                user.nickname ?: "",
                                user.imageUrlCommunity,
                                user.level,
                                null
                            )
                        }
                    },
                    onReportClick = { comment ->
                        commentToReport = comment
                        showCommentReportDialog = true
                    },
                    onDeleteClick = { comment ->
                        commentToDelete = comment
                        showCommentDeleteDialog = true
                    },
                    onTranslateClick = { comment ->
                        commentViewModel.translateComment(comment.id)
                    },
                    onTranslatableChecked = { commentId, isTranslatable ->
                        commentViewModel.updateCommentTranslatable(commentId, isTranslatable)
                    }
                )
            }

            // 하단 입력 영역 - 키보드와 함께 움직임
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
            ) {
                // 이미지 프리뷰 (CommentInput 위에 표시) - old: cl_preview
                selectedImageUri?.let { uri ->
                    ImagePreview(
                        imageUri = uri,
                        isGif = isGifImage,
                        onClose = { commentViewModel.clearImage() }
                    )
                }

                // 이모티콘 프리뷰 (CommentInput 위에 표시)
                selectedEmoticonUrl?.takeIf { it.isNotEmpty() }?.let { url ->
                    EmoticonPreview(
                        emoticonUrl = url,
                        onClose = { commentViewModel.clearEmoticon() }
                    )
                }

                // 댓글 입력창
                CommentInput(
                    value = commentText,
                    onValueChange = commentViewModel::setCommentText,
                    onSubmit = {
                        val articleIdLong = article.id.toLongOrNull() ?: return@CommentInput
                        // 이모티콘/이미지 없이 텍스트만 있는 경우 5글자 이상 필요
                        val hasEmoticon = !selectedEmoticonUrl.isNullOrEmpty()
                        val hasImage = selectedImageUri != null
                        if (!hasEmoticon && !hasImage && commentText.trim().length < 5) {
                            showCommentLengthDialog = true
                            return@CommentInput
                        }
                        commentViewModel.submitComment(articleIdLong)
                    },
                    onAttachClick = {
                        // 첨부 버튼 클릭 시 이미지 피커 열기 (old: btnGallery)
                        imagePickerLauncher.launch("image/*")
                    },
                    onEmoticonClick = {
                        // 이모티콘 버튼 클릭 시 키보드 내리고 패널 토글
                        keyboardController?.hide()
                        focusManager.clearFocus()
                        commentViewModel.toggleEmoticonPanel()
                    },
                    isEmoticonPanelOpen = showEmoticonPanel,
                    selectedEmoticonUrl = selectedEmoticonUrl,
                    selectedImageUri = selectedImageUri,
                    focusRequester = commentFocusRequester,
                    onInputFocused = {
                        // 입력창 포커스 시 이모티콘 패널 닫기
                        commentViewModel.hideEmoticonPanel()
                    }
                )

                // 이모티콘 패널 (댓글 입력창 아래에 표시)
                ExoEmoticonPanel(
                    visible = showEmoticonPanel,
                    onEmoticonSelected = { emoticon ->
                        val url = emoticon.imageUrl.ifEmpty { emoticon.thumbnail }
                        commentViewModel.selectEmoticon(emoticon.id, url)
                        // 이모티콘 선택 후 패널 닫고 댓글창 포커스
                        commentViewModel.hideEmoticonPanel()
                        commentFocusRequester.requestFocus()
                    }
                )

                // Navigation bar + IME 영역 (배경색 적용)
                Spacer(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(ColorPalette.background100)
                        .windowInsetsBottomHeight(
                            if (!showEmoticonPanel)
                                WindowInsets.navigationBars.union(WindowInsets.ime)
                            else
                                WindowInsets.navigationBars
                        )
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

    // 더보기 바텀시트
    if (showMoreBottomSheet) {
        val actionItems = buildList {
            // 작성자 또는 관리자: 수정, 삭제
            if (isMine || isAdmin) {
                add(ExoBottomSheetActionItem(R.string.title_edit) {
                    articleViewModel.onEditArticle(article)
                })
                add(ExoBottomSheetActionItem(R.string.title_remove) {
                    showMoreBottomSheet = false
                    showDeleteDialog = true
                })
            }
            // 본인 게시글이 아닌 경우: 신고
            if (!isMine) {
                add(ExoBottomSheetActionItem(R.string.title_report) {
                    showMoreBottomSheet = false
                    showReportDialog = true
                })
            }
            // 공유는 모든 사용자에게 표시
            add(ExoBottomSheetActionItem(R.string.title_share) {
                shareArticle(context, article)
            })
        }

        ExoBottomSheetAction(
            items = actionItems,
            onDismissRequest = { showMoreBottomSheet = false }
        )
    }

    // 신고 확인 다이얼로그
    if (showReportDialog) {
        val reportHeart = articleViewModel.reportHeart
        // 하트 숫자를 빨간색으로 표시
        val reportMessage = buildAnnotatedString {
            val fullText = stringResource(R.string.msg_report_confirm, reportHeart)
            val heartText = reportHeart.toString()
            val startIndex = fullText.indexOf(heartText)
            if (startIndex >= 0) {
                append(fullText.substring(0, startIndex))
                withStyle(SpanStyle(color = ColorPalette.main)) {
                    append(heartText)
                }
                append(fullText.substring(startIndex + heartText.length))
            } else {
                append(fullText)
            }
        }
        ExoConfirmDialog(
            title = stringResource(R.string.title_report),
            message = reportMessage,
            confirmButtonText = stringResource(R.string.yes),
            dismissButtonText = stringResource(R.string.no),
            onConfirm = {
                showReportDialog = false
                articleViewModel.reportArticle(
                    articleId = article.id,
                    onSuccess = {
                        Toast.makeText(
                            context,
                            context.getString(R.string.report_done),
                            Toast.LENGTH_SHORT
                        ).show()
                    },
                    onError = { gcode ->
                        reportErrorMessage = when (gcode) {
                            ExoArticleViewModel.GCODE_ALREADY_REPORTED ->
                                context.getString(R.string.failed_to_report__already_reported)
                            ExoArticleViewModel.GCODE_DAILY_LIMIT ->
                                context.getString(R.string.failed_to_report_2202)
                            ExoArticleViewModel.GCODE_TIME_LIMIT ->
                                context.getString(R.string.failed_to_report_2203)
                            ExoArticleViewModel.GCODE_NOT_ENOUGH_HEART ->
                                context.getString(R.string.not_enough_heart)
                            else -> context.getString(R.string.error_abnormal_default)
                        }
                        showReportErrorDialog = true
                    }
                )
            },
            onDismiss = { showReportDialog = false }
        )
    }

    // 신고 에러 다이얼로그
    if (showReportErrorDialog) {
        ExoErrorDialog(
            message = reportErrorMessage,
            onDismiss = { showReportErrorDialog = false }
        )
    }

    // 삭제 확인 다이얼로그
    if (showDeleteDialog) {
        ExoConfirmDialog(
            title = stringResource(R.string.title_remove),
            message = stringResource(R.string.remove_desc),
            confirmButtonText = stringResource(R.string.yes),
            dismissButtonText = stringResource(R.string.no),
            onConfirm = {
                showDeleteDialog = false
                articleViewModel.deleteArticle(
                    articleId = article.id,
                    onSuccess = {
                        onArticleDeleted?.invoke()
                    },
                    onError = {
                        Toast.makeText(
                            context,
                            context.getString(R.string.error_abnormal_default),
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                )
            },
            onDismiss = { showDeleteDialog = false }
        )
    }

    // 댓글 삭제 확인 다이얼로그
    if (showCommentDeleteDialog && commentToDelete != null) {
        ExoConfirmDialog(
            title = stringResource(R.string.title_remove),
            message = stringResource(R.string.remove_desc),
            confirmButtonText = stringResource(R.string.yes),
            dismissButtonText = stringResource(R.string.no),
            onConfirm = {
                showCommentDeleteDialog = false
                commentToDelete?.let { comment ->
                    commentViewModel.deleteComment(comment.id)
                }
                commentToDelete = null
            },
            onDismiss = {
                showCommentDeleteDialog = false
                commentToDelete = null
            }
        )
    }

    // 댓글 신고 확인 다이얼로그
    if (showCommentReportDialog && commentToReport != null) {
        val commentReportHeart = commentViewModel.reportHeart
        // 하트 숫자를 빨간색으로 표시
        val commentReportMessage = buildAnnotatedString {
            val fullText = stringResource(R.string.msg_report_confirm, commentReportHeart)
            val heartText = commentReportHeart.toString()
            val startIndex = fullText.indexOf(heartText)
            if (startIndex >= 0) {
                append(fullText.substring(0, startIndex))
                withStyle(SpanStyle(color = ColorPalette.main)) {
                    append(heartText)
                }
                append(fullText.substring(startIndex + heartText.length))
            } else {
                append(fullText)
            }
        }
        ExoConfirmDialog(
            title = stringResource(R.string.title_report),
            message = commentReportMessage,
            confirmButtonText = stringResource(R.string.yes),
            dismissButtonText = stringResource(R.string.no),
            onConfirm = {
                showCommentReportDialog = false
                commentToReport?.let { comment ->
                    commentViewModel.reportComment(
                        commentId = comment.id,
                        onSuccess = {
                            Toast.makeText(
                                context,
                                context.getString(R.string.report_done),
                                Toast.LENGTH_SHORT
                            ).show()
                        },
                        onError = { gcode ->
                            commentReportErrorMessage = when (gcode) {
                                CommentSectionViewModel.GCODE_ALREADY_REPORTED ->
                                    context.getString(R.string.comment_report_error_2501)
                                CommentSectionViewModel.GCODE_DAILY_LIMIT ->
                                    context.getString(R.string.failed_to_report_2202)
                                CommentSectionViewModel.GCODE_TIME_LIMIT ->
                                    context.getString(R.string.failed_to_report_2203)
                                CommentSectionViewModel.GCODE_NOT_ENOUGH_HEART ->
                                    context.getString(R.string.not_enough_heart)
                                else -> context.getString(R.string.error_abnormal_default)
                            }
                            showCommentReportErrorDialog = true
                        }
                    )
                }
                commentToReport = null
            },
            onDismiss = {
                showCommentReportDialog = false
                commentToReport = null
            }
        )
    }

    // 댓글 신고 에러 다이얼로그
    if (showCommentReportErrorDialog) {
        ExoErrorDialog(
            message = commentReportErrorMessage,
            onDismiss = { showCommentReportErrorDialog = false }
        )
    }

    // 댓글 길이 부족 다이얼로그
    if (showCommentLengthDialog) {
        ExoErrorDialog(
            message = stringResource(R.string.comment_minimum_characters, 5),
            onDismiss = { showCommentLengthDialog = false }
        )
    }
}

/**
 * 게시글 공유 (old 프로젝트와 동일한 방식)
 */
private fun shareArticle(context: android.content.Context, article: ArticleModel) {
    val locale = LocaleUtil.getWikiLocale(context)
    val shareUrl = "${ServerUrl.HOST}/articles/${article.id}/?locale=$locale"

    // 공유 메시지: 내용 30자 + 아이돌 이름
    val contentPreview = article.content?.take(30)?.trim() ?: ""
    val idolName = article.idol?.let { LocaleUtil.getLocalizedIdolName(context, it) } ?: ""

    val shareMsg = buildString {
        if (contentPreview.isNotEmpty()) {
            append(contentPreview)
            if ((article.content?.length ?: 0) > 30) append("...")
        }
        if (idolName.isNotEmpty()) {
            if (isNotEmpty()) append(" - ")
            append(idolName)
        }
    }

    val shareText = if (shareMsg.isNotEmpty()) {
        "$shareMsg\n$shareUrl"
    } else {
        shareUrl
    }

    val shareIntent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, shareText)
    }

    context.startActivity(
        Intent.createChooser(shareIntent, context.getString(R.string.title_share))
    )
}

/**
 * 이미지 리사이즈 (old: onArticlePhotoSelected와 유사)
 * 최대 크기를 넘지 않도록 비율 유지하며 리사이즈
 */
private fun resizeImage(bytes: ByteArray, maxSize: Int): ByteArray {
    return try {
        val options = BitmapFactory.Options().apply {
            inJustDecodeBounds = true
        }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)

        val width = options.outWidth
        val height = options.outHeight

        // 리사이즈가 필요 없으면 원본 반환
        if (width <= maxSize && height <= maxSize) {
            return bytes
        }

        // 샘플 사이즈 계산
        var sampleSize = 1
        while (width / sampleSize > maxSize || height / sampleSize > maxSize) {
            sampleSize *= 2
        }

        val decodeOptions = BitmapFactory.Options().apply {
            inSampleSize = sampleSize
        }
        val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, decodeOptions)
            ?: return bytes

        // 정확한 크기로 리사이즈
        val scaleFactor = minOf(maxSize.toFloat() / bitmap.width, maxSize.toFloat() / bitmap.height)
        val newWidth = (bitmap.width * scaleFactor).toInt()
        val newHeight = (bitmap.height * scaleFactor).toInt()

        val resizedBitmap = if (scaleFactor < 1f) {
            Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
        } else {
            bitmap
        }

        // JPEG로 압축
        val outputStream = java.io.ByteArrayOutputStream()
        resizedBitmap.compress(Bitmap.CompressFormat.JPEG, 85, outputStream)

        if (resizedBitmap != bitmap) {
            resizedBitmap.recycle()
        }
        bitmap.recycle()

        outputStream.toByteArray()
    } catch (e: Exception) {
        bytes // 에러 시 원본 반환
    }
}
