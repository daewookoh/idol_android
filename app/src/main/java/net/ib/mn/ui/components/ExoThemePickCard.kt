package net.ib.mn.ui.components

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import net.ib.mn.R
import net.ib.mn.ui.theme.ColorPalette

/**
 * 테마픽 카드 상태
 */
enum class ThemePickState {
    UPCOMING,   // 진행 예정
    ACTIVE,     // 진행 중
    ENDED       // 종료
}

/**
 * 테마픽 카드 컴포넌트
 *
 * @param state 카드 상태 (UPCOMING, ACTIVE, ENDED)
 * @param title 제목
 * @param subTitle 부제목
 * @param imageUrl 배경 이미지 URL
 * @param voteCount 전체 투표수
 * @param periodDate 투표 기간
 * @param voteStatus 투표 상태 ("N": 투표가능, "V": 광고후 투표, "Y": 오늘 투표 완료)
 * @param isNew 신규 카드 여부 (48시간 이내 시작된 카드면 N 아이콘 표시)
 * @param onCardClick 카드 클릭 이벤트
 * @param onVoteClick 투표 클릭 이벤트
 * @param onCurrentRankingClick 현재 순위 보기 클릭 이벤트 (ACTIVE 상태에서만 사용)
 * @param modifier Modifier
 */
@Composable
fun ExoThemePickCard(
    state: ThemePickState,
    title: String,
    subTitle: String,
    imageUrl: String,
    voteCount: String,
    periodDate: String,
    voteStatus: String = "N",
    isNew: Boolean = false,
    onCardClick: () -> Unit,
    onVoteClick: () -> Unit,
    onCurrentRankingClick: () -> Unit = onVoteClick,
    modifier: Modifier = Modifier
) {
    // ENDED 상태가 아니면서 isNew가 true일 때만 N 아이콘 표시
    val showNewIcon = isNew && state != ThemePickState.ENDED

    when (state) {
        ThemePickState.ENDED -> ThemePickEndedCard(
            imageUrl = imageUrl,
            title = title,
            periodDate = periodDate,
            voteCount = voteCount,
            onCardClick = onCardClick,
            onVoteClick = onVoteClick,
            modifier = modifier
        )
        ThemePickState.UPCOMING -> ThemePickUpcomingCard(
            imageUrl = imageUrl,
            title = title,
            subTitle = subTitle,
            periodDate = periodDate,
            showNewIcon = showNewIcon,
            onCardClick = onCardClick,
            modifier = modifier
        )
        ThemePickState.ACTIVE -> ThemePickActiveCard(
            imageUrl = imageUrl,
            title = title,
            periodDate = periodDate,
            voteCount = voteCount,
            voteStatus = voteStatus,
            showNewIcon = showNewIcon,
            onCardClick = onCardClick,
            onVoteClick = onVoteClick,
            onCurrentRankingClick = onCurrentRankingClick,
            modifier = modifier
        )
    }
}

/**
 * ENDED 상태 테마픽 카드
 */
@Composable
private fun ThemePickEndedCard(
    imageUrl: String,
    title: String,
    periodDate: String,
    voteCount: String,
    onCardClick: () -> Unit,
    onVoteClick: () -> Unit,
    modifier: Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .padding(top = 15.dp)
            .clickable(onClick = onCardClick),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(0.dp),
        colors = CardDefaults.cardColors(
            containerColor = ColorPalette.background200
        )
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            // Layer 1: 기본 콘텐츠
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            ) {
                ThemePickCardContent(imageUrl, title, periodDate, voteCount)
                Spacer(Modifier.height(50.dp))
            }

            // Layer 2: Dimmed 박스
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(Color.Black.copy(alpha = 0.5f))
            )

            // Layer 3: 버튼
            Box(modifier = Modifier.matchParentSize()) {
                ExoButton(
                    onClick = onVoteClick,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .padding(bottom = 16.dp),
                    text = stringResource(R.string.see_result),
                    fontSize = 14.sp,
                    height = 41.dp,
                    shape = RoundedCornerShape(20.dp),
                    containerColor = ColorPalette.fixGray900,
                    contentColor = ColorPalette.fixWhite,
                )
            }
        }
    }
}

