package net.ib.mn.ui.components

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import net.ib.mn.R
import net.ib.mn.ui.theme.ColorPalette
import net.ib.mn.ui.theme.ExoTypo

/**
 * 투표 상태 코드
 * 서버에서 반환하는 vote 값
 */
object VoteStatusCode {
    const val CAN_VOTE = "N"           // 첫 투표 가능
    const val NEED_VIDEO_AD = "V"       // 광고 시청 후 투표 가능
    const val VOTED_TODAY = "Y"         // 오늘 투표 완료
}

/**
 * 투표 버튼 상태 데이터
 *
 * @param textResId 버튼 텍스트 리소스 ID
 * @param containerColor 버튼 배경색
 * @param contentColor 버튼 텍스트색
 * @param enabled 버튼 활성화 여부
 */
data class VoteButtonState(
    val textResId: Int,
    val containerColor: Color,
    val contentColor: Color,
    val enabled: Boolean
)

/**
 * voteStatus 값에 따른 버튼 상태를 반환
 *
 * @param voteStatus 투표 상태 코드 ("N": 투표가능, "V": 광고후 투표, "Y": 오늘 투표 완료)
 * @param isImagePick 이미지픽 여부 (true: 이미지픽용 스트링, false: 테마픽용 스트링)
 * @return VoteButtonState
 */
@Composable
fun rememberVoteButtonState(voteStatus: String, isImagePick: Boolean = false): VoteButtonState {
    return when (voteStatus) {
        VoteStatusCode.VOTED_TODAY -> VoteButtonState(
            textResId = R.string.themepick_today_voted,  // "오늘 투표 완료" (테마픽/이미지픽 공용)
            containerColor = ColorPalette.fixGray900,
            contentColor = ColorPalette.fixWhite,
            enabled = false
        )
        VoteStatusCode.NEED_VIDEO_AD -> VoteButtonState(
            textResId = if (isImagePick) R.string.imagepick_vote_with_ad else R.string.themepick_vote_again,
            containerColor = ColorPalette.mainLight,
            contentColor = ColorPalette.textWhiteBlack,
            enabled = true
        )
        else -> VoteButtonState(
            textResId = R.string.guide_vote_title,  // "투표하기" (테마픽/이미지픽 공용)
            containerColor = ColorPalette.mainLight,
            contentColor = ColorPalette.textWhiteBlack,
            enabled = true
        )
    }
}

/**
 * 픽 카드용 투표 버튼 (테마픽/이미지픽 공용)
 *
 * voteStatus에 따라 버튼 텍스트와 색상이 자동으로 결정됨:
 * - "N": 투표하기/참여하기 (mainLight)
 * - "V": 추가 투표하기/추가 투표하기 [AD] (mainLight)
 * - "Y": 오늘 투표 완료/오늘 참여 완료 (gray, 비활성화)
 *
 * @param voteStatus 투표 상태 코드
 * @param onClick 클릭 이벤트
 * @param isImagePick 이미지픽 여부 (true: 이미지픽용 스트링, false: 테마픽용 스트링)
 * @param modifier Modifier
 */
@Composable
fun ExoPickVoteButton(
    voteStatus: String,
    onClick: () -> Unit,
    isImagePick: Boolean = false,
    modifier: Modifier = Modifier
) {
    val buttonState = rememberVoteButtonState(voteStatus, isImagePick)

    ExoButton(
        onClick = onClick,
        modifier = modifier,
        enabled = buttonState.enabled,
        text = stringResource(buttonState.textResId),
        style = ExoTypo.typo14,
        height = 41.dp,
        shape = RoundedCornerShape(20.dp),
        containerColor = buttonState.containerColor,
        disabledContainerColor = buttonState.containerColor,
        contentColor = buttonState.contentColor,
        disabledContentColor = buttonState.contentColor
    )
}
