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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import net.ib.mn.R
import net.ib.mn.ui.theme.ColorPalette
import net.ib.mn.ui.theme.ExoTypo

/**
 * 이미지픽 카드 상태
 */
enum class ImagePickState {
    UPCOMING,   // 진행 예정
    ACTIVE,     // 진행 중
    ENDED       // 종료
}

/**
 * 이미지픽 카드 컴포넌트
 *
 * @param state 카드 상태 (UPCOMING, ACTIVE, ENDED)
 * @param title 제목
 * @param subTitle 부제목
 * @param voteCount 전체 투표수
 * @param periodDate 투표 기간
 * @param onCardClick 카드 클릭 이벤트
 * @param onVoteClick 투표 클릭 이벤트
 * @param modifier Modifier
 */
@Composable
fun ExoImagePickCard(
    state: ImagePickState,
    title: String,
    subTitle: String,
    voteCount: String,
    periodDate: String,
    onCardClick: () -> Unit,
    onVoteClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    when (state) {
        ImagePickState.ENDED -> ImagePickEndedCard(
            title = title,
            subTitle = subTitle,
            periodDate = periodDate,
            voteCount = voteCount,
            onCardClick = onCardClick,
            onVoteClick = onVoteClick,
            modifier = modifier
        )
        ImagePickState.UPCOMING -> ImagePickUpcomingCard(
            title = title,
            subTitle = subTitle,
            periodDate = periodDate,
            onCardClick = onCardClick,
            modifier = modifier
        )
        ImagePickState.ACTIVE -> ImagePickActiveCard(
            title = title,
            subTitle = subTitle,
            periodDate = periodDate,
            voteCount = voteCount,
            onCardClick = onCardClick,
            onVoteClick = onVoteClick,
            modifier = modifier
        )
    }
}

/**
 * ENDED 상태 이미지픽 카드
 */
@Composable
private fun ImagePickEndedCard(
    title: String,
    subTitle: String,
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
                    .padding(16.dp)
            ) {
                ImagePickCardContent(
                    title = title,
                    subTitle = subTitle,
                    periodDate = periodDate,
                    voteCount = voteCount,
                    voteCountLabel = R.string.themepick_total_votes,
                )

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
 * UPCOMING 상태 이미지픽 카드
 */
@Composable
private fun ImagePickUpcomingCard(
    title: String,
    subTitle: String,
    periodDate: String,
    onCardClick: () -> Unit,
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
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 제목
            ExoNoticeBox(
                text = title,
                style = ExoTypo.title16
            )

            Spacer(modifier = Modifier.height(30.dp))

            // 부제목 (D-Day)
            Text(
                text = subTitle,
                style = ExoTypo.title21,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            )

            // 투표 기간
            Text(
                text = "${stringResource(R.string.onepick_period)} : $periodDate",
                style = ExoTypo.caption10,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 30.dp)
            )

            // 투표 알림 설정 버튼
            ExoButton(
                onClick = onCardClick,
                modifier = Modifier.fillMaxWidth(),
                text = stringResource(R.string.vote_alert_before),
                fontSize = 14.sp,
                height = 41.dp,
                shape = RoundedCornerShape(20.dp),
                containerColor = ColorPalette.mainLight,
                contentColor = ColorPalette.textWhiteBlack
            )
        }
    }
}

/**
 * ACTIVE 상태 이미지픽 카드
 */
@Composable
private fun ImagePickActiveCard(
    title: String,
    subTitle: String,
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
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            ImagePickCardContent(
                title = title,
                subTitle = subTitle,
                periodDate = periodDate,
                voteCount = voteCount,
                voteCountLabel = R.string.num_participants
            )

            Spacer(modifier = Modifier.height(12.dp))

            // 현재 순위 보기
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
                    style = ExoTypo.body13.copy(color = ColorPalette.mainLight)
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
}

/**
 * 이미지픽 카드 공통 콘텐츠 (Active/Ended 공용)
 */
@Composable
private fun ImagePickCardContent(
    title: String,
    subTitle: String,
    periodDate: String,
    voteCount: String,
    voteCountLabel: Int,
) {
    // 타이틀
    ExoNoticeBox(text = title)

    Spacer(modifier = Modifier.height(10.dp))

    // 부제목
    Text(
        text = subTitle,
        style = ExoTypo.body13
    )

    Spacer(modifier = Modifier.height(10.dp))

    // 투표 기간
    Text(
        text = "${stringResource(R.string.onepick_period)} : $periodDate",
        style = ExoTypo.body13
    )

    Spacer(modifier = Modifier.height(6.dp))

    // 투표수 (전체 투표수 or 참여인원)
    Text(
        text = "${stringResource(voteCountLabel)} : $voteCount${if (voteCountLabel == R.string.themepick_total_votes) stringResource(R.string.votes) else ""}",
        style = ExoTypo.body13
    )
}
