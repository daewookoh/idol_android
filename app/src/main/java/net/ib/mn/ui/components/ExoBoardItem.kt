package net.ib.mn.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import java.text.SimpleDateFormat
import java.util.*
import net.ib.mn.R
import net.ib.mn.domain.model.ArticleFile
import net.ib.mn.domain.model.ArticleModel
import net.ib.mn.domain.model.ArticleUser
import net.ib.mn.domain.model.NoticeModel
import net.ib.mn.ui.theme.ColorPalette

/**
 * 게시글 아이템 타입
 */
enum class ExoBoardItemType {
    NORMAL,  // 전체 표시 (프로필, 제목, 내용, 이미지, 통계, 액션 버튼)
    MINI,    // 간단 표시 (제목, 내용 한줄, 작성자-작성시간, 좋아요/댓글/조회수)
    NOTICE   // 공지사항/고정글 (핀 아이콘 + 제목)
}

/**
 * 게시글 아이템 컴포넌트
 *
 * old 프로젝트의 community_item.xml 레이아웃을 Compose로 구현
 *
 * @param article 게시글 모델
 * @param onItemClick 아이템 클릭 콜백
 * @param onUserClick 사용자 프로필 클릭 콜백
 * @param onLikeClick 좋아요 클릭 콜백
 * @param onCommentClick 댓글 클릭 콜백
 * @param onMoreClick 더보기 클릭 콜백
 * @param modifier Modifier
 * @param itemType 아이템 타입 (NORMAL, MINI)
 * @param showTag 태그 표시 여부
 * @param tagName 태그 이름
 * @param showPopularIcon 인기글 아이콘 표시 여부
 */
@Composable
fun ExoBoardItem(
    article: ArticleModel,
    onItemClick: () -> Unit,
    onUserClick: () -> Unit = {},
    onLikeClick: () -> Unit = {},
    onCommentClick: () -> Unit = {},
    onMoreClick: () -> Unit = {},
    modifier: Modifier = Modifier,
    itemType: ExoBoardItemType = ExoBoardItemType.NORMAL,
    showTag: Boolean = false,
    tagName: String? = null,
    showPopularIcon: Boolean = false
) {
    when (itemType) {
        ExoBoardItemType.NORMAL -> {
            ExoBoardItemNormal(
                article = article,
                onItemClick = onItemClick,
                onUserClick = onUserClick,
                onLikeClick = onLikeClick,
                onCommentClick = onCommentClick,
                onMoreClick = onMoreClick,
                modifier = modifier,
                showTag = showTag,
                tagName = tagName,
                showPopularIcon = showPopularIcon
            )
        }
        ExoBoardItemType.MINI -> {
            ExoBoardItemMini(
                article = article,
                onItemClick = onItemClick,
                modifier = modifier,
                showPopularIcon = showPopularIcon
            )
        }
        ExoBoardItemType.NOTICE -> {
            // NOTICE 타입은 ExoBoardNoticeItem을 직접 사용해야 함
            // 이 케이스는 사용되지 않음 (ArticleModel이 아닌 NoticeModel 사용)
        }
    }
}

/**
 * 공지사항/고정글 아이템 컴포넌트
 * old 프로젝트의 item_board_notice.xml 레이아웃을 Compose로 구현
 *
 * @param notice 공지사항 모델
 * @param onItemClick 아이템 클릭 콜백
 * @param modifier Modifier
 * @param showDivider 하단 구분선 표시 여부
 */
@Composable
fun ExoBoardNoticeItem(
    notice: NoticeModel,
    onItemClick: () -> Unit,
    modifier: Modifier = Modifier,
    showDivider: Boolean = true
) {
    Column(
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(34.dp)
                .background(ColorPalette.main100)
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() },
                    onClick = onItemClick
                )
                .padding(horizontal = 20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 핀 아이콘
            Icon(
                painter = painterResource(R.drawable.icon_community_pin),
                contentDescription = "Notice",
                tint = androidx.compose.ui.graphics.Color.Unspecified
            )

            Spacer(modifier = Modifier.width(5.dp))

            // 공지 제목
            Text(
                text = notice.title,
                fontSize = 12.sp,
                color = ColorPalette.mainLight,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        // 하단 구분선
        if (showDivider) {
            HorizontalDivider(
                thickness = 0.3.dp,
                color = ColorPalette.main300
            )
        }
    }
}

