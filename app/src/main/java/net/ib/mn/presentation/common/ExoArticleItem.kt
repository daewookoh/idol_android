package net.ib.mn.presentation.common

import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.offset
import androidx.hilt.navigation.compose.hiltViewModel
import coil.ImageLoader
import coil.compose.AsyncImage
import coil.decode.GifDecoder
import coil.decode.ImageDecoderDecoder
import coil.request.ImageRequest
import coil.request.repeatCount
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import net.ib.mn.R
import net.ib.mn.domain.model.ArticleFile
import net.ib.mn.domain.model.ArticleModel
import net.ib.mn.ui.components.ExoArticleVoteDialog
import net.ib.mn.ui.components.ExoBottomSheetAction
import net.ib.mn.ui.components.ExoBottomSheetActionItem
import net.ib.mn.ui.components.ExoConfirmDialog
import net.ib.mn.ui.components.ExoErrorDialog
import net.ib.mn.ui.components.ExoProfileImage
import net.ib.mn.ui.components.ExoVideoPlayer
import net.ib.mn.ui.components.ExoYouTubePlayer
import net.ib.mn.ui.components.ProfileImageType
import net.ib.mn.ui.theme.ColorPalette
import net.ib.mn.ui.theme.ExoTypo
import net.ib.mn.util.DateTimeUtil
import net.ib.mn.util.IdolImageUtil.toSecureUrl
import net.ib.mn.util.LocaleUtil
import net.ib.mn.util.MediaCacheUtil
import net.ib.mn.util.NumberFormatUtil
import net.ib.mn.util.ServerUrl
import net.ib.mn.util.YoutubeHelper
import java.util.concurrent.TimeUnit

/**
 * ExoArticleItem 타입 (목록용)
 */
enum class ArticleItemType {
    FEED,            // 커뮤니티 피드 - 하트투표, 좋아요, 댓글, 번역기능
    FREE_BOARD,      // 자유게시판/커뮤니티톡 - 좋아요, 댓글 (하트투표 없음)
    ADMIN_NOTICE;    // 관리자 공지 (핀 아이콘 + 제목)

    /** FEED 타입인지 여부 */
    val isFeed: Boolean
        get() = this == FEED

    /** FREE_BOARD 타입인지 여부 */
    val isFreeBoard: Boolean
        get() = this == FREE_BOARD

    /** ADMIN_NOTICE 타입인지 여부 */
    val isAdminNotice: Boolean
        get() = this == ADMIN_NOTICE
}

/**
 * ExoArticleItem - 목록용 게시글 아티클 컴포넌트
 *
 * @param article 게시글 데이터
 * @param type 게시글 타입 (FEED: 전체UI, FREE_BOARD: 컴팩트UI, ADMIN_NOTICE: 공지UI)
 * @param tagName FREE_BOARD 타입에서 표시할 태그 이름
 * @param isVisible 화면에 보이는지 여부 (GIF/비디오 최적화용)
 * @param showTranslation 번역 버튼 표시 여부
 * @param showPopularIcon 인기글 아이콘 표시 여부
 * @param onDeleted 삭제 콜백
 * @param onArticleUpdated 게시글 업데이트 콜백
 * @param viewModel ExoArticleViewModel
 * @param modifier Modifier
 */
