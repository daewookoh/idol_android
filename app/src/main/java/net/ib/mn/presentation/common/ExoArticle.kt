package net.ib.mn.presentation.common

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
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
import net.ib.mn.ui.components.ExoVideoPlayer
import net.ib.mn.ui.components.ExoYouTubePlayer
import net.ib.mn.ui.theme.ColorPalette
import net.ib.mn.ui.theme.ExoTypo
import net.ib.mn.util.DateTimeUtil
import net.ib.mn.util.LocaleUtil
import net.ib.mn.util.MediaCacheUtil
import net.ib.mn.ui.components.ExoBottomSheetAction
import net.ib.mn.ui.components.ExoBottomSheetActionItem
import net.ib.mn.ui.components.ExoConfirmDialog
import net.ib.mn.ui.components.ExoErrorDialog
import net.ib.mn.ui.components.ExoProfileImage
import net.ib.mn.ui.components.ProfileImageType
import android.widget.Toast
import net.ib.mn.util.IdolImageUtil.toSecureUrl
import net.ib.mn.util.NumberFormatUtil
import net.ib.mn.util.ServerUrl
import net.ib.mn.util.YoutubeHelper
import java.util.concurrent.TimeUnit
import android.content.Intent
import java.util.Locale
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState

/**
 * ExoArticle 타입
 */
enum class ArticleType {
    FEED,       // 커뮤니티 피드 - 하트투표, 좋아요, 댓글, 번역기능
    FREE_BOARD, // 자유게시판 - 좋아요, 댓글 (하트투표 없음)
    COMMUNITY,  // 커뮤니티 게시글 (피드에서 표시) - FEED와 동일하나 상단에 커뮤니티 이름 표시
    DETAIL      // 게시글 상세 - FEED와 동일하나 메뉴 아이콘, 댓글 버튼 없음
}

/**
 * ExoArticle - 게시글 아티클 컴포넌트
 *
 * Old 프로젝트의 community_item.xml + CommunityArticleViewHolder 기능을 Compose로 재현
 * 모든 클릭 액션을 내부에서 처리
 *
 * @param article 게시글 데이터
 * @param type 게시글 타입 (FEED: 프로필 클릭 활성화, FREE_BOARD: 자유게시판, COMMUNITY: 프로필 클릭 비활성화)
 * @param isVisible 화면에 보이는지 여부 (GIF/비디오 최적화용)
 * @param showTranslation 번역 버튼 표시 여부
 * @param viewModel ExoArticleViewModel
 * @param modifier Modifier
 */
