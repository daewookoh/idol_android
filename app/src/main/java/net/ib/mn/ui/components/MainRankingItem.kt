package net.ib.mn.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.CachePolicy
import coil.request.ImageRequest
import net.ib.mn.R

/**
 * MainRankingItem - old 프로젝트 ranking_item.xml 기반 리뉴얼
 *
 * 주요 기능:
 * 1. 프로필 이미지 + 테두리 (miracleCount, fairyCount, angelCount 기준)
 * 2. 배지 시스템 (생일, 데뷔, 컴백, 몰빵일, 올인데이)
 * 3. 순위 + 이름 + 그룹명
 * 4. 투표수 프로그레스 바
 * 5. 아이콘 배지 (Angel, Fairy, Miracle, Rookie, Super Rookie)
 * 6. 하트 투표 버튼
 * 7. 최애 하이라이트 (배경색 변경)
 * 8. 펼치기 기능 (ExoTop3 사용)
 *
 * 최적화:
 * - remember로 변경된 값만 리컴포지션
 * - 이미지/영상 URL 24시간 캐싱
 */
@Composable
fun MainRankingItem(
    rank: Int,
    name: String,
    voteCount: String,
    photoUrl: String? = null,
    groupName: String? = null,
    anniversary: String? = null,
    anniversaryDays: Int = 0,
    miracleCount: Int = 0,
    fairyCount: Int = 0,
    angelCount: Int = 0,
    rookieCount: Int = 0,
    superRookieCount: Int = 0,
    isFavorite: Boolean = false,
    heartCount: Long = 0,
    maxHeartCount: Long = 0,
    minHeartCount: Long = 0,
    top3ImageUrls: List<String?> = listOf(null, null, null),
    top3VideoUrls: List<String?> = listOf(null, null, null),
    showDivider: Boolean = false,
    onClick: () -> Unit = {},
    onVote: () -> Unit = {},
    onPhotoClick: () -> Unit = {}
) {
    var isExpanded by remember { mutableStateOf(false) }

    // 최애 여부에 따른 배경색 - remember로 캐싱
    val backgroundColor = remember(isFavorite) {
        if (isFavorite) R.color.main100 else R.color.background_100
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(colorResource(backgroundColor))
    ) {
        // 메인 랭킹 아이템
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onClick() }
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 프로필 영역 (81dp)
            Box(
                modifier = Modifier.width(81.dp),
                contentAlignment = Alignment.Center
            ) {
                // 프로필 테두리 + 이미지
                Box(
                    modifier = Modifier.size(55.dp),
                    contentAlignment = Alignment.Center
                ) {
                    // 테두리 - remember로 캐싱
                    Box(
                        modifier = Modifier
                            .size(55.dp)
                            .border(
                                BorderStroke(
                                    2.dp,
                                    getProfileBorderColor(miracleCount, fairyCount, angelCount)
                                ),
                                CircleShape
                            )
                    )

                    // 프로필 이미지 (45dp) - 24시간 캐싱
                    val context = LocalContext.current
                    val imageRequest = remember(photoUrl) {
                        ImageRequest.Builder(context)
                            .data(photoUrl)
                            .memoryCacheKey(photoUrl)
                            .diskCacheKey(photoUrl)
                            .diskCachePolicy(CachePolicy.ENABLED)
                            .memoryCachePolicy(CachePolicy.ENABLED)
                            .crossfade(true)
                            .build()
                    }

                    AsyncImage(
                        model = imageRequest,
                        contentDescription = "프로필 이미지",
                        modifier = Modifier
                            .size(45.dp)
                            .clip(CircleShape)
                            .background(colorResource(R.color.gray100))
                            .clickable { onPhotoClick(); isExpanded = !isExpanded },
                        contentScale = ContentScale.Crop
                    )
                }

                // 배지들
                AnniversaryBadges(
                    anniversary = anniversary,
                    anniversaryDays = anniversaryDays,
                    modifier = Modifier.size(55.dp)
                )
            }

            // 정보 영역
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 10.dp)
            ) {
                // 순위 + 이름 + 그룹명
                Row(
                    verticalAlignment = Alignment.Bottom
                ) {
                    // 순위 - remember로 캐싱
                    Text(
                        text = remember(rank) { "${rank}위" },
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = colorResource(R.color.main)
                    )

                    Spacer(modifier = Modifier.width(5.dp))

                    // 이름
                    Text(
                        text = name,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = colorResource(R.color.text_default),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    // 그룹명 - 이름과 같은 색상
                    if (groupName != null) {
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = groupName,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = colorResource(R.color.text_default),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // 프로그레스 바 + 투표수
                VoteProgressBar(
                    voteCount = voteCount,
                    heartCount = heartCount,
                    maxHeartCount = maxHeartCount,
                    minHeartCount = minHeartCount
                )

                Spacer(modifier = Modifier.height(2.dp))

                // 아이콘 배지
                IconBadges(
                    angelCount = angelCount,
                    fairyCount = fairyCount,
                    miracleCount = miracleCount,
                    rookieCount = rookieCount,
                    superRookieCount = superRookieCount
                )
            }

            // 하트 투표 버튼 (50dp)
            IconButton(
                onClick = onVote,
                modifier = Modifier.size(50.dp)
            ) {
                Text(
                    text = "♥",
                    fontSize = 24.sp,
                    color = colorResource(R.color.main)
                )
            }
        }

        // 펼치기 영역 (ExoTop3 사용)
        AnimatedVisibility(
            visible = isExpanded,
            enter = expandVertically(),
            exit = shrinkVertically()
        ) {
            ExoTop3(
                id = remember(rank) { "ranking_item_$rank" },
                imageUrls = top3ImageUrls,
                videoUrls = top3VideoUrls,
                isVisible = isExpanded
            )
        }
    }
}