@Composable
fun ExoArticleItem(
    article: ArticleModel,
    type: ArticleItemType = ArticleItemType.FEED,
    tagName: String? = null,
    isVisible: Boolean = true,
    showTranslation: Boolean = true,
    showPopularIcon: Boolean = false,
    onDeleted: ((String) -> Unit)? = null,
    onArticleUpdated: ((ArticleModel) -> Unit)? = null,
    viewModel: ExoArticleViewModel = hiltViewModel(),
    modifier: Modifier = Modifier
) {
    // ADMIN_NOTICE 타입: 공지사항 UI
    if (type.isAdminNotice) {
        val isDarkMode = isSystemInDarkTheme()
        ArticleItemNotice(
            article = article,
            onClick = { viewModel.navigateToNoticeDetail(article, isDarkMode) },
            modifier = modifier
        )
        return
    }

    // FREE_BOARD 타입: 컴팩트 UI
    if (type.isFreeBoard) {
        ArticleItemCompact(
            article = article,
            showPopularIcon = showPopularIcon,
            onClick = { viewModel.navigateToArticleDetail(article) },
            modifier = modifier
        )
        return
    }

    // FEED: 전체 UI

    val context = LocalContext.current

    // 로컬 상태 (즉시 UI 업데이트용)
    var localIsLiked by remember { mutableStateOf(article.isUserLike) }
    var localLikeCount by remember { mutableIntStateOf(article.likeCount) }
    var localHeartCount by remember { mutableLongStateOf(article.heart) }

    // article 값이 외부에서 변경되면 로컬 상태 동기화
    LaunchedEffect(article.id, article.isUserLike, article.likeCount, article.heart) {
        localIsLiked = article.isUserLike
        localLikeCount = article.likeCount
        localHeartCount = article.heart
    }

    // 상태 변경 시 콜백 호출 (백스택 데이터 동기화)
    LaunchedEffect(localIsLiked, localLikeCount, localHeartCount) {
        if (localIsLiked != article.isUserLike ||
            localLikeCount != article.likeCount ||
            localHeartCount != article.heart) {
            onArticleUpdated?.invoke(
                article.copy(
                    isUserLike = localIsLiked,
                    likeCount = localLikeCount,
                    heart = localHeartCount
                )
            )
        }
    }

    // 좋아요 클릭 throttling
    var lastLikeClickTime by remember { mutableLongStateOf(0L) }

    // 다이얼로그 상태
    var showVoteDialog by remember { mutableStateOf(false) }
    var showMoreBottomSheet by remember { mutableStateOf(false) }
    var showReportDialog by remember { mutableStateOf(false) }
    var showReportErrorDialog by remember { mutableStateOf(false) }
    var reportErrorMessage by remember { mutableStateOf("") }
    var showDeleteDialog by remember { mutableStateOf(false) }

    // 작성자 여부 확인
    val myUserId = viewModel.myUserId
    val isAdmin = viewModel.isAdmin
    val isMine = myUserId != null && article.user?.id == myUserId

    // 커뮤니티 이름 (COMMUNITY 타입에서 사용)
    val communityName = remember(article.idol) {
        article.idol?.let { LocaleUtil.getLocalizedIdolName(context, it) }
    }

    // 시간 표시: FEED는 전체 날짜+시간, 그 외는 KST 기준 오늘이면 시간만/아니면 날짜만
    val createdAt = remember(article.createdAt, type) {
        if (type.isFeed) {
            DateTimeUtil.formatFullDateKorea(article.createdAt)
        } else {
            DateTimeUtil.formatBoardDate(article.createdAt)
        }
    }

    // YouTube 링크 여부 확인
    val isYoutubeLink = YoutubeHelper.isYoutubeLink(article.linkUrl, article.imageUrl, article.umjjalUrl)
    var isExpanded by remember { mutableStateOf(false) }

    // 프로필 이미지 URL
    val profileImageUrl = article.user?.imageUrlCommunity

    // 좋아요 클릭 핸들러
    val onLikeClick: () -> Unit = {
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastLikeClickTime >= 1000L) {
            lastLikeClickTime = currentTime
            val newLiked = !localIsLiked
            localIsLiked = newLiked
            localLikeCount = if (newLiked) localLikeCount + 1 else (localLikeCount - 1).coerceAtLeast(0)
            viewModel.postLike(article.id, newLiked)
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(ColorPalette.gray80)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(ColorPalette.background100)
        ) {
            // 1. 사용자 프로필 섹션
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 20.dp, top = 13.dp, end = 11.dp, bottom = 11.dp),
                verticalAlignment = Alignment.Top
            ) {
                // 프로필 이미지
                Box(
                    modifier = Modifier.clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        article.user?.let { user ->
                            val mostIdolName = user.most?.let { most ->
                                LocaleUtil.getLocalizedIdolName(context, most)
                            }
                            viewModel.navigateToProfile(
                                userId = user.id,
                                nickname = user.nickname ?: "",
                                imageUrl = user.imageUrlCommunity,
                                level = user.level,
                                mostIdolName = mostIdolName
                            )
                        }
                    }
                ) {
                    ExoProfileImage(
                        imageUrl = profileImageUrl,
                        type = ProfileImageType.SMALL
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                // 이름, 레벨, 시간
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // 이모티콘
                        val userEmoticonUrl = article.user?.emoticon?.emojiUrl
                        if (!userEmoticonUrl.isNullOrEmpty()) {
                            AsyncImage(
                                model = userEmoticonUrl,
                                contentDescription = "Emoticon",
                                modifier = Modifier
                                    .size(20.dp)
                                    .padding(end = 1.dp),
                                contentScale = ContentScale.Fit
                            )
                        }

                        // 레벨 아이콘
                        val userLevel = article.user?.level ?: 0
                        val maxLevel = 40
                        val clampedLevel = userLevel.coerceIn(0, maxLevel)
                        val levelIconRes = context.resources.getIdentifier(
                            "icon_level_$clampedLevel",
                            "drawable",
                            context.packageName
                        ).takeIf { it != 0 } ?: R.drawable.icon_level_0

                        Image(
                            painter = painterResource(levelIconRes),
                            contentDescription = "Level $userLevel",
                            modifier = Modifier.padding(top = 4.dp, end = 2.dp)
                        )

                        // 몰빵일 뱃지 (레벨 아이콘 우측)
                        if (article.user?.hasAllInDayBadge == true) {
                            Image(
                                painter = painterResource(R.drawable.allinbadge_level_icon),
                                contentDescription = "All-in Day Badge",
                                modifier = Modifier.padding(top = 4.dp, end = 2.dp)
                            )
                        }

                        Text(
                            text = article.user?.nickname ?: "",
                            style = ExoTypo.body14Main,
                            modifier = Modifier.clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) {
                                article.user?.let { user ->
                                    val mostIdolName = user.most?.let { most ->
                                        LocaleUtil.getLocalizedIdolName(context, most)
                                    }
                                    viewModel.navigateToProfile(
                                        userId = user.id,
                                        nickname = user.nickname ?: "",
                                        imageUrl = user.imageUrlCommunity,
                                        level = user.level,
                                        mostIdolName = mostIdolName
                                    )
                                }
                            }
                        )
                    }

                    // 작성 시간
                    Row(
                        verticalAlignment = Alignment.Bottom,
                        modifier = Modifier.padding(top = 5.dp)
                    ) {
                        Text(
                            text = createdAt,
                            style = ExoTypo.body12
                        )

                        if (article.isMostOnly == "Y") {
                            Spacer(modifier = Modifier.width(7.dp))
                            Icon(
                                painter = painterResource(R.drawable.icon_onlymyidol),
                                contentDescription = "Only my idol",
                                modifier = Modifier
                                    .height(13.dp)
                                    .padding(bottom = 1.dp),
                                tint = Color.Unspecified
                            )
                        }
                    }
                }

                // 더보기 버튼
                Icon(
                    painter = painterResource(R.drawable.icon_view_more),
                    contentDescription = "More",
                    modifier = Modifier
                        .padding(4.dp)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            showMoreBottomSheet = true
                        },
                    tint = Color.Unspecified
                )
            }

            // 2. 태그 (FREE_BOARD에서만 표시)
            if (type.isFreeBoard) {
                val tag = tagName ?: viewModel.getTagName(article.tagId)
                if (!tag.isNullOrEmpty()) {
                    Box(
                        modifier = Modifier
                            .padding(start = 20.dp, top = 9.dp, end = 20.dp, bottom = 3.dp)
                            .background(
                                color = ColorPalette.main200,
                                shape = RoundedCornerShape(6.dp)
                            )
                            .padding(horizontal = 7.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = tag,
                            style = ExoTypo.label13.copy(fontWeight = FontWeight.Medium)
                        )
                    }
                }
            }

            // 3. 제목
            if (!article.title.isNullOrEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 20.dp, end = 20.dp, bottom = 21.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (article.isPopular) {
                        Icon(
                            painter = painterResource(R.drawable.icon_popularpost_title),
                            contentDescription = "Popular post",
                            modifier = Modifier.padding(end = 5.dp),
                            tint = Color.Unspecified
                        )
                    }

                    if (article.isMostOnly == "Y") {
                        Icon(
                            painter = painterResource(R.drawable.icon_onlymyidol),
                            contentDescription = "Only my idol",
                            modifier = Modifier.padding(end = 5.dp),
                            tint = Color.Unspecified
                        )
                    }

                    Text(
                        text = article.title!!,
                        style = ExoTypo.title15Default,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // 4. 내용
            val content = article.content ?: ""
            val displayContent = if (isYoutubeLink) YoutubeHelper.removeYoutubeLink(content) else content
            if (displayContent.isNotEmpty()) {
                Text(
                    text = displayContent,
                    style = ExoTypo.body14,
                    maxLines = if (isExpanded) Int.MAX_VALUE else 3,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                )

                if (!isExpanded && displayContent.length > 100) {
                    Text(
                        text = "... 더보기",
                        style = ExoTypo.body13.copy(color = ColorPalette.textDimmed),
                        modifier = Modifier
                            .padding(start = 20.dp, top = 13.dp, end = 20.dp)
                            .clickable { isExpanded = true }
                    )
                }
            }

            // 번역 버튼 (FREE_BOARD에서는 숨김)
            val hasTranslatableContent = content.isNotEmpty() && !isYoutubeLink
            if (!type.isFreeBoard && showTranslation && hasTranslatableContent) {
                Text(
                    text = stringResource(R.string.see_translate),
                    style = ExoTypo.body12.copy(color = ColorPalette.textGray),
                    modifier = Modifier
                        .padding(start = 20.dp, top = 6.dp, end = 20.dp, bottom = 6.dp)
                        .clickable {
                            viewModel.translateContent(article.content ?: "", article.nation)
                        }
                )
            }

            // 5. YouTube 링크 + 임베드 플레이어
            val hasValidLinkTitle = !article.linkTitle.isNullOrEmpty() && article.linkTitle != "None"
            if (isYoutubeLink && !article.linkUrl.isNullOrEmpty() && hasValidLinkTitle) {
                ExoYouTubePlayer(
                    linkUrl = article.linkUrl!!,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 5.dp)
                )
            }
            // 6. 미디어
            else if (article.mediaFiles.isNotEmpty()) {
                ArticleItemMediaSection(
                    mediaFiles = article.mediaFiles,
                    isVisible = isVisible,
                    onMediaClick = { mediaIndex ->
                        viewModel.navigateToMediaDetail(article, mediaIndex)
                    }
                )
            }

            // 7. 통계 섹션
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 20.dp, top = 13.dp, end = 16.dp, bottom = 13.dp),
                horizontalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                // 하트 카운트 (FREE_BOARD에서는 숨김)
                if (!type.isFreeBoard) {
                    ArticleItemStatItem(
                        iconRes = R.drawable.icon_community_heart,
                        count = localHeartCount.toInt(),
                        onClick = { showVoteDialog = true }
                    )
                }
                // 좋아요 (FEED에서만 색상 표시)
                val showLikeColor = type == ArticleItemType.FEED && localIsLiked
                ArticleItemStatItem(
                    iconRes = if (showLikeColor) R.drawable.icon_board_like_active else R.drawable.icon_board_like,
                    count = localLikeCount,
                    tintColor = if (showLikeColor) null else ColorPalette.textDefault,
                    onClick = onLikeClick
                )
                // 댓글
                ArticleItemStatItem(
                    iconRes = R.drawable.icon_community_comment,
                    count = article.commentCount,
                    tintColor = ColorPalette.textDefault,
                    onClick = { viewModel.navigateToArticleDetail(article) }
                )
            }

            // 8. 액션 버튼
            HorizontalDivider(
                color = ColorPalette.gray110,
                thickness = 0.3.dp
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(40.dp)
            ) {
                // 하트투표 버튼 (FREE_BOARD 제외)
                if (!type.isFreeBoard) {
                    ArticleItemActionButton(
                        iconRes = R.drawable.icon_community_heart,
                        label = stringResource(R.string.lable_community_heart_vote),
                        onClick = { showVoteDialog = true },
                        modifier = Modifier.weight(1f)
                    )
                    ArticleItemVerticalDivider()
                }

                // 좋아요 버튼 (FEED에서만 색상 표시)
                val showLikeColorBtn = type == ArticleItemType.FEED && localIsLiked
                ArticleItemActionButton(
                    iconRes = if (showLikeColorBtn) R.drawable.icon_board_like_active else R.drawable.icon_board_like,
                    label = stringResource(R.string.support_sympathy),
                    onClick = onLikeClick,
                    modifier = Modifier.weight(1f),
                    tintColor = if (showLikeColorBtn) null else ColorPalette.textDefault
                )

                // 댓글 버튼
                ArticleItemVerticalDivider()
                ArticleItemActionButton(
                    iconRes = R.drawable.icon_community_comment,
                    label = stringResource(R.string.lable_community_comment),
                    onClick = { viewModel.navigateToArticleDetail(article) },
                    modifier = Modifier.weight(1f),
                    tintColor = ColorPalette.textDefault
                )
            }
        }

        // 하단 간격
        Spacer(
            modifier = Modifier
                .fillMaxWidth()
                .height(10.dp)
        )
    }

    // 투표 다이얼로그
    if (showVoteDialog) {
        ExoArticleVoteDialog(
            articleId = article.id,
            articleHeart = localHeartCount,
            onVote = { hearts, onSuccess, onError ->
                viewModel.voteArticle(
                    articleId = article.id,
                    hearts = hearts,
                    onSuccess = { response ->
                        val bonusHeart = response.bonusHeart ?: 0
                        localHeartCount += hearts + bonusHeart
                        onSuccess(response)
                    },
                    onError = onError
                )
            },
            onDismiss = { showVoteDialog = false }
        )
    }

    // 더보기 바텀시트
    if (showMoreBottomSheet) {
        val actionItems = buildList {
            if (isMine || isAdmin) {
                add(ExoBottomSheetActionItem(R.string.title_edit) {
                    viewModel.onEditArticle(article)
                })
                add(ExoBottomSheetActionItem(R.string.title_remove) {
                    showMoreBottomSheet = false
                    showDeleteDialog = true
                })
            }
            if (!isMine) {
                add(ExoBottomSheetActionItem(R.string.title_report) {
                    showMoreBottomSheet = false
                    showReportDialog = true
                })
            }
            add(ExoBottomSheetActionItem(R.string.title_share) {
                shareArticleItem(context, article)
            })
        }

        ExoBottomSheetAction(
            items = actionItems,
            onDismissRequest = { showMoreBottomSheet = false }
        )
    }

    // 신고 확인 다이얼로그
    if (showReportDialog) {
        val reportHeart = viewModel.reportHeart
        ExoConfirmDialog(
            title = stringResource(R.string.title_report),
            message = stringResource(R.string.msg_report_confirm, reportHeart),
            confirmButtonText = stringResource(R.string.yes),
            dismissButtonText = stringResource(R.string.no),
            onConfirm = {
                showReportDialog = false
                viewModel.reportArticle(
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
                viewModel.deleteArticle(
                    articleId = article.id,
                    onSuccess = {
                        onDeleted?.invoke(article.id)
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
 * 게시글 공유
 */
private fun shareArticleItem(context: android.content.Context, article: ArticleModel) {
    val locale = LocaleUtil.getWikiLocale(context)
    val shareUrl = "${ServerUrl.HOST}/articles/${article.id}/?locale=$locale"

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
 * 미디어 섹션
 */
@Composable
private fun ArticleItemMediaSection(
    mediaFiles: List<ArticleFile>,
    isVisible: Boolean,
    onMediaClick: (Int) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val pagerState = rememberPagerState(pageCount = { mediaFiles.size })

    val gifImageLoader = remember(context) {
        ImageLoader.Builder(context)
            .components {
                if (android.os.Build.VERSION.SDK_INT >= 28) {
                    add(ImageDecoderDecoder.Factory())
                } else {
                    add(GifDecoder.Factory())
                }
            }
            .build()
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .clip(RectangleShape)
            .background(ColorPalette.background100)
    ) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize()
        ) { pageIndex ->
            val media = mediaFiles[pageIndex]
            val isCurrentPageVisible = isVisible && pagerState.currentPage == pageIndex

            ArticleItemMediaItem(
                media = media,
                isVisible = isCurrentPageVisible,
                gifImageLoader = gifImageLoader,
                onMediaClick = { onMediaClick(pageIndex) }
            )
        }

        if (mediaFiles.size > 1) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 16.dp, end = 16.dp)
                    .height(24.dp)
                    .background(
                        color = ColorPalette.textDefault.copy(alpha = 0.7f),
                        shape = RoundedCornerShape(13.dp)
                    )
                    .padding(horizontal = 8.dp)
            ) {
                Text(
                    text = "${pagerState.currentPage + 1}/${mediaFiles.size}",
                    style = ExoTypo.stat10.copy(color = ColorPalette.textWhiteBlack),
                    modifier = Modifier.align(Alignment.Center)
                )
            }
        }
    }
}

/**
 * 개별 미디어 아이템
 */
@Composable
private fun ArticleItemMediaItem(
    media: ArticleFile,
    isVisible: Boolean,
    gifImageLoader: ImageLoader,
    onMediaClick: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onMediaClick
            ),
        contentAlignment = Alignment.Center
    ) {
        var videoDurationMs by remember { mutableLongStateOf(0L) }
        var isFirstFrameRendered by remember(isVisible) { mutableStateOf(false) }

        LaunchedEffect(media.originUrl) {
            if (media.isVideo && videoDurationMs == 0L && !media.originUrl.isNullOrEmpty()) {
                MediaCacheUtil.getVideoDuration(media.originUrl!!.toSecureUrl())?.let { duration ->
                    videoDurationMs = duration
                }
            }
        }

        fun formatDurationMs(durationMs: Long): String {
            if (durationMs <= 0) return ""
            val minutes = TimeUnit.MILLISECONDS.toMinutes(durationMs)
            val seconds = TimeUnit.MILLISECONDS.toSeconds(durationMs) - TimeUnit.MINUTES.toSeconds(minutes)
            return "%02d:%02d".format(minutes, seconds)
        }

        val displayDuration = formatDurationMs(videoDurationMs)

        when {
            media.isVideo && isVisible && !media.umjjalUrl.isNullOrEmpty() -> {
                ExoVideoPlayer(
                    videoUrl = media.umjjalUrl!!.toSecureUrl(),
                    isVisible = isVisible,
                    modifier = Modifier.fillMaxSize(),
                    isMuted = true,
                    isLooping = true,
                    onFirstFrameRendered = {
                        coroutineScope.launch {
                            delay(100L)
                            isFirstFrameRendered = true
                        }
                    },
                    onClick = onMediaClick
                )
                if (!isFirstFrameRendered) {
                    AsyncImage(
                        model = media.thumbnailUrl.toSecureUrl(),
                        contentDescription = "Video Thumbnail",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }
            }
            media.isVideo -> {
                AsyncImage(
                    model = media.thumbnailUrl.toSecureUrl(),
                    contentDescription = "Video Thumbnail",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }
            media.isGif && isVisible && !media.umjjalUrl.isNullOrEmpty() -> {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(media.umjjalUrl.toSecureUrl())
                        .repeatCount(-1)
                        .crossfade(true)
                        .build(),
                    imageLoader = gifImageLoader,
                    contentDescription = "GIF Media",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit
                )
            }
            media.isGif -> {
                AsyncImage(
                    model = media.thumbnailUrl.toSecureUrl(),
                    contentDescription = "GIF Thumbnail",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit
                )
            }
            else -> {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(media.displayUrl.toSecureUrl())
                        .crossfade(true)
                        .build(),
                    contentDescription = "Media",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit
                )
            }
        }

        if (media.isVideo && displayDuration.isNotEmpty()) {
            Text(
                text = displayDuration,
                style = ExoTypo.body12.copy(
                    color = Color.White,
                    shadow = Shadow(
                        color = Color.Black,
                        offset = Offset(3f, 3f),
                        blurRadius = 3f
                    )
                ),
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 16.dp, end = 16.dp)
            )
        }

        if (media.isGif) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(bottom = 10.dp, end = 10.dp)
                    .background(
                        color = Color.Black.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(4.dp)
                    )
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    text = "GIF",
                    style = ExoTypo.stat10.copy(color = Color.White)
                )
            }
        }
    }
}

