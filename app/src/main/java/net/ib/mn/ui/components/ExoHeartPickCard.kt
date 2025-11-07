package net.ib.mn.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.CachePolicy
import coil.request.ImageRequest
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
                        android.util.Log.d("HeartPickUpcoming", "Loading background image: $backgroundImageUrl")

                        val context = LocalContext.current
                        val imageModel = remember(backgroundImageUrl) {
                            coil.request.ImageRequest.Builder(context)
                                .data(backgroundImageUrl)
                                .crossfade(true)
                                .listener(
                                    onStart = {
                                        android.util.Log.d("HeartPickUpcoming", "Image load started: $backgroundImageUrl")
                                    },
                                    onSuccess = { _, _ ->
                                        android.util.Log.d("HeartPickUpcoming", "Image load SUCCESS: $backgroundImageUrl")
                                    },
                                    onError = { _, result ->
                                        android.util.Log.e("HeartPickUpcoming", "Image load FAILED: $backgroundImageUrl, error: ${result.throwable.message}")
                                    }
                                )
                                .build()
                        }

                        AsyncImage(
                            model = imageModel,
                            contentDescription = "Background",
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(1f / 0.4f),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(1f / 0.4f)
                                .background(colorResource(R.color.background_400))
                        )
                    }

                    // 그라데이션 오버레이
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(1f / 0.4f)
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
                                lineHeight = 20.sp,
                                color = colorResource(R.color.fix_white),
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // 제목
                        Text(
                            text = title,
                            fontSize = 19.sp,
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
                                lineHeight = 20.sp,
                                color = colorResource(R.color.fix_white)
                            )
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        // 제목
                        Text(
                            text = title,
                            fontSize = 19.sp,
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
                                    .height(90.dp)
                                    .background(colorResource(R.color.background_200))
                            ) {
                        // 1위 프로필 이미지 (타이틀 영역을 침범하도록 위로 이동)
                        Box(
                            modifier = Modifier
                                .offset(y = (-20).dp, x=16.dp)
                                .size(90.dp)
                                .align(Alignment.TopStart)
                        ) {
                            ExoProfileImage(
                                imageUrl = firstPlaceIdol.photoUrl,
                                rank = 1,
                                contentDescription = "First Place",
                                modifier = Modifier.fillMaxSize()
                            )
                            Icon(
                                painter = painterResource(R.drawable.icon_heartpick_1st),
                                contentDescription = "1st",
                                modifier = Modifier
                                    .size(30.dp),
                                tint = Color.Unspecified
                            )
                        }

                        // 1위 정보 텍스트
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .align(Alignment.BottomStart)
                                .padding(start = 125.dp, bottom = 20.dp),
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = firstPlaceIdol.name,
                                fontSize = 16.sp,
                                lineHeight = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = colorResource(R.color.text_default),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = firstPlaceIdol.groupName,
                                fontSize = 12.sp,
                                lineHeight = 12.sp,
                                color = colorResource(R.color.text_dimmed),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.padding(top = 1.dp)
                            )
                            Text(
                                text = firstPlaceIdol.voteCount,
                                fontSize = 14.sp,
                                lineHeight = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = colorResource(R.color.main_light),
                                modifier = Modifier.padding(top = 1.dp)
                            )
                        }

                        // 퍼센트 박스 (우하단 고정)
                        Box(
                            modifier = Modifier
                                .padding(end = 10.dp, bottom = 20.dp)
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
                        // RankingItemData 리스트 생성
                        val rankingItems = remember(otherIdols, firstPlaceIdol) {
                            val firstPlaceVotes = firstPlaceIdol?.voteCount?.replace(",", "")?.toLongOrNull() ?: 0L
                            android.util.Log.d("HeartPickCard", "firstPlaceIdol voteCount: ${firstPlaceIdol?.voteCount}, parsed: $firstPlaceVotes")

                            otherIdols.mapIndexed { index, idol ->
                                val idolVotes = idol.voteCount.replace(",", "").toLongOrNull() ?: 0L
                                android.util.Log.d("HeartPickCard", "Rank ${index + 2}: ${idol.name}, votes=${idol.voteCount}, parsed=$idolVotes, maxHeartCount=$firstPlaceVotes")

                                RankingItemData(
                                    rank = index + 2,
                                    name = "${idol.name}_${idol.groupName}",
                                    voteCount = idol.voteCount,
                                    photoUrl = idol.photoUrl,
                                    id = "${index + 2}_${idol.name}",
                                    heartCount = idolVotes,
                                    maxHeartCount = firstPlaceVotes
                                )
                            }
                        }

                        LazyRow(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(horizontal = 20.dp),
                            horizontalArrangement = Arrangement.spacedBy(15.dp)
                        ) {
                            items(
                                items = rankingItems,
                                key = { item -> item.itemKey() }
                            ) { item ->
                                HeartPickRankingItem(
                                    item = item,
                                )
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
            Box(
                modifier = Modifier.fillMaxWidth()
            ) {
                // Layer 1: Column with background and content
                Column(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // 상단 영역 (배경 이미지 + 그라데이션 + 타이틀)
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

                        // 타이틀과 서브타이틀 (Layer 1)
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(1f / 0.42f)
                                .padding(horizontal = 16.dp)
                                .padding(top = 56.dp)
                        ) {
                            // 제목
                            Text(
                                text = title,
                                fontSize = 19.sp,
                                color = colorResource(R.color.fix_white),
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )

                            Spacer(modifier = Modifier.height(2.dp))

                            // 부제목
                            Text(
                                text = subTitle,
                                fontSize = 13.sp,
                                color = colorResource(R.color.fix_white),
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    // 1위 아이돌 정보
                    if (firstPlaceIdol != null) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(90.dp)
                                .background(colorResource(R.color.background_200))
                        ) {
                        // 1위 프로필 이미지 (타이틀 영역을 침범하도록 위로 이동)
                        Box(
                            modifier = Modifier
                                .offset(y = (-20).dp, x=16.dp)
                                .size(90.dp)
                                .align(Alignment.TopStart)
                        ) {
                            ExoProfileImage(
                                imageUrl = firstPlaceIdol.photoUrl,
                                rank = 1,
                                contentDescription = "First Place",
                                modifier = Modifier.fillMaxSize()
                            )
                            Icon(
                                painter = painterResource(R.drawable.icon_heartpick_1st),
                                contentDescription = "1st",
                                modifier = Modifier
                                    .size(30.dp),
                                tint = Color.Unspecified
                            )
                        }

                        // 1위 정보 텍스트
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .align(Alignment.BottomStart)
                                .padding(start = 125.dp, bottom = 20.dp),
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = firstPlaceIdol.name,
                                fontSize = 16.sp,
                                lineHeight = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = colorResource(R.color.text_default),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = firstPlaceIdol.groupName,
                                fontSize = 12.sp,
                                lineHeight = 12.sp,
                                color = colorResource(R.color.text_dimmed),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.padding(top = 1.dp)
                            )
                            Text(
                                text = firstPlaceIdol.voteCount,
                                fontSize = 14.sp,
                                lineHeight = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = colorResource(R.color.main_light),
                                modifier = Modifier.padding(top = 1.dp)
                            )
                        }

                        // 퍼센트 박스 (우하단 고정)
                        Box(
                            modifier = Modifier
                                .padding(end = 10.dp, bottom = 20.dp)
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

                // 결과보기 버튼
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
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
                        color = colorResource(R.color.text_default)
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

                // Layer 2: Dimmed 박스 (전체 어둡게)
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .background(Color.Black.copy(alpha = 0.5f))
                )

                // Layer 3: 뱃지 박스 + 콘텐츠 박스
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
                            lineHeight = 20.sp,
                            color = colorResource(R.color.fix_white)
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // 제목
                    Text(
                        text = title,
                        fontSize = 19.sp,
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