/**
 * NORMAL 타입 게시글 아이템
 */
@Composable
private fun ExoBoardItemNormal(
    article: ArticleModel,
    onItemClick: () -> Unit,
    onUserClick: () -> Unit,
    onLikeClick: () -> Unit,
    onCommentClick: () -> Unit,
    onMoreClick: () -> Unit,
    modifier: Modifier,
    showTag: Boolean,
    tagName: String?,
    showPopularIcon: Boolean
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(ColorPalette.background100)
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() },
                onClick = onItemClick
            )
    ) {
        // Header: 프로필 이미지, 닉네임, 날짜, 더보기 버튼
        ArticleHeader(
            user = article.user,
            createdAt = article.createdAt,
            isMostOnly = article.isMostOnly == "Y",
            onUserClick = onUserClick,
            onMoreClick = onMoreClick
        )

        // 태그 (선택적)
        if (showTag && !tagName.isNullOrEmpty()) {
            ArticleTag(tagName = tagName)
        }

        // 제목 (인기글 아이콘 포함)
        if (!article.title.isNullOrEmpty()) {
            ArticleTitle(
                title = article.title,
                isPopular = showPopularIcon && article.isPopular
            )
        }

        // 본문 내용
        if (!article.content.isNullOrEmpty()) {
            ArticleContent(content = article.content)
        }

        // 이미지 (첨부파일이 있는 경우)
        if (article.files.isNotEmpty()) {
            ArticleImages(
                files = article.files,
                onClick = onItemClick
            )
        }

        // 통계 (좋아요, 댓글, 조회수)
        ArticleStats(
            likeCount = article.likeCount,
            commentCount = article.commentCount,
            viewCount = article.viewCount,
            isUserLike = article.isUserLike
        )

        // 하단 액션 버튼 (좋아요, 댓글)
        ArticleActions(
            isUserLike = article.isUserLike,
            onLikeClick = onLikeClick,
            onCommentClick = onCommentClick
        )

        // 구분선
        HorizontalDivider(
            thickness = 10.dp,
            color = ColorPalette.gray80
        )
    }
}

/**
 * MINI 타입 게시글 아이템
 * 제목, 내용 한줄, 작성자-작성시간, 좋아요/댓글/조회수, 썸네일(있을 경우)
 * old 프로젝트의 item_small_talk.xml 참고
 */