@Composable
private fun ArticleItemVerticalDivider() {
    Box(
        modifier = Modifier
            .width(0.3.dp)
            .height(40.dp)
            .background(ColorPalette.gray110)
    )
}

@Composable
private fun ArticleItemStatItem(
    iconRes: Int,
    count: Int,
    tintColor: Color? = null,
    onClick: (() -> Unit)? = null
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = if (onClick != null) {
            Modifier.clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
        } else {
            Modifier
        }
    ) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = null,
            modifier = Modifier.size(14.dp),
            tint = tintColor ?: Color.Unspecified
        )
        Spacer(modifier = Modifier.width(3.dp))
        Text(
            text = NumberFormatUtil.formatWithComma(count.toLong()),
            style = ExoTypo.stat13
        )
    }
}

@Composable
private fun ArticleItemActionButton(
    iconRes: Int,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tintColor: Color? = null
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(40.dp)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                painter = painterResource(iconRes),
                contentDescription = null,
                modifier = Modifier.size(14.dp),
                tint = tintColor ?: Color.Unspecified
            )
            Spacer(modifier = Modifier.width(7.dp))
            Text(
                text = label,
                style = ExoTypo.body14.copy(color = ColorPalette.textGray)
            )
        }
    }
}

/**
 * 공지사항 아이템 (ADMIN_NOTICE)
 */
