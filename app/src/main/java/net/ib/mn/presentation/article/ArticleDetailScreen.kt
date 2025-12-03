package net.ib.mn.presentation.article

import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import net.ib.mn.R
import net.ib.mn.domain.model.ArticleModel
import net.ib.mn.presentation.common.ArticleType
import net.ib.mn.presentation.common.ExoArticle
import net.ib.mn.presentation.common.ExoArticleNavigation
import net.ib.mn.presentation.common.ExoArticleViewModel
import net.ib.mn.ui.components.ExoAppBar
import net.ib.mn.ui.components.ExoBottomSheetAction
import net.ib.mn.ui.components.ExoBottomSheetActionItem
import net.ib.mn.ui.components.ExoConfirmDialog
import net.ib.mn.ui.components.ExoErrorDialog
import net.ib.mn.ui.components.ExoScaffold
import net.ib.mn.ui.theme.ColorPalette
import net.ib.mn.util.LocaleUtil
import net.ib.mn.util.ServerUrl

/**
 * ArticleDetailScreen - 게시글 상세 화면
 */
@Composable
fun ArticleDetailScreen(
    article: ArticleModel,
    onBackClick: () -> Unit = {},
    onArticleUpdated: (ArticleModel) -> Unit = {},
    onArticleDeleted: (() -> Unit)? = null,
    onNavigateToProfile: (userId: Int, nickname: String, imageUrl: String?, level: Int, mostIdolName: String?) -> Unit = { _, _, _, _, _ -> },
    onNavigateToPhotoDetail: (ArticleModel, Int) -> Unit = { _, _ -> },
    articleViewModel: ExoArticleViewModel = hiltViewModel()
) {
    val context = LocalContext.current

    // 메뉴 관련 상태
    var showMoreBottomSheet by remember { mutableStateOf(false) }
    var showReportDialog by remember { mutableStateOf(false) }
    var showReportErrorDialog by remember { mutableStateOf(false) }
    var reportErrorMessage by remember { mutableStateOf("") }
    var showDeleteDialog by remember { mutableStateOf(false) }

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

    BackHandler(onBack = onBackClick)

    ExoScaffold(
        topBar = {
            ExoAppBar(
                title = stringResource(R.string.post_detail),
                onNavigationClick = onBackClick,
                actions = {
                    Icon(
                        painter = painterResource(R.drawable.icon_view_more),
                        contentDescription = "Menu",
                        modifier = Modifier
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) {
                                showMoreBottomSheet = true
                            },
                        tint = ColorPalette.textDefault
                    )
                }
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
                    onDeleted = { onArticleDeleted?.invoke() },
                    viewModel = articleViewModel
                )
            }
            // TODO: 댓글 목록
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
        ExoConfirmDialog(
            title = stringResource(R.string.title_report),
            message = stringResource(R.string.msg_report_confirm, reportHeart),
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