/**
 * UPCOMING 상태 테마픽 카드
 */
@Composable
private fun ThemePickUpcomingCard(
    imageUrl: String,
    title: String,
    subTitle: String,
    periodDate: String,
    showNewIcon: Boolean,
    onCardClick: () -> Unit,
    modifier: Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .padding(top = 15.dp)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onCardClick),
            shape = RoundedCornerShape(20.dp),
            elevation = CardDefaults.cardElevation(0.dp),
            colors = CardDefaults.cardColors(
                containerColor = ColorPalette.background200
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            ) {
                // 배경 이미지
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 10.dp, end = 10.dp, top = 16.dp)
                ) {
                    if (imageUrl.isNotEmpty()) {
                        AsyncImage(
                            model = imageUrl,
                            contentDescription = "ThemePick Image",
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(3.3f / 1f)
                                .clip(RoundedCornerShape(10.dp)),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(3.3f / 1f)
                                .background(ColorPalette.background200, RoundedCornerShape(10.dp))
                        )
                    }
                }

                // 제목
                Text(
                    text = title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = ColorPalette.mainLight,
                    modifier = Modifier
                        .padding(start = 16.dp)
                        .padding(top = 10.dp)
                )

                // 부제목 (D-Day)
                Text(
                    text = subTitle,
                    fontSize = 21.sp,
                    lineHeight = 21.sp,
                    fontWeight = FontWeight.Bold,
                    color = ColorPalette.mainLight,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .padding(top = 12.dp)
                )

                // 투표 기간
                Text(
                    text = "${stringResource(R.string.onepick_period)} : $periodDate",
                    fontSize = 12.sp,
                    lineHeight = 12.sp,
                    letterSpacing = (-0.5).sp,
                    color = ColorPalette.textDimmed,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .padding(top = 4.dp)
                )

                // 투표 미리보기 버튼
                ExoButton(
                    onClick = onCardClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 20.dp, start = 16.dp, end = 16.dp),
                    text = stringResource(R.string.vote_preview),
                    fontSize = 14.sp,
                    height = 41.dp,
                    shape = RoundedCornerShape(20.dp),
                    containerColor = ColorPalette.main200,
                    contentColor = ColorPalette.mainLight
                )
            }
        }

        // N 아이콘 (신규 카드 표시)
        if (showNewIcon) {
            Icon(
                painter = painterResource(R.drawable.icon_heartpick_new),
                contentDescription = "New",
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 21.dp)
                    .size(width = 23.dp, height = 31.dp),
                tint = Color.Unspecified
            )
        }
    }
}

/**
 * ACTIVE 상태 테마픽 카드
 *
 * @param voteStatus 투표 상태:
 *   - "N": 첫 투표 가능 → "투표하기" 버튼 (mainLight)
 *   - "V": 광고 시청 후 추가 투표 가능 → "다시 투표" 버튼 (mainLight)
 *   - "Y": 오늘 투표 완료 → "오늘 투표 완료" 버튼 (gray, 비활성화)
 */