@Composable
private fun ArticleItemNotice(
    article: ArticleModel,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(34.dp)
                .background(ColorPalette.main100)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onClick
                )
                .padding(horizontal = 20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                painter = painterResource(R.drawable.icon_community_pin),
                contentDescription = "Notice",
                tint = Color.Unspecified
            )
            Spacer(modifier = Modifier.width(5.dp))
            Text(
                text = article.title ?: "",
                style = ExoTypo.body12.copy(color = ColorPalette.mainLight),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        HorizontalDivider(
            thickness = 0.3.dp,
            color = ColorPalette.main300
        )
    }
}

/**
 * 간략 표시 아이템 (FREE_BOARD 타입)
 * old 프로젝트의 ExoBoardItemMini와 동일한 UI
 */
@Composable
private fun ArticleItemCompact(
    article: ArticleModel,
    showPopularIcon: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    // 이미지 파일 찾기 - 기존 ExoBoardItemMini와 동일한 우선순위
    // 1. article.thumbnailUrl 우선
    // 2. article.imageUrl
    // 3. files 배열에서 이미지 찾기
    val imageFiles = article.files.filter { it.fileType == "image" || it.fileUrl?.contains("image") == true }
    val thumbnailUrl: String? = when {
        !article.thumbnailUrl.isNullOrEmpty() -> article.thumbnailUrl
        !article.imageUrl.isNullOrEmpty() -> article.imageUrl
        else -> imageFiles.firstOrNull()?.let { it.thumbnailUrl ?: it.fileUrl }
    }
    val imageCount = if (imageFiles.isNotEmpty()) imageFiles.size else if (thumbnailUrl != null) 1 else 0

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(ColorPalette.background100)
    ) {
        // 상단 구분선
        HorizontalDivider(
            thickness = 0.3.dp,
            color = ColorPalette.gray110
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onClick
                )
                .padding(horizontal = 20.dp, vertical = 17.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 왼쪽 컨텐츠 영역
            Column(
                modifier = Modifier
                    .weight(1f)
                    .then(if (thumbnailUrl != null) Modifier.padding(end = 12.dp) else Modifier)
            ) {
                // 제목 (인기글 아이콘, lock 아이콘 포함)
                if (!article.title.isNullOrEmpty()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 0.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (showPopularIcon && article.isPopular) {
                            Icon(
                                painter = painterResource(R.drawable.icon_popularpost_title),
                                contentDescription = "Popular",
                                modifier = Modifier.padding(end = 5.dp),
                                tint = Color.Unspecified
                            )
                        }
                        if (article.isMostOnly == "Y") {
                            Icon(
                                painter = painterResource(R.drawable.icon_onlymyidol),
                                contentDescription = "Only my idol",
                                modifier = Modifier.padding(end = 5.dp),
                                tint = Color.Unspecified
                            )
                        }
                        Text(
                            text = article.title!!,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = ColorPalette.textDefault,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // 본문 내용 (한 줄)
                if (!article.content.isNullOrEmpty()) {
                    Text(
                        text = article.content!!,
                        fontSize = 13.sp,
                        color = ColorPalette.textGray,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .fillMaxWidth()
                            .offset(y = (-3).dp)
                    )
                }

                // 작성자, 작성시간
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = 0.dp)
                ) {
                    Text(
                        text = article.user?.nickname ?: "",
                        fontSize = 11.sp,
                        color = ColorPalette.textDimmed,
                        maxLines = 1
                    )
                    Text(
                        text = " · ",
                        fontSize = 11.sp,
                        color = ColorPalette.textDimmed
                    )
                    Text(
                        text = DateTimeUtil.formatBoardDate(article.createdAt),
                        fontSize = 11.sp,
                        color = ColorPalette.textDimmed
                    )
                }

                // 좋아요, 댓글, 조회수
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = 0.dp)
                ) {
                    Icon(
                        painter = painterResource(R.drawable.icon_board_like),
                        contentDescription = "Like",
                        modifier = Modifier.size(14.dp),
                        tint = ColorPalette.textGray
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = article.likeCount.toString(),
                        fontSize = 12.sp,
                        color = ColorPalette.textGray
                    )

                    Spacer(modifier = Modifier.width(14.dp))

                    Icon(
                        painter = painterResource(R.drawable.icon_board_comment),
                        contentDescription = "Comment",
                        modifier = Modifier.size(14.dp),
                        tint = ColorPalette.textGray
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = article.commentCount.toString(),
                        fontSize = 12.sp,
                        color = ColorPalette.textGray
                    )

                    Spacer(modifier = Modifier.width(14.dp))

                    Icon(
                        painter = painterResource(R.drawable.icon_board_hits),
                        contentDescription = "Views",
                        modifier = Modifier.size(14.dp),
                        tint = ColorPalette.textGray
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = article.viewCount.toString(),
                        fontSize = 12.sp,
                        color = ColorPalette.textGray
                    )
                }
            }

            // 오른쪽 썸네일 (이미지가 있을 경우)
            if (thumbnailUrl != null) {
                Box {
                    AsyncImage(
                        model = thumbnailUrl,
                        contentDescription = "Thumbnail",
                        modifier = Modifier
                            .size(70.dp)
                            .clip(RoundedCornerShape(10.dp)),
                        contentScale = ContentScale.Crop
                    )
                    if (imageCount > 1) {
                        Text(
                            text = "+${imageCount - 1}",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = ColorPalette.textLight,
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(top = 3.dp, end = 5.dp)
                        )
                    }
                }
            }
        }
    }
}