/**
 * 프로필 테두리 색상
 * miracleCount, fairyCount, angelCount에 따라 다른 색상 반환
 */
@Composable
private fun getProfileBorderColor(
    miracleCount: Int,
    fairyCount: Int,
    angelCount: Int
): Color {
    val miracleColor = colorResource(R.color.border_miracle)
    val fairyColor = colorResource(R.color.border_fairy)
    val angelColor = colorResource(R.color.border_angel)
    val defaultColor = colorResource(R.color.gray300)

    return remember(miracleCount, fairyCount, angelCount, miracleColor, fairyColor, angelColor, defaultColor) {
        when {
            miracleCount > 0 -> miracleColor
            fairyCount > 0 -> fairyColor
            angelCount > 0 -> angelColor
            else -> defaultColor
        }
    }
}

/**
 * 기념일 배지
 * 생일, 데뷔, 컴백, 몰빵일, 올인데이
 */
@Composable
private fun AnniversaryBadges(
    anniversary: String?,
    anniversaryDays: Int,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier) {
        when (anniversary) {
            "BIRTH" -> {
                // 생일 배지
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(start = 5.dp, top = 3.dp)
                        .size(16.dp)
                        .background(colorResource(R.color.badge_birth), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "B",
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                        color = colorResource(R.color.white)
                    )
                }
            }
            "DEBUT" -> {
                // 데뷔 배지
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(start = 5.dp, top = 3.dp)
                        .size(16.dp)
                        .background(colorResource(R.color.badge_debut), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "D",
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                        color = colorResource(R.color.white)
                    )
                }
            }
            "COMEBACK" -> {
                // 컴백 배지
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(start = 3.dp)
                        .size(16.dp)
                        .background(colorResource(R.color.badge_comeback), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "C",
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                        color = colorResource(R.color.white)
                    )
                }
            }
            "MEMORIAL_DAY" -> {
                // 몰빵일 - 일수 표시
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .background(
                            color = colorResource(R.color.badge_memorial_day),
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(4.dp)
                        )
                        .padding(horizontal = 5.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = remember(anniversaryDays) { "${anniversaryDays}일" },
                        fontSize = 7.sp,
                        fontWeight = FontWeight.Bold,
                        color = colorResource(R.color.white)
                    )
                }
            }
            "ALL_IN_DAY" -> {
                // 올인데이 배지
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(start = 15.dp, top = 5.dp)
                        .size(16.dp)
                        .background(colorResource(R.color.badge_allin_day), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "A",
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                        color = colorResource(R.color.white)
                    )
                }
            }
        }
    }
}

