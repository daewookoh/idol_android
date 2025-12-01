package net.ib.mn.ui.components

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
import net.ib.mn.ui.theme.ColorPalette
import net.ib.mn.ui.theme.ExoTypo
import net.ib.mn.util.DateTimeUtil
import net.ib.mn.util.LocaleUtil
import net.ib.mn.util.MediaCacheUtil
import net.ib.mn.util.NumberFormatUtil
import net.ib.mn.util.YoutubeHelper
import java.util.concurrent.TimeUnit

/**
 * ExoArticle 타입
 */
enum class ArticleType {
    FEED,       // 커뮤니티 피드 - 하트투표, 좋아요, 댓글, 번역기능
    FREE_BOARD, // 자유게시판 - 좋아요, 댓글 (하트투표 없음)
    COMMUNITY   // 커뮤니티 게시글 (피드에서 표시) - FEED와 동일하나 상단에 커뮤니티 이름 표시
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
    viewModel: ExoArticleViewModel = hiltViewModel(),
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // 로컬 상태 (즉시 UI 업데이트용)
    var localIsLiked by remember(article.id) { mutableStateOf(article.isUserLike) }
    var localLikeCount by remember(article.id) { mutableIntStateOf(article.likeCount) }
    var localHeartCount by remember(article.id) { mutableLongStateOf(article.heart) }

    // 좋아요 클릭 throttling
    var lastLikeClickTime by remember { mutableLongStateOf(0L) }

    // 투표 다이얼로그 상태
    var showVoteDialog by remember { mutableStateOf(false) }

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
    val profileImageUrl = article.user?.imageUrlCommunity ?: ""

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
                AsyncImage(
                    model = profileImageUrl,
                    contentDescription = "Profile",
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(ColorPalette.gray110)
                        .then(
                            // COMMUNITY 타입에서는 프로필 클릭 비활성화, FEED/FREE_BOARD는 활성화
                            if (type == ArticleType.COMMUNITY) {
                                Modifier
                            } else {
                                Modifier.clickable {
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
                        ),
                    contentScale = ContentScale.Crop,
                    placeholder = painterResource(R.drawable.menu_profile_default2),
                    error = painterResource(R.drawable.menu_profile_default2)
                )

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

                // 더보기 버튼
                Icon(
                    painter = painterResource(R.drawable.icon_view_more),
                    contentDescription = "More",
                    modifier = Modifier
                        .padding(4.dp)
                        .clickable {
                            viewModel.showMoreOptions(article)
                        },
                    tint = Color.Unspecified
                )
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
                ArticleType.FEED, ArticleType.COMMUNITY -> {
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
                    ArticleType.FEED -> {
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
                StatItem(
                    iconRes = R.drawable.icon_community_comment,
                    count = article.commentCount,
                    tintColor = ColorPalette.textDefault,
                    onClick = {
                        viewModel.navigateToArticleDetail(article)
                    }
                )
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
}

/**
 * 미디어 섹션
 */
@Composable
private fun MediaSection(
    mediaFiles: List<ArticleFile>,
    isVisible: Boolean,
    onMediaClick: (Int) -> Unit
) {
    val context = LocalContext.current
    val firstMedia = mediaFiles.first()
    val coroutineScope = rememberCoroutineScope()

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
            .clip(RoundedCornerShape(0.dp))
            .clickable { onMediaClick(0) },
        contentAlignment = Alignment.Center
    ) {
        var videoDurationMs by remember { mutableStateOf(0L) }

        LaunchedEffect(firstMedia.originUrl) {
            if (firstMedia.isVideo && videoDurationMs == 0L && !firstMedia.originUrl.isNullOrEmpty()) {
                MediaCacheUtil.getVideoDuration(firstMedia.originUrl!!)?.let { duration ->
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
        var isFirstFrameRendered by remember(isVisible) { mutableStateOf(false) }

        when {
            firstMedia.isVideo && isVisible && !firstMedia.umjjalUrl.isNullOrEmpty() -> {
                ExoVideoPlayer(
                    videoUrl = firstMedia.umjjalUrl!!,
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
                        model = firstMedia.thumbnailUrl,
                        contentDescription = "Video Thumbnail",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }
            }
            firstMedia.isVideo -> {
                AsyncImage(
                    model = firstMedia.thumbnailUrl,
                    contentDescription = "Video Thumbnail",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }
            firstMedia.isGif && isVisible && !firstMedia.umjjalUrl.isNullOrEmpty() -> {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(firstMedia.umjjalUrl)
                        .repeatCount(-1)
                        .crossfade(true)
                        .build(),
                    imageLoader = gifImageLoader,
                    contentDescription = "GIF Media",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit
                )
            }
            firstMedia.isGif -> {
                AsyncImage(
                    model = firstMedia.thumbnailUrl,
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
                        .data(firstMedia.displayUrl)
                        .crossfade(true)
                        .build(),
                    contentDescription = "Media",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit
                )
            }
        }

        if (firstMedia.isVideo && displayDuration.isNotEmpty()) {
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

        if (mediaFiles.size > 1) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = if (firstMedia.isVideo) 40.dp else 29.dp, end = 16.dp)
                    .height(24.dp)
                    .background(
                        color = ColorPalette.textDefault.copy(alpha = 0.7f),
                        shape = RoundedCornerShape(13.dp)
                    )
                    .padding(horizontal = 8.dp)
            ) {
                Text(
                    text = "1/${mediaFiles.size}",
                    style = ExoTypo.stat10.copy(color = ColorPalette.textWhiteBlack),
                    modifier = Modifier.align(Alignment.Center)
                )
            }
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
