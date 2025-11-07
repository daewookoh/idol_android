package net.ib.mn.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import net.ib.mn.ui.theme.ColorPalette
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import coil.compose.AsyncImage
import coil.compose.rememberAsyncImagePainter
import net.ib.mn.R

/**
 * ExoArticle - 게시글 아티클 컴포넌트
 *
 * Old 프로젝트의 community_item.xml + CommunityArticleViewHolder 기능을 Compose로 재현
 *
 * @param profileImageUrl 사용자 프로필 이미지 URL
 * @param userName 사용자 이름
 * @param userLevel 사용자 레벨
 * @param createdAt 작성 시간 문자열
 * @param title 게시글 제목
 * @param content 게시글 내용
 * @param mediaUrls 미디어 URL 리스트
 * @param heartCount 하트 수
 * @param likeCount 좋아요 수
 * @param commentCount 댓글 수
 * @param viewCount 조회수
 * @param isPrivate 최애만 보기 여부
 * @param isPopular 인기 게시글 여부
 * @param tag 태그
 * @param onProfileClick 프로필 클릭 콜백
 * @param onMoreClick 더보기 버튼 클릭 콜백
 * @param onHeartClick 하트 투표 클릭 콜백
 * @param onLikeClick 좋아요 클릭 콜백
 * @param onCommentClick 댓글 클릭 콜백
 * @param modifier Modifier
 */
@Composable
fun ExoArticle(
    profileImageUrl: String,
    userName: String,
    userLevel: Int = 0,
    createdAt: String,
    title: String? = null,
    content: String,
    mediaUrls: List<String> = emptyList(),
    heartCount: Int = 0,
    likeCount: Int = 0,
    commentCount: Int = 0,
    viewCount: Int = 0,
    isPrivate: Boolean = false,
    isPopular: Boolean = false,
    tag: String? = null,
    onProfileClick: () -> Unit = {},
    onMoreClick: () -> Unit = {},
    onHeartClick: () -> Unit = {},
    onLikeClick: () -> Unit = {},
    onCommentClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
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
                    // 이름 + 레벨 아이콘
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // 레벨 아이콘
                        if (userLevel in 0..9) {
                            val levelIconRes = when (userLevel) {
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
                            Image(
                                painter = painterResource(levelIconRes),
                                contentDescription = "Level $userLevel",
                                modifier = Modifier
                                    .padding(top = 4.dp, end = 2.dp)
                            )
                        }

                        Text(
                            text = userName,
                            fontSize = 14.sp,
                            color = ColorPalette.main,
                            fontWeight = FontWeight.Normal
                        )
                    }

                    // 작성 시간
                    Row(
                        verticalAlignment = Alignment.Bottom,
                        modifier = Modifier.padding(top = 5.dp)
                    ) {
                        Text(
                            text = createdAt,
                            fontSize = 12.sp,
                            color = ColorPalette.textDimmed
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
                        fontSize = 13.sp,
                        color = ColorPalette.mainLight
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
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = ColorPalette.textDefault,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // 4. 내용
            Text(
                text = content,
                fontSize = 14.sp,
                color = ColorPalette.textDefault,
                maxLines = if (isExpanded) Int.MAX_VALUE else 3,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
            )

            // 더보기 버튼
            if (!isExpanded && content.length > 100) {
                Text(
                    text = "... 더보기",
                    fontSize = 13.sp,
                    color = ColorPalette.textDimmed,
                    modifier = Modifier
                        .padding(start = 20.dp, top = 13.dp, end = 20.dp)
                        .clickable { isExpanded = true }
                )
            }

            // 5. 미디어 (첫 번째 이미지만 표시)
            if (mediaUrls.isNotEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f)
                        .background(ColorPalette.background100)
                ) {
                    AsyncImage(
                        model = mediaUrls.first(),
                        contentDescription = "Media",
                        modifier = Modifier.fillMaxWidth(),
                        contentScale = ContentScale.Crop
                    )

                    if (mediaUrls.size > 1) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(top = 29.dp, end = 16.dp)
                                .height(24.dp)
                                .background(
                                    color = ColorPalette.textDefault.copy(alpha = 0.7f),
                                    shape = RoundedCornerShape(13.dp)
                                )
                                .padding(horizontal = 8.dp)
                        ) {
                            Text(
                                text = "1/${mediaUrls.size}",
                                fontSize = 10.sp,
                                color = ColorPalette.textWhiteBlack,
                                modifier = Modifier.align(Alignment.Center)
                            )
                        }
                    }
                }
            }

            // 6. 통계 섹션
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 10.dp, top = 13.dp, end = 16.dp, bottom = 13.dp),
                horizontalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                StatItem(R.drawable.icon_community_heart, heartCount)
                StatItem(R.drawable.icon_board_like, likeCount)
                StatItem(R.drawable.icon_community_comment, commentCount, tintColor = ColorPalette.textDefault)
                StatItem(R.drawable.icon_board_hits, viewCount, tintColor = ColorPalette.textDefault)
            }

            Divider(
                color = ColorPalette.gray110,
                thickness = 0.3.dp
            )

            // 7. 액션 버튼
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(40.dp)
            ) {
                ActionButton(
                    iconRes = R.drawable.icon_community_heart,
                    label = stringResource(R.string.lable_community_heart_vote),
                    onClick = onHeartClick,
                    modifier = Modifier.weight(1f)
                )
                Divider(
                    color = ColorPalette.gray110,
                    modifier = Modifier
                        .width(0.3.dp)
                        .height(40.dp)
                )
                ActionButton(
                    iconRes = R.drawable.icon_board_like,
                    label = stringResource(R.string.support_sympathy),
                    onClick = onLikeClick,
                    modifier = Modifier.weight(1f)
                )
                Divider(
                    color = ColorPalette.gray110,
                    modifier = Modifier
                        .width(0.3.dp)
                        .height(40.dp)
                )
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
private fun StatItem(
    iconRes: Int,
    count: Int,
    tintColor: Color? = null
) {
    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 아이콘 (14dp x 14dp)
        Icon(
            painter = painterResource(iconRes),
            contentDescription = null,
            modifier = Modifier.size(14.dp),
            tint = tintColor ?: Color.Unspecified
        )
        Spacer(modifier = Modifier.width(3.dp))
        Text(
            text = count.toString(),
            fontSize = 13.sp,
            color = ColorPalette.textDefault
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
            .clickable(onClick = onClick),
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
                fontSize = 14.sp,
                color = ColorPalette.textGray
            )
        }
    }
}
