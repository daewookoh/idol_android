package net.ib.mn.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import net.ib.mn.R

/**
 * ExoHeartPickCard - 하트픽 투표 카드 컴포넌트
 */
@Composable
fun ExoHeartPickCard(
    state: HeartPickState,
    title: String,
    subTitle: String,
    backgroundImageUrl: String,
    dDay: String,
    firstPlaceIdol: IdolRankInfo? = null,
    otherIdols: List<IdolRankInfo> = emptyList(),
    heartVoteCount: String = "0",
    commentCount: String = "0",
    periodDate: String = "",
    openDate: String = "",
    openPeriod: String = "",
    isNew: Boolean = false,
    onCardClick: () -> Unit = {},
    onVoteClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    when (state) {
        HeartPickState.UPCOMING -> UpcomingHeartPickCard(
            title = title,
            subTitle = subTitle,
            backgroundImageUrl = backgroundImageUrl,
            dDay = dDay,
            openDate = openDate,
            openPeriod = openPeriod,
            isNew = isNew,
            onCardClick = onCardClick,
            onVoteClick = onVoteClick,
            modifier = modifier
        )
        HeartPickState.ACTIVE -> ActiveHeartPickCard(
            title = title,
            subTitle = subTitle,
            backgroundImageUrl = backgroundImageUrl,
            dDay = dDay,
            firstPlaceIdol = firstPlaceIdol,
            otherIdols = otherIdols,
            heartVoteCount = heartVoteCount,
            commentCount = commentCount,
            periodDate = periodDate,
            isNew = isNew,
            onCardClick = onCardClick,
            onVoteClick = onVoteClick,
            modifier = modifier
        )
        HeartPickState.ENDED -> EndedHeartPickCard(
            title = title,
            subTitle = subTitle,
            backgroundImageUrl = backgroundImageUrl,
            dDay = dDay,
            firstPlaceIdol = firstPlaceIdol,
            otherIdols = otherIdols,
            heartVoteCount = heartVoteCount,
            commentCount = commentCount,
            periodDate = periodDate,
            isNew = isNew,
            onCardClick = onCardClick,
            onVoteClick = onVoteClick,
            modifier = modifier
        )
    }
}

/**
 * 진행예정 하트픽 카드
 */
@Composable
private fun UpcomingHeartPickCard(
    title: String,
    subTitle: String,
    backgroundImageUrl: String,
    dDay: String,
    openDate: String,
    openPeriod: String,
    isNew: Boolean,
    onCardClick: () -> Unit,
    onVoteClick: () -> Unit,
    modifier: Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(colorResource(R.color.background_400))
            .padding(vertical = 7.5.dp)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .clickable(onClick = onCardClick),
            shape = RoundedCornerShape(20.dp),
            elevation = CardDefaults.cardElevation(0.dp),
            colors = CardDefaults.cardColors(
                containerColor = colorResource(R.color.background_200)
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            ) {
                // 상단 영역 (배경 이미지 + 제목)
                Box(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // 배경 이미지
                    if (backgroundImageUrl.isNotEmpty()) {
                        AsyncImage(
                            model = backgroundImageUrl,
                            contentDescription = "Background",
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(1f / 0.3f),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(1f / 0.3f)
                                .background(colorResource(R.color.background_400))
                        )
                    }

                    // 그라데이션 오버레이
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(1f / 0.3f)
                            .background(
                                brush = Brush.verticalGradient(
                                    colors = listOf(
                                        Color.Black.copy(alpha = 0.3f),
                                        Color.Black.copy(alpha = 0.5f)
                                    )
                                )
                            )
                    )

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(1f / 0.3f)
                    ) {
                        // D-Day 배지
                        Row(
                            modifier = Modifier
                                .padding(start = 16.dp, top = 16.dp)
                                .background(
                                    color = colorResource(R.color.fix_gray900),
                                    shape = RoundedCornerShape(10.dp)
                                )
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                                .height(20.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.icon_heartpick_timer),
                                contentDescription = null,
                                modifier = Modifier.size(11.dp),
                                tint = Color.Unspecified
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = dDay,
                                fontSize = 12.sp,
                                color = colorResource(R.color.fix_white)
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // 제목
                        Text(
                            text = title,
                            fontSize = 19.sp,
                            fontWeight = FontWeight.Bold,
                            color = colorResource(R.color.fix_white),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )

                        Spacer(modifier = Modifier.height(2.dp))

                        // 부제목
                        Text(
                            text = subTitle,
                            fontSize = 13.sp,
                            color = colorResource(R.color.fix_white),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )

                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }

                // 진행예정 날짜 정보
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = openDate,
                        fontSize = 21.sp,
                        fontWeight = FontWeight.Bold,
                        color = colorResource(R.color.main_light)
                    )
                    Text(
                        text = openPeriod,
                        fontSize = 12.sp,
                        color = colorResource(R.color.text_dimmed),
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }

                // 투표 버튼
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .height(41.dp)
                        .background(
                            color = colorResource(R.color.main200),
                            shape = RoundedCornerShape(20.dp)
                        )
                        .clickable(onClick = onVoteClick),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(R.string.vote_preview),
                        fontSize = 14.sp,
                        color = colorResource(R.color.main_light)
                    )
                }
            }
        }

        // NEW 배지
        if (isNew) {
            Icon(
                painter = painterResource(R.drawable.icon_heartpick_new),
                contentDescription = "New",
                modifier = Modifier
                    .size(width = 23.dp, height = 31.dp)
                    .align(Alignment.TopEnd)
                    .offset(x = (-13).dp, y = 16.dp),
                tint = Color.Unspecified
            )
        }
    }
}

