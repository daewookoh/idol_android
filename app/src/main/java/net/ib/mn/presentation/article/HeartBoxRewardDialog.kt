package net.ib.mn.presentation.article

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import net.ib.mn.R
import net.ib.mn.ui.components.ExoBottomSheet
import net.ib.mn.ui.components.ExoBottomSheetType
import net.ib.mn.ui.theme.ColorPalette
import java.text.NumberFormat
import java.util.Locale

/**
 * 하트박스 보상 다이얼로그
 *
 * old 프로젝트의 RewardBottomSheetDialogFragment.setWidePhotoRewardDialog()와 동일한 로직
 * VoteCompleteBottomSheet와 동일한 레이아웃 구조 사용
 *
 * @param heartBoxReward 하트박스 보상 데이터
 * @param showVideoAdButton 비디오 광고 버튼 표시 여부 (현재 광고 시청 가능 상태)
 * @param onDismiss 다이얼로그 닫기
 * @param onWatchVideoAd 비디오 광고 보기 클릭 (heart=0이고 button=true일 때만 호출됨)
 */
@Composable
fun HeartBoxRewardDialog(
    heartBoxReward: HeartBoxReward,
    showVideoAdButton: Boolean = true,
    onDismiss: () -> Unit,
    onWatchVideoAd: () -> Unit = {}
) {
    val numberFormat = NumberFormat.getNumberInstance(Locale.getDefault())
    val heart = heartBoxReward.heart
    val button = heartBoxReward.button

    // 이미지 리소스 결정 (old 프로젝트와 동일)
    val imageRes = when {
        heart == 0 -> R.drawable.img_popup_heartbox_0
        heart < 20 -> R.drawable.img_popup_heartbox_7
        heart < 100 -> R.drawable.img_popup_heartbox_20
        heart < 1000 -> R.drawable.img_popup_heartbox_100
        else -> R.drawable.img_popup_heartbox_1000
    }

    // heart=0이고 button=true이면 비디오 광고 유도
    val showVideoAdOption = heart == 0 && button && showVideoAdButton

    ExoBottomSheet(
        onDismissRequest = onDismiss,
        type = ExoBottomSheetType.DESIGN
    ) {
        // old: cl_reward_root - paddingTop 10dp, transparent background
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 10.dp)
        ) {
            // 콘텐츠 영역 (배경 포함)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 109.dp)  // 이미지 중앙 (168dp/2) + 25dp 낮춤
                    .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
                    .background(ColorPalette.textWhiteBlack)
                    .navigationBarsPadding(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // 이미지 나머지 절반 + 18dp margin - 25dp
                Spacer(modifier = Modifier.height(59.dp + 18.dp))

                // old: tv_reward - 22sp bold
                val titleText = if (heart == 0) {
                    stringResource(R.string.lable_receive_no_heart)
                } else {
                    stringResource(R.string.reward_heartbox)
                }

                Text(
                    text = titleText,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = ColorPalette.textChat,
                    textAlign = TextAlign.Center
                )

                // old: cl_heart - 하트 개수 (heart > 0일 때만)
                if (heart > 0) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(top = 8.dp)
                    ) {
                        // old: tv_plus - 24sp, main color
                        Text(
                            text = "+",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = ColorPalette.main
                        )
                        // old: tv_heart - 24sp, main color
                        Text(
                            text = numberFormat.format(heart),
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = ColorPalette.main
                        )
                        // old: iv_heart - 21x17dp, marginStart 2dp
                        Spacer(modifier = Modifier.width(2.dp))
                        Image(
                            painter = painterResource(R.drawable.img_popup_heart),
                            contentDescription = null,
                            modifier = Modifier.size(21.dp, 17.dp)
                        )
                    }
                }

                // old: tv_reward_detail - 14sp, dimmed color
                // 1000개 이상일 때 또는 heart=0이고 비디오 광고 가능할 때
                val detailText = when {
                    heart >= 1000 -> stringResource(R.string.reward_heartbox1000_sub)
                    showVideoAdOption -> stringResource(R.string.label_see_video_for_heartbox)
                    else -> null
                }

                if (detailText != null) {
                    Text(
                        text = detailText,
                        fontSize = 14.sp,
                        color = ColorPalette.textDimmed,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 12.dp)
                    )
                }

                // old: tv_confirm - 55dp height, marginTop 26dp, marginHorizontal 24dp, radius 28dp
                val confirmText = if (showVideoAdOption) {
                    stringResource(R.string.receive_more_heart_after_video_ads, "30")
                } else {
                    stringResource(R.string.confirm)
                }

                Button(
                    onClick = {
                        if (showVideoAdOption) {
                            onWatchVideoAd()
                        }
                        onDismiss()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 24.dp, end = 24.dp, top = 26.dp)
                        .height(55.dp),
                    shape = RoundedCornerShape(28.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ColorPalette.main
                    )
                ) {
                    Text(
                        text = confirmText,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                // 취소 버튼 (비디오 광고 옵션일 때만 - old: tv_cancel)
                if (showVideoAdOption) {
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.padding(top = 8.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.quit),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = ColorPalette.mainLight
                        )
                    }
                }

                // old: bottom view - 30dp height
                Spacer(modifier = Modifier.height(30.dp))
            }

            // old: img_review - width=0dp(match_parent), height=168dp
            Image(
                painter = painterResource(id = imageRes),
                contentDescription = "Heart Box Reward",
                modifier = Modifier
                    .fillMaxWidth()
                    .height(168.dp)
                    .align(Alignment.TopCenter)
            )

            // old: btn_close - 62dp, padding 25dp (icon ~12dp), marginTop 10dp from bg top, marginEnd 10dp
            // bg top = 109dp, so absolute top = 109dp + 10dp = 119dp
            // heart=0이고 button=true일 때만 표시
            if (showVideoAdOption) {
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 119.dp, end = 10.dp)
                        .size(62.dp)
                ) {
                    Icon(
                        painter = painterResource(R.drawable.btn_popup_close),
                        contentDescription = "Close",
                        modifier = Modifier.padding(25.dp),
                        tint = Color.Unspecified
                    )
                }
            }
        }
    }
}