@Composable
fun ExoArticle(
    article: ArticleModel,
    type: ArticleType = ArticleType.FEED,
    isVisible: Boolean = true,
    showTranslation: Boolean = true,
    onDeleted: ((String) -> Unit)? = null,
    onArticleUpdated: ((ArticleModel) -> Unit)? = null,
    viewModel: ExoArticleViewModel = hiltViewModel(),
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // 로컬 상태 (즉시 UI 업데이트용)
    // article의 실제 값을 key로 사용하여 외부에서 변경된 값이 반영되도록 함
    var localIsLiked by remember(article.id, article.isUserLike) { mutableStateOf(article.isUserLike) }
    var localLikeCount by remember(article.id, article.likeCount) { mutableIntStateOf(article.likeCount) }
    var localHeartCount by remember(article.id, article.heart) { mutableLongStateOf(article.heart) }

    // 상태 변경 시 콜백 호출 (백스택 데이터 동기화)
    LaunchedEffect(localIsLiked, localLikeCount, localHeartCount) {
        // 초기값과 다를 때만 콜백 호출
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

    // 투표 다이얼로그 상태
    var showVoteDialog by remember { mutableStateOf(false) }

    // 더보기 바텀시트 상태
    var showMoreBottomSheet by remember { mutableStateOf(false) }

    // 신고 확인 다이얼로그 상태
    var showReportDialog by remember { mutableStateOf(false) }

    // 신고 에러 다이얼로그 상태
    var showReportErrorDialog by remember { mutableStateOf(false) }
    var reportErrorMessage by remember { mutableStateOf("") }

    // 삭제 확인 다이얼로그 상태
    var showDeleteDialog by remember { mutableStateOf(false) }

    // 작성자 여부 확인 (ViewModel에서 현재 사용자 정보 가져옴)
    val myUserId = viewModel.myUserId
    val isAdmin = viewModel.isAdmin
    val isMine = myUserId != null && article.user?.id == myUserId

    // 커뮤니티 이름 (COMMUNITY 타입에서 사용)
    val communityName = remember(article.idol) {
        article.idol?.let { LocaleUtil.getLocalizedIdolName(context, it) }
    }

    // 시간 표시 (COMMUNITY 타입은 풀타임, 나머지는 상대 시간)
    val createdAt = remember(article.createdAt, type) {
        when (type) {
            ArticleType.COMMUNITY -> DateTimeUtil.formatFullDate(article.createdAt)
            else -> DateTimeUtil.getRelativeTimeSpan(article.createdAt)
        }
    }

    // YouTube 링크 여부 확인
    val isYoutubeLink = YoutubeHelper.isYoutubeLink(article.linkUrl, article.imageUrl, article.umjjalUrl)
    var isExpanded by remember { mutableStateOf(false) }

    // 프로필 이미지 URL
    val profileImageUrl = article.user?.imageUrlCommunity

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(ColorPalette.gray80)
    ) {
        // 게시글 컨테이너
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(ColorPalette.background100)
        ) {
            // 0. 커뮤니티 정보 (COMMUNITY 타입에서만 표시)
            if (type == ArticleType.COMMUNITY && !communityName.isNullOrEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(ColorPalette.background100)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            article.idol?.id?.let { idolId ->
                                viewModel.navigateToCommunity(idolId)
                            }
                        }
                        .padding(horizontal = 20.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = communityName,
                        style = ExoTypo.body12.copy(
                            color = ColorPalette.gray580,
                            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                        )
                    )
                    Spacer(modifier = Modifier.width(2.dp))
                    Text(
                        text = stringResource(R.string.community_post),
                        style = ExoTypo.body12.copy(color = ColorPalette.gray300)
                    )
                }
                HorizontalDivider(
                    color = ColorPalette.gray110,
                    thickness = 0.5.dp
                )
            }

            // 1. 사용자 프로필 섹션
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 20.dp, top = 13.dp, end = 11.dp, bottom = 11.dp),
                verticalAlignment = Alignment.Top
            ) {
                // 프로필 이미지
                Box(
                    modifier = Modifier.then(
                        // COMMUNITY 타입에서는 프로필 클릭 비활성화, FEED/FREE_BOARD는 활성화
                        if (type == ArticleType.COMMUNITY) {
                            Modifier
                        } else {
                            Modifier.clickable(
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
                        }
                    )
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

                        Text(
                            text = article.user?.nickname ?: "",
                            style = ExoTypo.body14Main,
                            modifier = if (type == ArticleType.COMMUNITY) {
                                // COMMUNITY 타입에서는 프로필 클릭 비활성화
                                Modifier
                            } else {
                                Modifier.clickable(
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

                // 더보기 버튼 (호버 효과 없음) - DETAIL 타입에서는 숨김
                if (type != ArticleType.DETAIL) {
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
            }

            // 2. 태그 (FREE_BOARD 타입에서만 표시, FEED/COMMUNITY는 태그 숨김)
            if (type == ArticleType.FREE_BOARD) {
                val tag = article.type
                if (!tag.isNullOrEmpty()) {
                    Box(
                        modifier = Modifier
                            .padding(start = 20.dp, top = 9.dp, end = 20.dp, bottom = 6.dp)
                            .background(
                                color = ColorPalette.main200,
                                shape = RoundedCornerShape(6.dp)
                            )
                            .padding(horizontal = 7.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = tag,
                            style = ExoTypo.label13
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

            // 번역 버튼
            val hasTranslatableContent = content.isNotEmpty() && !isYoutubeLink
            when (type) {
                ArticleType.FEED, ArticleType.COMMUNITY, ArticleType.DETAIL -> {
                    if (showTranslation && hasTranslatableContent) {
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
                }
                ArticleType.FREE_BOARD -> { /* 자유게시판은 번역 버튼 없음 */ }
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
                MediaSection(
                    mediaFiles = article.mediaFiles,
                    isVisible = isVisible,
                    onMediaClick = { mediaIndex ->
                        viewModel.navigateToMediaDetail(article, mediaIndex)
                    }
                )
            }

            // 6. 통계 섹션
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 10.dp, top = 13.dp, end = 16.dp, bottom = 13.dp),
                horizontalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                when (type) {
                    ArticleType.FEED, ArticleType.DETAIL -> {
                        StatItem(
                            iconRes = R.drawable.icon_community_heart,
                            count = localHeartCount.toInt(),
                            onClick = { showVoteDialog = true }
                        )
                    }
                    ArticleType.COMMUNITY -> {
                        StatItem(
                            iconRes = R.drawable.icon_community_heart,
                            count = localHeartCount.toInt(),
                            onClick = { showVoteDialog = true }
                        )
                    }
                    ArticleType.FREE_BOARD -> { /* 자유게시판은 하트 카운트 없음 */ }
                }
                StatItem(
                    iconRes = R.drawable.icon_board_like,
                    count = localLikeCount,
                    tintColor = ColorPalette.textDefault,
                    onClick = {
                        val currentTime = System.currentTimeMillis()
                        if (currentTime - lastLikeClickTime < 1000L) return@StatItem
                        lastLikeClickTime = currentTime

                        val newLiked = !localIsLiked
                        localIsLiked = newLiked
                        localLikeCount = if (newLiked) localLikeCount + 1 else (localLikeCount - 1).coerceAtLeast(0)
                        viewModel.postLike(article.id, newLiked)
                    }
                )
                // DETAIL 타입에서는 댓글 통계 클릭 비활성화
                if (type != ArticleType.DETAIL) {
                    StatItem(
                        iconRes = R.drawable.icon_community_comment,
                        count = article.commentCount,
                        tintColor = ColorPalette.textDefault,
                        onClick = {
                            viewModel.navigateToArticleDetail(article)
                        }
                    )
                } else {
                    StatItem(
                        iconRes = R.drawable.icon_community_comment,
                        count = article.commentCount,
                        tintColor = ColorPalette.textDefault
                    )
                }
            }

            HorizontalDivider(
                color = ColorPalette.gray110,
                thickness = 0.3.dp
            )

            // 7. 액션 버튼
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(40.dp)
            ) {
                when (type) {
                    ArticleType.FEED -> {
                        ActionButton(
                            iconRes = R.drawable.icon_community_heart,
                            label = stringResource(R.string.lable_community_heart_vote),
                            onClick = { showVoteDialog = true },
                            modifier = Modifier.weight(1f)
                        )
                        VerticalDivider()
                        ActionButton(
                            iconRes = if (localIsLiked) R.drawable.icon_board_like_active else R.drawable.icon_board_like,
                            label = stringResource(R.string.support_sympathy),
                            onClick = {
                                val currentTime = System.currentTimeMillis()
                                if (currentTime - lastLikeClickTime < 1000L) return@ActionButton
                                lastLikeClickTime = currentTime

                                val newLiked = !localIsLiked
                                localIsLiked = newLiked
                                localLikeCount = if (newLiked) localLikeCount + 1 else (localLikeCount - 1).coerceAtLeast(0)
                                viewModel.postLike(article.id, newLiked)
                            },
                            modifier = Modifier.weight(1f),
                            tintColor = if (localIsLiked) null else ColorPalette.textDefault
                        )
                        VerticalDivider()
                        ActionButton(
                            iconRes = R.drawable.icon_community_comment,
                            label = stringResource(R.string.lable_community_comment),
                            onClick = { viewModel.navigateToArticleDetail(article) },
                            modifier = Modifier.weight(1f),
                            tintColor = ColorPalette.textDefault
                        )
                    }
                    ArticleType.COMMUNITY -> {
                        ActionButton(
                            iconRes = R.drawable.icon_community_heart,
                            label = stringResource(R.string.lable_community_heart_vote),
                            onClick = { showVoteDialog = true },
                            modifier = Modifier.weight(1f)
                        )
                        VerticalDivider()
                        ActionButton(
                            iconRes = if (localIsLiked) R.drawable.icon_board_like_active else R.drawable.icon_board_like,
                            label = stringResource(R.string.support_sympathy),
                            onClick = {
                                val currentTime = System.currentTimeMillis()
                                if (currentTime - lastLikeClickTime < 1000L) return@ActionButton
                                lastLikeClickTime = currentTime

                                val newLiked = !localIsLiked
                                localIsLiked = newLiked
                                localLikeCount = if (newLiked) localLikeCount + 1 else (localLikeCount - 1).coerceAtLeast(0)
                                viewModel.postLike(article.id, newLiked)
                            },
                            modifier = Modifier.weight(1f),
                            tintColor = if (localIsLiked) null else ColorPalette.textDefault
                        )
                        VerticalDivider()
                        ActionButton(
                            iconRes = R.drawable.icon_community_comment,
                            label = stringResource(R.string.lable_community_comment),
                            onClick = { viewModel.navigateToArticleDetail(article) },
                            modifier = Modifier.weight(1f),
                            tintColor = ColorPalette.textDefault
                        )
                    }
                    ArticleType.DETAIL -> {
                        // DETAIL 타입: 하트투표, 좋아요만 표시 (댓글 버튼 없음)
                        ActionButton(
                            iconRes = R.drawable.icon_community_heart,
                            label = stringResource(R.string.lable_community_heart_vote),
                            onClick = { showVoteDialog = true },
                            modifier = Modifier.weight(1f)
                        )
                        VerticalDivider()
                        ActionButton(
                            iconRes = if (localIsLiked) R.drawable.icon_board_like_active else R.drawable.icon_board_like,
                            label = stringResource(R.string.support_sympathy),
                            onClick = {
                                val currentTime = System.currentTimeMillis()
                                if (currentTime - lastLikeClickTime < 1000L) return@ActionButton
                                lastLikeClickTime = currentTime

                                val newLiked = !localIsLiked
                                localIsLiked = newLiked
                                localLikeCount = if (newLiked) localLikeCount + 1 else (localLikeCount - 1).coerceAtLeast(0)
                                viewModel.postLike(article.id, newLiked)
                            },
                            modifier = Modifier.weight(1f),
                            tintColor = if (localIsLiked) null else ColorPalette.textDefault
                        )
                    }
                    ArticleType.FREE_BOARD -> {
                        ActionButton(
                            iconRes = if (localIsLiked) R.drawable.icon_board_like_active else R.drawable.icon_board_like,
                            label = stringResource(R.string.support_sympathy),
                            onClick = {
                                val currentTime = System.currentTimeMillis()
                                if (currentTime - lastLikeClickTime < 1000L) return@ActionButton
                                lastLikeClickTime = currentTime

                                val newLiked = !localIsLiked
                                localIsLiked = newLiked
                                localLikeCount = if (newLiked) localLikeCount + 1 else (localLikeCount - 1).coerceAtLeast(0)
                                viewModel.postLike(article.id, newLiked)
                            },
                            modifier = Modifier.weight(1f),
                            tintColor = if (localIsLiked) null else ColorPalette.textDefault
                        )
                        VerticalDivider()
                        ActionButton(
                            iconRes = R.drawable.icon_community_comment,
                            label = stringResource(R.string.lable_community_comment),
                            onClick = { viewModel.navigateToArticleDetail(article) },
                            modifier = Modifier.weight(1f),
                            tintColor = ColorPalette.textDefault
                        )
                    }
                }
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
                        // 투표한 하트 수만큼 증가 (보너스 하트 포함)
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
            // 작성자 또는 관리자: 수정, 삭제
            if (isMine || isAdmin) {
                add(ExoBottomSheetActionItem(R.string.title_edit) {
                    viewModel.onEditArticle(article)
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

    // 신고 에러 다이얼로그 (타이틀 없음, 버튼 1개)
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
private fun shareArticle(context: android.content.Context, article: ArticleModel) {
    // locale 설정 (ko, en, ja 등)
    val locale = Locale.getDefault().language

    // 공유 URL 생성: {HOST}/articles/{articleId}/?locale={locale}
    val shareUrl = "${ServerUrl.HOST}/articles/${article.id}/?locale=$locale"

    // 공유 텍스트: 제목 + URL
    val shareText = if (!article.title.isNullOrEmpty()) {
        "${article.title}\n$shareUrl"
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
 * 미디어 섹션 (스와이프 지원)
 */
@Composable
private fun MediaSection(
    mediaFiles: List<ArticleFile>,
    isVisible: Boolean,
    onMediaClick: (Int) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val pagerState = rememberPagerState(pageCount = { mediaFiles.size })

    // GIF ImageLoader
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
            .background(ColorPalette.background100)
    ) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize()
        ) { pageIndex ->
            val media = mediaFiles[pageIndex]
            val isCurrentPageVisible = isVisible && pagerState.currentPage == pageIndex

            MediaItem(
                media = media,
                isVisible = isCurrentPageVisible,
                gifImageLoader = gifImageLoader,
                onMediaClick = { onMediaClick(pageIndex) }
            )
        }

        // 페이지 인디케이터 (여러 장일 경우)
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
private fun MediaItem(
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
            .clickable(onClick = onMediaClick),
        contentAlignment = Alignment.Center
    ) {
        var videoDurationMs by remember { mutableStateOf(0L) }
        var isFirstFrameRendered by remember(isVisible) { mutableStateOf(false) }

        LaunchedEffect(media.originUrl) {
            if (media.isVideo && videoDurationMs == 0L && !media.originUrl.isNullOrEmpty()) {
                MediaCacheUtil.getVideoDuration(media.originUrl!!)?.let { duration ->
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
                    }
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

        // 비디오 재생 시간 표시
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
    }
}

@Composable
private fun VerticalDivider() {
    Box(
        modifier = Modifier
            .width(0.3.dp)
            .height(40.dp)
            .background(ColorPalette.gray110)
    )
}

@Composable
private fun StatItem(
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
private fun ActionButton(
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
