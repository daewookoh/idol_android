package net.ib.mn.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
 * OnePick (테마픽/이미지픽) 카드 타입
 */
enum class OnePickState {
    UPCOMING,   // 진행 예정
    ACTIVE,     // 진행 중
    ENDED       // 종료
}

/**
 * OnePick 카드 컴포넌트
 *
 * 테마픽과 이미지픽에 모두 사용되는 공통 카드
 *
 * @param state 카드 상태 (UPCOMING, ACTIVE, ENDED)
 * @param title 제목
 * @param subTitle 부제목
 * @param imageUrl 배경 이미지 URL
 * @param voteCount 전체 투표수
 * @param periodDate 투표 기간
 * @param onCardClick 카드 클릭 이벤트
 * @param onVoteClick 투표 클릭 이벤트
 * @param modifier Modifier
 */
@Composable
fun ExoOnePickCard(
    state: OnePickState,
    title: String,
    subTitle: String,
    imageUrl: String,
    voteCount: String,
    periodDate: String,
    onCardClick: () -> Unit,
    onVoteClick: () -> Unit,
    modifier: Modifier = Modifier
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
        if (state == OnePickState.ENDED) {
            // ENDED 상태: 3-layer 구조
            Box(
                modifier = Modifier.fillMaxWidth()
            ) {
                // Layer 1: 기본 콘텐츠
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                ) {
                    CardContent(imageUrl, title, periodDate, voteCount)
                    Spacer(Modifier.height(50.dp))
                }

                // Layer 2: Dimmed 박스 (전체 어둡게)
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .background(Color.Black.copy(alpha = 0.5f))
                )

                // Layer 3: 버튼
                Box(
                    modifier = Modifier.matchParentSize()
                ) {
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
        } else if (state == OnePickState.UPCOMING) {
            // UPCOMING 상태: 특별한 레이아웃
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
                            contentDescription = "OnePick Image",
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

                // 부제목 (큰 텍스트) - 가운데 정렬
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

                // 투표 기간 - 가운데 정렬
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
        } else {
            // ACTIVE 상태: 기존 구조
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            ) {
                CardContent(imageUrl, title, periodDate, voteCount)

                ActiveContent(
                    onVoteClick = onVoteClick,
                    modifier = Modifier.padding(top = 10.dp, start = 16.dp, end = 16.dp)
                )
            }
        }
    }
}

/**
 * 카드 공통 콘텐츠 (이미지, 제목, 투표 정보)
 */
@Composable
private fun CardContent(
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
                contentDescription = "OnePick Image",
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

/**
 * 진행 중 카드의 하단 컨텐츠
 */
@Composable
private fun ActiveContent(
    onVoteClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth()
    ) {
        // 현재 순위 보기 (작은 버튼, 왼쪽 정렬)
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(6.dp))
                .background(ColorPalette.main200)
                .clickable(onClick = onVoteClick)
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
            text = stringResource(R.string.guide_vote_title),
            fontSize = 14.sp,
            height = 41.dp,
            shape = RoundedCornerShape(20.dp),
            containerColor = ColorPalette.mainLight,
            contentColor = ColorPalette.textWhiteBlack
        )
    }
}