/**
 * 투표수 프로그레스 바
 * old 프로젝트의 VotePercentage.getVotePercentage() 로직 구현
 */
@Composable
private fun VoteProgressBar(
    voteCount: String,
    heartCount: Long,
    maxHeartCount: Long,
    minHeartCount: Long
) {
    // 프로그레스 비율 계산 (최소 10%, 최대 100%) - remember로 캐싱
    val progressPercent = remember(heartCount, maxHeartCount, minHeartCount) {
        if (maxHeartCount > 0) {
            val range = (maxHeartCount - minHeartCount).toFloat()
            if (range > 0) {
                val progress = ((heartCount - minHeartCount).toFloat() / range)
                (0.1f + (progress * 0.9f)).coerceIn(0.1f, 1f)
            } else {
                1f
            }
        } else {
            1f
        }
    }

    val animatedProgress by animateFloatAsState(targetValue = progressPercent, label = "progress")

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(17.dp)
    ) {
        // 프로그레스 바 배경
        Box(
            modifier = Modifier
                .fillMaxWidth(animatedProgress)
                .height(17.dp)
                .background(
                    color = colorResource(R.color.main100),
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp)
                )
        )

        // 투표수 텍스트
        Text(
            text = voteCount,
            fontSize = 11.sp,
            color = colorResource(R.color.text_heart_votes),
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 10.dp)
        )
    }
}

/**
 * 아이콘 배지 (Angel, Fairy, Miracle, Rookie, Super Rookie)
 * old 프로젝트의 setIdolBadgeIcon() 로직 구현
 */
@Composable
private fun IconBadges(
    angelCount: Int,
    fairyCount: Int,
    miracleCount: Int,
    rookieCount: Int,
    superRookieCount: Int
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Angel 배지
        if (angelCount > 0) {
            BadgeIcon(
                count = angelCount,
                backgroundColor = colorResource(R.color.badge_angel_bg),
                textColor = colorResource(R.color.white)
            )
        }

        // Fairy 배지
        if (fairyCount > 0) {
            BadgeIcon(
                count = fairyCount,
                backgroundColor = colorResource(R.color.badge_fairy_bg),
                textColor = colorResource(R.color.white)
            )
        }

        // Miracle 배지
        if (miracleCount > 0) {
            BadgeIcon(
                count = miracleCount,
                backgroundColor = colorResource(R.color.badge_miracle_bg),
                textColor = colorResource(R.color.white)
            )
        }

        // Rookie 배지
        if (rookieCount > 0) {
            BadgeIcon(
                count = rookieCount,
                backgroundColor = colorResource(R.color.badge_rookie_bg),
                textColor = colorResource(R.color.white)
            )
        }

        // Super Rookie 배지
        if (superRookieCount > 0) {
            Box(
                modifier = Modifier
                    .size(13.dp, 16.dp)
                    .background(
                        color = colorResource(R.color.badge_super_rookie_bg),
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(2.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "S",
                    fontSize = 7.sp,
                    fontWeight = FontWeight.Bold,
                    color = colorResource(R.color.white)
                )
            }
        }
    }
}

/**
 * 배지 아이콘 (Angel, Fairy, Miracle, Rookie)
 */
@Composable
private fun BadgeIcon(
    count: Int,
    backgroundColor: Color,
    textColor: Color
) {
    Box(
        modifier = Modifier
            .size(13.dp, 16.dp)
            .background(
                color = backgroundColor,
                shape = androidx.compose.foundation.shape.RoundedCornerShape(2.dp)
            ),
        contentAlignment = Alignment.Center
    ) {
        // 숫자 표시 - remember로 캐싱
        Text(
            text = remember(count) { count.toString() },
            fontSize = 7.sp,
            fontWeight = FontWeight.Bold,
            color = textColor
        )
    }
}