@Composable
private fun ExoBoardItemMini(
    article: ArticleModel,
    onItemClick: () -> Unit,
    modifier: Modifier,
    showPopularIcon: Boolean
) {
    // 이미지 파일 찾기 (files 배열 또는 직접 imageUrl/thumbnailUrl 필드)
    val imageFiles = article.files.filter { it.fileType == "image" || it.fileUrl?.contains("image") == true }
    val thumbnailUrl = when {
        // 1. ArticleModel의 직접 thumbnailUrl 필드 확인
        !article.thumbnailUrl.isNullOrEmpty() -> article.thumbnailUrl
        // 2. ArticleModel의 직접 imageUrl 필드 확인
        !article.imageUrl.isNullOrEmpty() -> article.imageUrl
        // 3. files 배열에서 찾기
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
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() },
                    onClick = onItemClick
                )
                .padding(horizontal = 20.dp, vertical = 17.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 왼쪽 컨텐츠 영역
            Column(
                modifier = Modifier
                    .weight(1f)
                    .then(
                        if (thumbnailUrl != null) Modifier.padding(end = 12.dp) else Modifier
                    )
            ) {
                // 제목 (인기글 아이콘 포함)
                if (!article.title.isNullOrEmpty()) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 0.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // 인기글 아이콘 (HOT일 경우 이미지 사용)
                        if (showPopularIcon && article.isPopular) {
                            Icon(
                                painter = painterResource(R.drawable.icon_popularpost_title),
                                contentDescription = "Popular",
                                modifier = Modifier.padding(end = 5.dp),
                                tint = androidx.compose.ui.graphics.Color.Unspecified
                            )
                        }

                        Text(
                            text = article.title,
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
                        text = article.content,
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
                    // 작성자
                    Text(
                        text = article.user?.nickname ?: "",
                        fontSize = 11.sp,
                        color = ColorPalette.textDimmed,
                        maxLines = 1
                    )

                    // 구분점
                    Text(
                        text = " · ",
                        fontSize = 11.sp,
                        color = ColorPalette.textDimmed
                    )

                    // 작성시간
                    Text(
                        text = formatDateShort(article.createdAt),
                        fontSize = 11.sp,
                        color = ColorPalette.textDimmed
                    )
                }

                // 좋아요, 댓글, 조회수
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = 0.dp)
                ) {
                    // 좋아요
                    Icon(
                        painter = painterResource(
                            if (article.isUserLike) R.drawable.icon_board_like_active else R.drawable.icon_board_like
                        ),
                        contentDescription = "Like",
                        modifier = Modifier.size(14.dp),
                        tint = if (article.isUserLike) ColorPalette.main else ColorPalette.textGray
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = article.likeCount.toString(),
                        fontSize = 12.sp,
                        color = ColorPalette.textGray
                    )

                    Spacer(modifier = Modifier.width(14.dp))

                    // 댓글
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

                    // 조회수
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

                    // 이미지 개수 표시 (2개 이상일 경우)
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

/**
 * 게시글 헤더 (프로필, 닉네임, 날짜, 더보기)
 */
@Composable
private fun ArticleHeader(
    user: ArticleUser?,
    createdAt: String?,
    isMostOnly: Boolean,
    onUserClick: () -> Unit,
    onMoreClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 20.dp, top = 13.dp, end = 11.dp, bottom = 11.dp),
        verticalAlignment = Alignment.Top
    ) {
        // 프로필 이미지
        AsyncImage(
            model = user?.imageUrlCommunity,
            contentDescription = "Profile",
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .clickable(onClick = onUserClick),
            contentScale = ContentScale.Crop,
            placeholder = painterResource(R.drawable.menu_profile_default2),
            error = painterResource(R.drawable.menu_profile_default2)
        )

        Spacer(modifier = Modifier.width(10.dp))

        // 닉네임 및 날짜
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 레벨 아이콘
                Icon(
                    painter = painterResource(getLevelIconRes(user?.level ?: 0)),
                    contentDescription = "Level",
                    modifier = Modifier.size(16.dp),
                    tint = ColorPalette.main
                )

                Spacer(modifier = Modifier.width(2.dp))

                // 닉네임
                Text(
                    text = user?.nickname ?: "",
                    fontSize = 14.sp,
                    color = ColorPalette.main,
                    fontWeight = FontWeight.Normal,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(5.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 날짜
                Text(
                    text = formatDate(createdAt),
                    fontSize = 12.sp,
                    color = ColorPalette.textDimmed
                )

                // 최애만 보기 아이콘
                if (isMostOnly) {
                    Spacer(modifier = Modifier.width(7.dp))
                    Icon(
                        painter = painterResource(R.drawable.icon_onlymyidol),
                        contentDescription = "Only my idol",
                        modifier = Modifier.height(13.dp),
                        tint = ColorPalette.main
                    )
                }
            }
        }

        // 더보기 버튼
        Icon(
            painter = painterResource(R.drawable.icon_view_more),
            contentDescription = "More",
            modifier = Modifier
                .clickable(onClick = onMoreClick)
                .padding(4.dp),
            tint = ColorPalette.textGray
        )
    }
}

/**
 * 게시글 태그
 */
@Composable
private fun ArticleTag(tagName: String) {
    Box(
        modifier = Modifier
            .padding(horizontal = 20.dp)
            .padding(top = 9.dp, bottom = 6.dp)
            .background(
                color = ColorPalette.main200,
                shape = RoundedCornerShape(6.dp)
            )
            .padding(horizontal = 7.dp, vertical = 3.dp)
    ) {
        Text(
            text = tagName,
            fontSize = 13.sp,
            color = ColorPalette.mainLight
        )
    }
}

/**
 * 게시글 제목
 */