@Composable
private fun ThemePickActiveCard(
    imageUrl: String,
    title: String,
    periodDate: String,
    voteCount: String,
    voteStatus: String,
    showNewIcon: Boolean,
    onCardClick: () -> Unit,
    onVoteClick: () -> Unit,
    onCurrentRankingClick: () -> Unit,
    modifier: Modifier
) {
    // 투표 상태에 따른 버튼 설정
    val hasVotedToday = voteStatus == "Y"
    val needsVideoAd = voteStatus == "V"

    val buttonText = when {
        hasVotedToday -> stringResource(R.string.themepick_today_voted)   // "오늘 투표 완료"
        needsVideoAd -> stringResource(R.string.themepick_vote_again)     // "다시 투표"
        else -> stringResource(R.string.guide_vote_title)                 // "투표하기"
    }

    val buttonColor = if (hasVotedToday) {
        ColorPalette.fixGray900  // 회색 (비활성)
    } else {
        ColorPalette.mainLight   // 메인 색상 (활성)
    }

    val buttonTextColor = if (hasVotedToday) {
        ColorPalette.fixWhite
    } else {
        ColorPalette.textWhiteBlack
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .padding(top = 15.dp)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onCardClick),
            shape = RoundedCornerShape(20.dp),
            elevation = CardDefaults.cardElevation(0.dp),
            colors = CardDefaults.cardColors(
                containerColor = ColorPalette.background200
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            ) {
                ThemePickCardContent(imageUrl, title, periodDate, voteCount)

                // 하단 컨텐츠
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp, start = 16.dp, end = 16.dp)
                ) {
                    // 현재 순위 보기
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(ColorPalette.main200)
                            .clickable(onClick = onCurrentRankingClick)
                            .padding(horizontal = 7.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Start
                    ) {
                        Text(
                            text = stringResource(R.string.see_current_ranking),
                            fontSize = 13.sp,
                            color = ColorPalette.mainLight
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Icon(
                            painter = painterResource(R.drawable.arrow_left_to_right),
                            contentDescription = null,
                            modifier = Modifier.size(8.dp),
                            tint = ColorPalette.mainLight
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // 투표 참여 버튼
                    ExoButton(
                        onClick = onVoteClick,
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !hasVotedToday,
                        text = buttonText,
                        fontSize = 14.sp,
                        height = 41.dp,
                        shape = RoundedCornerShape(20.dp),
                        containerColor = buttonColor,
                        disabledContainerColor = buttonColor,
                        contentColor = buttonTextColor,
                        disabledContentColor = buttonTextColor
                    )
                }
            }
        }

        // N 아이콘 (신규 카드 표시)
        if (showNewIcon) {
            Icon(
                painter = painterResource(R.drawable.icon_heartpick_new),
                contentDescription = "New",
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 21.dp)
                    .size(width = 23.dp, height = 31.dp),
                tint = Color.Unspecified
            )
        }
    }
}

/**
 * 테마픽 카드 공통 콘텐츠 (이미지, 제목, 투표 정보)
 */
@Composable
private fun ThemePickCardContent(
    imageUrl: String,
    title: String,
    periodDate: String,
    voteCount: String
) {
    // 배경 이미지
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 10.dp, end = 10.dp, top = 16.dp)
    ) {
        if (imageUrl.isNotEmpty()) {
            AsyncImage(
                model = imageUrl,
                contentDescription = "ThemePick Image",
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(3.3f / 1f)
                    .clip(RoundedCornerShape(10.dp)),
                contentScale = ContentScale.Crop
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(3.3f / 1f)
                    .background(ColorPalette.background200, RoundedCornerShape(10.dp))
            )
        }
    }

    // 제목
    Text(
        text = title,
        fontSize = 16.sp,
        fontWeight = FontWeight.Bold,
        color = ColorPalette.mainLight,
        modifier = Modifier
            .padding(start = 16.dp)
            .padding(top = 10.dp)
    )

    // 투표 기간
    Text(
        text = "${stringResource(R.string.onepick_period)} : $periodDate",
        fontSize = 13.sp,
        lineHeight = 13.sp,
        color = ColorPalette.textDefault,
        modifier = Modifier
            .padding(start = 16.dp)
            .padding(top = 10.dp)
    )

    // 전체 투표수
    Text(
        text = "${stringResource(R.string.themepick_total_votes)} : $voteCount${stringResource(R.string.votes)}",
        fontSize = 13.sp,
        lineHeight = 13.sp,
        color = ColorPalette.textDefault,
        modifier = Modifier
            .padding(start = 16.dp)
            .padding(top = 6.dp)
    )
}
