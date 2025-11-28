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
import net.ib.mn.ui.theme.ColorPalette
import net.ib.mn.ui.theme.ExoTypo
import net.ib.mn.util.MediaCacheUtil
import net.ib.mn.util.NumberFormatUtil
import net.ib.mn.util.YoutubeHelper
import java.util.concurrent.TimeUnit

/**
 * ExoArticle 타입
 */
enum class ArticleType {
    FEED,       // 커뮤니티 피드 - 하트투표, 좋아요, 댓글, 번역기능
    FREE_BOARD  // 자유게시판 - 좋아요, 댓글 (하트투표 없음)
}

/**
 * ExoArticle - 게시글 아티클 컴포넌트
 *
 * Old 프로젝트의 community_item.xml + CommunityArticleViewHolder 기능을 Compose로 재현
 *
 * @param type 게시글 타입 (FEED: 커뮤니티 피드, FREE_BOARD: 자유게시판)
 * @param profileImageUrl 사용자 프로필 이미지 URL
 * @param userName 사용자 이름
 * @param userLevel 사용자 레벨
 * @param createdAt 작성 시간 문자열 (full 형식: 2024.11.27 오후 5:05)
 * @param title 게시글 제목
 * @param content 게시글 내용
 * @param mediaFiles 미디어 파일 리스트 (ArticleFile) - GIF 최적화를 위해 파일 정보 필요
 * @param heartCount 하트 수 (FEED 타입에서만 표시)
 * @param likeCount 좋아요 수
 * @param commentCount 댓글 수
 * @param isPrivate 최애만 보기 여부
 * @param isPopular 인기 게시글 여부
 * @param tag 태그
 * @param showTranslation 번역 버튼 표시 여부
 * @param isVisible 화면에 보이는지 여부 (GIF 최적화용)
 * @param onProfileClick 프로필 클릭 콜백
 * @param onMoreClick 더보기 버튼 클릭 콜백
 * @param onHeartClick 하트 투표 클릭 콜백 (FEED 타입에서만 사용)
 * @param onLikeClick 좋아요 클릭 콜백
 * @param onCommentClick 댓글 클릭 콜백
 * @param onTranslateClick 번역 클릭 콜백
 * @param onMediaClick 미디어 클릭 콜백
 * @param modifier Modifier
 */