@Composable
private fun ArticleTitle(
    title: String,
    isPopular: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .padding(bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 인기글 아이콘
        if (isPopular) {
            Icon(
                painter = painterResource(R.drawable.icon_popularpost_title),
                contentDescription = "Popular",
                modifier = Modifier.padding(end = 5.dp),
                tint = ColorPalette.main
            )
        }

        Text(
            text = title,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = ColorPalette.textDefault,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/**
 * 게시글 본문
 */
@Composable
private fun ArticleContent(content: String) {
    Text(
        text = content,
        fontSize = 14.sp,
        color = ColorPalette.textDefault,
        maxLines = 3,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
    )
}

/**
 * 게시글 이미지
 */
@Composable
private fun ArticleImages(
    files: List<ArticleFile>,
    onClick: () -> Unit
) {
    val imageFile = files.firstOrNull { it.fileType == "image" || it.fileUrl?.contains("image") == true }

    if (imageFile != null) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 13.dp)
                .clickable(onClick = onClick)
        ) {
            AsyncImage(
                model = imageFile.thumbnailUrl ?: imageFile.fileUrl,
                contentDescription = "Article image",
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f),
                contentScale = ContentScale.Crop
            )

            // 이미지 개수 표시
            if (files.size > 1) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 29.dp, end = 16.dp)
                        .background(
                            color = ColorPalette.textDefault.copy(alpha = 0.7f),
                            shape = RoundedCornerShape(10.dp)
                        )
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "1/${files.size}",
                        fontSize = 10.sp,
                        color = ColorPalette.textLight
                    )
                }
            }
        }
    }
}

/**
 * 게시글 통계 (좋아요, 댓글, 조회수)
 */
@Composable
private fun ArticleStats(
    likeCount: Int,
    commentCount: Int,
    viewCount: Int,
    isUserLike: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .padding(top = 13.dp, bottom = 13.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 좋아요
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(end = 18.dp)
        ) {
            Icon(
                painter = painterResource(
                    if (isUserLike) R.drawable.icon_board_like_active else R.drawable.icon_board_like
                ),
                contentDescription = "Like",
                modifier = Modifier.size(14.dp),
                tint = if (isUserLike) ColorPalette.main else ColorPalette.textDefault
            )
            Spacer(modifier = Modifier.width(3.dp))
            Text(
                text = likeCount.toString(),
                fontSize = 13.sp,
                color = ColorPalette.textDefault
            )
        }

        // 댓글
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(end = 18.dp)
        ) {
            Icon(
                painter = painterResource(R.drawable.icon_community_comment),
                contentDescription = "Comment",
                modifier = Modifier.size(14.dp),
                tint = ColorPalette.textDefault
            )
            Spacer(modifier = Modifier.width(3.dp))
            Text(
                text = commentCount.toString(),
                fontSize = 13.sp,
                color = ColorPalette.textDefault
            )
        }

        // 조회수
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                painter = painterResource(R.drawable.icon_board_hits),
                contentDescription = "Views",
                modifier = Modifier.size(14.dp),
                tint = ColorPalette.textDefault
            )
            Spacer(modifier = Modifier.width(3.dp))
            Text(
                text = viewCount.toString(),
                fontSize = 13.sp,
                color = ColorPalette.textDefault
            )
        }
    }
}

/**
 * 게시글 하단 액션 버튼
 */
@Composable
private fun ArticleActions(
    isUserLike: Boolean,
    onLikeClick: () -> Unit,
    onCommentClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(40.dp)
    ) {
        // 좋아요 버튼
        Row(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .clickable(onClick = onLikeClick),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                painter = painterResource(
                    if (isUserLike) R.drawable.icon_board_like_active else R.drawable.icon_board_like
                ),
                contentDescription = "Like",
                modifier = Modifier.size(14.dp),
                tint = if (isUserLike) ColorPalette.main else ColorPalette.textGray
            )
            Spacer(modifier = Modifier.width(7.dp))
            Text(
                text = stringResource(R.string.support_sympathy),
                fontSize = 14.sp,
                color = ColorPalette.textGray
            )
        }

        // 구분선
        Box(
            modifier = Modifier
                .width(0.3.dp)
                .fillMaxHeight()
                .background(ColorPalette.gray110)
        )

        // 댓글 버튼
        Row(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .clickable(onClick = onCommentClick),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                painter = painterResource(R.drawable.icon_community_comment),
                contentDescription = "Comment",
                modifier = Modifier.size(14.dp),
                tint = ColorPalette.textGray
            )
            Spacer(modifier = Modifier.width(7.dp))
            Text(
                text = stringResource(R.string.lable_community_comment),
                fontSize = 14.sp,
                color = ColorPalette.textGray
            )
        }
    }

    // 상단 구분선
    HorizontalDivider(
        thickness = 0.3.dp,
        color = ColorPalette.gray110
    )
}

