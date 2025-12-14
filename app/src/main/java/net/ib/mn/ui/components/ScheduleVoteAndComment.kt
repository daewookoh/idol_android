package net.ib.mn.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import net.ib.mn.BuildConfig
import net.ib.mn.R
import net.ib.mn.domain.model.ScheduleModel
import net.ib.mn.ui.theme.ColorPalette
import net.ib.mn.ui.theme.ExoTypo

/**
 * 투표 다이얼로그 타입
 */
enum class ScheduleVoteDialogType {
    NONE,
    NOT_MOST,       // 최애 아닐 때
    LEVEL_LOW,      // 레벨 부족
    VOTE_YES,       // 맞아요 투표 확인
    VOTE_NO,        // 틀려요 투표 확인
    ALREADY_VOTED   // 이미 투표함
}

/**
 * 스케줄 댓글/투표 버튼 컴포넌트
 *
 * CommunityScheduleSubPage와 ScheduleDetailScreen에서 공통으로 사용되는 컴포넌트.
 * 댓글 수와 맞아요/틀려요 투표 기능을 제공합니다.
 *
 * @param schedule 스케줄 모델
 * @param isMost 최애 아이돌인지 여부
 * @param hasVoteLevel 투표 레벨 충족 여부
 * @param scheduleVoteLevel 투표 필요 레벨 (다이얼로그 메시지용)
 * @param onCommentClick 댓글 버튼 클릭 콜백
 * @param onVoteYes 맞아요 투표 콜백
 * @param onVoteNo 틀려요 투표 콜백
 * @param modifier Modifier
 */
@Composable
fun ScheduleVoteAndComment(
    schedule: ScheduleModel,
    isMost: Boolean = true,
    hasVoteLevel: Boolean = true,
    scheduleVoteLevel: Int = 0,
    onCommentClick: () -> Unit,
    onVoteYes: () -> Unit,
    onVoteNo: () -> Unit,
    modifier: Modifier = Modifier
) {
    val borderColor = ColorPalette.gray110
    val mainColor = ColorPalette.main
    val gray300 = ColorPalette.gray300

    // 다이얼로그 상태
    var dialogType by remember { mutableStateOf(ScheduleVoteDialogType.NONE) }

    // 다이얼로그 표시
    when (dialogType) {
        ScheduleVoteDialogType.NOT_MOST -> {
            ExoDialog(
                message = stringResource(
                    if (BuildConfig.CELEB) R.string.schedule_eval_most_actor
                    else R.string.schedule_eval_most
                ),
                onDismiss = { dialogType = ScheduleVoteDialogType.NONE }
            )
        }
        ScheduleVoteDialogType.LEVEL_LOW -> {
            ExoDialog(
                message = stringResource(R.string.schedule_eval_level, scheduleVoteLevel),
                onDismiss = { dialogType = ScheduleVoteDialogType.NONE }
            )
        }
        ScheduleVoteDialogType.VOTE_YES -> {
            ExoConfirmDialog(
                title = "",
                message = stringResource(R.string.schedule_yes),
                onConfirm = {
                    dialogType = ScheduleVoteDialogType.NONE
                    onVoteYes()
                },
                onDismiss = { dialogType = ScheduleVoteDialogType.NONE }
            )
        }
        ScheduleVoteDialogType.VOTE_NO -> {
            ExoConfirmDialog(
                title = "",
                message = stringResource(R.string.schedule_no),
                onConfirm = {
                    dialogType = ScheduleVoteDialogType.NONE
                    onVoteNo()
                },
                onDismiss = { dialogType = ScheduleVoteDialogType.NONE }
            )
        }
        ScheduleVoteDialogType.ALREADY_VOTED -> {
            ExoDialog(
                message = stringResource(R.string.schedule_eval_done),
                onDismiss = { dialogType = ScheduleVoteDialogType.NONE }
            )
        }
        ScheduleVoteDialogType.NONE -> { /* 다이얼로그 없음 */ }
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(33.dp)
    ) {
        // 댓글 버튼
        ScheduleActionButton(
            iconRes = R.drawable.icon_community_comment,
            text = schedule.numComments.toString(),
            isActive = false,
            borderColor = borderColor,
            activeColor = mainColor,
            inactiveColor = gray300,
            showEndBorder = true,
            onClick = onCommentClick,
            modifier = Modifier.weight(1f)
        )

        // 맞아요 버튼
        ScheduleActionButton(
            iconRes = if (schedule.isVotedYes) R.drawable.btn_schedule_yes_on else R.drawable.btn_schedule_yes_off,
            text = schedule.numYes.toString(),
            isActive = schedule.isVotedYes,
            borderColor = borderColor,
            activeColor = mainColor,
            inactiveColor = gray300,
            showEndBorder = true,
            onClick = {
                handleVoteClick(
                    isMost = isMost,
                    hasVoteLevel = hasVoteLevel,
                    isVoted = schedule.isVoted,
                    voteType = "Y",
                    onShowDialog = { dialogType = it }
                )
            },
            modifier = Modifier.weight(1f)
        )

        // 틀려요 버튼
        ScheduleActionButton(
            iconRes = if (schedule.isVotedNo) R.drawable.btn_schedule_no_on else R.drawable.btn_schedule_no_off,
            text = schedule.numNo.toString(),
            isActive = schedule.isVotedNo,
            borderColor = borderColor,
            activeColor = mainColor,
            inactiveColor = gray300,
            showEndBorder = false,
            onClick = {
                handleVoteClick(
                    isMost = isMost,
                    hasVoteLevel = hasVoteLevel,
                    isVoted = schedule.isVoted,
                    voteType = "N",
                    onShowDialog = { dialogType = it }
                )
            },
            modifier = Modifier.weight(1f)
        )
    }
}

/**
 * 투표 버튼 클릭 핸들러
 */
private fun handleVoteClick(
    isMost: Boolean,
    hasVoteLevel: Boolean,
    isVoted: Boolean,
    voteType: String,
    onShowDialog: (ScheduleVoteDialogType) -> Unit
) {
    when {
        !isMost -> onShowDialog(ScheduleVoteDialogType.NOT_MOST)
        !hasVoteLevel -> onShowDialog(ScheduleVoteDialogType.LEVEL_LOW)
        isVoted -> onShowDialog(ScheduleVoteDialogType.ALREADY_VOTED)
        voteType == "Y" -> onShowDialog(ScheduleVoteDialogType.VOTE_YES)
        voteType == "N" -> onShowDialog(ScheduleVoteDialogType.VOTE_NO)
    }
}

/**
 * 스케줄 액션 버튼 (댓글/맞아요/틀려요)
 */
@Composable
private fun ScheduleActionButton(
    iconRes: Int,
    text: String,
    isActive: Boolean,
    borderColor: Color,
    activeColor: Color,
    inactiveColor: Color,
    showEndBorder: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val color = if (isActive) activeColor else inactiveColor

    Row(
        modifier = modifier
            .fillMaxSize()
            .background(ColorPalette.background200)
            .drawBehind {
                drawLine(borderColor, Offset(0f, 0f), Offset(size.width, 0f), 1.dp.toPx())
                if (showEndBorder) {
                    drawLine(borderColor, Offset(size.width, 0f), Offset(size.width, size.height), 1.dp.toPx())
                }
            }
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            ) { onClick() },
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(14.dp)
        )
        Spacer(modifier = Modifier.width(5.dp))
        Text(
            text = text,
            style = ExoTypo.typo11.copy(color = color)
        )
    }
}