@Composable
fun ExoArticle(
    type: ArticleType = ArticleType.FEED,
    profileImageUrl: String,
    userName: String,
    userLevel: Int = 0,
    userEmoticonUrl: String? = null,
    createdAt: String,
    title: String? = null,
    content: String,
    mediaFiles: List<ArticleFile> = emptyList(),
    linkUrl: String? = null,
    linkTitle: String? = null,
    imageUrl: String? = null,
    umjjalUrl: String? = null,
    heartCount: Int = 0,
    likeCount: Int = 0,
    commentCount: Int = 0,
    isLiked: Boolean = false,
    isPrivate: Boolean = false,
    isPopular: Boolean = false,
    tag: String? = null,
    showTranslation: Boolean = false,
    isVisible: Boolean = true,
    onProfileClick: () -> Unit = {},
    onMoreClick: () -> Unit = {},
    onHeartClick: () -> Unit = {},
    onLikeClick: () -> Unit = {},
    onCommentClick: () -> Unit = {},
    onTranslateClick: () -> Unit = {},
    onMediaClick: (Int) -> Unit = {},
    modifier: Modifier = Modifier
) {
    // YouTube 링크 여부 확인
    val isYoutubeLink = YoutubeHelper.isYoutubeLink(linkUrl, imageUrl, umjjalUrl)
    var isExpanded by remember { mutableStateOf(false) }

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
                        .clickable(onClick = onProfileClick),
                    contentScale = ContentScale.Crop,
                    placeholder = painterResource(R.drawable.menu_profile_default2),
                    error = painterResource(R.drawable.menu_profile_default2)
                )

                Spacer(modifier = Modifier.width(10.dp))

                // 이름, 레벨, 시간
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    // 이모티콘 + 레벨 아이콘 + 이름 (Old 프로젝트: emoticon -> level -> name)
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // 이모티콘 (Old 프로젝트의 BaseArticleViewHolder.setUserSection과 동일)
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

                        // 레벨 아이콘 (Old 프로젝트: Util.getLevelResId - 0~40까지 지원)
                        val context = LocalContext.current
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
                            modifier = Modifier
                                .padding(top = 4.dp, end = 2.dp)
                        )

                        Text(
                            text = userName,
                            style = ExoTypo.body14Main
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

                        if (isPrivate) {
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
                        .clickable(onClick = onMoreClick),
                    tint = Color.Unspecified
                )
            }

            // 2. 태그
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

            // 3. 제목
            if (!title.isNullOrEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 20.dp, end = 20.dp, bottom = 21.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (isPopular) {
                        Icon(
                            painter = painterResource(R.drawable.icon_popularpost_title),
                            contentDescription = "Popular post",
                            modifier = Modifier.padding(end = 5.dp),
                            tint = Color.Unspecified
                        )
                    }

                    Text(
                        text = title,
                        style = ExoTypo.title15Default,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // 4. 내용 (YouTube 링크인 경우 content에서 링크 제거)
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

                // 더보기 버튼
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

            // 번역 버튼 (FEED 타입이고, 번역할 내용이 있을 때만 표시)
            // 유튜브 링크만 있는 경우 번역 버튼 숨김 (Old 프로젝트 TranslateUiHelper 로직)
            val hasTranslatableContent = content.isNotEmpty() && !isYoutubeLink
            if (type == ArticleType.FEED && showTranslation && hasTranslatableContent) {
                Text(
                    text = stringResource(R.string.see_translate),
                    style = ExoTypo.body12.copy(color = ColorPalette.textGray),
                    modifier = Modifier
                        .padding(start = 20.dp, top = 6.dp, end = 20.dp, bottom = 6.dp)
                        .clickable(onClick = onTranslateClick)
                )
            }

            // 5. YouTube 링크 + 임베드 플레이어
            // Old 프로젝트: 2025.4.11 링크 URL 텍스트 제거됨 - 플레이어만 표시
            val hasValidLinkTitle = !linkTitle.isNullOrEmpty() && linkTitle != "None"
            if (isYoutubeLink && !linkUrl.isNullOrEmpty() && hasValidLinkTitle) {
                // YouTube 임베드 플레이어 (Old 프로젝트와 동일)
                ExoYouTubePlayer(
                    linkUrl = linkUrl,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 5.dp)
                )
            }
            // 6. 미디어 (첫 번째 미디어만 표시, 유튜브가 아닌 경우)
            // 정사각 영역에 이미지가 짤리지 않고 가운데에 표시
            // GIF/비디오의 경우 화면에 보일 때만 원본 로드, 그렇지 않으면 썸네일
            else if (mediaFiles.isNotEmpty()) {
                val context = LocalContext.current
                val firstMedia = mediaFiles.first()

                // GIF ImageLoader (움짤 자동 재생용)
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
                        .clip(RoundedCornerShape(0.dp)) // 영역 밖으로 넘치지 않도록 clip
                        .clickable { onMediaClick(0) },
                    contentAlignment = Alignment.Center
                ) {
                    // 비디오 duration 상태 (Old처럼 MediaMetadataRetriever로 가져옴)
                    var videoDurationMs by remember { mutableStateOf(0L) }

                    // Old 프로젝트처럼 MediaMetadataRetriever로 duration 가져오기
                    LaunchedEffect(firstMedia.originUrl) {
                        if (firstMedia.isVideo && videoDurationMs == 0L && !firstMedia.originUrl.isNullOrEmpty()) {
                            MediaCacheUtil.getVideoDuration(firstMedia.originUrl!!)?.let { duration ->
                                videoDurationMs = duration
                            }
                        }
                    }

                    // duration 포맷팅 (Old convertTimeMillsToTimerFormat과 동일)
                    fun formatDurationMs(durationMs: Long): String {
                        if (durationMs <= 0) return ""
                        val minutes = TimeUnit.MILLISECONDS.toMinutes(durationMs)
                        val seconds = TimeUnit.MILLISECONDS.toSeconds(durationMs) - TimeUnit.MINUTES.toSeconds(minutes)
                        return "%02d:%02d".format(minutes, seconds)
                    }

                    // duration 표시 (MediaMetadataRetriever에서 가져온 값 사용)
                    val displayDuration = formatDurationMs(videoDurationMs)

                    // 첫 프레임 렌더링 여부 (Old의 onRenderedFirstFrame과 동일)
                    // isVisible이 변경될 때 리셋하여 깜빡임 방지
                    var isFirstFrameRendered by remember(isVisible) { mutableStateOf(false) }
                    val coroutineScope = rememberCoroutineScope()

                    when {
                        // MP4 비디오인 경우: ExoPlayer로 재생 + 썸네일 오버레이
                        firstMedia.isVideo && isVisible && !firstMedia.umjjalUrl.isNullOrEmpty() -> {
                            // 비디오 플레이어
                            ExoVideoPlayer(
                                videoUrl = firstMedia.umjjalUrl!!,
                                isVisible = isVisible,
                                modifier = Modifier.fillMaxSize(),
                                isMuted = true,
                                isLooping = true,
                                onFirstFrameRendered = {
                                    // 첫 프레임 렌더링 후 0.1초 후에 썸네일 숨김 (깜빡임 방지)
                                    coroutineScope.launch {
                                        delay(100L)
                                        isFirstFrameRendered = true
                                    }
                                }
                            )
                            // 첫 프레임 렌더링 전까지 썸네일 오버레이 (검은 화면/깜빡임 방지)
                            if (!isFirstFrameRendered) {
                                AsyncImage(
                                    model = firstMedia.thumbnailUrl,
                                    contentDescription = "Video Thumbnail",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            }
                        }
                        // MP4 비디오 썸네일 (보이지 않을 때)
                        firstMedia.isVideo -> {
                            AsyncImage(
                                model = firstMedia.thumbnailUrl,
                                contentDescription = "Video Thumbnail",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        }
                        // GIF인 경우: 화면에 보일 때 움짤 재생
                        firstMedia.isGif && isVisible && !firstMedia.umjjalUrl.isNullOrEmpty() -> {
                            AsyncImage(
                                model = ImageRequest.Builder(context)
                                    .data(firstMedia.umjjalUrl)
                                    .repeatCount(-1) // 무한 반복 (-1 = INFINITE)
                                    .crossfade(true)
                                    .build(),
                                imageLoader = gifImageLoader,
                                contentDescription = "GIF Media",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Fit
                            )
                        }
                        // GIF 썸네일 (보이지 않을 때)
                        firstMedia.isGif -> {
                            AsyncImage(
                                model = firstMedia.thumbnailUrl,
                                contentDescription = "GIF Thumbnail",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Fit
                            )
                            // GIF 라벨 표시
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
                        // 일반 이미지
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

                    // 비디오 duration 표시 (우측 상단) - Old 프로젝트 스타일: 흰색 텍스트 + 그림자
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

                    // 미디어 개수 표시
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

            // 6. 통계 섹션 (viewCount 제외)
            // 안쪽 카운트 영역 - 항상 기본 아이콘 사용 (색칠 안됨)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 10.dp, top = 13.dp, end = 16.dp, bottom = 13.dp),
                horizontalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                // FEED 타입에서만 하트 카운트 표시
                if (type == ArticleType.FEED) {
                    StatItem(
                        iconRes = R.drawable.icon_community_heart,
                        count = heartCount,
                        onClick = onHeartClick
                    )
                }
                // 좋아요 - 안쪽 영역은 항상 기본 아이콘 (Old: likeCountIcon은 항상 icon_board_like)
                StatItem(
                    iconRes = R.drawable.icon_board_like,
                    count = likeCount,
                    tintColor = ColorPalette.textDefault,
                    onClick = onLikeClick
                )
                StatItem(R.drawable.icon_community_comment, commentCount, tintColor = ColorPalette.textDefault)
            }

            HorizontalDivider(
                color = ColorPalette.gray110,
                thickness = 0.3.dp
            )

            // 7. 액션 버튼 (type에 따라 다르게 표시)
            // 하단 버튼 영역 - 좋아요 상태에 따라 아이콘 변경 (Old: ivLike)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(40.dp)
            ) {
                // FEED 타입: 하트투표, 좋아요, 댓글
                // FREE_BOARD 타입: 좋아요, 댓글
                if (type == ArticleType.FEED) {
                    ActionButton(
                        iconRes = R.drawable.icon_community_heart,
                        label = stringResource(R.string.lable_community_heart_vote),
                        onClick = onHeartClick,
                        modifier = Modifier.weight(1f)
                    )
                    VerticalDivider()
                }
                // 좋아요 버튼 - 상태에 따라 아이콘 변경 (isLiked면 active 아이콘)
                ActionButton(
                    iconRes = if (isLiked) R.drawable.icon_board_like_active else R.drawable.icon_board_like,
                    label = stringResource(R.string.support_sympathy),
                    onClick = onLikeClick,
                    modifier = Modifier.weight(1f),
                    tintColor = if (isLiked) null else ColorPalette.textDefault
                )
                VerticalDivider()
                ActionButton(
                    iconRes = R.drawable.icon_community_comment,
                    label = stringResource(R.string.lable_community_comment),
                    onClick = onCommentClick,
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
        // 아이콘 (14dp x 14dp)
        Icon(
            painter = painterResource(iconRes),
            contentDescription = null,
            modifier = Modifier.size(14.dp),
            tint = tintColor ?: Color.Unspecified
        )
        Spacer(modifier = Modifier.width(3.dp))
        // 애니메이션 없이 즉시 표시 (Old 프로젝트와 동일)
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
            // 아이콘 (14dp x 14dp)
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