/**
 * 레벨에 따른 아이콘 리소스 반환
 */
private fun getLevelIconRes(level: Int): Int {
    return when (level) {
        0 -> R.drawable.icon_level_0
        1 -> R.drawable.icon_level_1
        2 -> R.drawable.icon_level_2
        3 -> R.drawable.icon_level_3
        4 -> R.drawable.icon_level_4
        5 -> R.drawable.icon_level_5
        6 -> R.drawable.icon_level_6
        7 -> R.drawable.icon_level_7
        8 -> R.drawable.icon_level_8
        9 -> R.drawable.icon_level_9
        else -> R.drawable.icon_level_0
    }
}

/**
 * 날짜 포맷팅 (전체)
 * 서버에서 받은 ISO 형식 날짜 문자열을 표시 형식으로 변환
 */
private fun formatDate(dateString: String?): String {
    if (dateString.isNullOrEmpty()) return ""

    return try {
        // 서버 날짜 형식: "2025-11-23T03:43:40"
        val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
        val outputFormat = SimpleDateFormat("yyyy.M.d a h:mm", Locale.getDefault())
        val date = inputFormat.parse(dateString)
        if (date != null) outputFormat.format(date) else dateString
    } catch (e: Exception) {
        dateString
    }
}

/**
 * 날짜 포맷팅 (간략)
 * MINI 타입에서 사용
 * 오늘: 시간 표시 (오후 2:20)
 * 그 외: 날짜 표시 (2023.03.17)
 */
private fun formatDateShort(dateString: String?): String {
    if (dateString.isNullOrEmpty()) return ""

    return try {
        val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
        val date = inputFormat.parse(dateString) ?: return dateString

        val now = Calendar.getInstance()
        val dateCalendar = Calendar.getInstance().apply { time = date }

        val isToday = now.get(Calendar.YEAR) == dateCalendar.get(Calendar.YEAR) &&
                now.get(Calendar.DAY_OF_YEAR) == dateCalendar.get(Calendar.DAY_OF_YEAR)

        if (isToday) {
            // 오늘이면 시간만 표시
            val timeFormat = SimpleDateFormat("a h:mm", Locale.getDefault())
            timeFormat.format(date)
        } else {
            // 그 외 날짜 표시
            val dateFormat = SimpleDateFormat("yyyy.MM.dd", Locale.getDefault())
            dateFormat.format(date)
        }
    } catch (e: Exception) {
        dateString
    }
}

@Preview(showBackground = true)
@Composable
fun ExoBoardItemNormalPreview() {
    ExoBoardItem(
        article = ArticleModel(
            id = "1",
            title = "같이 콘서트 갈사람 있나요!!",
            content = "내용 자리 입니다. 테스트 내용을 적어봅니다. 조금 더 긴 내용을 테스트해봅니다.",
            likeCount = 123,
            commentCount = 45,
            viewCount = 1000,
            isPopular = true,
            user = ArticleUser(
                id = 1,
                nickname = "닉네임",
                level = 5
            )
        ),
        onItemClick = {},
        itemType = ExoBoardItemType.NORMAL,
        showTag = true,
        tagName = "자유",
        showPopularIcon = true
    )
}

@Preview(showBackground = true)
@Composable
fun ExoBoardItemMiniPreview() {
    ExoBoardItem(
        article = ArticleModel(
            id = "1",
            title = "같이 콘서트 갈사람 있나요!!",
            content = "내용 자리 입니다. 테스트 내용을 적어봅니다. 조금 더 긴 내용을 테스트해봅니다.",
            likeCount = 123,
            commentCount = 45,
            viewCount = 1000,
            isPopular = true,
            createdAt = "2025-11-25T10:30:00",
            user = ArticleUser(
                id = 1,
                nickname = "닉네임",
                level = 5
            )
        ),
        onItemClick = {},
        itemType = ExoBoardItemType.MINI,
        showPopularIcon = true
    )
}