/**
 * 진행중 하트픽 카드
 */
@Composable
private fun ActiveHeartPickCard(
    title: String,
    subTitle: String,
    backgroundImageUrl: String,
    dDay: String,
    firstPlaceIdol: IdolRankInfo?,
    otherIdols: List<IdolRankInfo>,
    heartVoteCount: String,
    commentCount: String,
    periodDate: String,
    isNew: Boolean,
    onCardClick: () -> Unit,
    onVoteClick: () -> Unit,
    modifier: Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(colorResource(R.color.background_400))
            .padding(vertical = 7.5.dp)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .clickable(onClick = onCardClick),
            shape = RoundedCornerShape(20.dp),
            elevation = CardDefaults.cardElevation(0.dp),
            colors = CardDefaults.cardColors(
                containerColor = colorResource(R.color.background_200)
            )
        ) {
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                // 상단 영역 (배경 이미지 + 제목 + 1위 아이돌)
                Box(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // 배경 이미지
                    if (backgroundImageUrl.isNotEmpty()) {
                        AsyncImage(
                            model = backgroundImageUrl,
                            contentDescription = "Background",
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(1f / 0.42f),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(1f / 0.42f)
                                .background(colorResource(R.color.background_400))
                        )
                    }

                    // 그라데이션 오버레이
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(1f / 0.42f)
                            .background(
                                brush = Brush.verticalGradient(
                                    colors = listOf(
                                        Color.Black.copy(alpha = 0.3f),
                                        Color.Black.copy(alpha = 0.5f)
                                    )
                                )
                            )
                    )

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(1f / 0.42f)
                    ) {
                        // D-Day 배지
                        Row(
                            modifier = Modifier
                                .padding(start = 16.dp, top = 16.dp)
                                .background(
                                    color = colorResource(R.color.fix_gray900),
                                    shape = RoundedCornerShape(10.dp)
                                )
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                                .height(20.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.icon_heartpick_timer),
                                contentDescription = null,
                                modifier = Modifier.size(11.dp),
                                tint = Color.Unspecified
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = dDay,
                                fontSize = 12.sp,
                                color = colorResource(R.color.fix_white)
                            )
                        }

                        Spacer(modifier = Modifier.height(40.dp))

                        // 제목
                        Text(
                            text = title,
                            fontSize = 19.sp,
                            fontWeight = FontWeight.Bold,
                            color = colorResource(R.color.fix_white),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )

                        Spacer(modifier = Modifier.height(2.dp))

                        // 부제목
                        Text(
                            text = subTitle,
                            fontSize = 13.sp,
                            color = colorResource(R.color.fix_white),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )

                        Spacer(modifier = Modifier.height(38.dp))
                    }
                }

                // 1위 아이돌 정보
                if (firstPlaceIdol != null) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .aspectRatio(1f / 0.23f)
                                    .background(colorResource(R.color.background_200))
                            ) {
                        // 1위 프로필 이미지
                        Box(
                            modifier = Modifier
                                .padding(start = 16.dp, bottom = 10.dp)
                                .align(Alignment.BottomStart)
                        ) {
                            AsyncImage(
                                model = firstPlaceIdol.photoUrl,
                                contentDescription = "First Place",
                                modifier = Modifier
                                    .size(90.dp)
                                    .clip(CircleShape),
                                contentScale = ContentScale.Crop
                            )
                            Icon(
                                painter = painterResource(R.drawable.icon_heartpick_1st),
                                contentDescription = "1st",
                                modifier = Modifier
                                    .size(30.dp)
                                    .offset(x = 7.dp),
                                tint = Color.Unspecified
                            )
                        }

                        // 1위 정보 텍스트
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 125.dp, top = 5.dp, end = 15.dp),
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = firstPlaceIdol.name,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = colorResource(R.color.text_default),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = firstPlaceIdol.groupName,
                                fontSize = 12.sp,
                                color = colorResource(R.color.text_dimmed),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.padding(top = 1.dp)
                            )
                            Text(
                                text = firstPlaceIdol.voteCount,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = colorResource(R.color.main_light),
                                modifier = Modifier.padding(top = 1.dp)
                            )
                        }

                        // 퍼센트 박스 (우하단 고정)
                        Box(
                            modifier = Modifier
                                .padding(end = 10.dp, bottom = 10.dp)
                                .align(Alignment.BottomEnd)
                                .size(width = 49.dp, height = 27.dp)
                                .background(
                                    color = colorResource(R.color.main200),
                                    shape = RoundedCornerShape(9.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "${firstPlaceIdol.percentage}%",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = colorResource(R.color.main_light)
                            )
                        }
                    }
                }

                // 다른 아이돌 목록
                if (otherIdols.isNotEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(1f / 0.24f)
                            .background(
                                brush = Brush.verticalGradient(
                                    colors = listOf(
                                        colorResource(R.color.background_200).copy(alpha = 0f),
                                        colorResource(R.color.background_200)
                                    )
                                )
                            )
                    ) {
                        LazyRow(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(otherIdols) { idol ->
                                IdolRankItem(idol)
                            }
                        }
                    }
                }

                // 투표 버튼
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .height(41.dp)
                        .background(
                            color = colorResource(R.color.main_light),
                            shape = RoundedCornerShape(20.dp)
                        )
                        .clickable(onClick = onVoteClick),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(R.string.guide_vote_title),
                        fontSize = 14.sp,
                        color = colorResource(R.color.text_white_black)
                    )
                }

                // 하단 정보 (하트 투표, 댓글, 기간)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(40.dp)
                        .background(colorResource(R.color.background_200))
                        .padding(horizontal = 20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        painter = painterResource(R.drawable.icon_community_heart),
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = Color.Unspecified
                    )
                    Text(
                        text = heartVoteCount,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = colorResource(R.color.text_gray),
                        modifier = Modifier.padding(start = 3.dp)
                    )

                    Spacer(modifier = Modifier.width(11.dp))

                    Icon(
                        painter = painterResource(R.drawable.icon_community_comment),
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = Color.Unspecified
                    )
                    Text(
                        text = commentCount,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = colorResource(R.color.text_gray),
                        modifier = Modifier.padding(start = 3.dp)
                    )

                    Spacer(modifier = Modifier.weight(1f))

                    Text(
                        text = periodDate,
                        fontSize = 12.sp,
                        color = colorResource(R.color.text_dimmed)
                    )
                }
            }
        }

        // NEW 배지
        if (isNew) {
            Icon(
                painter = painterResource(R.drawable.icon_heartpick_new),
                contentDescription = "New",
                modifier = Modifier
                    .size(width = 23.dp, height = 31.dp)
                    .align(Alignment.TopEnd)
                    .offset(x = (-13).dp, y = 16.dp),
                tint = Color.Unspecified
            )
        }
    }
}

/**
 * 종료 하트픽 카드
 */
@Composable
private fun EndedHeartPickCard(
    title: String,
    subTitle: String,
    backgroundImageUrl: String,
    dDay: String,
    firstPlaceIdol: IdolRankInfo?,
    otherIdols: List<IdolRankInfo>,
    heartVoteCount: String,
    commentCount: String,
    periodDate: String,
    isNew: Boolean,
    onCardClick: () -> Unit,
    onVoteClick: () -> Unit,
    modifier: Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(colorResource(R.color.background_400))
            .padding(vertical = 7.5.dp)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .clickable(onClick = onCardClick),
            shape = RoundedCornerShape(20.dp),
            elevation = CardDefaults.cardElevation(0.dp),
            colors = CardDefaults.cardColors(
                containerColor = colorResource(R.color.background_200)
            )
        ) {
            Box(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    // cl_top: 상단 영역 (배경 이미지 + 제목 + 1위 아이돌)
                    Box(modifier = Modifier.fillMaxWidth()) {
                        // 배경 이미지
                        if (backgroundImageUrl.isNotEmpty()) {
                            AsyncImage(
                                model = backgroundImageUrl,
                                contentDescription = "Background",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .aspectRatio(1f / 0.92f),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .aspectRatio(1f / 0.92f)
                                    .background(colorResource(R.color.background_400))
                            )
                        }

                        // 그라데이션 오버레이
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(1f / 0.92f)
                                .background(
                                    brush = Brush.verticalGradient(
                                        colors = listOf(
                                            Color.Black.copy(alpha = 0.3f),
                                            Color.Black.copy(alpha = 0.5f)
                                        )
                                    )
                                )
                        )

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(1f / 0.92f)
                        ) {
                            Spacer(modifier = Modifier.height(56.dp))

                            // 제목
                            Text(
                                text = title,
                                fontSize = 19.sp,
                                fontWeight = FontWeight.Bold,
                                color = colorResource(R.color.fix_white),
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )

                            Spacer(modifier = Modifier.height(2.dp))

                            // 부제목
                            Text(
                                text = subTitle,
                                fontSize = 13.sp,
                                color = colorResource(R.color.fix_white),
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )

                            Spacer(modifier = Modifier.height(38.dp))

                            // 1위 아이돌 정보
                            if (firstPlaceIdol != null) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(colorResource(R.color.background_200))
                                ) {
                                    // 1위 프로필 이미지
                                    Box(
                                        modifier = Modifier
                                            .padding(start = 16.dp, bottom = 10.dp)
                                            .align(Alignment.BottomStart)
                                    ) {
                                        AsyncImage(
                                            model = firstPlaceIdol.photoUrl,
                                            contentDescription = "First Place",
                                            modifier = Modifier
                                                .size(90.dp)
                                                .clip(CircleShape),
                                            contentScale = ContentScale.Crop
                                        )
                                        Icon(
                                            painter = painterResource(R.drawable.icon_heartpick_1st),
                                            contentDescription = "1st",
                                            modifier = Modifier
                                                .size(30.dp)
                                                .offset(x = 7.dp),
                                            tint = Color.Unspecified
                                        )
                                    }

                                    // 1위 정보 텍스트 (ConstraintLayout의 vertical chain 효과)
                                    Column(
                                        modifier = Modifier
                                            .align(Alignment.CenterStart)
                                            .padding(start = 125.dp, end = 15.dp),
                                        verticalArrangement = Arrangement.spacedBy(1.dp, Alignment.CenterVertically)
                                    ) {
                                        Text(
                                            text = firstPlaceIdol.name,
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = colorResource(R.color.text_default),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        if (firstPlaceIdol.groupName.isNotEmpty()) {
                                            Text(
                                                text = firstPlaceIdol.groupName,
                                                fontSize = 12.sp,
                                                color = colorResource(R.color.text_dimmed),
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                        Text(
                                            text = firstPlaceIdol.voteCount,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = colorResource(R.color.main_light)
                                        )
                                    }

                                    // 퍼센트 박스
                                    Box(
                                        modifier = Modifier
                                            .padding(end = 10.dp, bottom = 10.dp)
                                            .align(Alignment.BottomEnd)
                                            .size(width = 49.dp, height = 27.dp)
                                            .background(
                                                color = colorResource(R.color.main200),
                                                shape = RoundedCornerShape(9.dp)
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "${firstPlaceIdol.percentage}%",
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = colorResource(R.color.main_light)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // 하단 영역 (하트, 댓글, 기간)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(40.dp)
                            .background(colorResource(R.color.background_200))
                            .padding(horizontal = 20.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.icon_community_heart),
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = Color.Unspecified
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = heartVoteCount,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = colorResource(R.color.text_gray)
                        )

                        Spacer(modifier = Modifier.width(11.dp))

                        Icon(
                            painter = painterResource(R.drawable.icon_community_comment),
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = Color.Unspecified
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = commentCount,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = colorResource(R.color.text_gray)
                        )

                        Spacer(modifier = Modifier.weight(1f))

                        Text(
                            text = periodDate,
                            fontSize = 12.sp,
                            color = colorResource(R.color.text_dimmed)
                        )
                    }
                }

                // cl_shadow: 딤드 오버레이 (전체를 덮지만 D-Day와 버튼은 위에)
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .background(colorResource(R.color.gray1000_opacity_60))
                )

                // 결과보기 버튼 (cl_shadow 위에, 딤드 영향 안받음)
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .offset(y = (-50).dp)
                        .height(41.dp)
                        .background(
                            color = colorResource(R.color.fix_gray900),
                            shape = RoundedCornerShape(20.dp)
                        )
                        .clickable(onClick = onVoteClick),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(R.string.see_result),
                        fontSize = 14.sp,
                        color = colorResource(R.color.fix_white)
                    )
                }

                // D-Day 배지 (cl_shadow 위에)
                Row(
                    modifier = Modifier
                        .padding(start = 16.dp, top = 16.dp)
                        .background(
                            color = colorResource(R.color.fix_gray900),
                            shape = RoundedCornerShape(10.dp)
                        )
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                        .height(20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        painter = painterResource(R.drawable.icon_heartpick_timer),
                        contentDescription = null,
                        modifier = Modifier.size(11.dp),
                        tint = Color.Unspecified
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = dDay,
                        fontSize = 12.sp,
                        color = colorResource(R.color.fix_white)
                    )
                }
            }
        }

        // NEW 배지
        if (isNew) {
            Icon(
                painter = painterResource(R.drawable.icon_heartpick_new),
                contentDescription = "New",
                modifier = Modifier
                    .size(width = 23.dp, height = 31.dp)
                    .align(Alignment.TopEnd)
                    .offset(x = (-13).dp, y = 16.dp),
                tint = Color.Unspecified
            )
        }
    }
}

@Composable
private fun IdolRankItem(idol: IdolRankInfo) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        AsyncImage(
            model = idol.photoUrl,
            contentDescription = idol.name,
            modifier = Modifier
                .size(60.dp)
                .clip(CircleShape),
            contentScale = ContentScale.Crop
        )
        Text(
            text = idol.name,
            fontSize = 12.sp,
            color = colorResource(R.color.text_default),
            maxLines = 1,
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}

enum class HeartPickState {
    ACTIVE,     // 진행중
    ENDED,      // 종료
    UPCOMING    // 진행예정
}

data class IdolRankInfo(
    val name: String,
    val groupName: String,
    val photoUrl: String,
    val voteCount: String,
    val percentage: Int
)
